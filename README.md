# Home Automation System with Akka

This project implements a home automation system using the Akka framework, following the Actor Model paradigm. The system includes a living room with a kitchen containing various sensors and actuators.

## Project Structure

- `akka/` - Main home automation system implementation
- `ops/` - External order processing system connected via GRPC

## Features

- Environment simulation with temperature and weather conditions
- Sensors measuring the environment (Temperature and Weather)
- Actuators responding to environment changes (AC, Blinds)
- Media Station for playing movies
- Fridge for managing products with automatic reordering
- External order processing system using GRPC
- MQTT connection for external weather simulation data
- Web UI for interacting with the system

## Architecture

The system implements a Blackboard pattern where various actors communicate through a central BlackboardActor. This allows for decoupled components that can observe and react to changes in the system state.

### Key Components

1. **BlackboardActor**: Central knowledge base that other actors can post to and observe
2. **Sensors**: Temperature and Weather sensors that measure environmental values
3. **Actuators**: AC and Blinds that respond to sensor readings
4. **Media Station**: Controls movie playback and affects blinds state
5. **Fridge**: Manages products with weight and capacity constraints
6. **Order Processor**: External system for processing product orders

### Interaction Patterns

The implementation uses various actor interaction patterns:

- **Scheduled Self-Messages**: Used by environment simulators for periodic updates
```java
getContext().scheduleOnce(
    Duration.ofSeconds(5),
    getContext().getSelf(),
    new UpdateTemperature(random.nextDouble(0, 40))
);
```

- **Request-Response with Ask**: Used for querying the blackboard
```java
blackboardRef.tell(new QueryBlackboard<>(BlackboardField.TEMPERATURE.key(), temperatureResponse, context.getSelf()));
```

- **Fire and Forget**: Used by actuators responding to sensor values
```java
blackboardRef.tell(new PostValue(state, BlackboardField.AC.key()));
```

- **Per Session Child Actor**: Used for order processing
```java
String handlerName = "order-handler-" + UUID.randomUUID();
ActorRef<OrderProduct> handler = getContext().spawn(
        OrderHandlerActor.create(itemRegistry),
        handlerName
);
```

## Rules Implementation

The system enforces the following automation rules:

- Blinds close when weather is sunny or a movie is playing
- Blinds open when weather is not sunny (unless a movie is playing)
- AC starts cooling when temperature is above 20°C and turns off when below
- Fridge checks capacity and weight constraints before processing orders
- Products are automatically reordered when they run out

## Running the Application

1. Start the Order Processor system first:
```
cd ops
./gradlew run
```

2. Start the Home Automation system:
```
cd akka
./gradlew run
```

## Accessing the UI

The web interface is available at: http://localhost:8081

### UI Features:

- Control the Media Station (play/stop(to be implemented) movies)
- View and modify environment values (temperature, weather)
- Order and consume products from the Fridge
- View order history and current fridge contents

## Command Interface

The system provides a command-line interface with the following commands:

- `play` - Play a movie on the Media Station
- `order [product1] [quantity1] [product2] [quantity2] ...` - Order products for the Fridge
  Example: `order milk 2 bread 1 cheese 3`
- `consume [product] [quantity]` - Consume products from the Fridge
  Example: `consume milk 1`
- `view` - View current products in the Fridge
- `orders` - View order history from the Fridge

### GRPC Service (Port 50051)

The external order processor system exposes a GRPC service on port 50051 with the following method:
- `order` - Process a product order and return a receipt

### MQTT Integration

The system can connect to an external MQTT broker at `10.0.40.161:1883` to receive environmental data on the following topics:
- `weather/temperature` - Temperature readings
- `weather/condition` - Weather condition updates
