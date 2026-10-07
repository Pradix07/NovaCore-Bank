package com.bank.gui;

import com.bank.model.Account;
import com.bank.repository.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * ============================================================================
 * SWING GUI: DepositFrame (Matching Reference Architecture)
 * ============================================================================
 * Deposits funds into any NovaCore Bank of India account with instant credit.
 */
public class DepositFrame extends JFrame {

    private final JComboBox<String> cmbAccount = new JComboBox<>();
    private final JTextField txtAmount = new JTextField("5000.00", 15);
    private final JTextField txtMemo = new JTextField("Cash Deposit at Branch", 15);
    private final JLabel lblResult = new JLabel(" ", SwingConstants.CENTER);

    public DepositFrame() {
        setTitle("Deposit Funds — NovaCore Bank of India");
        setSize(480, 380);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel();
        header.setBackground(new Color(16, 185, 129)); // Emerald Green
        header.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("💰 Deposit Money into Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(new EmptyBorder(15, 25, 15, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Target Account:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        loadAccounts();
        form.add(cmbAccount, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Amount (₹):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        form.add(txtAmount, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Remarks / Source:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        form.add(txtMemo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblResult, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setOpaque(false);

        JButton btnDeposit = new JButton("Process Deposit");
        btnDeposit.setBackground(new Color(16, 185, 129));
        btnDeposit.setForeground(Color.WHITE);
        btnDeposit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDeposit.addActionListener(e -> handleDeposit());

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> setVisible(false));

        btnPanel.add(btnDeposit);
        btnPanel.add(btnClose);
        form.add(btnPanel, gbc);

        add(form, BorderLayout.CENTER);
    }

    private void loadAccounts() {
        cmbAccount.removeAllItems();
        for (Account a : DataStore.getInstance().getAllAccounts()) {
            cmbAccount.addItem(a.getAccountNumber() + " (" + a.getAccountType() + " - ₹" + String.format("%,.2f", a.getBalance()) + ")");
        }
    }

    private void handleDeposit() {
        try {
            String sel = (String) cmbAccount.getSelectedItem();
            if (sel == null) {
                lblResult.setText("Please select an account.");
                return;
            }
            String accNum = sel.split(" ")[0].trim();
            double amt = Double.parseDouble(txtAmount.getText().trim());

            GUIForm.bank.deposit(accNum, amt);

            Account updated = GUIForm.bank.findAccount(accNum);
            lblResult.setForeground(new Color(16, 185, 129));
            lblResult.setText("✓ Credited ₹" + String.format("%,.2f", amt) + ". New Balance: ₹" + String.format("%,.2f", updated.getBalance()));

            loadAccounts();
            GUIForm.updateDisplay();
            if (GUIForm.menuFrame != null) GUIForm.menuFrame.refreshStats();

        } catch (Exception ex) {
            lblResult.setForeground(new Color(220, 38, 38));
            lblResult.setText("Error: " + ex.getMessage());
        }
    }
}
