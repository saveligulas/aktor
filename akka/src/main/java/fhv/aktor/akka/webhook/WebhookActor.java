package fhv.aktor.akka.webhook;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.query.QueryBlackboard;
import fhv.aktor.akka.commons.BlackboardField;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebhookActor extends AbstractBehavior<WebhookCommand> {
    private final ActorRef<BlackboardCommand> blackboardRef;
    private final Map<String, String> currentValues = new ConcurrentHashMap<>();
    private final TimerScheduler<WebhookCommand> timers;

    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(2);
    
    public static Behavior<WebhookCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(context -> 
                Behaviors.withTimers(timers -> 
                        new WebhookActor(context, blackboardRef, timers)));
    }
    
    private WebhookActor(ActorContext<WebhookCommand> context, ActorRef<BlackboardCommand> blackboardRef, TimerScheduler<WebhookCommand> timers) {
        super(context);
        this.blackboardRef = blackboardRef;
        this.timers = timers;

        this.timers.startTimerWithFixedDelay("fetch-values", new FetchBlackboardValues(), REFRESH_INTERVAL);

        for (BlackboardField field : BlackboardField.values()) {
            currentValues.put(field.key(), "UNDEFINED");
        }

        ValuesHolder.updateValues(new ConcurrentHashMap<>(currentValues));

        getContext().getSelf().tell(new FetchBlackboardValues());
        
        getContext().getLog().info("WebhookActor started - will fetch blackboard values every {} seconds", REFRESH_INTERVAL.getSeconds());
    }
    
    @Override
    public Receive<WebhookCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(FetchBlackboardValues.class, this::onFetchBlackboardValues)
                .onMessage(WebhookFetchResponse.class, this::onFetchResponse)
                .onMessage(GetCurrentValues.class, this::onGetCurrentValues)
                .build();
    }

    private Behavior<WebhookCommand> onFetchResponse(WebhookFetchResponse command) {
        currentValues.put(command.key(), command.value());
        ValuesHolder.updateValues(new ConcurrentHashMap<>(currentValues));
        getContext().getLog().debug("Updated value for {}: {}", command.key(), command.value());
        return this;
    }

    private Behavior<WebhookCommand> onFetchBlackboardValues(FetchBlackboardValues command) {
        for (BlackboardField field : BlackboardField.values()) {
            fetchFieldValue(field.key());
        }
        return this;
    }

    
    private Behavior<WebhookCommand> onGetCurrentValues(GetCurrentValues command) {
        Map<String, String> valuesToSend = new ConcurrentHashMap<>(currentValues);
        ValuesHolder.updateValues(valuesToSend);
        command.replyTo.tell(valuesToSend);
        return this;
    }
    
    private void fetchFieldValue(String fieldKey) {
        blackboardRef.tell(new QueryBlackboard<>(fieldKey, new WebhookFetchResponse(), getContext().getSelf()));
    }

    public static Map<String, String> getValuesSnapshot(ActorRef<WebhookCommand> webhookActor) {
        return ValuesHolder.getValues();
    }

    private static class ValuesHolder {
        private static final Map<String, String> currentValues = new ConcurrentHashMap<>();
        
        static Map<String, String> getValues() {
            return new ConcurrentHashMap<>(currentValues);
        }
        
        static void updateValues(Map<String, String> values) {
            currentValues.clear();
            currentValues.putAll(values);
        }
    }
}