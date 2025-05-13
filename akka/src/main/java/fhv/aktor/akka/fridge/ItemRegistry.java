package fhv.aktor.akka.fridge;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private final Map<String, Double> weightRegistry  = new HashMap<>();

    public void registerItem(String itemName, double weight) {
        weightRegistry.put(itemName, weight);
    }

    public boolean exists(String name) {
        return weightRegistry.containsKey(name);
    }

    public double getWeight(String name) {
        if (!weightRegistry.containsKey(name)) {
            throw new IllegalArgumentException("No such item: " + name);
        }
        return weightRegistry.get(name);
    }
}
