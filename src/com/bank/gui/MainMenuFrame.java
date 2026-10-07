package com.bank.gui;

import com.bank.model.Account;
import com.bank.repository.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * ============================================================================
 * SWING GUI: MainMenuFrame (Matching Reference Architecture)
 * ============================================================================
 * Central dashboard with quick action buttons for all Indian Banking operations.
 */
public class MainMenuFrame extends JFrame {

    private final JLabel lblUserContext = new JLabel("Welcome, Customer");
    private final JLabel lblBankStats = new JLabel("Loading bank records...");

    public MainMenuFrame() {
        setTitle("NovaCore Bank of India — Operations Dashboard");
        setSize(650, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initUI();
    }

    public void setUserContext(String fullName, String role) {
        lblUserContext.setText("👤 Authenticated: " + fullName + " (" + role + ") | IFSC: NOVA0001001");
        refreshStats();
    }

    private void initUI() {
        // Header Banner
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(15, 23, 42)); // Deep Slate Navy
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel lblTitle = new JLabel("🏦 NovaCore Bank of India — Core Banking Portal");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(248, 250, 252));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblUserContext.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUserContext.setForeground(new Color(56, 189, 248)); // Sky blue
        lblUserContext.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(lblUserContext);
        add(headerPanel, BorderLayout.NORTH);

        // Center Action Grid
        JPanel gridPanel = new JPanel(new GridLayout(3, 2, 14, 14));
        gridPanel.setBackground(new Color(241, 245, 249));
        gridPanel.setBorder(new EmptyBorder(22, 25, 22, 25));

        JButton btnAddAcc = createActionButton("➕ Open Bank Account", "Create Savings, Current (GST), or Student Account", new Color(37, 99, 235));
        btnAddAcc.addActionListener(e -> {
            if (GUIForm.addAccountFrame == null) GUIForm.addAccountFrame = new AddAccountFrame();
            GUIForm.addAccountFrame.setLocationRelativeTo(this);
            GUIForm.addAccountFrame.setVisible(true);
        });

        JButton btnDeposit = createActionButton("💰 Deposit Funds", "Add funds via UPI or Instant Cash Deposit", new Color(16, 185, 129));
        btnDeposit.addActionListener(e -> {
            if (GUIForm.depositFrame == null) GUIForm.depositFrame = new DepositFrame();
            GUIForm.depositFrame.setLocationRelativeTo(this);
            GUIForm.depositFrame.setVisible(true);
        });

        JButton btnWithdraw = createActionButton("💸 Withdraw Money", "ATM Cash withdrawal / Counter disbursement", new Color(217, 119, 6));
        btnWithdraw.addActionListener(e -> {
            if (GUIForm.withdrawFrame == null) GUIForm.withdrawFrame = new WithdrawFrame();
            GUIForm.withdrawFrame.setLocationRelativeTo(this);
            GUIForm.withdrawFrame.setVisible(true);
        });

        JButton btnTransfer = createActionButton("⚡ Instant Transfer", "Send money via UPI, IMPS, NEFT, or RTGS", new Color(124, 58, 237));
        btnTransfer.addActionListener(e -> {
            if (GUIForm.transferFrame == null) GUIForm.transferFrame = new TransferFrame();
            GUIForm.transferFrame.setLocationRelativeTo(this);
            GUIForm.transferFrame.setVisible(true);
        });

        JButton btnDisplay = createActionButton("📋 Accounts & Passbook", "Display all accounts, balances & IFSC details", new Color(14, 116, 144));
        btnDisplay.addActionListener(e -> {
            if (GUIForm.displayAccountsFrame == null) GUIForm.displayAccountsFrame = new DisplayAccountsFrame();
            GUIForm.displayAccountsFrame.refreshTable();
            GUIForm.displayAccountsFrame.setLocationRelativeTo(this);
            GUIForm.displayAccountsFrame.setVisible(true);
        });

        JButton btnSignOut = createActionButton("🚪 Sign Out", "Securely terminate session & save state", new Color(220, 38, 38));
        btnSignOut.addActionListener(e -> {
            setVisible(false);
            GUIForm.showLogin();
        });

        gridPanel.add(btnAddAcc);
        gridPanel.add(btnDeposit);
        gridPanel.add(btnWithdraw);
        gridPanel.add(btnTransfer);
        gridPanel.add(btnDisplay);
        gridPanel.add(btnSignOut);

        add(gridPanel, BorderLayout.CENTER);

        // Footer Status
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(new Color(226, 232, 240));
        lblBankStats.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblBankStats.setForeground(new Color(51, 65, 85));
        footerPanel.add(lblBankStats);
        add(footerPanel, BorderLayout.SOUTH);
    }

    public void refreshStats() {
        List<Account> accs = DataStore.getInstance().getAllAccounts();
        double totalBal = 0;
        for (Account a : accs) totalBal += a.getBalance();
        lblBankStats.setText(String.format("🏦 Total Registered Accounts: %d | Total Active Vault Deposits: ₹%,.2f", accs.size(), totalBal));
    }

    private JButton createActionButton(String title, String subtitle, Color themeColor) {
        JButton btn = new JButton("<html><div style='text-align:center;'><b><font size='4'>" + title + "</font></b><br><font size='2' color='#64748b'>" + subtitle + "</font></div></html>");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(Color.WHITE);
        btn.setForeground(new Color(15, 23, 42));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
