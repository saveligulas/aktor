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
import fhv.aktor.akka.command.sensor.UpdateTemperature;
import fhv.aktor.akka.command.sensor.UpdateWeather;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public class MqttStreamService {
    public static void start(ActorSystem<?> system, ActorRef<UpdateWeather> weatherActor, ActorRef<UpdateTemperature> temperatureActor, boolean useInternalWeatherSimulation, boolean useInternalTemperatureSimulation) {
        Materializer materializer = Materializer.createMaterializer(system);

        MqttConnectionSettings connectionSettings = MqttConnectionSettings.create(
                "tcp://10.0.40.161:1883",
                "akka-home-automation-client",
                new MemoryPersistence()
        ).withCleanSession(true);

        system.log().info("Connecting to MQTT broker at tcp://10.0.40.161:1883");

        MqttSubscriptions subscriptions = MqttSubscriptions.create(
                        "weather/temperature", MqttQoS.atMostOnce())
                .addSubscription("weather/condition", MqttQoS.atMostOnce());

        int bufferSize = 8;

        Source<MqttMessage, CompletionStage<Done>> mqttSource = MqttSource.atMostOnce(
                connectionSettings,
                subscriptions,
                bufferSize
        );

        ObjectMapper objectMapper = new ObjectMapper();

        CompletionStage<Done> completionStage = mqttSource.runForeach(message -> {
            String topic = message.topic();
            String payload = message.payload().utf8String();

            system.log().info("Received message on topic: {}, payload: {}", topic, payload);

            try {
                JsonNode jsonNode = objectMapper.readTree(payload);

                if ("weather/temperature".equals(topic)) {
                    try {
                        String tempStr = jsonNode.get("temperature").asText();
                        double temp = Double.parseDouble(tempStr);
                        temperatureActor.tell(new UpdateTemperature(temp));
                        system.log().info("Sent temperature reading: {}", temp);
                    } catch (Exception e) {
                        system.log().warn("Invalid temperature payload: {}", payload, e);
                    }
                } else if ("weather/condition".equals(topic)) {
                    try {
                        String condition = jsonNode.get("condition").asText();
                        weatherActor.tell(new UpdateWeather(condition));
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

        completionStage.thenAccept(done -> system.log().info("MQTT stream completed"));

        completionStage.exceptionally(ex -> {
            system.log().error("MQTT stream error: {}", ex.getMessage(), ex);
            return null;
        });
    }
}


