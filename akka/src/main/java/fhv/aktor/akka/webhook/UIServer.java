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
                // Serve static files
                path(segment(""), () -> getFromResource("static/index.html")),
                pathPrefix("static", () -> getFromResourceDirectory("static")),
                
                // API endpoint for blackboard values
                path(segment("api").slash("blackboard-values"), () ->
                        get(() -> {
                            // Get the latest values from the static holder
                            Map<String, String> values = WebhookActor.getValuesSnapshot(webhookActor);
                            
                            system.log().info("Sending values to client: {}", values);
                            for (Map.Entry<String, String> entry : values.entrySet()) {
                                system.log().info("  {} = {}", entry.getKey(), entry.getValue());
                            }
                            
                            // Test the map directly
                            StringBuilder debugInfo = new StringBuilder("<html><body><h2>Current Values</h2><ul>");
                            for (Map.Entry<String, String> entry : values.entrySet()) {
                                debugInfo.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>");
                            }
                            debugInfo.append("</ul></body></html>");
                            
                            String jsonResponse = createJsonResponse(values);
                            system.log().info("JSON response: {}", jsonResponse);
                            return complete(HttpEntities.create(
                                    ContentTypes.APPLICATION_JSON, jsonResponse));
                        })),
                
                // Debug endpoint to show the current values
                path(segment("api").slash("debug"), () ->
                        get(() -> {
                            Map<String, String> values = WebhookActor.getValuesSnapshot(webhookActor);
                            
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
                            sb.append("<pre>").append(createJsonResponse(values)).append("</pre>");
                            
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
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) {
                sb.append(",\n");
            }
            
            // Escape quotes in keys and values for valid JSON
            String key = entry.getKey().replace("\"", "\\\"");
            String value = entry.getValue();
            // Replace null with UNDEFINED to avoid JSON null
            if (value == null) {
                value = "UNDEFINED";
            }
            // Escape special characters for JSON
            value = value.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t");
            
            sb.append("  \"")
              .append(key)
              .append("\":")
              .append("\"")
              .append(value)
              .append("\"");
            
            first = false;
        }
        
        sb.append("\n}");
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