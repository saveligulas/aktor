package fhv.aktor.akka.message;

import akka.actor.typed.ActorRef;

public class LoggingProvider {
    private static ActorRef<MessageCommand> messageRef;

    public static EnhancedLogger withContext(org.slf4j.Logger logger) {
        return new EnhancedLogger(logger);
    }

    public static class EnhancedLogger {
        private final org.slf4j.Logger logger;

        private EnhancedLogger(org.slf4j.Logger logger) {
            this.logger = logger;
        }

        public void debug(String message) {
            logger.debug(message);
            forwardToActor("DEBUG", message);
        }

        public void debug(String format, Object... args) {
            logger.debug(format, args);
            try {
                String formattedMessage = String.format(format, args);
                forwardToActor("DEBUG", formattedMessage);
            } catch (Exception e) {
                logger.error("Error formatting debug message: {}", e.getMessage());
            }
        }

        public void info(String message) {
            logger.info(message);
            forwardToActor("INFO", message);
        }

        public void info(String format, Object... args) {
            logger.info(format, args);
            try {
                String formattedMessage = String.format(format, args);
                forwardToActor("INFO", formattedMessage);
            } catch (Exception e) {
                logger.error("Error formatting info message: {}", e.getMessage());
            }
        }

        public void warning(String message) {
            logger.warn(message);
            forwardToActor("WARNING", message);
        }

        public void warning(String format, Object... args) {
            logger.warn(format, args);
            try {
                String formattedMessage = String.format(format, args);
                forwardToActor("WARNING", formattedMessage);
            } catch (Exception e) {
                logger.error("Error formatting warning message: {}", e.getMessage());
            }
        }

        public void error(String message) {
            logger.error(message);
            forwardToActor("ERROR", message);
        }

        public void error(String format, Object... args) {
            logger.error(format, args);
            try {
                String formattedMessage = String.format(format, args);
                forwardToActor("ERROR", formattedMessage);
            } catch (Exception e) {
                logger.error("Error formatting error message: {}", e.getMessage());
            }
        }

        public void error(Throwable cause, String message) {
            logger.error(message, cause);
            forwardToActor("ERROR", message + " - Exception: " + cause.getMessage());
        }

        public void error(Throwable cause, String format, Object... args) {
            logger.error(format, args, cause);
            try {
                String formattedMessage = String.format(format, args);
                forwardToActor("ERROR", formattedMessage + " - Exception: " + cause.getMessage());
            } catch (Exception e) {
                logger.error("Error formatting error message with cause: {}", e.getMessage());
            }
        }

        private void forwardToActor(String level, String message) {
            if (messageRef != null) {
                try {
                    String source = "unknown";
                    if (logger instanceof ch.qos.logback.classic.Logger) {
                        source = logger.getName();
                        if (source.contains(".")) {
                            source = source.substring(source.lastIndexOf('.') + 1);
                        }
                    }

                    MessageCommand command = new MessageCommand(source, level, message);
                    messageRef.tell(command);
                } catch (Exception e) {
                    logger.error("Failed to forward log message to actor: {}", e.getMessage());
                }
            }
        }
    }

    public static void send(MessageCommand message) {
        if (messageRef != null) {
            messageRef.tell(message);
        }
    }

    public static ActorRef<MessageCommand> getMessageRef() {
        if (messageRef == null) {
            throw new IllegalStateException("LoggingProvider has not been initialized with a message actor reference");
        }
        return messageRef;
    }

    public static void setMessageRef(ActorRef<MessageCommand> newMessageRef) {
        LoggingProvider.messageRef = newMessageRef;
    }

    public static boolean hasMessageRef() {
        return messageRef != null;
    }
}
