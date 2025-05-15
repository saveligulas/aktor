package fhv.aktor.akka.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageCommand {
    private final String source;
    private final String level;
    private final String message;
    private final long timestamp;
    private final Map<String, Object> attributes;

    public MessageCommand(String source, String level, String message) {
        this(source, level, message, new HashMap<>());
    }

    public MessageCommand(String source, String level, String message, Map<String, Object> attributes) {
        this.source = source;
        this.level = level;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.attributes = new HashMap<>(attributes);
    }

    public static MessageCommand terminal(String message) {
        return new MessageCommand("TERMINAL", "INFO", message);
    }

    public static MessageCommand notification(String message) {
        return new MessageCommand("SYSTEM", "NOTIFICATION", message);
    }

    public String getSource() {
        return source;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public MessageCommand withAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.attributes);
        newAttributes.put(key, value);
        return new MessageCommand(this.source, this.level, this.message, newAttributes);
    }

    public String format() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        StringBuilder sb = new StringBuilder();

        sb.append(dateFormat.format(new Date(timestamp)))
                .append(" [").append(level).append("] ");

        if (source != null && !source.isEmpty()) {
            sb.append("[").append(source).append("] ");
        }

        sb.append(message);

        if (!attributes.isEmpty()) {
            sb.append(" {");
            boolean first = true;
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("}");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return format();
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("source", source);
            messageMap.put("level", level);
            messageMap.put("message", message);
            messageMap.put("timestamp", timestamp);
            messageMap.put("attributes", attributes);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageMap);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Failed to convert message to JSON\", \"message\":\"" + toString() + "\"}";
        }
    }
}
