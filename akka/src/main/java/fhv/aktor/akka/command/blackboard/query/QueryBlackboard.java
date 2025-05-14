package fhv.aktor.akka.command.blackboard.query;

import akka.actor.typed.ActorRef;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public class QueryBlackboard<C extends QueryResponseCommand<V>, V> implements Query<C, V>, BlackboardCommand {
    private final String key;
    private final C command;
    private final ActorRef<C> replyTo;

    public QueryBlackboard(String key, C command, ActorRef<C> replyTo) {
        this.key = key;
        this.command = command;
        this.replyTo = replyTo;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public C command() {
        return command;
    }

    @Override
    public ActorRef<C> replyTo() {
        return replyTo;
    }
}
