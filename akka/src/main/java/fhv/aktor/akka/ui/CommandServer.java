package fhv.aktor.akka.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

public class CommandServer extends AllDirectives { // TODO: merge with UIServer

    public void start(ActorSystem<Void> system, ActorRef<UserCommand> commandRef) throws IOException {
        Route route = new AllDirectives() {
        }.concat(
                path("command", () ->
                        get(() ->
                                parameterOptional("input", optionalInput -> {
                                    if (optionalInput.isPresent()) {
                                        String input = optionalInput.get();
                                        try {
                                            input = URLDecoder.decode(input, StandardCharsets.UTF_8.name());

                                            if (input == null || input.isBlank()) {
                                                return complete(StatusCodes.BAD_REQUEST, "No input provided.");
                                            }

                                            commandRef.tell(new TerminalCommand(input));
                                            return complete(
                                                    HttpEntities.create(
                                                            ContentTypes.TEXT_PLAIN_UTF8,
                                                            "EXECUTING: " + input
                                                    )
                                            );

                                        } catch (Exception e) {
                                            return complete(
                                                    StatusCodes.INTERNAL_SERVER_ERROR,
                                                    "Error processing request: " + e.getMessage()
                                            );
                                        }
                                    } else {
                                        return complete(StatusCodes.BAD_REQUEST, "No input provided.");
                                    }
                                })
                        )
                )
        );

        try {
            CompletionStage<ServerBinding> serverBindingFuture =
                    Http.get(system)
                            .newServerAt("localhost", 8082)
                            .bind(route);

            serverBindingFuture.toCompletableFuture().get();
            System.out.println("TerminalServer API started on http://localhost:8082/command");

        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            system.terminate();
        }
    }
}