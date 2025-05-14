package fhv.aktor.akka.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

public class TerminalServer {
    public static void start(UserCommandParser parser) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve index.html on "/"
        server.createContext("/", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream inputStream = TerminalServer.class.getClassLoader().getResourceAsStream("static/index.html");
            if (inputStream == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] response = inputStream.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        server.createContext("/command", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }

                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery();
                String input = null;

                if (query != null && query.startsWith("input=")) {
                    input = query.substring("input=".length());
                    input = java.net.URLDecoder.decode(input, "UTF-8");
                }

                String result;
                int status;
                if (input == null || input.isBlank()) {
                    result = "No input provided.";
                    status = 400;
                } else {
                    // Example processing
                    try {
                        CommandResponse response = parser.execute(input);
                        result = response.response();
                        status = 200;
                    } catch (InputParsingException e) {
                        result = "Invalid input: " + input;
                        status = 404;
                    }
                }

                byte[] response = result.getBytes("UTF-8");
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(status, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            }
        });

        server.setExecutor(null); // Default executor
        server.start();
        System.out.println("Server started on http://localhost:8080");
    }
}
