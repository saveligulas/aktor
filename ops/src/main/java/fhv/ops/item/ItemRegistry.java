package fhv.ops.item;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private final Map<String, Double> priceRegistry = new HashMap<>();

    public static ItemRegistry withDefaults() {
        ItemRegistry itemRegistry = new ItemRegistry();
        itemRegistry.registerItem("milk_gallon", 4.50);
        itemRegistry.registerItem("bread", 1.20);
        itemRegistry.registerItem("egg", 0.40);
        itemRegistry.registerItem("milk_chocolate_drink", 1.80);
        itemRegistry.registerItem("watermelon", 6.80);
        itemRegistry.registerItem("apple", 0.80);
        itemRegistry.registerItem("doener", 8.50);
        return itemRegistry;
    }

    public void registerItem(String itemName, double price) {
        priceRegistry.put(itemName, price);
    }

    public boolean exists(String name) {
        return priceRegistry.containsKey(name);
    }

    public double getPrice(String name) {
        if (!priceRegistry.containsKey(name)) {
            throw new IllegalArgumentException("No such item: " + name);
        }
        return priceRegistry.get(name);
    }
}
