package fhv.aktor.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.blackboard.BlackboardActor;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.device.MediaStationCommand;
import fhv.aktor.akka.command.sensor.TemperatureSensorCommand;
import fhv.aktor.akka.command.sensor.WeatherSensorCommand;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.fridge.FridgeActor;
import fhv.aktor.akka.fridge.FridgeCommand;
import fhv.aktor.akka.fridge.ItemRegistry;
import fhv.aktor.akka.fridge.SetOrderRef;
import fhv.aktor.akka.message.LoggingProvider;
import fhv.aktor.akka.message.MessageActor;
import fhv.aktor.akka.message.MessageCommand;
import fhv.aktor.akka.mqtt.DefaultWeatherConditionConverter;
import fhv.aktor.akka.mqtt.MqttStreamService;
import fhv.aktor.akka.order.GrpcClient;
import fhv.aktor.akka.order.OrderCommand;
import fhv.aktor.akka.subordinate.device.ACActor;
import fhv.aktor.akka.subordinate.device.BlindsActor;
import fhv.aktor.akka.subordinate.device.MediaStationActor;
import fhv.aktor.akka.subordinate.sensor.TemperatureSensor;
import fhv.aktor.akka.subordinate.sensor.WeatherSensor;
import fhv.aktor.akka.ui.CommandServer;
import fhv.aktor.akka.ui.CommandParser;
import fhv.aktor.akka.ui.UserCommand;
import fhv.aktor.akka.ui.UIServer;
import fhv.aktor.akka.webhook.WebhookActor;
import fhv.aktor.akka.webhook.WebhookCommand;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HomeAutomationActor extends AbstractBehavior<Void> {

    private HomeAutomationActor(ActorContext<Void> context, SystemSettings systemSettings) throws IOException, InterruptedException {
        super(context);

        ActorRef<BlackboardCommand> blackboard = context.spawn(BlackboardActor.create(new BlackboardField.Registry()), "blackboard");
        ActorRef<TemperatureSensorCommand> tempSensor = context.spawn(TemperatureSensor.create(blackboard, systemSettings.internalTemperatureSimulation()), "temperatureSensor");
        ActorRef<WeatherSensorCommand> weatherSensor = context.spawn(WeatherSensor.create(blackboard, systemSettings.internalWeatherSimulation(), new DefaultWeatherConditionConverter()), "weatherSensor");
        context.spawn(BlindsActor.create(blackboard), "blinds");
        context.spawn(ACActor.create(blackboard), "ac");

        ItemRegistry itemRegistry = ItemRegistry.withDefaults();
        ActorRef<FridgeCommand> fridge = context.spawn(FridgeActor.create(null, itemRegistry, 250), "fridge");
        ActorRef<MediaStationCommand> mediaStation = context.spawn(MediaStationActor.create(blackboard), "mediaStation");

        if (!systemSettings.internalWeatherSimulation() || !systemSettings.internalTemperatureSimulation()) {
            MqttStreamService.start(context.getSystem(), weatherSensor.narrow(), tempSensor.narrow(), systemSettings.internalWeatherSimulation(), systemSettings.internalTemperatureSimulation());
        }

        // Start the webhook actor and HTTP server for web interface
        ActorRef<WebhookCommand> webhookActor = context.spawn(WebhookActor.create(blackboard), "webhookActor");
        context.getLog().info("Current blackboard state: {}");

        CommandServer commandServer = new CommandServer();
        ActorRef<UserCommand> parser = context.spawn(CommandParser.create(mediaStation, fridge, webhookActor.narrow()), "homeAutomationCommandParser");
        commandServer.start(getContext().getSystem(), parser);

        ActorRef<MessageCommand> messageCommandRef = context.spawn(MessageActor.create(webhookActor.narrow()), "messageCommand");
        LoggingProvider.setMessageRef(messageCommandRef);

        String target = "127.0.0.1:50051";
        ActorRef<OrderCommand> orderRef;
        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create())
                .build();
        try {
            orderRef = context.spawn(GrpcClient.create(fridge, "localhost", 50051), "grpcClient");
            fridge.tell(new SetOrderRef(orderRef));
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }

        UIServer UIServer = new UIServer(webhookActor, getContext().getSystem(), parser);
        UIServer.start();

        context.getLog().info("Home automation system initialized with webhook server. Access the web interface at http://localhost:8081");
    }

    public static Behavior<Void> create(SystemSettings systemSettings) {
        return Behaviors.setup(ctx -> new HomeAutomationActor(ctx, systemSettings));
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().build();
    }
}
