package fhv.aktor.akka.fridge.command.query;

import fhv.aktor.akka.fridge.command.value.Order;
import fhv.aktor.akka.ui.UserCommand;

import java.util.List;

public record OrderHistoryResponse(List<Order> orders) implements UserCommand {
}
