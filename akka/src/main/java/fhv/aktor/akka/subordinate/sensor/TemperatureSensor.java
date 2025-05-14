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

public class TemperatureSensor extends AbstractBlackboardSubordinateActor<TemperatureSensorCommand> {
    private final Random random;
    private final boolean useInternalSimulation;

    public static Behavior<TemperatureSensorCommand> create(ActorRef<BlackboardCommand> blackboardRef, boolean useInternalSimulation) {
        return Behaviors.setup(ctx -> {
            TemperatureSensor temperatureSensor = new TemperatureSensor(ctx, blackboardRef, useInternalSimulation);

            if (useInternalSimulation) {
                ctx.getSelf().tell(new UpdateTemperature(0));
            }

            return temperatureSensor;
        });
    }

    private TemperatureSensor(ActorContext<TemperatureSensorCommand> context, ActorRef<BlackboardCommand> blackboardRef, boolean useInternalSimulation) {
        super(context, blackboardRef);
        this.useInternalSimulation = useInternalSimulation;
        this.random = new Random();
    }

    @Override
    public Receive<TemperatureSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateTemperature.class, this::onTemperatureUpdate)
                .build();
    }

    private Behavior<TemperatureSensorCommand> onTemperatureUpdate(UpdateTemperature updateTemperature) {
        blackboardRef.tell(new PostValue(updateTemperature.temperature(), BlackboardField.TEMPERATURE.key()));

        if (useInternalSimulation) {
            getContext().scheduleOnce(
                    Duration.ofSeconds(5),
                    getContext().getSelf(),
                    new UpdateTemperature(random.nextDouble(0, 40))
            );
        }

        return Behaviors.same();
    }
}
