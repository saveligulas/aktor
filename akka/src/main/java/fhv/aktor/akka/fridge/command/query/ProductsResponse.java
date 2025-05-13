package fhv.aktor.akka.fridge.command.query;

import fhv.aktor.akka.ui.UserCommand;

import java.util.List;

public record ProductsResponse(List<String> products) implements UserCommand {
}
