package fhv.aktor.akka.command.device;

import fhv.aktor.akka.commons.CommandBuilder;
import fhv.aktor.akka.commons.WeatherCondition;

public record WeatherConditionAlert(WeatherCondition weatherCondition) implements BlindsCommand {
    public static class Builder implements CommandBuilder<WeatherCondition, WeatherConditionAlert> {
        public static final Builder INSTANCE = new Builder();
        
        private Builder() {
        }
        
        @Override
        public WeatherConditionAlert buildCommand(WeatherCondition weatherCondition) {
            return new WeatherConditionAlert(weatherCondition);
        }
    }
}
