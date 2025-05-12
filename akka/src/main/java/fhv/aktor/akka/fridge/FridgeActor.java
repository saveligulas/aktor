package fhv.aktor.akka.fridge;

import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.FridgeCommand;

public class FridgeActor extends AbstractBehavior<FridgeCommand> {
    private final double MAX_WEIGHT;

    public FridgeActor(ActorContext<FridgeCommand> context, double maxWeight) {
        super(context);
        MAX_WEIGHT = maxWeight;
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return null;
    }
}
