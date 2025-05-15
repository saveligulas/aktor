package fhv.aktor.akka.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fhv.aktor.akka.webhook.WebhookActor;
import fhv.aktor.akka.webhook.WebhookCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static akka.http.javadsl.server.PathMatchers.segment;

public class UIServer extends AllDirectives {

    private final ActorRef<WebhookCommand> webhookActor;
    private final ActorSystem<?> system;
    private final ActorRef<UserCommand> commandParser;
    
    public UIServer(ActorRef<WebhookCommand> webhookActor, ActorSystem<?> system, ActorRef<UserCommand> commandParser) {
        this.webhookActor = webhookActor;
        this.system = system;
        this.commandParser = commandParser;
    }


    public Route createRoutes() {
        return concat(
                path(segment(""), () -> getFromResource("static/index.html")),
                pathPrefix("static", () -> getFromResourceDirectory("static")),

                path(segment("api").slash("blackboard-values"), () ->
                        get(() -> {
                            // Get the latest values from the static holder
                            Map<String, String> values = WebhookActor.getValuesSnapshot();

                            system.log().info("Sending values to client: {}", values);
                            for (Map.Entry<String, String> entry : values.entrySet()) {
                                system.log().info("  {} = {}", entry.getKey(), entry.getValue());
                            }

                            String jsonResponse = createJsonResponse(values);
                            system.log().info("JSON response: {}", jsonResponse);
                            return complete(HttpEntities.create(
                                    ContentTypes.APPLICATION_JSON, jsonResponse));
                        })),

                path(segment("api").slash("terminal-messages"), () ->
                        get(() -> {
                            List<String> messages = WebhookActor.getMessagesSnapshot();

                            // Create a JSON response with messages field containing the list
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                ObjectNode rootNode = mapper.createObjectNode();
                                ArrayNode messagesArray = rootNode.putArray("messages");

                                // Add each message to the array
                                for (String message : messages) {
                                    messagesArray.add(message);
                                }

                                String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                                return complete(HttpEntities.create(
                                        ContentTypes.APPLICATION_JSON, jsonResponse));
                            } catch (JsonProcessingException e) {
                                system.log().error("Error creating JSON response", e);
                                return complete(StatusCodes.INTERNAL_SERVER_ERROR, "Error generating JSON response");
                            }
                        })),

                // Debug endpoint to show the current values
                path(segment("api").slash("debug"), () ->
                        get(() -> {
                            Map<String, String> values = WebhookActor.getValuesSnapshot();

                            // Create a detailed debug view
                            StringBuilder sb = new StringBuilder("<html><body><h1>Debug Values</h1>");

                            // Add the current values table
                            sb.append("<h2>Current Values (Direct from Map)</h2>");
                            sb.append("<table border='1'>");
                            sb.append("<tr><th>Key</th><th>Value</th></tr>");
                            for (Map.Entry<String, String> entry : values.entrySet()) {
                                sb.append("<tr><td>").append(entry.getKey()).append("</td>");
                                sb.append("<td>").append(entry.getValue()).append("</td></tr>");
                            }
                            sb.append("</table>");

                            // Show the ValuesHolder content
                            sb.append("<h2>JSON Representation</h2>");

                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                sb.append("<pre>").append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(values)).append("</pre>");
                            } catch (JsonProcessingException e) {
                                sb.append("<p>Error generating JSON: ").append(e.getMessage()).append("</p>");
                            }

                            // Add refresh control
                            sb.append("<p><a href='/api/debug'>Refresh</a></p>");
                            sb.append("</body></html>");

                            return complete(HttpEntities.create(
                                    ContentTypes.TEXT_HTML_UTF8, sb.toString()));
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
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Convert null values to "UNDEFINED" string as in the original implementation
            Map<String, String> sanitizedValues = new HashMap<>();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                sanitizedValues.put(entry.getKey(), entry.getValue() == null ? "UNDEFINED" : entry.getValue());
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sanitizedValues);
        } catch (JsonProcessingException e) {
            system.log().error("Error creating JSON response", e);
            return "{}"; // Return empty JSON object in case of error
        }
    }
    
    public void start() {
        Http.get(system).newServerAt("localhost", 8081)
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