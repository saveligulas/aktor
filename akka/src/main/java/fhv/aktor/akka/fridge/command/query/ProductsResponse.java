package fhv.aktor.akka.fridge.command.query;

import java.util.List;

public record ProductsResponse(List<String> products) {
}
