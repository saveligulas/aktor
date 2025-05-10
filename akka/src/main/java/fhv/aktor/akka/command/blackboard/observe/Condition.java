package fhv.aktor.akka.command.blackboard.observe;

import akka.actor.typed.ActorRef;
import fhv.aktor.akka.commons.CommandBuilder;

public interface Condition<ACTOR_COMMAND, VALUE, COMMAND extends ACTOR_COMMAND, COMMAND_BUILDER extends CommandBuilder<VALUE, COMMAND>> {
    // TODO: allow condition to be reliant on multiple other fields (allowing to fetch and check them)

    default void doCheck(VALUE value, VALUE previousValue) {
        ActorRef<ACTOR_COMMAND> ref = getRef();
        if (conditionMet(value, previousValue)) {
            ref.tell(buildReturnCommand(value));
        }
    }

    default boolean conditionMet(VALUE value, VALUE previousValue) {
        return true;
    }
    default COMMAND buildReturnCommand(VALUE value) {
        return getCommandBuilderInstance().buildCommand(value);
    }

    COMMAND_BUILDER getCommandBuilderInstance();
    ActorRef<ACTOR_COMMAND> getRef();
}
