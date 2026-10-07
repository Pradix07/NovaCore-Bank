package com.bank.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class BankHttpServer {

    private final int port;
    private HttpServer server;

    public BankHttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Core Java Multi-threading: Thread pool for concurrent HTTP connection handling
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);
        server.setExecutor(threadPool);

        // API Endpoint Context
        server.createContext("/api", new ApiHandler());

        // Static Web UI Files Context
        server.createContext("/", new StaticFileHandler("web"));

        server.start();

        System.out.println("===============================================================");
        System.out.println("   ONLINE BANKING SYSTEM - CORE JAVA BACKEND INITIALIZED       ");
        System.out.println("===============================================================");
        System.out.println(">> Server running at : http://localhost:" + port);
        System.out.println(">> Admin Portal      : http://localhost:" + port + "/admin-dashboard.html");
        System.out.println(">> Customer Portal   : http://localhost:" + port + "/customer-dashboard.html");
        System.out.println(">> Active Threads    : " + threadPool.getPoolSize());
        System.out.println("===============================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println(">> Banking server shut down cleanly.");
        }
    }
}
