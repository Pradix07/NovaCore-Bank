package com.bank.gui;

import com.bank.model.Account;
import com.bank.repository.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * ============================================================================
 * SWING GUI: WithdrawFrame (Matching Reference Architecture)
 * ============================================================================
 * Withdraws funds subject to account-type specific limits & minimum balance rules.
 */
public class WithdrawFrame extends JFrame {

    private final JComboBox<String> cmbAccount = new JComboBox<>();
    private final JTextField txtAmount = new JTextField("2000.00", 15);
    private final JTextField txtMemo = new JTextField("Branch ATM Cash Withdrawal", 15);
    private final JLabel lblResult = new JLabel(" ", SwingConstants.CENTER);

    public WithdrawFrame() {
        setTitle("Withdraw Funds — NovaCore Bank of India");
        setSize(480, 380);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel();
        header.setBackground(new Color(217, 119, 6)); // Amber / Warm Orange
        header.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("💸 Cash / ATM Withdrawal");
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
        form.add(new JLabel("Source Account:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        loadAccounts();
        form.add(cmbAccount, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Amount (₹):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        form.add(txtAmount, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Purpose / Channel:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        form.add(txtMemo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblResult, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setOpaque(false);

        JButton btnWithdraw = new JButton("Authorize Withdrawal");
        btnWithdraw.setBackground(new Color(217, 119, 6));
        btnWithdraw.setForeground(Color.WHITE);
        btnWithdraw.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnWithdraw.addActionListener(e -> handleWithdraw());

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> setVisible(false));

        btnPanel.add(btnWithdraw);
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

    private void handleWithdraw() {
        try {
            String sel = (String) cmbAccount.getSelectedItem();
            if (sel == null) {
                lblResult.setText("Please select an account.");
                return;
            }
            String accNum = sel.split(" ")[0].trim();
            double amt = Double.parseDouble(txtAmount.getText().trim());

            GUIForm.bank.withdraw(accNum, amt);

            Account updated = GUIForm.bank.findAccount(accNum);
            lblResult.setForeground(new Color(16, 185, 129));
            lblResult.setText("✓ Debited ₹" + String.format("%,.2f", amt) + ". Remaining Balance: ₹" + String.format("%,.2f", updated.getBalance()));

            loadAccounts();
            GUIForm.updateDisplay();
            if (GUIForm.menuFrame != null) GUIForm.menuFrame.refreshStats();

        } catch (Exception ex) {
            lblResult.setForeground(new Color(220, 38, 38));
            lblResult.setText("Error: " + ex.getMessage());
        }
    }
}
