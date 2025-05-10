package fhv.aktor.akka.receiver;

public interface Receiver<T> {
    void receive(T t);
}
