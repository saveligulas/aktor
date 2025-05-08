package fhv.aktor.akka.command.blackboard.receive;

import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public record HourAdvanced(int hour) implements BlackboardCommand {
}
