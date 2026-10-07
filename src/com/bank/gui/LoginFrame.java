package com.bank.gui;

import com.bank.model.User;
import com.bank.repository.DataStore;
import com.bank.util.SecurityUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * ============================================================================
 * SWING GUI: LoginFrame
 * ============================================================================
 * Modern authentication dialog for NovaCore Bank of India.
 */
public class LoginFrame extends JFrame {

    private final JTextField txtUsername = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JLabel lblStatus = new JLabel(" ", SwingConstants.CENTER);

    public LoginFrame() {
        setTitle("NovaCore Bank of India — Secure Desktop Portal");
        setSize(480, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        // Header Banner
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(15, 23, 42)); // Deep Slate Navy
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(25, 20, 20, 20));

        JLabel lblTitle = new JLabel("🏦 NovaCore Bank of India");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(248, 250, 252));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Reserve Bank of India Compliant Core Banking System");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(148, 163, 184));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        headerPanel.add(lblSubtitle);
        add(headerPanel, BorderLayout.NORTH);

        // Center Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(241, 245, 249));
        formPanel.setBorder(new EmptyBorder(20, 30, 15, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblUser = new JLabel("Username / Client ID:");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(lblUser, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblPass = new JLabel("Password / MPIN:");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(lblPass, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(new Color(220, 38, 38));
        formPanel.add(lblStatus, gbc);

        // Action Buttons
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        btnPanel.setOpaque(false);

        JButton btnLogin = new JButton("Sign In Securely");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogin.setBackground(new Color(16, 185, 129)); // Emerald Green
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.addActionListener(this::handleLogin);

        JButton btnExit = new JButton("Exit");
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnExit.addActionListener(e -> System.exit(0));

        btnPanel.add(btnLogin);
        btnPanel.add(btnExit);
        formPanel.add(btnPanel, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Quick Demo Fillers
        JPanel demoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        demoPanel.setBackground(new Color(226, 232, 240));
        demoPanel.setBorder(BorderFactory.createTitledBorder("⚡ 1-Click Demo Accounts"));

        JButton btnDemoAdmin = new JButton("🛡️ Admin");
        btnDemoAdmin.addActionListener(e -> { txtUsername.setText("admin"); txtPassword.setText("admin123"); });

        JButton btnDemoAarav = new JButton("👤 Aarav (Savings/Current)");
        btnDemoAarav.addActionListener(e -> { txtUsername.setText("aarav.patel"); txtPassword.setText("customer123"); });

        JButton btnDemoStudent = new JButton("🎓 Rohan (Student)");
        btnDemoStudent.addActionListener(e -> { txtUsername.setText("rohan.verma"); txtPassword.setText("customer123"); });

        demoPanel.add(btnDemoAdmin);
        demoPanel.add(btnDemoAarav);
        demoPanel.add(btnDemoStudent);
        add(demoPanel, BorderLayout.SOUTH);
    }

    private void handleLogin(ActionEvent e) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please enter both username and password.");
            return;
        }

        User user = DataStore.getInstance().getUserByUsername(username);
        if (user == null || !SecurityUtil.verifyPassword(password, user.getPasswordHash())) {
            lblStatus.setText("Invalid credentials. Please verify your login details.");
            return;
        }

        if (!user.isActive()) {
            lblStatus.setText("Account is disabled. Contact Bank Admin.");
            return;
        }

        lblStatus.setText(" ");
        GUIForm.showMenu(user.getFullName(), user.getRole());
    }
}
