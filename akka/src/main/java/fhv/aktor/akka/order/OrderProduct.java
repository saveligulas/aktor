package fhv.aktor.akka.order;

public record OrderProduct(String name, int quantity) implements OrderCommand {
}
