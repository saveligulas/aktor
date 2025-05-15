package fhv.aktor.akka.order;

import fhv.aktor.akka.fridge.FridgeCommand;

public record OrderProduct(String name, int quantity) implements OrderCommand, FridgeCommand {
}
