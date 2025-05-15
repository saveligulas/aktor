package fhv.aktor.akka.message;

import java.time.Instant;

public class BasicMessage implements MessageCommand {
    private final String message;
    private final Instant timestamp;
    private final String source;

    public BasicMessage(String message) {
        this(message, Instant.now(), "SYSTEM");
    }

    public BasicMessage(String message, Instant timestamp, String source) {
        this.message = message;
        this.timestamp = timestamp;
        this.source = source;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public String source() {
        return source;
    }
}
