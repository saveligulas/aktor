package fhv.aktor.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.sensor.TemperatureSensorCommand;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.mqtt.MqttStreamService;
import fhv.aktor.akka.subordinate.device.ACActor;
import fhv.aktor.akka.subordinate.device.BlindsActor;
import fhv.aktor.akka.subordinate.sensor.TemperatureSensor;
import fhv.aktor.akka.subordinate.sensor.WeatherSensor;

public class HomeAutomationActor extends AbstractBehavior<Void> {
    public static Behavior<Void> create(SystemSettings systemSettings) {
        return Behaviors.setup(ctx -> new HomeAutomationActor(ctx, systemSettings));
    }

    private HomeAutomationActor(ActorContext<Void> context, SystemSettings systemSettings) {
        super(context);

        int cycleDuration = systemSettings.updateCycle();

        ActorRef<BlackboardCommand> blackboard = context.spawn(BlackboardActor.create(new BlackboardField.Registry()), "blackboard");

        ActorRef<TemperatureSensorCommand> tempSensor = context.spawn(TemperatureSensor.create(blackboard, systemSettings.internalTemperatureSimulation(), cycleDuration),  "temperatureSensor");
        context.spawn(WeatherSensor.create(blackboard, systemSettings.internalWeatherSimulation(), cycleDuration), "weatherSensor");

        context.spawn(BlindsActor.create(blackboard), "blindsActor");
        context.spawn(ACActor.create(blackboard), "ac");

        MqttStreamService.start(context.getSystem(), tempSensor.narrow(), systemSettings.internalTemperatureSimulation(), systemSettings.internalWeatherSimulation());
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().build();
    }
}
