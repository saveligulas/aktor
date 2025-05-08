package fhv.aktor.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.subordinate.sensor.TemperatureSensor;

public class GuardianActor extends AbstractBehavior<Void> {
    public static Behavior<Void> create() {
        return Behaviors.setup(GuardianActor::new);
    }

    private GuardianActor(ActorContext<Void> context) {
        super(context);

        ActorRef<BlackboardCommand> blackboard = context.spawn(BlackboardActor.create(), "blackboard");

        context.spawn(TemperatureSensor.create(blackboard),  "temperatureSensor");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().build();
    }
}
