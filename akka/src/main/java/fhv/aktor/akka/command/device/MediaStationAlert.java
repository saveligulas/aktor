package fhv.aktor.akka.command.device;

import fhv.aktor.akka.commons.CommandBuilder;
import fhv.aktor.akka.commons.MediaStationState;

public record MediaStationAlert(MediaStationState state) implements BlindsCommand {
    public static class Builder implements CommandBuilder<MediaStationState, MediaStationAlert> {
        public static final Builder INSTANCE = new Builder();
        
        private Builder() {
        }

        @Override
        public MediaStationAlert buildCommand(MediaStationState mediaStationState) {
            return new MediaStationAlert(mediaStationState);
        }
    }
}
