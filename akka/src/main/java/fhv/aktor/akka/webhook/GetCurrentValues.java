package fhv.aktor.akka.webhook;

import akka.actor.typed.ActorRef;

import java.util.Map;

public class GetCurrentValues implements WebhookCommand {
    final ActorRef<Map<String, String>> replyTo;

    public GetCurrentValues(ActorRef<Map<String, String>> replyTo) {
        this.replyTo = replyTo;
    }
}
