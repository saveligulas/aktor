package fhv.aktor.akka.order;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.fridge.FridgeCommand;
import fhv.aktor.akka.fridge.command.OrderProducts;
import fhv.aktor.akka.fridge.command.ReceiveProducts;
import fhv.aktor.akka.message.LoggingProvider;
import fhv.ops.proto.OrderProcessorGrpc;
import fhv.ops.proto.Product;
import fhv.ops.proto.ProductOrderRequest;
import fhv.ops.proto.Receipt;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GrpcClient extends AbstractBehavior<OrderCommand> {
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 1000;
    private final ActorRef<FridgeCommand> fridgeRef;
    private final OrderProcessorGrpc.OrderProcessorBlockingStub blockingStub;
    private final ManagedChannel channel;
    private final LoggingProvider.EnhancedLogger logger;

    public GrpcClient(ActorContext<OrderCommand> context, ActorRef<FridgeCommand> fridgeRef, String host, int port) {
        super(context);
        this.fridgeRef = fridgeRef;
        this.logger = LoggingProvider.withContext(getContext().getLog());

        // Create a managed channel with proper configuration
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // Remove this in production and use TLS
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .enableRetry()
                .maxRetryAttempts(3)
                .build();

        this.blockingStub = OrderProcessorGrpc.newBlockingStub(channel)
                .withDeadlineAfter(15, TimeUnit.SECONDS);

        // Register a shutdown hook to ensure clean channel shutdown
        context.getSystem().getWhenTerminated().thenRun(() -> {
            try {
                if (!channel.isShutdown()) {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                getContext().getLog().error("Error shutting down gRPC channel", e);
            }
        });
    }

    public static Behavior<OrderCommand> create(ActorRef<FridgeCommand> fridgeRef, String host, int port) {
        return Behaviors.setup(context ->
                new GrpcClient(context, fridgeRef, host, port));
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderProducts.class, this::onOrderProduct)
                .build();
    }

    private Behavior<OrderCommand> onOrderProduct(OrderProducts orderProducts) {
        getContext().getLog().info("Processing order: {}", orderProducts.productQuantities());

        ProductOrderRequest request = ProductOrderRequest.newBuilder()
                .putAllProductAndAmount(orderProducts.productQuantities())
                .build();

        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                // Create a new stub for each retry to avoid using expired deadlines
                Receipt response = OrderProcessorGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(10, TimeUnit.SECONDS)
                        .order(request);

                Map<String, Integer> productQuantities = new HashMap<>();
                for (Product product : response.getItemsList()) {
                    productQuantities.put(product.getName(), product.getAmount());
                }

                fridgeRef.tell(new ReceiveProducts(productQuantities));
                logger.info(beautifyReceipt(response));

                getContext().getLog().info("Order processed successfully");
                return Behaviors.same();

            } catch (StatusRuntimeException e) {
                attempts++;
                getContext().getLog().error("gRPC error (attempt {}/{}): {}",
                        attempts, MAX_RETRY_ATTEMPTS, e.getMessage());

                if (attempts < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.error("Failed to process order: " + e.getMessage());
                }
            }
        }

        return Behaviors.same();
    }


    private String beautifyReceipt(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ORDER RECEIPT ===\n");

        double total = 0.0;
        for (Product product : receipt.getItemsList()) {
            double itemTotal = product.getAmount() * product.getTotalPrice();
            total += itemTotal;
            sb.append(String.format("%s: %d x %.2f = %.2f\n",
                    product.getName(), product.getAmount(), product.getTotalPrice(), itemTotal));
        }

        sb.append("-------------------\n");
        sb.append(String.format("Total: %.2f\n", total));
        return sb.toString();
    }
}
