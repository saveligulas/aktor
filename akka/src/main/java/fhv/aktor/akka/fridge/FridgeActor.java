package fhv.aktor.akka.fridge;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import fhv.aktor.akka.fridge.command.ConsumeProduct;
import fhv.aktor.akka.fridge.command.OrderProducts;
import fhv.aktor.akka.fridge.command.ReceiveProducts;
import fhv.aktor.akka.fridge.command.query.OrderHistoryResponse;
import fhv.aktor.akka.fridge.command.query.ProductsResponse;
import fhv.aktor.akka.fridge.command.query.QueryOrders;
import fhv.aktor.akka.fridge.command.query.QueryProducts;
import fhv.aktor.akka.fridge.command.value.Order;
import fhv.aktor.akka.message.LoggingProvider;
import fhv.aktor.akka.message.MessageStatus;
import fhv.aktor.akka.order.OrderCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FridgeActor extends AbstractBehavior<FridgeCommand> {
    private final ItemRegistry itemRegistry;
    private final Map<String, Integer> itemQuantities = new HashMap<>();
    private final List<Order> orderHistory = new ArrayList<>();
    private final double MAX_WEIGHT;
    private final LoggingProvider.EnhancedLogger logger;
    private ActorRef<OrderCommand> orderRef;

    private FridgeActor(ActorContext<FridgeCommand> context, ActorRef<OrderCommand> orderRef, ItemRegistry itemRegistry, double maxWeight) {
        super(context);
        this.orderRef = orderRef;
        this.itemRegistry = itemRegistry;
        this.MAX_WEIGHT = maxWeight;
        this.logger = LoggingProvider.withContext(getContext().getLog());
    }

    public static Behavior<FridgeCommand> create(ActorRef<OrderCommand> orderRef, ItemRegistry itemRegistry, double maxWeight) {
        return Behaviors.setup(context -> new FridgeActor(context, orderRef, itemRegistry, maxWeight));
    }

    @Override
    public Receive<FridgeCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetOrderRef.class, this::setOrderRef)
                .onMessage(OrderProducts.class, this::onOrderProducts)
                .onMessage(ReceiveProducts.class, this::receiveProduct)
                .onMessage(QueryProducts.class, this::onQueryProducts)
                .onMessage(QueryOrders.class, this::onQueryOrders)
                .onMessage(ConsumeProduct.class, this::consumeProduct)
                .build();
    }

    private Behavior<FridgeCommand> setOrderRef(SetOrderRef setOrderRef) {
        this.orderRef = setOrderRef.orderRef(); // dirty but works

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onOrderProducts(OrderProducts orderProducts) {
        if (orderRef != null) { // need to do this because dirty work before
            for (String item : orderProducts.productQuantities().keySet()) {
                if (!itemRegistry.exists(item)) {
                    logger.error("Order contains item that is not registered: " + item);
                    return Behaviors.same();
                }
            }

            double weight = getWeight();
            double orderWeight = calcWeightFromItemQuantities(orderProducts.productQuantities());
            double futureWeight = orderWeight + weight;

            if (futureWeight > MAX_WEIGHT) {
                logger.error("Order would exceed max weight of fridge: current=" + weight + " - " + "future=" + futureWeight + " - " + "max=" + MAX_WEIGHT, MessageStatus.INVALID_INPUT);
                return Behaviors.same();
            }

            orderHistory.add(new Order(orderProducts.productQuantities()));
            orderRef.tell(orderProducts);
        } else {
            logger.info("No order ref available");
        }

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> receiveProduct(ReceiveProducts receiveProducts) {
        for (Map.Entry<String, Integer> entry : receiveProducts.productQuantities().entrySet()) {
            if (!itemQuantities.containsKey(entry.getKey())) {
                itemQuantities.put(entry.getKey(), entry.getValue());
            } else {
                itemQuantities.compute(entry.getKey(), (k, currentQuantity) -> currentQuantity + entry.getValue());
            }
        }
        logger.info("Received product of " + itemQuantities + " items");

        return Behaviors.same();
    }

    private Behavior<FridgeCommand> onQueryProducts(QueryProducts queryProducts) {
        queryProducts.replyTo().tell(new ProductsResponse(itemQuantities));

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
            logger.info("Restocked product of " + itemName);
        } else {
            itemQuantities.put(itemName, currentQuantity - quantity);
        }

        logger.info("Consumed product of " + itemName + " with quantity " + quantity);

        return Behaviors.same();
    }

    private void restockProduct(String name, int quantity) {
        orderRef.tell(new OrderProducts(Map.of(name, quantity)));
    }

    private double calcWeightFromItemQuantities(Map<String, Integer> itemQuantities) {
        return itemQuantities.entrySet().stream().mapToDouble(entry -> itemRegistry.getWeight(entry.getKey()) * entry.getValue()).sum();
    }

    private double getWeight() { // could have this as field and update after every receive and consume
        return this.itemQuantities.entrySet().stream().mapToDouble(entry -> itemRegistry.getWeight(entry.getKey()) * entry.getValue()).sum();
    }
}
