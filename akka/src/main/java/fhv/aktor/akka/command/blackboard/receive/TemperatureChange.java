package fhv.aktor.akka.command.blackboard.receive;

import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public record TemperatureChange(double temperature) implements BlackboardCommand {
}
