package fhv.aktor.akka.command.blackboard.query;

import akka.actor.typed.ActorRef;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public record QueryBlackboard<C extends QueryResponseCommand<V>, V>(
        String key,
        C command,
        ActorRef<C> replyTo
) implements Query<C, V>, BlackboardCommand {
}

