package com.bank.gui;

import com.bank.model.Bank;
import com.bank.repository.DataStore;

import javax.swing.*;
import java.awt.*;

/**
 * ============================================================================
 * GUI COORDINATOR: GUIForm (Matching Reference Architecture)
 * ============================================================================
 * Manages Swing Window navigation and global Bank instance state.
 */
public class GUIForm {

    public static Bank bank = new Bank("NovaCore Bank of India", "NOVA0001001");
    public static LoginFrame loginFrame;
    public static MainMenuFrame menuFrame;
    public static AddAccountFrame addAccountFrame;
    public static DepositFrame depositFrame;
    public static WithdrawFrame withdrawFrame;
    public static TransferFrame transferFrame;
    public static DisplayAccountsFrame displayAccountsFrame;

    static {
        try {
            // Apply native System Look and Feel for modern UI rendering
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    public static void init() {
        DataStore.getInstance(); // ensure data is loaded
        bank.syncWithDataStore();

        loginFrame = new LoginFrame();
        menuFrame = new MainMenuFrame();
        addAccountFrame = new AddAccountFrame();
        depositFrame = new DepositFrame();
        withdrawFrame = new WithdrawFrame();
        transferFrame = new TransferFrame();
        displayAccountsFrame = new DisplayAccountsFrame();
    }

    public static void showLogin() {
        if (loginFrame == null) init();
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    public static void showMenu(String username, String role) {
        if (menuFrame == null) init();
        menuFrame.setUserContext(username, role);
        menuFrame.setLocationRelativeTo(null);
        menuFrame.setVisible(true);
        if (loginFrame != null) loginFrame.setVisible(false);
    }

    public static void updateDisplay() {
        bank.syncWithDataStore();
        if (displayAccountsFrame != null && displayAccountsFrame.isVisible()) {
            displayAccountsFrame.refreshTable();
        }
    }
}
