package fhv.ops.actor;

import akka.actor.typed.ActorSystem;
import fhv.ops.order.OrderCommand;
import fhv.ops.registry.ItemRegistry;

public class OrderProcessingSystem {

    private static ActorSystem<OrderCommand> orderProcessingSystem;

    public static ActorSystem<OrderCommand> createOrderProcessorSystem() {
        if (orderProcessingSystem == null) {
            orderProcessingSystem = ActorSystem.create(
                    OrderProcessorActor.create(ItemRegistry.withDefaults()),
                    "orderProcessingSystem"
            );
        }
        return orderProcessingSystem;
    }

    public static ActorSystem<OrderCommand> getOrCreateSystem() {
        if (orderProcessingSystem == null) {
            return createOrderProcessorSystem();
        }
        return orderProcessingSystem;
    }

    public static void shutdown() {
        if (orderProcessingSystem != null) {
            orderProcessingSystem.terminate();
            orderProcessingSystem = null;
        }
    }
}
