package com.bank.gui;

import javax.swing.SwingUtilities;

/**
 * ============================================================================
 * MAIN SWING APPLICATION ENTRY POINT
 * ============================================================================
 * Launches the NovaCore Bank of India Desktop Swing GUI application.
 */
public class BankSwingApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                GUIForm.init();
                GUIForm.showLogin();
            } catch (Exception e) {
                System.err.println("Failed to initialize Swing GUI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
