package fhv.aktor.akka.message;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class MessageActor extends AbstractBehavior<MessageCommand> {
    private final ActorRef<DisplayMessage> displayRef; // delegating to webhook

    public static Behavior<MessageCommand> create(ActorRef<DisplayMessage> displayRef) {
        return Behaviors.setup(ctx -> new MessageActor(ctx, displayRef));
    }

    public MessageActor(ActorContext<MessageCommand> context, ActorRef<DisplayMessage> displayRef) {
        super(context);
        this.displayRef = displayRef;
    }

    @Override
    public Receive<MessageCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(MessageCommand.class, this::onBasicMessage)
                .build();
    }

    private Behavior<MessageCommand> onBasicMessage(MessageCommand basicMessage) {
        displayRef.tell(new DisplayMessage(basicMessage.toString()));

        return Behaviors.same();
    }
}
