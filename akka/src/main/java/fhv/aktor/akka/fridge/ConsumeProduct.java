package fhv.aktor.akka.fridge;

public record ConsumeProduct(String itemName, int quantity) implements FridgeCommand {
}
