package fhv.ops;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.ops.order.OrderCommand;
import fhv.ops.order.OrderProduct;

public class OrderProcessorActor extends AbstractBehavior<OrderCommand> {
    // Private constructor so it can only be instantiated by the create() method
    private OrderProcessorActor(ActorContext<OrderCommand> context) {
        super(context);

        getContext().getLog().info("Order Processing System created");
    }

    // Factory method tho create the actor and gives it access to the ActorContext
    public static Behavior<OrderCommand> create() {
        return Behaviors.setup(OrderProcessorActor::new);
    }

    @Override
    public Receive<OrderCommand> createReceive() {
        return newReceiveBuilder().onMessage(OrderCommand.class, this::onOrder).build();
    }

    private Behavior<OrderCommand> onOrder(OrderCommand command) {
        if (command instanceof OrderProduct(String name, int quantity)) {
            getContext().getLog().info("Processing Order: " + name + " " + quantity + "\n");
        }
        return this;
    }
}
