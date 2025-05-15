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
import fhv.aktor.akka.fridge.command.ConsumeProduct;
import fhv.aktor.akka.fridge.command.OrderProducts;
import fhv.aktor.akka.fridge.command.query.QueryOrders;
import fhv.aktor.akka.fridge.command.query.QueryProducts;
import fhv.aktor.akka.message.DisplayMessage;
import fhv.aktor.akka.message.LoggingProvider;

import java.util.HashMap;
import java.util.Map;

public class CommandParser extends AbstractBehavior<UserCommand> {
    private final ActorRef<MediaStationCommand> mediaStation;
    private final ActorRef<FridgeCommand> fridge;
    private final ActorRef<DisplayMessage> display;
    private final LoggingProvider.EnhancedLogger logger;

    public CommandParser(ActorContext<UserCommand> context, ActorRef<MediaStationCommand> mediaStation, ActorRef<FridgeCommand> fridge, ActorRef<DisplayMessage> display) {
        super(context);
        this.mediaStation = mediaStation;
        this.fridge = fridge;
        this.display = display;
        this.logger = LoggingProvider.withContext(getContext().getLog());
    }

    public static Behavior<UserCommand> create(ActorRef<MediaStationCommand> mediaStation, ActorRef<FridgeCommand> fridge, ActorRef<DisplayMessage> display) {
        return Behaviors.setup(context -> new CommandParser(context, mediaStation, fridge, display));
    }

    private Behavior<UserCommand> execute(TerminalCommand input) throws InputParsingException {
        String[] inputParts = input.command().split(" ");
        logger.info("Processing command: " + input.command());

        switch (inputParts[0]) {
            case "play" -> routeToMediaStation(inputParts);
            case "order" -> routeToFridge(inputParts);
            case "consume" -> fridgeConsume(inputParts);
            case "view" -> fridgeView(inputParts);
            case "orders" -> fridgeViewOrders(inputParts);
        }

        return Behaviors.same();
    }

    private void fridgeViewOrders(String[] inputParts) {
        logger.info("Processed Fridge orders Command");
        fridge.tell(new QueryOrders(getContext().getSelf().narrow()));
    }

    private void fridgeView(String[] inputParts) {
        logger.info("Processed Fridge view Command");
        fridge.tell(new QueryProducts(getContext().getSelf().narrow()));
    }

    private void fridgeConsume(String[] inputParts) {
        logger.info("Processed Fridge consume Command");
        String productName = inputParts[1];
        int quantity = Integer.parseInt(inputParts[2]);

        fridge.tell(new ConsumeProduct(productName, quantity));
    }

    private void routeToMediaStation(String[] inputParts) {
        logger.info("Processed Media Station play Command");
        mediaStation.tell(new PlayMovie() {
        });
    }

    private void routeToFridge(String[] inputParts) {
        logger.info("Processed Fridge order Command");
        Map<String, Integer> productQuantities = new HashMap<>();

        for (int i = 1; i < inputParts.length; i += 2) {
            productQuantities.put(inputParts[i], Integer.parseInt(inputParts[i + 1]));
        }

        fridge.tell(new OrderProducts(productQuantities));
    }

    @Override
    public Receive<UserCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(UserCommandResponse.class, this::routeResponse)
                .onMessage(TerminalCommand.class, this::execute)
                .build();
    }

    private Behavior<UserCommand> routeResponse(UserCommandResponse userCommandResponse) {
        display.tell(new DisplayMessage(userCommandResponse.message()));

        return Behaviors.same();
    }

}
