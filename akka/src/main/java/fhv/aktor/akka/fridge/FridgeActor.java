package fhv.aktor.akka.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.fridge.command.ConsumeProduct;
import fhv.aktor.akka.fridge.command.OrderProduct;
import fhv.aktor.akka.fridge.command.ReceiveProducts;
import fhv.aktor.akka.fridge.command.query.OrderHistoryResponse;
import fhv.aktor.akka.fridge.command.query.ProductsResponse;
import fhv.aktor.akka.fridge.command.query.QueryOrders;
import fhv.aktor.akka.fridge.command.query.QueryProducts;
import fhv.aktor.akka.fridge.command.value.Order;
import fhv.aktor.akka.order.OrderCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FridgeActor extends AbstractBehavior<FridgeCommand> {
    private final ItemRegistry itemRegistry;
    private final Map<String, Integer> itemQuantities = new HashMap<>();
    private final List<Order> orderHistory = new ArrayList<>();
    private ActorRef<OrderCommand> orderRef;

    protected FridgeActor(ActorContext<FridgeCommand> context, ActorRef<OrderCommand> orderRef, ItemRegistry itemRegistry) {
        super(context);
        this.orderRef = orderRef;
        this.itemRegistry = itemRegistry;
    }

    public static Behavior<FridgeCommand> create(ActorRef<OrderCommand> orderRef, ItemRegistry itemRegistry) {
        return Behaviors.setup(context -> new FridgeActor(context, orderRef, itemRegistry));
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetOrderRef.class, this::setOrderRef)
                .onMessage(OrderProduct.class, this::onOrderProduct)
                .onMessage(ReceiveProducts.class, this::receiveProduct)
                .onMessage(QueryProducts.class, this::onQueryProducts)
                .onMessage(QueryOrders.class, this::onQueryOrders)
                .onMessage(ConsumeProduct.class, this::consumeProduct)
                .build();
    }

    private Behavior<FridgeCommand> setOrderRef(SetOrderRef setOrderRef) {
        this.orderRef = setOrderRef.orderRef();

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onOrderProduct(OrderProduct orderProduct) {
        if (orderRef != null) {
            orderRef.tell(orderProduct);
        } else {
            getContext().getLog().info("No order ref available");
        }

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> receiveProduct(ReceiveProducts receiveProducts) {
        for (Map.Entry<String, Integer> entry : itemQuantities.entrySet()) {
            if (!itemQuantities.containsKey(entry.getKey())) {
                itemQuantities.put(entry.getKey(), entry.getValue());
            } else {
                itemQuantities.compute(entry.getKey(), (k, currentQuantity) -> currentQuantity + entry.getValue());
            }
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
        orderRef.tell(new OrderProduct(Map.of(name, quantity)));
    }
}
