package fhv.aktor.akka.fridge;

import akka.actor.typed.ActorRef;
import fhv.aktor.akka.order.OrderCommand;

public record SetOrderRef(ActorRef<OrderCommand> orderRef) implements FridgeCommand {
}
