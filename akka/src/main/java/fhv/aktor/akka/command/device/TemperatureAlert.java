package fhv.aktor.akka.command.device;

import fhv.aktor.akka.commons.CommandBuilder;

public record TemperatureAlert(Double temperature) implements ACCommand {
    public static class Builder implements CommandBuilder<Double, TemperatureAlert> {
        public static final Builder INSTANCE = new Builder();

        private Builder() {
        }

        @Override
        public TemperatureAlert buildCommand(Double temperature) {
            return new TemperatureAlert(temperature);
        }
    }
}
