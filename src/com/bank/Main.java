package com.bank;

import com.bank.gui.GUIForm;
import com.bank.repository.DataStore;
import com.bank.server.BankHttpServer;

import javax.swing.SwingUtilities;

/**
 * ============================================================================
 * NOVACORE BANK OF INDIA - MAIN LAUNCHER
 * ============================================================================
 * Supports dual deployment modes:
 * 1. Web REST Server & Web Portal (http://localhost:8080)
 * 2. Interactive Desktop Java Swing GUI Application
 * 3. Unified Mode (runs both Web Server & Desktop GUI simultaneously)
 */
public class Main {

    public static void main(String[] args) {
        boolean launchGui = false;
        boolean serverOnly = true;
        int port = 8080;

        for (String arg : args) {
            if ("--gui".equalsIgnoreCase(arg) || "gui".equalsIgnoreCase(arg)) {
                launchGui = true;
                serverOnly = false;
            } else if ("--gui-only".equalsIgnoreCase(arg) || "gui-only".equalsIgnoreCase(arg)) {
                launchGui = true;
                serverOnly = false;
            } else {
                try {
                    port = Integer.parseInt(arg);
                } catch (NumberFormatException ignored) {}
            }
        }

        System.out.println("===============================================================");
        System.out.println("   🏦 NOVACORE BANK OF INDIA - CORE JAVA BANKING PLATFORM     ");
        System.out.println("   Reserve Bank of India (RBI) Compliant Architecture          ");
        System.out.println("===============================================================");
        System.out.println("Initializing Indian Banking Data Store...");
        DataStore.getInstance(); // Trigger repository and initial seed load

        if (launchGui) {
            System.out.println("Starting Desktop Java Swing GUI interface...");
            SwingUtilities.invokeLater(() -> {
                GUIForm.init();
                GUIForm.showLogin();
            });
        }

        // Unless specifically requested gui-only without server, always start HTTP Server
        boolean startHttp = true;
        for (String arg : args) {
            if ("--gui-only".equalsIgnoreCase(arg) || "gui-only".equalsIgnoreCase(arg)) {
                startHttp = false;
                break;
            }
        }

        if (startHttp) {
            BankHttpServer server = new BankHttpServer(port);
            try {
                server.start();

                // Register JVM shutdown hook to persist all data before terminating
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Persisting in-memory bank database to disk...");
                    DataStore.getInstance().saveToFile();
                    server.stop();
                }));

            } catch (Exception e) {
                System.err.println("Fatal: Could not start banking HTTP server: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
