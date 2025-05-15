package fhv.ops.order;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.ops.registry.ItemRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Actor that handles a single order and terminates after processing.
 */
public class OrderHandlerActor extends AbstractBehavior<OrderProduct> {
    private final ItemRegistry itemRegistry;

    private OrderHandlerActor(ActorContext<OrderProduct> context, ItemRegistry itemRegistry) {
        super(context);
        this.itemRegistry = itemRegistry;
        getContext().getLog().info("Order Handler created");
    }

    public static Behavior<OrderProduct> create(ItemRegistry itemRegistry) {
        return Behaviors.setup(context -> new OrderHandlerActor(context, itemRegistry));
    }

    @Override
    public Receive<OrderProduct> createReceive() {
        return newReceiveBuilder()
                .onMessage(OrderProduct.class, this::handleOrder)
                .build();
    }

    private Behavior<OrderProduct> handleOrder(OrderProduct command) {
        getContext().getLog().info("Handler processing Order: {}", command.productAndAmount());
        List<Product> products = new ArrayList<>();
        double totalPrice = 0;

        for (Map.Entry<String, Integer> item : command.productAndAmount().entrySet()) {
            String itemName = item.getKey();
            int amount = item.getValue();
            double unitPrice = itemRegistry.getPrice(itemName);
            double totalItemPrice = unitPrice * amount;

            products.add(new Product(itemName, amount, unitPrice, totalItemPrice));
            totalPrice += totalItemPrice;
        }

        long timestamp = Instant.now().getEpochSecond();
        Receipt receipt = new Receipt(products, totalPrice, timestamp);

        command.replyTo().tell(receipt);

        getContext().getLog().info("Order processed, handler terminating");
        return Behaviors.stopped();
    }
}
