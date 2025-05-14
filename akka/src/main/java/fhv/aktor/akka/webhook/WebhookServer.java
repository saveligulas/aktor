package fhv.aktor.akka.webhook;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import fhv.aktor.akka.ui.TerminalCommand;
import fhv.aktor.akka.ui.UserCommand;

import java.util.Map;

import static akka.http.javadsl.server.PathMatchers.segment;

public class WebhookServer extends AllDirectives {

    private final ActorRef<WebhookActor.Command> webhookActor;
    private final ActorSystem<?> system;
    private final ActorRef<UserCommand> commandParser;
    
    public WebhookServer(ActorRef<WebhookActor.Command> webhookActor, ActorSystem<?> system, ActorRef<UserCommand> commandParser) {
        this.webhookActor = webhookActor;
        this.system = system;
        this.commandParser = commandParser;
    }

    public Route createRoutes() {
        return concat(
                // Serve static files
                path(segment(""), () -> getFromResource("static/index.html")),
                pathPrefix("static", () -> getFromResourceDirectory("static")),
                
                // API endpoint for blackboard values
                path(segment("api").slash("blackboard-values"), () ->
                        get(() -> {
                            // Get the latest values from the static holder
                            Map<String, String> values = WebhookActor.getValuesSnapshot(webhookActor);
                            String jsonResponse = createJsonResponse(values);
                            return complete(HttpEntities.create(
                                    ContentTypes.APPLICATION_JSON, jsonResponse));
                        })),
                        
                // Command endpoint for the terminal
                path(segment("command"), () ->
                        get(() -> 
                                parameter("input", input -> {
                                    // Forward the command to the parser
                                    commandParser.tell(new TerminalCommand(input));
                                    return complete("Command sent: " + input);
                                })
                        ))
        );
    }
    
    private String createJsonResponse(Map<String, String> values) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    public void start() {
        Http.get(system).newServerAt("0.0.0.0", 8081)
                .bind(createRoutes())
                .thenAccept(binding -> {
                    system.log().info("WebhookServer started at http://{}:{}/", 
                            binding.localAddress().getHostString(),
                            binding.localAddress().getPort());
                })
                .exceptionally(exception -> {
                    system.log().error("Failed to start server: {}", exception.getMessage());
                    return null;
                });
    }
}