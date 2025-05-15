package fhv.aktor.akka.fridge.command.query;

import akka.actor.typed.ActorRef;
import fhv.aktor.akka.fridge.FridgeCommand;

public record QueryProducts(ActorRef<ProductsResponse> replyTo) implements FridgeCommand {
}
