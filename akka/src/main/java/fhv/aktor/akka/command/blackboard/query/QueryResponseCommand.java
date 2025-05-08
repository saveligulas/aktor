package fhv.aktor.akka.command.blackboard.query;

public interface QueryResponseCommand<T> {
    void fromValue(T value);
    Class<T> getValueType();
}
