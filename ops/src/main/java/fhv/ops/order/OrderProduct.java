package fhv.ops.order;

import java.util.Map;

public record OrderProduct(Map<String, Integer> productAndAmount) implements OrderCommand {
}
