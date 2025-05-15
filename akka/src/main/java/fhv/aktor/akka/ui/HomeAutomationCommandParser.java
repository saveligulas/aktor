package fhv.aktor.akka.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.command.device.MediaStationCommand;
import fhv.aktor.akka.command.device.PlayMovie;
import fhv.aktor.akka.fridge.FridgeCommand;
import fhv.aktor.akka.fridge.command.OrderProduct;

import java.util.HashMap;
import java.util.Map;

public class HomeAutomationCommandParser extends AbstractBehavior<UserCommand> {
    private final ActorRef<MediaStationCommand> mediaStation;
    private final ActorRef<FridgeCommand> fridge;

    public HomeAutomationCommandParser(ActorContext<UserCommand> context, ActorRef<MediaStationCommand> mediaStation, ActorRef<FridgeCommand> fridge) {
        super(context);
        this.mediaStation = mediaStation;
        this.fridge = fridge;
    }

    public static Behavior<UserCommand> create(ActorRef<MediaStationCommand> mediaStation, ActorRef<FridgeCommand> fridge) {
        return Behaviors.setup(context -> new HomeAutomationCommandParser(context, mediaStation, fridge));
    }

    private Behavior<UserCommand> execute(TerminalCommand input) throws InputParsingException {
        String[] inputParts = input.command().split(" ");

        switch (inputParts[0]) {
            case "play" -> routeToMediaStation(inputParts);
            case "order" -> routeToFridge(inputParts);
        }

        return Behaviors.same();
    }

    private void routeToMediaStation(String[] inputParts) {
        mediaStation.tell(new PlayMovie() {
        });
    }

    private void routeToFridge(String[] inputParts) {
        Map<String, Integer> productQuantities = new HashMap<>();

        for (int i = 1; i < inputParts.length; i += 2) {
            productQuantities.put(inputParts[i], Integer.parseInt(inputParts[i + 1]));
        }

        fridge.tell(new OrderProduct(productQuantities));
    }

    @Override
    public Receive<UserCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(CommandResponse.class, this::handleResponse)
                .onMessage(TerminalCommand.class, this::execute)
                .build();
    }

    private Behavior<UserCommand> handleResponse(CommandResponse commandResponse) {
        //TODO: implement with Webhooks
        return Behaviors.same();
    }
}
