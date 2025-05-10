package fhv.aktor.akka.command.blackboard;

public interface BlackboardRegistry {
    boolean isValidKeyAndType(String key, Class<?> clazz);
}
