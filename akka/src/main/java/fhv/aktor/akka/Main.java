package fhv.aktor.akka;

import akka.actor.typed.ActorSystem;

public class Main {
    public static void main(String[] args) {
        ActorSystem<Void> system = ActorSystem.create(HomeAutomationActor.create(), "akka-system");

        Runtime.getRuntime().addShutdownHook(new Thread(system::terminate));
    }
}
