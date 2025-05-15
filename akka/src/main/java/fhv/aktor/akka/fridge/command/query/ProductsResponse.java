package fhv.aktor.akka.fridge.command.query;

import fhv.aktor.akka.ui.UserCommandResponse;

import java.util.Map;

public record ProductsResponse(Map<String, Integer> productQuantities) implements UserCommandResponse {
    @Override
    public String message() {
        return productQuantities.toString();
    }
}
