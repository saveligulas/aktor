package fhv.aktor.akka.subordinate.device;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.blackboard.AbstractBlackboardSubordinateActor;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.observe.Condition;
import fhv.aktor.akka.command.blackboard.observe.ObserveField;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.device.BlindsCommand;
import fhv.aktor.akka.command.device.WeatherConditionAlert;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.commons.BlindsState;
import fhv.aktor.akka.commons.WeatherCondition;

public class BlindsActor extends AbstractBlackboardSubordinateActor<BlindsCommand> {

    private BlindsActor(ActorContext<BlindsCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);

        blackboardRef.tell(new ObserveField<BlindsCommand, WeatherCondition, WeatherConditionAlert, WeatherConditionAlert.Builder>() {

            @Override
            public String key() {
                return BlackboardField.WEATHER_CONDITION.key(); // TODO: abstract to interface with field registry
            }

            @Override
            public Condition<BlindsCommand, WeatherCondition, WeatherConditionAlert, WeatherConditionAlert.Builder> getCondition() {
                return new Condition<BlindsCommand, WeatherCondition, WeatherConditionAlert, WeatherConditionAlert.Builder>() {
                    @Override
                    public boolean conditionMet(WeatherCondition weatherCondition, WeatherCondition previousWeatherCondition) {
                        if (previousWeatherCondition == null) {
                            return false;
                        }

                        if (weatherCondition != previousWeatherCondition) {
                            if (weatherCondition == WeatherCondition.CLEAR || previousWeatherCondition == WeatherCondition.CLEAR) {
                                return true;
                            }
                        }

                        return false;
                    }

                    @Override
                    public WeatherConditionAlert.Builder getCommandBuilderInstance() {
                        return WeatherConditionAlert.Builder.INSTANCE; // in the future use dependency injection with abstract interface
                    }

                    @Override
                    public ActorRef<BlindsCommand> getRef() {
                        return getContext().getSelf();
                    }
                };
            }

            @Override
            public Class<WeatherCondition> getObservedValueClass() {
                return WeatherCondition.class;
            }
        });
    }

    public static Behavior<BlindsCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(ctx -> new BlindsActor(ctx, blackboardRef));
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WeatherConditionAlert.class, this::onAlert)
                .build();
    }

    private Behavior<BlindsCommand> onAlert(WeatherConditionAlert weatherConditionAlert) {
        WeatherCondition weatherCondition = weatherConditionAlert.weatherCondition();
        BlindsState newBlindsState;

        if (weatherCondition == WeatherCondition.CLEAR) {
            newBlindsState = BlindsState.CLOSED;
        } else {
            newBlindsState = BlindsState.OPEN;
        }

        blackboardRef.tell(new PostValue(newBlindsState, BlackboardField.BLINDS.key()));

        return Behaviors.same();
    }
}
