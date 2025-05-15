package fhv.aktor.akka.fridge.command.query;

import fhv.aktor.akka.fridge.command.value.Order;

import java.util.List;

public record OrderHistoryResponse(List<Order> orders) {
}
