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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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

        // Create JSON object mapper for parsing
        ObjectMapper objectMapper = new ObjectMapper();

        // Process messages
        CompletionStage<Done> completionStage = mqttSource.runForeach(message -> {
            String topic = message.topic();
            String payload = message.payload().utf8String();

            system.log().info("Received message on topic: {}, payload: {}", topic, payload);

            try {
                JsonNode jsonNode = objectMapper.readTree(payload);

                if ("weather/temperature".equals(topic)) {
                    try {
                        // Extract temperature value from JSON
                        String tempStr = jsonNode.get("temperature").asText();
                        double temp = Double.parseDouble(tempStr);
                        environmentActor.tell(new EnvironmentEvent.TemperatureReading(temp));
                        system.log().info("Sent temperature reading: {}", temp);
                    } catch (Exception e) {
                        system.log().warn("Invalid temperature payload: {}", payload, e);
                    }
                } else if ("weather/condition".equals(topic)) {
                    try {
                        // Extract condition value from JSON
                        String condition = jsonNode.get("condition").asText();
                        environmentActor.tell(new EnvironmentEvent.WeatherReading(condition));
                        system.log().info("Sent weather condition: {}", condition);
                    } catch (Exception e) {
                        system.log().warn("Invalid condition payload: {}", payload, e);
                    }
                } else {
                    system.log().warn("Unknown topic: {}", topic);
                }
            } catch (IOException e) {
                system.log().error("Failed to parse JSON payload: {}", payload, e);
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


