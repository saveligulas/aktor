package fhv.aktor.akka.mqtt;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.stream.Materializer;
import akka.stream.alpakka.mqtt.MqttConnectionSettings;
import akka.stream.alpakka.mqtt.MqttMessage;
import akka.stream.alpakka.mqtt.MqttQoS;
import akka.stream.alpakka.mqtt.MqttSubscriptions;
import akka.stream.alpakka.mqtt.javadsl.MqttSource;
import akka.stream.javadsl.Source;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.CompletionStage;

public class MqttStreamService {
    public static void start(ActorSystem<?> system, ActorRef<EnvironmentEvent> environmentActor) {
        Materializer materializer = Materializer.createMaterializer(system);

        // Connection settings exactly like the working example
        MqttConnectionSettings connectionSettings = MqttConnectionSettings.create(
                "tcp://10.0.40.161:1883",
                "akka-home-automation-client",
                new MemoryPersistence()
        ).withCleanSession(true);

        // Log connection attempt
        system.log().info("Connecting to MQTT broker at tcp://10.0.40.161:1883");

        // Create subscriptions
        MqttSubscriptions subscriptions = MqttSubscriptions.create(
                        "weather/temperature", MqttQoS.atMostOnce())
                .addSubscription("weather/condition", MqttQoS.atMostOnce());

        // Buffer size
        int bufferSize = 8;

        // Create MQTT source
        Source<MqttMessage, CompletionStage<Done>> mqttSource = MqttSource.atMostOnce(
                connectionSettings,
                subscriptions,
                bufferSize
        );

        // Process messages
        CompletionStage<Done> completionStage = mqttSource.runForeach(message -> {
            String topic = message.topic();
            String payload = message.payload().utf8String();

            system.log().info("Received message on topic: {}, payload: {}", topic, payload);

            if ("environment/temperature".equals(topic)) {
                try {
                    double temp = Double.parseDouble(payload);
                    environmentActor.tell(new TemperatureReading(temp));
                    system.log().info("Sent temperature reading: {}", temp);
                } catch (NumberFormatException e) {
                    system.log().warn("Invalid temperature payload: {}", payload);
                }
            } else if ("environment/weather".equals(topic)) {
                environmentActor.tell(new WeatherReading(payload));
                system.log().info("Sent weather reading: {}", payload);
            } else {
                system.log().warn("Unknown topic: {}", topic);
            }
        }, materializer);

        // Handle completion
        completionStage.thenAccept(done -> system.log().info("MQTT stream completed"));

        // Handle errors
        completionStage.exceptionally(ex -> {
            system.log().error("MQTT stream error: {}", ex.getMessage(), ex);
            return null;
        });
    }
}


