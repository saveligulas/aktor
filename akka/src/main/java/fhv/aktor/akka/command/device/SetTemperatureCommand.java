package fhv.aktor.akka.command.device;

public record SetTemperatureCommand(double temperature) implements ACCommand {
}
