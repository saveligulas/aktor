package fhv.ops;

import akka.actor.typed.ActorSystem;
import fhv.ops.actor.OrderProcessingSystem;
import fhv.ops.grpc.OrderProcessorImpl;
import fhv.ops.order.OrderCommand;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class OrderProcessorServer {

    private Server server;
    private ActorSystem<OrderCommand> actorSystem;

    public static void main(String[] args) throws IOException, InterruptedException {
        final OrderProcessorServer orderProcessorServer = new OrderProcessorServer();
        orderProcessorServer.start();
        orderProcessorServer.blockUntilShutdown();
    }

    private void start() throws IOException {
        actorSystem = OrderProcessingSystem.createOrderProcessorSystem();
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new OrderProcessorImpl())
                .build()
                .start();

        System.out.println("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** Shutting down gRPC server since JVM is shutting down ***");
            OrderProcessorServer.this.stop();
            System.err.println("*** Server shut down ***");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }

        if (actorSystem != null) {
            actorSystem.terminate();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
