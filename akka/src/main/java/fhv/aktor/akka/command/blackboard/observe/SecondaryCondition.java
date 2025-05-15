package fhv.aktor.akka.command.blackboard.observe;

public interface SecondaryCondition<V> {
    boolean conditionMet(V value);
    String key();
    Class<V> getValueType();
}
