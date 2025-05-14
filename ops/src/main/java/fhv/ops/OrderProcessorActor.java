package fhv.ops;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.ops.item.ItemRegistry;
import fhv.ops.order.OrderCommand;
import fhv.ops.order.OrderProduct;
import fhv.ops.order.Product;
import fhv.ops.order.Receipt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderProcessorActor extends AbstractBehavior<OrderCommand> {
    private final ItemRegistry itemRegistry;

    // Private constructor so it can only be instantiated by the create() method
    private OrderProcessorActor(ActorContext<OrderCommand> context, ItemRegistry itemRegistry) {
        super(context);
        this.itemRegistry = itemRegistry;

        getContext().getLog().info("Order Processing System created");
    }

    // Factory method tho create the actor and gives it access to the ActorContext
    public static Behavior<OrderCommand> qcreate(ItemRegistry itemRegistry) {
        return Behaviors.setup(context -> new OrderProcessorActor(context, itemRegistry));
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder().onMessage(OrderProduct.class, this::onOrder).build();
    }

    private Behavior<OrderCommand> onOrder(OrderProduct command) {
        getContext().getLog().info("Processing Order: {}\n", command.productAndAmount());
        List<Product> products = new ArrayList<>();
        double totalPrice = 0;

        for (Map.Entry<String, Integer> item : command.productAndAmount().entrySet()) {
            String itemName = item.getKey();
            int amount = item.getValue();
            double unitPrice = itemRegistry.getPrice(item.getKey());
            double totalItemPrice = unitPrice * amount;

            products.add(new Product(itemName, amount, unitPrice, totalItemPrice));
            totalPrice += totalItemPrice;
        }

        long timestamp = Instant.now().getEpochSecond();
        Receipt receipt = new Receipt(products, totalPrice, timestamp);

        return Behaviors.same();
    }
}
