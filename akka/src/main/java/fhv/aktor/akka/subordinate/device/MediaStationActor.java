package fhv.aktor.akka.subordinate.device;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.AbstractBlackboardSubordinateActor;
import fhv.aktor.akka.command.blackboard.BlackboardCommand;
import fhv.aktor.akka.command.device.MediaStationCommand;

public class MediaStationActor extends AbstractBlackboardSubordinateActor<MediaStationCommand> {
    protected MediaStationActor(ActorContext<MediaStationCommand> context, ActorRef<BlackboardCommand> blackboardRef) {
        super(context, blackboardRef);
    }

    @Override
    public Receive<MediaStationCommand> createReceive() {
        return null;
    }
}
