package fhv.aktor.akka.commons;

import fhv.aktor.akka.command.blackboard.BlackboardRegistry;
import fhv.aktor.akka.command.blackboard.RegisteredField;

import java.util.List;

public enum BlackboardField implements RegisteredField {
    TEMPERATURE(Double.class),
    BLINDS(BlindsState.class),
    MEDIA_STATION(MediaStationState.class),
    WEATHER_CONDITION(WeatherCondition.class),
    AC(ACState.class);

    public final Class<?> valueClass;

    BlackboardField(Class<?> clazz) {
        this.valueClass = clazz;
    }

    public String key() {
        return this.name();
    }

    @Override
    public Class<?> getFieldClass() {
        return valueClass;
    }

    public static class Registry implements BlackboardRegistry {
        List<BlackboardField> blackboardFields = List.of(BlackboardField.values());

        @Override
        public boolean isValidKeyAndType(String key, Class<?> clazz) {
            return blackboardFields.stream()
                    .anyMatch(blackboardField -> blackboardField.key().equals(key) &&
                            blackboardField.getFieldClass().equals(clazz));
        }
    }
}
