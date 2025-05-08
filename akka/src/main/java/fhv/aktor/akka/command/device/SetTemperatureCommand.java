package fhv.aktor.akka.command.device;

import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public record SetTemperatureCommand(double temperature) implements ACCommand {
}
