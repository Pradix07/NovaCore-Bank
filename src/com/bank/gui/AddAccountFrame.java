package com.bank.gui;

import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.model.User;
import com.bank.repository.DataStore;
import com.bank.service.AccountService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * ============================================================================
 * SWING GUI: AddAccountFrame (Matching Reference Architecture)
 * ============================================================================
 * Dedicated dialog to open Savings, Current (GSTIN), or Student Accounts.
 */
public class AddAccountFrame extends JFrame {

    private final JComboBox<String> cmbAccountType = new JComboBox<>(new String[]{"SAVINGS - Standard Individual (4.0% p.a.)", "CURRENT - Commercial / Business (with GSTIN)", "STUDENT - College / University (Zero Balance)"});
    private final JComboBox<String> cmbCustomer = new JComboBox<>();
    private final JTextField txtInitialDeposit = new JTextField("1000.00", 15);
    
    // Dynamic fields for Current / Student
    private final JLabel lblExtra1 = new JLabel("Specialized Info 1:");
    private final JTextField txtExtra1 = new JTextField(15);
    private final JLabel lblExtra2 = new JLabel("Specialized Info 2:");
    private final JTextField txtExtra2 = new JTextField(15);

    private final JLabel lblMsg = new JLabel(" ", SwingConstants.CENTER);
    private final AccountService accountService = new AccountService();

    public AddAccountFrame() {
        setTitle("Open New Bank Account — NovaCore Bank of India");
        setSize(520, 480);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel();
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("➕ Open New Indian Bank Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title);
        add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(248, 250, 252));
        form.setBorder(new EmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Account Type
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Account Type:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        cmbAccountType.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbAccountType.addItemListener(this::onTypeChanged);
        form.add(cmbAccountType, gbc);

        // Customer
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Select Customer:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        loadCustomers();
        form.add(cmbCustomer, gbc);

        // Initial Deposit
        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Initial Deposit (₹):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        form.add(txtInitialDeposit, gbc);

        // Extra 1
        gbc.gridx = 0; gbc.gridy = 3;
        lblExtra1.setText("Nominee / Reference:");
        form.add(lblExtra1, gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        form.add(txtExtra1, gbc);

        // Extra 2
        gbc.gridx = 0; gbc.gridy = 4;
        lblExtra2.setText("Remarks:");
        form.add(lblExtra2, gbc);
        gbc.gridx = 1; gbc.gridy = 4;
        form.add(txtExtra2, gbc);

        // Message
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMsg.setForeground(new Color(16, 185, 129));
        form.add(lblMsg, gbc);

        // Buttons
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setOpaque(false);

        JButton btnSubmit = new JButton("Create Account Now");
        btnSubmit.setBackground(new Color(37, 99, 235));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSubmit.addActionListener(e -> handleCreateAccount());

        JButton btnCancel = new JButton("Close");
        btnCancel.addActionListener(e -> setVisible(false));

        btnPanel.add(btnSubmit);
        btnPanel.add(btnCancel);
        form.add(btnPanel, gbc);

        add(form, BorderLayout.CENTER);
    }

    private void loadCustomers() {
        cmbCustomer.removeAllItems();
        for (User u : DataStore.getInstance().getAllUsers()) {
            if (u instanceof Customer) {
                cmbCustomer.addItem(u.getId() + " - " + u.getFullName());
            }
        }
    }

    private void onTypeChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            int idx = cmbAccountType.getSelectedIndex();
            if (idx == 0) { // Savings
                txtInitialDeposit.setText("1000.00");
                lblExtra1.setText("Nominee Name:");
                lblExtra2.setText("Branch City:");
                txtExtra1.setText("Family Relative");
                txtExtra2.setText("Mumbai Central");
            } else if (idx == 1) { // Current
                txtInitialDeposit.setText("5000.00");
                lblExtra1.setText("GSTIN / Trade License:");
                lblExtra2.setText("Business / Enterprise Name:");
                txtExtra1.setText("27AAAPA1234B1Z5");
                txtExtra2.setText("Alpha Commercial LLP");
            } else if (idx == 2) { // Student
                txtInitialDeposit.setText("500.00");
                lblExtra1.setText("Institution / University:");
                lblExtra2.setText("Student Roll / ID Number:");
                txtExtra1.setText("IIT Bombay / Delhi University");
                txtExtra2.setText("STU-2026-9021");
            }
        }
    }

    private void handleCreateAccount() {
        try {
            String selectedCust = (String) cmbCustomer.getSelectedItem();
            if (selectedCust == null) {
                lblMsg.setText("Please select a customer.");
                return;
            }
            String customerId = selectedCust.split(" - ")[0].trim();
            double deposit = Double.parseDouble(txtInitialDeposit.getText().trim());

            int idx = cmbAccountType.getSelectedIndex();
            String accType = (idx == 1) ? "CURRENT" : (idx == 2 ? "STUDENT" : "SAVINGS");

            String extra1 = txtExtra1.getText().trim();
            String extra2 = txtExtra2.getText().trim();

            Account created = accountService.openNewAccountDetailed(customerId, accType, deposit, extra1, extra2);

            lblMsg.setForeground(new Color(16, 185, 129));
            lblMsg.setText("✓ " + accType + " Acc #" + created.getAccountNumber() + " created! (IFSC: " + created.getIfscCode() + ")");

            GUIForm.updateDisplay();
            if (GUIForm.menuFrame != null) GUIForm.menuFrame.refreshStats();

        } catch (Exception ex) {
            lblMsg.setForeground(new Color(220, 38, 38));
            lblMsg.setText("Error: " + ex.getMessage());
        }
    }
}
