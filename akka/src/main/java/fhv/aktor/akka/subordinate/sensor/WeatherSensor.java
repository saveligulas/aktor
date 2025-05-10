package fhv.aktor.akka.subordinate.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.AbstractBlackboardSubordinateActor;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.sensor.UpdateWeather;
import fhv.aktor.akka.command.sensor.WeatherSensorCommand;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.commons.WeatherCondition;

import java.time.Duration;
import java.util.Random;

public class WeatherSensor extends AbstractBlackboardSubordinateActor<WeatherSensorCommand> {

    private final Random random;

    public static Behavior<WeatherSensorCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(context -> {
            WeatherSensor sensor = new WeatherSensor(context, blackboardRef);
            // Start the update loop immediately
            context.getSelf().tell(new UpdateWeather());
            return sensor;
        });
    }

    private WeatherSensor(ActorContext<WeatherSensorCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);
        this.random = new Random();
    }

    @Override
    public Receive<WeatherSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateWeather.class, this::onUpdateWeather)
                .build();
    }

    private Behavior<WeatherSensorCommand> onUpdateWeather(UpdateWeather command) {
        // Generate random weather condition
        WeatherCondition randomWeather = getRandomWeatherCondition();
        getContext().getLog().info("Weather changed to: {}", randomWeather);

        // Post to blackboard
        blackboardRef.tell(new PostValue(getRandomWeatherCondition(), BlackboardField.WEATHER_CONDITION.key()));

        // Schedule next update in 10 seconds
        getContext().scheduleOnce(
                Duration.ofSeconds(10),
                getContext().getSelf(),
                new UpdateWeather()
        );

        return Behaviors.same();
    }

    private WeatherCondition getRandomWeatherCondition() {
        WeatherCondition[] conditions = WeatherCondition.values();
        return conditions[random.nextInt(conditions.length)];
    }
}
