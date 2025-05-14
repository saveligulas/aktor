package fhv.ops.order;

import java.util.List;

public record Receipt(List<Product> products, double totalPrice, long timestamp) {
}
