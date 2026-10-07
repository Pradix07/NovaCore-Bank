package com.bank.gui;

import com.bank.model.Account;
import com.bank.repository.DataStore;
import com.bank.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/**
 * ============================================================================
 * SWING GUI: TransferFrame (Indian Banking Payment Rails)
 * ============================================================================
 * Executes atomic transfers across accounts via UPI, IMPS, NEFT, and RTGS.
 */
public class TransferFrame extends JFrame {

    private final JComboBox<String> cmbFromAccount = new JComboBox<>();
    private final JTextField txtToAccountOrUpi = new JTextField("10010003003", 16);
    private final JComboBox<String> cmbMode = new JComboBox<>(new String[]{"UPI - Instant 24x7 (< ₹1,00,000)", "IMPS - Immediate Payment Service (24x7)", "NEFT - National Electronic Fund Transfer", "RTGS - Real Time Gross Settlement (>= ₹2,00,000)"});
    private final JTextField txtIfsc = new JTextField("NOVA0001001", 16);
    private final JTextField txtAmount = new JTextField("5000.00", 16);
    private final JPasswordField txtPin = new JPasswordField("1234", 6);
    private final JLabel lblResult = new JLabel(" ", SwingConstants.CENTER);

    private final TransactionService transactionService = new TransactionService();

    public TransferFrame() {
        setTitle("Instant Funds Transfer — NovaCore Bank of India");
        setSize(540, 480);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel();
        header.setBackground(new Color(124, 58, 237)); // Royal Purple
        header.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("⚡ Instant Inter-Bank & UPI Funds Transfer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(new EmptyBorder(15, 25, 15, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("From Account:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        loadAccounts();
        form.add(cmbFromAccount, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("To Account / UPI ID:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        form.add(txtToAccountOrUpi, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Transfer Protocol:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        form.add(cmbMode, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        form.add(new JLabel("Recipient IFSC Code:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        form.add(txtIfsc, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        form.add(new JLabel("Amount (₹):"), gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        form.add(txtAmount, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        form.add(new JLabel("4-Digit Security PIN:"), gbc);
        gbc.gridx = 1; gbc.gridy = 5;
        form.add(txtPin, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        lblResult.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblResult, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setOpaque(false);

        JButton btnTransfer = new JButton("Authorize & Send Money");
        btnTransfer.setBackground(new Color(124, 58, 237));
        btnTransfer.setForeground(Color.WHITE);
        btnTransfer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnTransfer.addActionListener(e -> handleTransfer());

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> setVisible(false));

        btnPanel.add(btnTransfer);
        btnPanel.add(btnClose);
        form.add(btnPanel, gbc);

        add(form, BorderLayout.CENTER);
    }

    private void loadAccounts() {
        cmbFromAccount.removeAllItems();
        for (Account a : DataStore.getInstance().getAllAccounts()) {
            cmbFromAccount.addItem(a.getAccountNumber() + " (" + a.getAccountType() + " - ₹" + String.format("%,.2f", a.getBalance()) + ")");
        }
    }

    private void handleTransfer() {
        try {
            String fromSel = (String) cmbFromAccount.getSelectedItem();
            if (fromSel == null) {
                lblResult.setText("Please select a source account.");
                return;
            }
            String fromAcc = fromSel.split(" ")[0].trim();
            String toAcc = txtToAccountOrUpi.getText().trim();
            double amt = Double.parseDouble(txtAmount.getText().trim());
            String pin = new String(txtPin.getPassword()).trim();
            String ifsc = txtIfsc.getText().trim();

            int modeIdx = cmbMode.getSelectedIndex();
            String mode = (modeIdx == 0) ? "UPI" : (modeIdx == 1 ? "IMPS" : (modeIdx == 2 ? "NEFT" : "RTGS"));

            Account src = DataStore.getInstance().getAccountByNumber(fromAcc);
            if (src == null) {
                lblResult.setText("Source account not found.");
                return;
            }

            Map<String, Object> res = transactionService.transferFundsDetailed(
                    src.getCustomerId(),
                    fromAcc,
                    toAcc,
                    amt,
                    mode,
                    ifsc,
                    mode + " Transfer to " + toAcc,
                    pin
            );

            lblResult.setForeground(new Color(16, 185, 129));
            lblResult.setText("✓ " + mode + " Transfer Successful! Ref: " + res.get("referenceNumber"));

            loadAccounts();
            GUIForm.updateDisplay();
            if (GUIForm.menuFrame != null) GUIForm.menuFrame.refreshStats();

        } catch (Exception ex) {
            lblResult.setForeground(new Color(220, 38, 38));
            lblResult.setText("Error: " + ex.getMessage());
        }
    }
}
