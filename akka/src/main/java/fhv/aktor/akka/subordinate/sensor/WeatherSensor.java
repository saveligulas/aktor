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
import fhv.aktor.akka.commons.WeatherConditionAdapter;

import java.time.Duration;
import java.util.Random;

public class WeatherSensor extends AbstractBlackboardSubordinateActor<WeatherSensorCommand> {
    private final Random random;
    private final boolean useInternalSimulation;
    private WeatherConditionAdapter weatherConditionAdapter;

    public static Behavior<WeatherSensorCommand> create(ActorRef<BlackboardCommand> blackboardRef, boolean useInternalSimulation) {
        return Behaviors.setup(context -> {
            WeatherSensor sensor = new WeatherSensor(context, blackboardRef, useInternalSimulation);

            if (useInternalSimulation) {
                context.getSelf().tell(new UpdateWeather(WeatherCondition.CLEAR.name()));
            }

            return sensor;
        });
    }

    private WeatherSensor(ActorContext<WeatherSensorCommand> context, ActorRef<BlackboardCommand> blackboardRef, boolean useInternalSimulation) {
        super(context, blackboardRef);
        this.useInternalSimulation = useInternalSimulation;
        this.random = new Random();
    }

    @Override
    public Receive<WeatherSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateWeather.class, this::onUpdateWeather)
                .build();
    }

    private Behavior<WeatherSensorCommand> onUpdateWeather(UpdateWeather command) {
        blackboardRef.tell(new PostValue(getRandomWeatherCondition(), BlackboardField.WEATHER_CONDITION.key()));

        if (useInternalSimulation) {
            getContext().scheduleOnce(
                    Duration.ofSeconds(10),
                    getContext().getSelf(),
                    new UpdateWeather(getRandomWeatherCondition().name())
            );
        }

        return Behaviors.same();
    }

    private WeatherCondition parseWeatherCondition(String weatherCondition) {
        if (weatherConditionAdapter != null) {
            WeatherCondition convertedWeatherCondition = weatherConditionAdapter.convert(weatherCondition);
            if (convertedWeatherCondition != null) {
                return convertedWeatherCondition;
            }
        }

        try {
            return WeatherCondition.valueOf(weatherCondition);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unknown weather condition: " + weatherCondition + " - was provided");
        }
    }

    private WeatherCondition getRandomWeatherCondition() {
        WeatherCondition[] conditions = WeatherCondition.values();
        return conditions[random.nextInt(conditions.length)];
    }
}
