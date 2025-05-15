package fhv.ops.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.ops.order.OrderCommand;
import fhv.ops.order.OrderHandlerActor;
import fhv.ops.order.OrderProduct;
import fhv.ops.registry.ItemRegistry;

import java.util.UUID;

public class OrderProcessorActor extends AbstractBehavior<OrderCommand> {
    private final ItemRegistry itemRegistry;

    // Private constructor so it can only be instantiated by the create() method
    private OrderProcessorActor(ActorContext<OrderCommand> context, ItemRegistry itemRegistry) {
        super(context);
        this.itemRegistry = itemRegistry;

        getContext().getLog().info("Order Processing System created");
    }

    // Factory method tho create the actor and gives it access to the ActorContext
    public static Behavior<OrderCommand> create(ItemRegistry itemRegistry) {
        return Behaviors.setup(context -> new OrderProcessorActor(context, itemRegistry));
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder().onMessage(OrderProduct.class, this::onOrder).build();
    }

    private Behavior<OrderCommand> onOrder(OrderProduct command) {
        getContext().getLog().info("Received Order: {}, spawning handler", command.productAndAmount());

        // Create a unique name for the handler
        String handlerName = "order-handler-" + UUID.randomUUID();

        // Spawn the handler actor as a child
        ActorRef<OrderProduct> handler = getContext().spawn(
                OrderHandlerActor.create(itemRegistry),
                handlerName
        );

        // Forward the order to the handler
        handler.tell(command);

        return Behaviors.same();
    }
}
