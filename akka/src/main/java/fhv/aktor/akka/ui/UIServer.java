package fhv.aktor.akka.ui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fhv.aktor.akka.webhook.WebhookActor;
import fhv.aktor.akka.webhook.WebhookCommand;

import java.util.Arrays;
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
                            Map<String, String> values = WebhookActor.getValuesSnapshot();
                            String jsonResponse = createJsonResponse(values);
                            return complete(HttpEntities.create(
                                    ContentTypes.APPLICATION_JSON, jsonResponse));
                        })),

                path(segment("api").slash("terminal-messages"), () ->
                        get(() -> {
                            List<String> messages = WebhookActor.getMessagesSnapshot();

                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                ObjectNode rootNode = mapper.createObjectNode();
                                ArrayNode messagesArray = rootNode.putArray("messages");

                                for (String message : messages) {
                                    messagesArray.add(message);
                                }

                                System.out.println("API sending " + messages.size() + " messages to client");

                                String jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

                                return respondWithHeaders(
                                        Arrays.asList(
                                                RawHeader.create("Cache-Control", "no-cache, no-store, must-revalidate"),
                                                RawHeader.create("Pragma", "no-cache"),
                                                RawHeader.create("Expires", "0")
                                        ),
                                        () -> complete(HttpEntities.create(ContentTypes.APPLICATION_JSON, jsonResponse))
                                );
                            } catch (JsonProcessingException e) {
                                system.log().error("Error creating JSON response", e);
                                return complete(StatusCodes.INTERNAL_SERVER_ERROR, "Error generating JSON response");
                            }
                        })
                ),

                path(segment("api").slash("debug"), () ->
                        get(() -> {
                            Map<String, String> values = WebhookActor.getValuesSnapshot();

                            StringBuilder sb = new StringBuilder("<html><body><h1>Debug Values</h1>");

                            sb.append("<h2>Current Values (Direct from Map)</h2>");
                            sb.append("<table border='1'>");
                            sb.append("<tr><th>Key</th><th>Value</th></tr>");
                            for (Map.Entry<String, String> entry : values.entrySet()) {
                                sb.append("<tr><td>").append(entry.getKey()).append("</td>");
                                sb.append("<td>").append(entry.getValue()).append("</td></tr>");
                            }
                            sb.append("</table>");

                            sb.append("<h2>JSON Representation</h2>");

                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                sb.append("<pre>").append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(values)).append("</pre>");
                            } catch (JsonProcessingException e) {
                                sb.append("<p>Error generating JSON: ").append(e.getMessage()).append("</p>");
                            }

                            sb.append("<p><a href='/api/debug'>Refresh</a></p>");
                            sb.append("</body></html>");

                            return complete(HttpEntities.create(
                                    ContentTypes.TEXT_HTML_UTF8, sb.toString()));
                        })),

                path(segment("command"), () ->
                        get(() ->
                                parameter("input", input -> {
                                    commandParser.tell(new TerminalCommand(input));
                                    return complete("Command sent: " + input);
                                })
                        ))
        );
    }

    private String createJsonResponse(Map<String, String> values) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> sanitizedValues = new HashMap<>();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                sanitizedValues.put(entry.getKey(), entry.getValue() == null ? "UNDEFINED" : entry.getValue());
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sanitizedValues);
        } catch (JsonProcessingException e) {
            system.log().error("Error creating JSON response", e);
            return "{}";
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