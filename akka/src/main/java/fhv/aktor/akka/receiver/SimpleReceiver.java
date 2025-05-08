package fhv.aktor.akka.receiver;

public interface SimpleReceiver<T> {
    void receive(T t);
}
