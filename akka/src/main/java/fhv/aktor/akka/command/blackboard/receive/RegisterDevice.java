package fhv.aktor.akka.command.blackboard.receive;

import akka.actor.ActorRef;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public record RegisterDevice(String id, ActorRef ref)
        implements BlackboardCommand {
}
