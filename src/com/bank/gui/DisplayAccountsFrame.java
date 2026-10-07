package com.bank.gui;

import com.bank.model.Account;
import com.bank.model.User;
import com.bank.repository.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * ============================================================================
 * SWING GUI: DisplayAccountsFrame (Matching Reference Architecture)
 * ============================================================================
 * Displays formatted table of all active Indian Bank accounts and passbook records.
 */
public class DisplayAccountsFrame extends JFrame {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JTextField txtSearch = new JTextField(18);

    public DisplayAccountsFrame() {
        setTitle("Bank Account Registry & Passbook — NovaCore Bank of India");
        setSize(850, 480);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("📋 Master Account Directory & Live Balances");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);
        JLabel lblFind = new JLabel("Search Account / Name:");
        lblFind.setForeground(Color.WHITE);
        lblFind.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton btnSearch = new JButton("Filter");
        btnSearch.addActionListener(e -> refreshTable());

        searchPanel.add(lblFind);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        header.add(searchPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table
        String[] columns = {"Acc Number", "Account Type", "Holder Name", "Balance (₹)", "IFSC Code", "Branch", "UPI ID", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(226, 232, 240));

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        footer.setBackground(new Color(241, 245, 249));

        JButton btnRefresh = new JButton("🔄 Refresh Data");
        btnRefresh.addActionListener(e -> refreshTable());

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> setVisible(false));

        footer.add(btnRefresh);
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);

        refreshTable();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        String query = txtSearch.getText().trim().toLowerCase();

        List<Account> accounts = DataStore.getInstance().getAllAccounts();
        for (Account a : accounts) {
            User u = DataStore.getInstance().getUserById(a.getCustomerId());
            String holderName = u != null ? u.getFullName() : "Customer";

            if (!query.isEmpty()) {
                boolean matchAcc = a.getAccountNumber().toLowerCase().contains(query);
                boolean matchName = holderName.toLowerCase().contains(query);
                boolean matchType = a.getAccountType().toLowerCase().contains(query);
                boolean matchIfsc = a.getIfscCode() != null && a.getIfscCode().toLowerCase().contains(query);
                if (!matchAcc && !matchName && !matchType && !matchIfsc) {
                    continue;
                }
            }

            tableModel.addRow(new Object[]{
                    a.getAccountNumber(),
                    a.getAccountType(),
                    holderName,
                    String.format("₹%,.2f", a.getBalance()),
                    a.getIfscCode() != null ? a.getIfscCode() : "NOVA0001001",
                    a.getBranchName() != null ? a.getBranchName() : "Mumbai Fort",
                    a.getUpiId() != null ? a.getUpiId() : "-",
                    a.getStatus()
            });
        }
    }
}
