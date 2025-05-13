package fhv.ops;

import akka.actor.typed.ActorSystem;

public class OrderProcessingSystem {
    public static void main(String[] args) {
        ActorSystem<Void> system = ActorSystem.create(OrderProcessingActor.create(), "ops-system");

        Runtime.getRuntime().addShutdownHook(new Thread(system::terminate));
    }
}
