package fhv.aktor.akka.command.blackboard.observe;

import akka.actor.typed.ActorRef;
import fhv.aktor.akka.commons.CommandBuilder;

import java.util.List;
import java.util.Map;

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

    default boolean hasAdditionalConditionFields() {
        return !secondaryConditions().isEmpty();
    }

    default List<SecondaryCondition<?>> secondaryConditions() {
        return List.of();
    }

    default boolean secondaryConditionsMet(Map<String, Object> map) { // pass UnmodifiableMap here
        boolean conditionsMet = true;
        for (SecondaryCondition<?> condition : secondaryConditions()) {
            Class<?> type = condition.getValueType();
            Object value = map.get(condition.key());
            if (type.isInstance(value)) {
                conditionsMet = conditionsMet && applyCondition(condition, value, map);
                if (!conditionsMet) {
                    return false;
                }
            }
        }
        return conditionsMet;
    }

    @SuppressWarnings("unchecked")
    private <V> boolean applyCondition(SecondaryCondition<V> condition, Object currentValue, Map<String, Object> map) {
        V typedCurrentValue = (V) currentValue;
        return condition.conditionMet(typedCurrentValue);
    }

    COMMAND_BUILDER getCommandBuilderInstance();
    ActorRef<ACTOR_COMMAND> getRef();
}
