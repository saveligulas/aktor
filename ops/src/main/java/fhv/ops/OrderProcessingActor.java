package fhv.ops;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class OrderProcessingActor extends AbstractBehavior<Void> {
    public OrderProcessingActor(ActorContext<Void> context) {
        super(context);
    }

    public static Behavior<Void> create() {
        return Behaviors.setup(OrderProcessingActor::new);
    }

    @Override
    public Receive<Void> createReceive() {
        return null;
    }
}
