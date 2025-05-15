package fhv.aktor.akka.command.blackboard.query;

public interface QueryResponseCommand<T> {
    void build(String key, T value);
    Class<T> getValueType();
}
