package fhv.aktor.akka;

public class SystemSettings {
    private final boolean internalWeatherSimulation;
    private final boolean internalTemperatureSimulation;
    private final int updateCycle;

    public SystemSettings(int updateCycle) {
        this(false, updateCycle);
    }

    public SystemSettings(boolean internalSimulation) {
        this(internalSimulation, 10);
    }

    public SystemSettings(boolean internalSimulation, int updateCycle) {
        this(internalSimulation, internalSimulation, updateCycle);
    }

    public SystemSettings(boolean internalWeatherSimulation, boolean internalTemperatureSimulation, int updateCycle) {
        this.internalWeatherSimulation = internalWeatherSimulation;
        this.internalTemperatureSimulation = internalTemperatureSimulation;
        this.updateCycle = updateCycle;
    }

    public boolean internalWeatherSimulation() {
        return internalWeatherSimulation;
    }

    public boolean internalTemperatureSimulation() {
        return internalTemperatureSimulation;
    }

    public int updateCycle() {
        return updateCycle;
    }
}
