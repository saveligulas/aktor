package fhv.aktor.akka.mqtt;

import fhv.aktor.akka.commons.WeatherCondition;
import fhv.aktor.akka.commons.WeatherConditionAdapter;

public class DefaultWeatherConditionConverter implements WeatherConditionAdapter {

    @Override
    public WeatherCondition convert(String weatherString) {
        if (weatherString == null || weatherString.trim().isEmpty()) {
            return WeatherCondition.CLEAR;
        }

        String normalized = weatherString.trim().toUpperCase();

        if (normalized.contains("SUNNY") || normalized.contains("FAIR") ||
                normalized.contains("BRIGHT") || normalized.contains("CLEAR")) {
            return WeatherCondition.CLEAR;
        } else if (normalized.contains("PARTLY") && normalized.contains("CLOUD") ||
                normalized.contains("FEW CLOUDS") || normalized.contains("LIGHT CLOUD")) {
            return WeatherCondition.LIGHTLY_CLOUDED;
        } else if (normalized.contains("CLOUD") || normalized.contains("OVERCAST") ||
                normalized.contains("GRAY") || normalized.contains("GREY")) {
            return WeatherCondition.CLOUDED;
        } else if (normalized.contains("HEAVY CLOUD") || normalized.contains("THICK CLOUD") ||
                normalized.contains("DARK CLOUD") || normalized.contains("DENSE CLOUD")) {
            return WeatherCondition.STRONGLY_CLOUDED;
        } else if (normalized.contains("DRIZZLE") || normalized.contains("LIGHT RAIN") ||
                normalized.contains("SLIGHT RAIN") || normalized.contains("SPRINKLE")) {
            return WeatherCondition.LIGHT_RAIN;
        } else if (normalized.contains("RAIN") && !normalized.contains("HEAVY")) {
            return WeatherCondition.RAIN;
        } else if (normalized.contains("HEAVY RAIN") || normalized.contains("DOWNPOUR") ||
                normalized.contains("TORRENTIAL") || normalized.contains("DELUGE")) {
            return WeatherCondition.HEAVY_RAIN;
        } else if (normalized.contains("HAIL") || normalized.contains("ICE PELLET") ||
                normalized.contains("SLEET") || normalized.contains("HAILSTORM")) {
            return WeatherCondition.HAILSTORM;
        } else if (normalized.contains("SNOW") || normalized.contains("FLURRY") ||
                normalized.contains("BLIZZARD") || normalized.contains("FLAKE")) {
            return WeatherCondition.SNOW;
        } else if (normalized.contains("STORM") || normalized.contains("THUNDER")) {
            if (normalized.contains("HAIL")) {
                return WeatherCondition.HAILSTORM;
            } else {
                return WeatherCondition.HEAVY_RAIN;
            }
        }

        if (normalized.contains("CLOUD")) {
            return WeatherCondition.CLOUDED;
        } else if (normalized.contains("RAIN")) {
            return WeatherCondition.RAIN;
        }

        return WeatherCondition.CLEAR;
    }
}
