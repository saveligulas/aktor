package fhv.aktor.akka.fridge.command;

import fhv.aktor.akka.fridge.FridgeCommand;
import fhv.aktor.akka.order.OrderCommand;

import java.util.Map;

public record OrderProduct(Map<String, Integer> productQuantities) implements FridgeCommand, OrderCommand {
}
