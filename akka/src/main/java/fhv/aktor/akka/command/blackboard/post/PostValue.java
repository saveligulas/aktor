package fhv.aktor.akka.command.blackboard.post;

import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public record PostValue(Object value, String key) implements BlackboardCommand {
}
