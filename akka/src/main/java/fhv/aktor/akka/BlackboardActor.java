package fhv.aktor.akka;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.BlackboardRegistry;
import fhv.aktor.akka.command.blackboard.observe.Condition;
import fhv.aktor.akka.command.blackboard.observe.ObserveField;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.blackboard.query.Query;
import fhv.aktor.akka.command.blackboard.query.QueryBlackboard;
import fhv.aktor.akka.command.blackboard.query.QueryResponseCommand;
import fhv.aktor.akka.commons.StringFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlackboardActor extends AbstractBehavior<BlackboardCommand> {
    private final Map<String, Object> board = new ConcurrentHashMap<>();
    private final Map<String, List<ObserveField<?, ?, ?, ?>>> fieldObservers = new ConcurrentHashMap<>();
    private final BlackboardRegistry registry;

    public static Behavior<BlackboardCommand> create(BlackboardRegistry registry) {
        return Behaviors.setup(context -> new BlackboardActor(context, registry));
    }

    private BlackboardActor(ActorContext<BlackboardCommand> context, BlackboardRegistry registry) {
        super(context);
        this.registry = registry;
    }

    @Override
    public Receive<BlackboardCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(ObserveField.class, this::registerFieldObserver)
                .onMessage(PostValue.class, this::onPostValue)
                .onMessage(QueryBlackboard.class, this::respondToQuery) // TODO: fix
                .build();
    }

    private Behavior<BlackboardCommand> registerFieldObserver(ObserveField<?, ?, ?, ?> fieldObserver) {
        String key = fieldObserver.key();
        Class<?> observedValueClass = fieldObserver.getObservedValueClass();
        if (!registry.isValidKeyAndType(key, observedValueClass)) {
            throw new IllegalStateException("Observer is observing a non registered Field");
        }
        this.fieldObservers.computeIfAbsent(fieldObserver.key(), k -> new ArrayList<>()).add(fieldObserver); // TODO: implement registry to check observers for type safety

        return Behaviors.same();
    }


    private void registerObserver(ObserveField<?, ?, ?, ?> fieldObserver) {
        String key = fieldObserver.key();
        Class<?> observedValueClass = fieldObserver.getObservedValueClass();
        if (!registry.isValidKeyAndType(key, observedValueClass)) {
            throw new IllegalStateException("Observer is observing a non registered Field");
        }
        this.fieldObservers.computeIfAbsent(fieldObserver.key(), k -> new ArrayList<>()).add(fieldObserver); // TODO: implement registry to check observers for type safety
    }

    private <C extends QueryResponseCommand<V>, V> Behavior<BlackboardCommand> respondToQuery(Query<C, V> queryBlackboard) {
        C response = queryBlackboard.command();
        Object value = board.get(queryBlackboard.key());
        System.out.println("Query for key: " + queryBlackboard.key());

        if (response.getValueType().isInstance(value)) {
            V typed = response.getValueType().cast(value);
            response.fromValue(typed);
            System.out.println("Retrieved value for query: " + value);
            queryBlackboard.replyTo().tell(response);
        }

        return Behaviors.same();
    }

    private Behavior<BlackboardCommand> onPostValue(PostValue postValue) {
        Object newValue = postValue.value();
        Object oldValue = this.board.put(postValue.key(),  postValue.value());
        System.out.println("Post Value and key: " + postValue.value() + " | " + postValue.key());
        System.out.println(("Board: " + StringFormatter.formatMapForConsole(board)));

        List<ObserveField<?, ?, ?, ?>> observers = fieldObservers.get(postValue.key());
        if (observers != null && !observers.isEmpty()) {
            checkObservers(observers, newValue, oldValue);
        }

        return Behaviors.same();
    }

    @SuppressWarnings("unchecked")
    private void checkObservers(List<ObserveField<?, ?, ?, ?>> observers, Object newValue, Object oldValue) {
        for (ObserveField<?, ?, ?, ?> fieldObserver : observers) {
            Class<?> observedClass = fieldObserver.getObservedValueClass();
            if (observedClass.isInstance(newValue) && (oldValue == null || observedClass.isInstance(oldValue))) {
                // safe due to previous check
                Condition condition = fieldObserver.getCondition();
                condition.doCheck(observedClass.cast(newValue), observedClass.cast(oldValue));
            }
        }
    }
}
