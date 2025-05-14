package fhv.ops.order;

public record OrderProduct(String name, int quantity) implements OrderCommand {
}
