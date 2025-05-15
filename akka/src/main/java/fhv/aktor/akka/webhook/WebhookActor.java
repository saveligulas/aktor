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
import fhv.aktor.akka.message.DisplayMessage;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class WebhookActor extends AbstractBehavior<WebhookCommand> {
    private final ActorRef<BlackboardCommand> blackboardRef;
    private final Map<String, String> currentValues = new ConcurrentHashMap<>();
    private final TimerScheduler<WebhookCommand> timers;

    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(5);

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

        BlackboardValuesHolder.updateValues(new ConcurrentHashMap<>(currentValues));

        getContext().getSelf().tell(new FetchBlackboardValues());

        getContext().getLog().info("WebhookActor started - will fetch blackboard values every {} seconds", REFRESH_INTERVAL.getSeconds());
    }

    @Override
    public Receive<WebhookCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(DisplayMessage.class, this::onMessage)
                .onMessage(FetchBlackboardValues.class, this::onFetchBlackboardValues)
                .onMessage(WebhookFetchResponse.class, this::onFetchResponse)
                .onMessage(GetCurrentValues.class, this::onGetCurrentValues)
                .build();
    }

    private Behavior<WebhookCommand> onMessage(DisplayMessage basicMessage) {
        MessagesValuesHolder.addMessage(basicMessage.message());

        return Behaviors.same();
    }

    private Behavior<WebhookCommand> onFetchResponse(WebhookFetchResponse command) {
        currentValues.put(command.key(), command.value());
        BlackboardValuesHolder.updateValues(new ConcurrentHashMap<>(currentValues));
        getContext().getLog().debug("Updated value for {}: {}", command.key(), command.value());

        return Behaviors.same();
    }

    private Behavior<WebhookCommand> onFetchBlackboardValues(FetchBlackboardValues command) {
        for (BlackboardField field : BlackboardField.values()) {
            fetchFieldValue(field.key());
        }

        return Behaviors.same();
    }


    private Behavior<WebhookCommand> onGetCurrentValues(GetCurrentValues command) {
        Map<String, String> valuesToSend = new ConcurrentHashMap<>(currentValues);
        BlackboardValuesHolder.updateValues(valuesToSend);
        command.replyTo.tell(valuesToSend);

        return Behaviors.same();
    }

    private void fetchFieldValue(String fieldKey) {
        blackboardRef.tell(new QueryBlackboard<>(fieldKey, new WebhookFetchResponse(), getContext().getSelf()));
    }

    public static Map<String, String> getValuesSnapshot() {
        return BlackboardValuesHolder.getValues();
    }

    public static List<String> getMessagesSnapshot() {
        List<String> messages = MessagesValuesHolder.getMessages();
        return messages;
    }

    private static class BlackboardValuesHolder {
        private static final Map<String, String> currentValues = new ConcurrentHashMap<>();

        static Map<String, String> getValues() {
            return new ConcurrentHashMap<>(currentValues);
        }

        static void updateValues(Map<String, String> values) {
            currentValues.clear();
            currentValues.putAll(values);
        }
    }

    private static class MessagesValuesHolder {
        private static final List<TimestampedMessage> messageBuffer = new CopyOnWriteArrayList<>();
        private static final Object lockObject = new Object();
        private static final AtomicInteger messageCounter = new AtomicInteger(0);

        private static final int MAX_BUFFER_SIZE = 1000;
        private static final long MESSAGE_RETENTION_TIME_MS = 10000;

        static class TimestampedMessage {
            final String message;
            final long timestamp;
            final int id;

            TimestampedMessage(String message, int id) {
                this.message = message;
                this.timestamp = System.currentTimeMillis();
                this.id = id;
            }
        }

        static void addMessage(String message) {
            int id = messageCounter.incrementAndGet();
            TimestampedMessage timestampedMessage = new TimestampedMessage(message, id);
            messageBuffer.add(timestampedMessage);

            System.out.println("Added message: [" + id + "] " + message);

            if (id % 10 == 0 || messageBuffer.size() > MAX_BUFFER_SIZE) {
                cleanupOldMessages();
            }
        }

        private static void cleanupOldMessages() {
            long now = System.currentTimeMillis();
            long cutoffTime = now - MESSAGE_RETENTION_TIME_MS;

            synchronized (lockObject) {
                int sizeBefore = messageBuffer.size();
                messageBuffer.removeIf(msg -> msg.timestamp < cutoffTime);
                int removed = sizeBefore - messageBuffer.size();

                if (removed > 0) {
                    System.out.println("Cleaned up " + removed + " old messages");
                }

                while (messageBuffer.size() > MAX_BUFFER_SIZE) {
                    messageBuffer.remove(0);
                }
            }
        }

        static List<String> getMessages() {
            return messageBuffer.stream()
                    .map(tm -> "[" + tm.id + "] " + tm.message)
                    .collect(Collectors.toList());
        }

        static int getBufferSize() {
            return messageBuffer.size();
        }
    }
}