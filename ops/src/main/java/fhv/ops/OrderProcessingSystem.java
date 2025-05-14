package fhv.ops;

import akka.actor.typed.ActorSystem;
import fhv.ops.item.ItemRegistry;
import fhv.ops.order.OrderCommand;

public class OrderProcessingSystem {
    public static void main(String[] args) {
        ActorSystem<OrderCommand> system = ActorSystem.create(OrderProcessorActor.create(ItemRegistry.withDefaults()), "ops-system");

        Runtime.getRuntime().addShutdownHook(new Thread(system::terminate));
    }
}
