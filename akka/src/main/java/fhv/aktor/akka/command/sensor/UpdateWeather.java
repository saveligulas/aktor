package fhv.aktor.akka.command.sensor;

public record UpdateWeather(String weatherCondition) implements WeatherSensorCommand {
}
