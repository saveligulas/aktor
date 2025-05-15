package fhv.aktor.akka.ui;

public record CommandResponse(String response, String status) implements UserCommand {
}
