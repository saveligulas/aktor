package fhv.aktor.akka.fridge;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private final Map<String, Double> weightRegistry  = new HashMap<>();

    public static ItemRegistry withDefaults() {
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.registerItem("milk_gallon", 3.6);
        itemRegistry.registerItem("bread", 0.7);
        itemRegistry.registerItem("egg", 0.05);
        itemRegistry.registerItem("milk_chocolate_drink", 0.1);
        itemRegistry.registerItem("watermelon", 2.5);
        itemRegistry.registerItem("apple", 0.2);
        itemRegistry.registerItem("doener", 0.65);
        return itemRegistry;
    }

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
