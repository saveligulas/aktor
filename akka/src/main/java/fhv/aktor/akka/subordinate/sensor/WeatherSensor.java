package fhv.aktor.akka.subordinate.sensor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.blackboard.AbstractBlackboardSubordinateActor;
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
    private final WeatherConditionAdapter weatherConditionAdapter;

    private WeatherSensor(ActorContext<WeatherSensorCommand> context, ActorRef<BlackboardCommand> blackboardRef, boolean useInternalSimulation, WeatherConditionAdapter weatherConditionAdapter) {
        super(context, blackboardRef);
        this.useInternalSimulation = useInternalSimulation;
        this.weatherConditionAdapter = weatherConditionAdapter;
        this.random = new Random();
    }

    public static Behavior<WeatherSensorCommand> create(ActorRef<BlackboardCommand> blackboardRef, boolean useInternalSimulation, WeatherConditionAdapter weatherConditionAdapter) {
        return Behaviors.setup(context -> {
            WeatherSensor sensor = new WeatherSensor(context, blackboardRef, useInternalSimulation, weatherConditionAdapter);

            if (useInternalSimulation) {
                context.getSelf().tell(new UpdateWeather(WeatherCondition.CLEAR.name()));
            }

            return sensor;
        });
    }

    @Override
    public Receive<WeatherSensorCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateWeather.class, this::onUpdateWeather)
                .build();
    }

    private Behavior<WeatherSensorCommand> onUpdateWeather(UpdateWeather command) {
        blackboardRef.tell(new PostValue(parseWeatherCondition(command.weatherCondition()), BlackboardField.WEATHER_CONDITION.key()));

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
        try {
            return WeatherCondition.valueOf(weatherCondition);
        } catch (IllegalArgumentException e) {
            WeatherCondition convertedWeatherCondition = weatherConditionAdapter.convert(weatherCondition);
            if (convertedWeatherCondition != null) {
                return convertedWeatherCondition;
            }
        }
        return WeatherCondition.CLEAR;
    }

    private WeatherCondition getRandomWeatherCondition() {
        WeatherCondition[] conditions = WeatherCondition.values();
        return conditions[random.nextInt(conditions.length)];
    }
}
