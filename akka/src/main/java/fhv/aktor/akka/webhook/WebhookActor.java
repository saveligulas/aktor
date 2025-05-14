package fhv.aktor.akka.webhook;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.query.QueryBlackboard;
import fhv.aktor.akka.command.blackboard.query.StringResponseCommand;
import fhv.aktor.akka.commons.BlackboardField;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebhookActor extends AbstractBehavior<WebhookActor.Command> {

    public interface Command {}
    
    // Command to trigger fetching blackboard values
    private static class FetchBlackboardValues implements Command {}
    
    // Command to request current values
    public static class GetCurrentValues implements Command {
        final ActorRef<Map<String, String>> replyTo;
        
        public GetCurrentValues(ActorRef<Map<String, String>> replyTo) {
            this.replyTo = replyTo;
        }
    }
    
    // Internal command for when a value is received from the blackboard
    private static class BlackboardValueReceived implements Command {
        final String key;
        final String value;
        
        BlackboardValueReceived(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
    
    private final ActorRef<BlackboardCommand> blackboardRef;
    // Shared state to store the current values
    private final Map<String, String> currentValues = new ConcurrentHashMap<>();
    private final TimerScheduler<Command> timers;
    
    // How often to refresh values
    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(2);
    
    public static Behavior<Command> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(context -> 
                Behaviors.withTimers(timers -> 
                        new WebhookActor(context, blackboardRef, timers)));
    }
    
    private WebhookActor(ActorContext<Command> context, ActorRef<BlackboardCommand> blackboardRef, TimerScheduler<Command> timers) {
        super(context);
        this.blackboardRef = blackboardRef;
        this.timers = timers;
        
        // Start periodic fetching of blackboard values
        this.timers.startTimerWithFixedDelay("fetch-values", new FetchBlackboardValues(), REFRESH_INTERVAL);
        
        // Initialize all values to UNDEFINED
        for (BlackboardField field : BlackboardField.values()) {
            currentValues.put(field.key(), "UNDEFINED");
        }
        
        // Update the static holder with initial values
        ValuesHolder.updateValues(new ConcurrentHashMap<>(currentValues));
        
        // Fetch values immediately
        getContext().getSelf().tell(new FetchBlackboardValues());
        
        getContext().getLog().info("WebhookActor started - will fetch blackboard values every {} seconds", REFRESH_INTERVAL.getSeconds());
    }
    
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FetchBlackboardValues.class, this::onFetchBlackboardValues)
                .onMessage(BlackboardValueReceived.class, this::onBlackboardValueReceived)
                .onMessage(GetCurrentValues.class, this::onGetCurrentValues)
                .build();
    }
    
    private Behavior<Command> onFetchBlackboardValues(FetchBlackboardValues command) {
        for (BlackboardField field : BlackboardField.values()) {
            fetchFieldValue(field.key());
        }
        return this;
    }
    
    private Behavior<Command> onBlackboardValueReceived(BlackboardValueReceived command) {
        currentValues.put(command.key, command.value);
        // Update the static values holder with the latest values
        ValuesHolder.updateValues(new ConcurrentHashMap<>(currentValues));
        getContext().getLog().debug("Updated value for {}: {}", command.key, command.value);
        return this;
    }
    
    private Behavior<Command> onGetCurrentValues(GetCurrentValues command) {
        // Create a copy of the current values to send
        Map<String, String> valuesToSend = new ConcurrentHashMap<>(currentValues);
        // Update the static values holder
        ValuesHolder.updateValues(valuesToSend);
        // Reply to the sender
        command.replyTo.tell(valuesToSend);
        return this;
    }
    
    private void fetchFieldValue(String fieldKey) {
        // Create an adapter that will convert StringResponseCommand into our BlackboardValueReceived command
        ActorRef<StringResponseCommand> responseAdapter = 
                getContext().messageAdapter(StringResponseCommand.class, 
                        response -> new BlackboardValueReceived(fieldKey, response.value()));
        
        // Create a query command and send it to the blackboard
        StringResponseCommand responseCommand = new StringResponseCommand();
        blackboardRef.tell(new QueryBlackboard<>(fieldKey, responseCommand, responseAdapter));
    }
    
    // For the HTTP server to access current values
    public static Map<String, String> getValuesSnapshot(ActorRef<Command> webhookActor) {
        return ValuesHolder.getValues();
    }
    
    // Static holder to store latest values for HTTP access
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