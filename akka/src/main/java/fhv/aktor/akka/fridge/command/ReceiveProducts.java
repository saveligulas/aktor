package fhv.aktor.akka.fridge.command;

import fhv.aktor.akka.fridge.FridgeCommand;

import java.util.Map;

public record ReceiveProducts(Map<String, Integer> productQuantities) implements FridgeCommand {
}
