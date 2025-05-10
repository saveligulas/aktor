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
import fhv.aktor.akka.command.sensor.UpdateTemperature;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.receiver.ReceiveTemperatureChange;

import java.time.Duration;
import java.util.Random;

public class TemperatureSensor extends AbstractBlackboardSubordinateActor<TemperatureSensorCommand> implements ReceiveTemperatureChange {
    private double temperature; //TODO: REMOVE
    private final Random random;

    public static Behavior<TemperatureSensorCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(ctx -> {
            TemperatureSensor temperatureSensor = new TemperatureSensor(ctx, blackboardRef);
            ctx.getSelf().tell(new UpdateTemperature());
            return temperatureSensor;
        });
    }

    private TemperatureSensor(ActorContext<TemperatureSensorCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);

        this.random = new Random();
    }

    @Override
    public Receive<TemperatureSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateTemperature.class, this::onTemperatureUpdate)
                .build();
    }

    private Behavior<TemperatureSensorCommand> onTemperatureUpdate(UpdateTemperature updateTemperature) {
        Double newTemperature = random.nextDouble(10, 30);
        // Post to blackboard
        blackboardRef.tell(new PostValue(newTemperature, BlackboardField.TEMPERATURE.key()));

        // Schedule next update in 10 seconds
        getContext().scheduleOnce(
                Duration.ofSeconds(10),
                getContext().getSelf(),
                new UpdateTemperature()
        );

        return Behaviors.same();
    }

    @Override
    public void receive(Double temperature) {
        this.temperature = temperature;
        this.blackboardRef.tell(new PostValue(temperature, BlackboardField.TEMPERATURE.name()));
    }
}
