package fhv.ops.grpc;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Scheduler;
import akka.actor.typed.javadsl.AskPattern;
import fhv.ops.actor.OrderProcessingSystem;
import fhv.ops.order.OrderCommand;
import fhv.ops.order.OrderProduct;
import fhv.ops.order.Receipt;
import fhv.ops.proto.OrderProcessorGrpc;
import fhv.ops.proto.ProductOrderRequest;
import io.grpc.stub.StreamObserver;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class OrderProcessorImpl extends OrderProcessorGrpc.OrderProcessorImplBase {

    private final ActorRef<OrderCommand> orderProcessorActor;
    private final Scheduler scheduler;

    public OrderProcessorImpl(ActorRef<OrderCommand> orderProcessorActor, Scheduler scheduler) {
        this.orderProcessorActor = orderProcessorActor;
        this.scheduler = scheduler;
    }

    public OrderProcessorImpl() {
        // Create the actor system and order processor actor
        akka.actor.typed.ActorSystem<OrderCommand> system = OrderProcessingSystem.createOrderProcessorSystem();
        this.orderProcessorActor = OrderProcessingSystem.getOrCreateSystem(); // TODO: Get actor

        // Get the scheduler from the actor system
        this.scheduler = system.scheduler();
    }

    public void order(ProductOrderRequest request, StreamObserver<fhv.ops.proto.Receipt> responseObserver) {
        Duration timeout = Duration.ofSeconds(5);

        // Send the order to the actor using the ask pattern
        CompletionStage<Receipt> result = AskPattern.ask(
                orderProcessorActor,
                replyTo -> new OrderProduct(request.getProductAndAmountMap(), replyTo),
                timeout,
                scheduler
        );

        result.whenComplete((receipt, ex) -> {
            if (ex != null) {
                responseObserver.onError(ex);  // Handle errors if any
            } else {
                // Send the response to the client
                responseObserver.onNext(fhv.ops.proto.Receipt.newBuilder()
                        .addAllItems(receipt.products().stream()
                                .map(product -> fhv.ops.proto.Product.newBuilder()
                                        .setAmount(product.amount())
                                        .setName(product.name())
                                        .setTotalPrice(product.totalPrice())
                                        .setUnitPrice(product.unitPrice())
                                        .build())
                                .toList())
                        .setTimestamp(receipt.timestamp())
                        .setTotalPrice(receipt.totalPrice())
                        .build());
                responseObserver.onCompleted();
            }
        });
    }
}
