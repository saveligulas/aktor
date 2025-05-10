package fhv.aktor.akka.command.blackboard.observe;

import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.commons.CommandBuilder;

public interface ObserveField<ACTOR_COMMAND, VALUE, COMMAND extends ACTOR_COMMAND, COMMAND_BUILDER extends CommandBuilder<VALUE, COMMAND>> extends BlackboardCommand {
    String key();
    Condition<ACTOR_COMMAND, VALUE, COMMAND, COMMAND_BUILDER> getCondition();
    Class<VALUE> getObservedValueClass();
}
