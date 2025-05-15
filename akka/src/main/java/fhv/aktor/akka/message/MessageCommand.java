package fhv.aktor.akka.message;

import java.time.Instant;

public interface MessageCommand {
    String message();

    Instant timestamp();

    String source();
}
