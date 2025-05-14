package fhv.aktor.akka.subordinate.device;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.AbstractBlackboardSubordinateActor;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.blackboard.post.PostValue;
import fhv.aktor.akka.command.device.MediaStationCommand;
import fhv.aktor.akka.command.device.PlayMovie;
import fhv.aktor.akka.commons.BlackboardField;
import fhv.aktor.akka.commons.MediaStationState;

public class MediaStationActor extends AbstractBlackboardSubordinateActor<MediaStationCommand> {
    public static Behavior<MediaStationCommand> create(ActorRef<BlackboardCommand> blackboardRef) {
        return Behaviors.setup(context -> new MediaStationActor(context, blackboardRef));
    }

    protected MediaStationActor(ActorContext<MediaStationCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(PlayMovie.class, this::onPlayMovie)
                .build();
    }

    private Behavior<MediaStationCommand> onPlayMovie(PlayMovie playMovie) {
        blackboardRef.tell(new PostValue(MediaStationState.PLAYING_MOVIE, BlackboardField.MEDIA_STATION.key()));

        return Behaviors.same();
    }
}
