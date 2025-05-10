package fhv.aktor.akka.commons;

import java.util.Map;

public class StringFormatter {
    public static String formatMapForConsole(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "Map is empty or null.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Map Contents:\n");
        map.forEach((key, value) -> sb.append("  • ").append(key).append(" => ").append(value).append("\n"));
        return sb.toString();
    }

}
