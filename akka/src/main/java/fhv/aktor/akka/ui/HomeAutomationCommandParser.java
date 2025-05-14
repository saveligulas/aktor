package fhv.aktor.akka.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.device.MediaStationCommand;
import fhv.aktor.akka.command.device.PlayMovie;
import fhv.aktor.akka.fridge.FridgeCommand;

public class HomeAutomationCommandParser extends AbstractBehavior<CommandResponse> implements UserCommandParser {
    private final ActorRef<MediaStationCommand> mediaStation;
    private final ActorRef<FridgeCommand> fridge;

    public HomeAutomationCommandParser(ActorContext<CommandResponse> context, ActorRef<MediaStationCommand> mediaStation, ActorRef<FridgeCommand> fridge) {
        super(context);
        this.mediaStation = mediaStation;
        this.fridge = fridge;
    }

    @Override
    public CommandResponse execute(String input) throws InputParsingException {
        String[] inputParts = input.split(" ");

        switch(inputParts[0]) {
            case "play" -> routeToMediaStation(inputParts);
            case "order" -> routeToFridge(inputParts);
        }
    }

    private CommandResponse routeToMediaStation(String[] inputParts) {
        mediaStation.tell(new PlayMovie() {});
    }

    private void routeToFridge(String[] inputParts) {


    }

    @Override
    public Receive<CommandResponse> createReceive() {
        return newReceiveBuilder()
                .onMessage(CommandResponse.class, this::handleResponse)
                .build();
    }

    private Behavior<CommandResponse> handleResponse(CommandResponse commandResponse) {
    }
}
