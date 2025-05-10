package fhv.aktor.akka.subordinate.device;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.AbstractBlackboardSubordinateActor;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.observe.Condition;
import fhv.aktor.akka.command.blackboard.observe.ObserveField;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.device.ACCommand;
import fhv.aktor.akka.command.device.TemperatureAlert;
import fhv.aktor.akka.commons.ACState;
import fhv.aktor.akka.commons.BlackboardField;

public class ACActor extends AbstractBlackboardSubordinateActor<ACCommand> {

    public static Behavior<ACCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(context -> new ACActor(context, blackboardRef));
    }

    private ACActor(ActorContext<ACCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);

        blackboardRef.tell(new ObserveField<ACCommand, Double, TemperatureAlert, TemperatureAlert.Builder>() {
            @Override
            public String key() {
                return BlackboardField.TEMPERATURE.key();
            }

            @Override
            public Condition<ACCommand, Double, TemperatureAlert, TemperatureAlert.Builder> getCondition() {
                return new Condition<ACCommand, Double, TemperatureAlert, TemperatureAlert.Builder>() {
                    @Override
                    public boolean conditionMet(Double temp, Double previousTemp) {
                        if (!temp.equals(previousTemp) && previousTemp != null) {
                            if (previousTemp < 20 && temp >= 20) {
                                return true;
                            }
                            if (temp < 20 && previousTemp >= 20) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public TemperatureAlert.Builder getCommandBuilderInstance() {
                        return TemperatureAlert.Builder.INSTANCE;
                    }

                    @Override
                    public ActorRef<ACCommand> getRef() {
                        return getContext().getSelf();
                    }
                };
            }

            @Override
            public Class<Double> getObservedValueClass() {
                return Double.class;
            }
        });
    }

    @Override
    public Receive<ACCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(TemperatureAlert.class, this::onTemperatureAlert)
                .build();
    }

    private Behavior<ACCommand> onTemperatureAlert(TemperatureAlert temperatureAlert) {
        ACState state;
        if (temperatureAlert.temperature() >= 20) {
            state = ACState.ON;
        } else {
            state = ACState.OFF;
        }

        blackboardRef.tell(new PostValue(state, BlackboardField.AC.key()));

        return Behaviors.same();
    }
}
