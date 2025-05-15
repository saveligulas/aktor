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
import fhv.aktor.akka.command.blackboard.observe.SecondaryCondition;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.device.BlindsCommand;
import fhv.aktor.akka.command.device.MediaStationAlert;
import fhv.aktor.akka.command.device.WeatherConditionAlert;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.commons.BlindsState;
import fhv.aktor.akka.commons.MediaStationState;
import fhv.aktor.akka.commons.WeatherCondition;
import fhv.aktor.akka.message.LoggingProvider;

import java.util.List;

public class BlindsActor extends AbstractBlackboardSubordinateActor<BlindsCommand> {
    private final LoggingProvider.EnhancedLogger logger;

    private BlindsActor(ActorContext<BlindsCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);

        this.logger = LoggingProvider.withContext(getContext().getLog());

        // notify if weather goes above or below 20 if media station not playing a movie
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
                            return true; // simplified due to secondary condition
                        }

                        return false;
                    }

                    @Override
                    public List<SecondaryCondition<?>> secondaryConditions() {
                        return List.of(new SecondaryCondition<MediaStationState>() {

                            @Override
                            public boolean conditionMet(MediaStationState value) {
                                return value != MediaStationState.PLAYING_MOVIE;
                            }

                            @Override
                            public String key() {
                                return BlackboardField.MEDIA_STATION.key();
                            }

                            @Override
                            public Class<MediaStationState> getValueType() {
                                return MediaStationState.class;
                            }
                        });
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

        // notify if media station changes state
        blackboardRef.tell(new ObserveField<BlindsCommand, MediaStationState, MediaStationAlert, MediaStationAlert.Builder>() {
            @Override
            public String key() {
                return BlackboardField.MEDIA_STATION.key();
            }

            @Override
            public Condition<BlindsCommand, MediaStationState, MediaStationAlert, MediaStationAlert.Builder> getCondition() {
                return new Condition<BlindsCommand, MediaStationState, MediaStationAlert, MediaStationAlert.Builder>() {
                    @Override
                    public boolean conditionMet(MediaStationState mediaStationState, MediaStationState previousValue) {
                        return previousValue != mediaStationState;
                    }

                    @Override
                    public MediaStationAlert.Builder getCommandBuilderInstance() {
                        return MediaStationAlert.Builder.INSTANCE;
                    }

                    @Override
                    public ActorRef<BlindsCommand> getRef() {
                        return getContext().getSelf();
                    }
                };
            }

            @Override
            public Class<MediaStationState> getObservedValueClass() {
                return MediaStationState.class;
            }
        });
    }

    public static Behavior<BlindsCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(ctx -> new BlindsActor(ctx, blackboardRef));
    }

    @Override
    public Receive<BlindsCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(MediaStationAlert.class, this::onMediaAlert)
                .onMessage(WeatherConditionAlert.class, this::onAlert)
                .build();
    }

    private Behavior<BlindsCommand> onMediaAlert(MediaStationAlert mediaStationAlert) {
        MediaStationState state = mediaStationAlert.state();

        if (state == MediaStationState.PLAYING_MOVIE) {
            blackboardRef.tell(new PostValue(BlindsState.CLOSED, BlackboardField.BLINDS.key()));
            logger.info("Closed Blinds because Movie started playing");
        }

        return Behaviors.same();
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
