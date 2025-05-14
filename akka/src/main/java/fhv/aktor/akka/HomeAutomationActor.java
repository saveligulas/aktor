package fhv.aktor.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.device.MediaStationCommand;
import fhv.aktor.akka.command.sensor.TemperatureSensorCommand;
import fhv.aktor.akka.command.sensor.WeatherSensorCommand;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.fridge.FridgeActor;
import fhv.aktor.akka.fridge.FridgeCommand;
import fhv.aktor.akka.fridge.ItemRegistry;
import fhv.aktor.akka.mqtt.MqttStreamService;
import fhv.aktor.akka.subordinate.device.ACActor;
import fhv.aktor.akka.subordinate.device.BlindsActor;
import fhv.aktor.akka.subordinate.device.MediaStationActor;
import fhv.aktor.akka.subordinate.sensor.TemperatureSensor;
import fhv.aktor.akka.subordinate.sensor.WeatherSensor;
import fhv.aktor.akka.ui.HomeAutomationCommandParser;
import fhv.aktor.akka.ui.CommandServer;
import fhv.aktor.akka.ui.UserCommand;
import fhv.aktor.akka.webhook.WebhookActor;
import fhv.aktor.akka.webhook.WebhookCommand;
import fhv.aktor.akka.webhook.UIServer;

import java.io.IOException;

public class HomeAutomationActor extends AbstractBehavior<Void> {

    public static Behavior<Void> create(SystemSettings systemSettings) {
        return Behaviors.setup(ctx -> new HomeAutomationActor(ctx, systemSettings));
    }

    private HomeAutomationActor(ActorContext<Void> context, SystemSettings systemSettings) throws IOException {
        super(context);

        ActorRef<BlackboardCommand> blackboard = context.spawn(BlackboardActor.create(new BlackboardField.Registry()), "blackboard");
        ActorRef<TemperatureSensorCommand> tempSensor = context.spawn(TemperatureSensor.create(blackboard, systemSettings.internalTemperatureSimulation()),  "temperatureSensor");
        ActorRef<WeatherSensorCommand> weatherSensor = context.spawn(WeatherSensor.create(blackboard, systemSettings.internalWeatherSimulation()), "weatherSensor");
        context.spawn(BlindsActor.create(blackboard), "blinds");
        context.spawn(ACActor.create(blackboard), "ac");

        ItemRegistry itemRegistry = ItemRegistry.withDefaults();
        ActorRef<FridgeCommand> fridge = context.spawn(FridgeActor.create(null, itemRegistry), "fridge");
        ActorRef<MediaStationCommand> mediaStation = context.spawn(MediaStationActor.create(blackboard), "mediaStation");

        if (!systemSettings.internalWeatherSimulation() || !systemSettings.internalTemperatureSimulation()) {
            MqttStreamService.start(context.getSystem(), weatherSensor.narrow(), tempSensor.narrow(), systemSettings.internalWeatherSimulation(), systemSettings.internalTemperatureSimulation());
        }

        // Start the terminal server
        CommandServer commandServer = new CommandServer();
        ActorRef<UserCommand> parser = context.spawn(HomeAutomationCommandParser.create(mediaStation, fridge), "homeAutomationCommandParser");
        commandServer.start(getContext().getSystem(), parser);
        
        // Start the webhook actor and HTTP server for web interface
        ActorRef<WebhookCommand> webhookActor = context.spawn(WebhookActor.create(blackboard), "webhookActor");
        context.getLog().info("Current blackboard state: {}");
        
        UIServer UIServer = new UIServer(webhookActor, getContext().getSystem(), parser);
        UIServer.start();
        
        context.getLog().info("Home automation system initialized with webhook server. Access the web interface at http://localhost:8081");
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().build();
    }
}
