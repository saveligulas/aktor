package fhv.aktor.akka.mqtt;

import fhv.aktor.akka.command.sensor.TemperatureSensorCommand;

public interface EnvironmentEvent extends TemperatureSensorCommand {
    record TemperatureReading(double value) implements EnvironmentEvent, TemperatureSensorCommand {
    }

    record WeatherReading(String condition) implements EnvironmentEvent {
    }
}

