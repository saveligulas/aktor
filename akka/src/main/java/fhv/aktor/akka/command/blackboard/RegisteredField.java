package fhv.aktor.akka.command.blackboard;

public interface RegisteredField {
    String key();
    Class<?> getFieldClass();
}
