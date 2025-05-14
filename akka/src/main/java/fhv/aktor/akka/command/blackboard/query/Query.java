package fhv.aktor.akka.command.blackboard.query;

import akka.actor.typed.ActorRef;

public interface Query<C extends QueryResponseCommand<V>, V> {
    ActorRef<? super C> replyTo();
    String key();
    C command();
}
