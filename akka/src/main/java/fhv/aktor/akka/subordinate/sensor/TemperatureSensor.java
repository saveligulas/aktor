package fhv.aktor.akka.subordinate.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.AbstractBlackboardSubordinateActor;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.sensor.TemperatureSensorCommand;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.receiver.ReceiveTemperatureChange;

public class TemperatureSensor extends AbstractBlackboardSubordinateActor<TemperatureSensorCommand> implements ReceiveTemperatureChange {
    private double temperature;

    public static Behavior<TemperatureSensorCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(ctx -> new TemperatureSensor(ctx, blackboardRef));
    }

    private TemperatureSensor(ActorContext<TemperatureSensorCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);
    }

    @Override
    public Receive<TemperatureSensorCommand> createReceive() {
        return newReceiveBuilder().build();
    }

    @Override
    public void receive(Double temperature) {
        this.temperature = temperature;
        this.blackboardRef.tell(new PostValue(temperature, BlackboardField.TEMPERATURE.name()));
    }
}
