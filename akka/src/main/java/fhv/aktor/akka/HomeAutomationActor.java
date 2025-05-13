package fhv.aktor.akka;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.blackboard.query.QueryBlackboard;
import fhv.aktor.akka.command.blackboard.query.StringResponseCommand;
import fhv.aktor.akka.command.sensor.TemperatureSensorCommand;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.mqtt.MqttStreamService;
import fhv.aktor.akka.subordinate.device.ACActor;
import fhv.aktor.akka.subordinate.device.BlindsActor;
import fhv.aktor.akka.subordinate.sensor.TemperatureSensor;
import fhv.aktor.akka.subordinate.sensor.WeatherSensor;

public class HomeAutomationActor extends AbstractBehavior<Void> {
    public static Behavior<Void> create() {
        return Behaviors.setup(HomeAutomationActor::new);
    }

    private HomeAutomationActor(ActorContext<Void> context) {
        super(context);

        getContext().getLog().info("HomeAutomationActor created");

        ActorRef<BlackboardCommand> blackboard = context.spawn(BlackboardActor.create(new BlackboardField.Registry()), "blackboard");
        ActorRef<StringResponseCommand> blackboardStringResponse = blackboard.narrow();

        ActorRef<TemperatureSensorCommand> tempSensor = context.spawn(TemperatureSensor.create(blackboard),  "temperatureSensor");
        context.spawn(BlindsActor.create(blackboard), "blindsActor");
        context.spawn(WeatherSensor.create(blackboard), "weatherSensor");
        context.spawn(TemperatureSensor.create(blackboard), "tempSensor");
        context.spawn(ACActor.create(blackboard), "ac");
        MqttStreamService.start(context.getSystem(), tempSensor.narrow());

        blackboard.tell(new PostValue("Hello", "key"));
        blackboard.tell(new QueryBlackboard<>("key", new StringResponseCommand(), blackboardStringResponse));
    }

    @Override
    public Receive<Void> createReceive() {
        return newReceiveBuilder().build();
    }
}
