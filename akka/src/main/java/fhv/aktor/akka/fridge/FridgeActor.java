package fhv.aktor.akka.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.fridge.command.receiveProduct;
import fhv.aktor.akka.fridge.command.query.OrderHistoryResponse;
import fhv.aktor.akka.fridge.command.query.ProductsResponse;
import fhv.aktor.akka.fridge.command.value.Order;
import fhv.aktor.akka.fridge.command.query.QueryOrders;
import fhv.aktor.akka.fridge.command.query.QueryProducts;
import fhv.aktor.akka.order.OrderCommand;
import fhv.aktor.akka.order.OrderProduct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FridgeActor extends AbstractBehavior<FridgeCommand> {
    private final ActorRef<OrderCommand> orderRef;
    private final ItemRegistry itemRegistry;
    private final Map<String, Integer> itemQuantities = new HashMap<>();
    private final List<Order> orderHistory = new ArrayList<>();

    public static Behavior<FridgeCommand> create(ActorRef<OrderCommand> orderRef, ItemRegistry itemRegistry) {
        return Behaviors.setup(context -> new FridgeActor(context, orderRef, itemRegistry));
    }

    protected FridgeActor(ActorContext<FridgeCommand> context, ActorRef<OrderCommand> orderRef, ItemRegistry itemRegistry) {
        super(context);
        this.orderRef = orderRef;
        this.itemRegistry = itemRegistry;
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(receiveProduct.class, this::receiveProduct)
                .onMessage(QueryProducts.class, this::onQueryProducts)
                .onMessage(QueryOrders.class, this::onQueryOrders)
                .onMessage(ConsumeProduct.class, this::consumeProduct)
                .build();
    }

    private Behavior<FridgeCommand> receiveProduct(receiveProduct receiveProduct) {
        if (!itemRegistry.exists(receiveProduct.itemName())) {
            itemQuantities.put(receiveProduct.itemName(), receiveProduct.quantity());
        } else {
            itemQuantities.compute(receiveProduct.itemName(), (k, currentQuantity) -> currentQuantity + receiveProduct.quantity());
        }

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onQueryProducts(QueryProducts queryProducts) {
        queryProducts.replyTo().tell(new ProductsResponse(itemQuantities.keySet().stream().toList()));

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onQueryOrders(QueryOrders queryOrders) {
        queryOrders.replyTo().tell(new OrderHistoryResponse(orderHistory));

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> consumeProduct(ConsumeProduct consumeProduct) {
        String itemName = consumeProduct.itemName();
        int quantity = consumeProduct.quantity();

        if (!itemRegistry.exists(itemName)) {
            throw new IllegalArgumentException("Item does not exist");
        }

        if (!itemQuantities.containsKey(itemName)) {
            throw new IllegalArgumentException("Item is not in the fridge");
        }

        int currentQuantity = itemQuantities.get(itemName);

        if (currentQuantity < quantity) {
            throw new IllegalArgumentException("Not enough items in the fridge");
        }

        if (currentQuantity == quantity) {
            itemQuantities.remove(itemName);
            restockProduct(itemName, 1);
        } else {
            itemQuantities.put(itemName, currentQuantity - quantity);
        }

        return Behaviors.same();
    }

    private void restockProduct(String name, int quantity) {
        orderRef.tell(new OrderProduct(name, quantity));
    }
}
