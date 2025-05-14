package fhv.aktor.akka.command.sensor;

public record UpdateTemperature(double temperature) implements TemperatureSensorCommand {
}
