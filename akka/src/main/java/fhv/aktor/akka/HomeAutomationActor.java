package fhv.aktor.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.sensor.TemperatureSensorCommand;
import fhv.aktor.akka.command.sensor.WeatherSensorCommand;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.fridge.FridgeActor;
import fhv.aktor.akka.fridge.ItemRegistry;
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

        ActorRef<BlackboardCommand> blackboard = context.spawn(BlackboardActor.create(new BlackboardField.Registry()), "blackboard");
        ActorRef<TemperatureSensorCommand> tempSensor = context.spawn(TemperatureSensor.create(blackboard, systemSettings.internalTemperatureSimulation()),  "temperatureSensor");
        ActorRef<WeatherSensorCommand> weatherSensor = context.spawn(WeatherSensor.create(blackboard, systemSettings.internalWeatherSimulation()), "weatherSensor");
        context.spawn(BlindsActor.create(blackboard), "blinds");
        context.spawn(ACActor.create(blackboard), "ac");

        ItemRegistry itemRegistry = ItemRegistry.withDefaults();
        context.spawn(FridgeActor.create(null, itemRegistry), "fridge");

        if (!systemSettings.internalWeatherSimulation() && !systemSettings.internalTemperatureSimulation()) {
            return;
        }

        MqttStreamService.start(context.getSystem(), weatherSensor.narrow(), tempSensor.narrow(), systemSettings.internalWeatherSimulation(), systemSettings.internalTemperatureSimulation());
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().build();
    }
}
