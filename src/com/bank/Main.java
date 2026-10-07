package com.bank;

import com.bank.repository.DataStore;
import com.bank.server.BankHttpServer;

public class Main {

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("Initializing NovaCore Banking Data Store...");
        DataStore.getInstance(); // Trigger repository and initial seed load

        BankHttpServer server = new BankHttpServer(port);
        try {
            server.start();

            // Register JVM shutdown hook to persist all data before terminating
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Persisting in-memory database to disk...");
                DataStore.getInstance().saveToFile();
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("Fatal: Could not start banking HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
