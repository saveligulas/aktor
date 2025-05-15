package fhv.aktor.akka.fridge.command.value;

import java.util.Map;

public record Order(Map<String, Integer> productQuantities) {
}
