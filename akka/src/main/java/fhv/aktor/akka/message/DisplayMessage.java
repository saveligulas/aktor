package fhv.aktor.akka.message;

import fhv.aktor.akka.ui.UserCommandResponse;
import fhv.aktor.akka.webhook.WebhookCommand;

public record DisplayMessage(String message) implements WebhookCommand, UserCommandResponse {
}
