package fhv.aktor.akka;

import akka.actor.typed.ActorSystem;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        SystemSettings systemSettings = new SystemSettings(true);

        ActorSystem<Void> system = ActorSystem.create(HomeAutomationActor.create(systemSettings), "akka-system");

        Runtime.getRuntime().addShutdownHook(new Thread(system::terminate));
    }
}
