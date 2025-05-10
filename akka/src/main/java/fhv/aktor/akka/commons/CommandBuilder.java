package fhv.aktor.akka.commons;

public interface CommandBuilder<T, C> {
    C buildCommand(T t);
}
