package fhv.aktor.akka;

import akka.actor.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.blackboard.query.QueryResponseCommand;
import fhv.aktor.akka.command.blackboard.query.Query;
import fhv.aktor.akka.command.blackboard.receive.RegisterDevice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlackboardActor extends AbstractBehavior<BlackboardCommand> {
    private final Map<String, ActorRef> refs = new ConcurrentHashMap<>();
    private final Map<String, Object> board = new ConcurrentHashMap<>();

    public static Behavior<BlackboardCommand> create() {
        return Behaviors.setup(BlackboardActor::new);
    }

    private BlackboardActor(ActorContext<BlackboardCommand> context) {
        super(context);
    }

    @Override
    public Receive<BlackboardCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RegisterDevice.class, this::onRegister)
                .onMessage(Query.class, this::respondToQuery)
                .onMessage(PostValue.class, this::onPostValue)
                .build();
    }

    private Behavior<BlackboardCommand> onRegister(RegisterDevice register) {
        this.refs.put(register.id(), register.ref());
        return this;
    }

    private <C extends QueryResponseCommand<V>, V> Behavior<BlackboardCommand> respondToQuery(Query<C, V> queryBlackboard) {
        C response = queryBlackboard.command();
        Object value = board.get(queryBlackboard.key());

        if (response.getValueType().isInstance(value)) {
            V typed = response.getValueType().cast(value);
            response.fromValue(typed);
            queryBlackboard.replyTo().tell(response);
        }

        return this;
    }

    private Behavior<BlackboardCommand> onPostValue(PostValue postValue) {
        this.board.put(postValue.key(),  postValue.value());
        return this;
    }
}
