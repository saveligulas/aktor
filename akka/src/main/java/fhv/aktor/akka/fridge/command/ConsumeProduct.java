package fhv.aktor.akka.fridge.command;

import fhv.aktor.akka.fridge.FridgeCommand;

public record ConsumeProduct(String itemName, int quantity) implements FridgeCommand {
}
