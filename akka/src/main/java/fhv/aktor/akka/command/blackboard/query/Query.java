package fhv.aktor.akka.command.blackboard.query;

import akka.actor.typed.ActorRef;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public interface Query<C extends QueryResponseCommand<V>, V> {
    ActorRef<C> replyTo();
    String key();
    C command();
}
