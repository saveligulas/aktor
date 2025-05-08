package fhv.aktor.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;

public abstract class AbstractBlackboardSubordinateActor<T> extends AbstractBehavior<T> {
    protected final ActorRef<BlackboardCommand> blackboardRef;

    protected AbstractBlackboardSubordinateActor(ActorContext<T> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context);
        this.blackboardRef = blackboardRef;
    }
}
