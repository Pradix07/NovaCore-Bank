package com.bank.server;

import com.bank.exceptions.BankingException;
import com.bank.exceptions.UnauthorizedException;
import com.bank.exceptions.ValidationException;
import com.bank.model.User;
import com.bank.service.*;
import com.bank.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ApiHandler implements HttpHandler {

    private final AuthService authService = new AuthService();
    private final AccountService accountService = new AccountService();
    private final TransactionService transactionService = new TransactionService();
    private final BankingService bankingService = new BankingService();
    private final AdminService adminService = new AdminService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Enable CORS
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.startsWith("/api/auth/")) {
                handleAuth(exchange, path, method);
            } else if (path.startsWith("/api/customer/")) {
                handleCustomer(exchange, path, method);
            } else if (path.startsWith("/api/admin/")) {
                handleAdmin(exchange, path, method);
            } else {
                sendError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (BankingException be) {
            sendError(exchange, be.getStatusCode(), be.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleAuth(HttpExchange exchange, String path, String method) throws IOException {
        if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String u = (String) req.get("username");
            String p = (String) req.get("password");
            Map<String, Object> res = authService.login(u, p);
            sendJson(exchange, 200, res);
        } else if ("/api/auth/register".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String u = (String) req.get("username");
            String p = (String) req.get("password");
            String fn = (String) req.get("fullName");
            String em = (String) req.get("email");
            String ph = (String) req.get("phone");
            String addr = (String) req.get("address");
            String pan = (String) req.get("panOrTaxId");
            String accType = (String) req.get("accountType");
            double dep = req.containsKey("initialDeposit") ? ((Number) req.get("initialDeposit")).doubleValue() : 500.0;
            Map<String, Object> res = authService.registerCustomer(u, p, fn, em, ph, addr, pan, accType, dep);
            sendJson(exchange, 201, res);
        } else if ("/api/auth/logout".equals(path) && "POST".equalsIgnoreCase(method)) {
            String token = extractToken(exchange);
            authService.logout(token);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "SUCCESS");
            res.put("message", "Logged out successfully.");
            sendJson(exchange, 200, res);
        } else if ("/api/auth/me".equals(path) && "GET".equalsIgnoreCase(method)) {
            User user = requireUser(exchange);
            Map<String, Object> res = new HashMap<>();
            res.put("user", user.toMap());
            sendJson(exchange, 200, res);
        } else {
            sendError(exchange, 404, "Unknown auth route: " + path);
        }
    }

    private void handleCustomer(HttpExchange exchange, String path, String method) throws IOException {
        User user = requireUser(exchange);

        if ("/api/customer/overview".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, Object> res = accountService.getCustomerDashboardOverview(user.getId());
            sendJson(exchange, 200, res);
        } else if ("/api/customer/transfer".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String fromAcc = (String) req.get("fromAccount");
            String toAcc = (String) req.get("toAccount");
            double amount = req.containsKey("amount") ? ((Number) req.get("amount")).doubleValue() : 0.0;
            String desc = (String) req.get("description");
            String pin = (String) req.get("securityPin");
            Map<String, Object> res = transactionService.transferFunds(user.getId(), fromAcc, toAcc, amount, desc, pin);
            sendJson(exchange, 200, res);
        } else if ("/api/customer/deposit".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String acc = (String) req.get("accountNumber");
            double amount = req.containsKey("amount") ? ((Number) req.get("amount")).doubleValue() : 0.0;
            String desc = (String) req.get("description");
            Map<String, Object> res = transactionService.deposit(user.getId(), acc, amount, desc);
            sendJson(exchange, 200, res);
        } else if ("/api/customer/withdraw".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String acc = (String) req.get("accountNumber");
            double amount = req.containsKey("amount") ? ((Number) req.get("amount")).doubleValue() : 0.0;
            String desc = (String) req.get("description");
            Map<String, Object> res = transactionService.withdraw(user.getId(), acc, amount, desc);
            sendJson(exchange, 200, res);
        } else if ("/api/customer/transactions".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String type = query.get("type");
            String search = query.get("search");
            Double min = query.containsKey("min") ? Double.parseDouble(query.get("min")) : null;
            Double max = query.containsKey("max") ? Double.parseDouble(query.get("max")) : null;
            List<Map<String, Object>> res = transactionService.filterTransactions(user.getId(), type, search, min, max);
            sendJson(exchange, 200, res);
        } else if ("/api/customer/loans/apply".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String loanType = (String) req.get("loanType");
            double amount = req.containsKey("amount") ? ((Number) req.get("amount")).doubleValue() : 0.0;
            int tenure = req.containsKey("tenureMonths") ? ((Number) req.get("tenureMonths")).intValue() : 12;
            String purpose = (String) req.get("purpose");
            Object res = bankingService.applyForLoan(user.getId(), loanType, amount, tenure, purpose).toMap();
            sendJson(exchange, 201, res);
        } else if ("/api/customer/loans/pay-emi".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String loanId = (String) req.get("loanId");
            String fromAcc = (String) req.get("fromAccount");
            Map<String, Object> res = bankingService.payLoanEmi(user.getId(), loanId, fromAcc);
            sendJson(exchange, 200, res);
        } else if ("/api/customer/investments/create".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String fromAcc = (String) req.get("fromAccount");
            String invType = (String) req.get("type");
            String name = (String) req.get("name");
            double amount = req.containsKey("amount") ? ((Number) req.get("amount")).doubleValue() : 0.0;
            int duration = req.containsKey("durationMonths") ? ((Number) req.get("durationMonths")).intValue() : 12;
            Object res = bankingService.createInvestment(user.getId(), fromAcc, invType, name, amount, duration).toMap();
            sendJson(exchange, 201, res);
        } else if ("/api/customer/investments/withdraw".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String invId = (String) req.get("investmentId");
            String toAcc = (String) req.get("toAccount");
            Map<String, Object> res = bankingService.withdrawInvestment(user.getId(), invId, toAcc);
            sendJson(exchange, 200, res);
        } else if ("/api/customer/accounts/open".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String type = (String) req.get("accountType");
            double initDep = req.containsKey("initialDeposit") ? ((Number) req.get("initialDeposit")).doubleValue() : 500.0;
            Object res = accountService.openNewAccount(user.getId(), type, initDep).toMap();
            sendJson(exchange, 201, res);
        } else if ("/api/customer/cards/toggle-freeze".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String acc = (String) req.get("accountNumber");
            boolean frozen = accountService.toggleCardFreeze(user.getId(), acc);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "SUCCESS");
            res.put("cardFrozen", frozen);
            sendJson(exchange, 200, res);
        } else if ("/api/customer/profile/update".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String fn = (String) req.get("fullName");
            String em = (String) req.get("email");
            String ph = (String) req.get("phone");
            String addr = (String) req.get("address");
            String curPass = (String) req.get("currentPassword");
            String newPass = (String) req.get("newPassword");
            String pin = (String) req.get("securityPin");
            accountService.updateCustomerProfile(user.getId(), fn, em, ph, addr, curPass, newPass, pin);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "SUCCESS");
            res.put("message", "Profile updated successfully.");
            sendJson(exchange, 200, res);
        } else {
            sendError(exchange, 404, "Unknown customer endpoint: " + path);
        }
    }

    private void handleAdmin(HttpExchange exchange, String path, String method) throws IOException {
        User user = requireUser(exchange);
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new UnauthorizedException("Administrative privileges are required.");
        }

        if ("/api/admin/metrics".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, Object> res = adminService.getAdminDashboardMetrics();
            sendJson(exchange, 200, res);
        } else if ("/api/admin/users".equals(path) && "GET".equalsIgnoreCase(method)) {
            List<Map<String, Object>> res = adminService.getAllUsersWithDetails();
            sendJson(exchange, 200, res);
        } else if ("/api/admin/users/create".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String u = (String) req.get("username");
            String p = (String) req.get("password");
            String fn = (String) req.get("fullName");
            String em = (String) req.get("email");
            String ph = (String) req.get("phone");
            String role = (String) req.get("role");
            String accType = (String) req.get("accountType");
            double initBal = req.containsKey("initialBalance") ? ((Number) req.get("initialBalance")).doubleValue() : 1000.0;
            String addr = (String) req.get("address");
            String pan = (String) req.get("panOrTaxId");
            User created = adminService.createUser(user.getId(), u, p, fn, em, ph, role, accType, initBal, addr, pan);
            sendJson(exchange, 201, created.toMap());
        } else if ("/api/admin/users/status".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String targetId = (String) req.get("userId");
            boolean active = Boolean.TRUE.equals(req.get("active"));
            adminService.updateUserStatus(user.getId(), targetId, active);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "SUCCESS");
            sendJson(exchange, 200, res);
        } else if ("/api/admin/users/delete".equals(path) && ("POST".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
            Map<String, Object> req = readJsonBody(exchange);
            String targetId = (String) req.get("userId");
            adminService.deleteUser(user.getId(), targetId);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "SUCCESS");
            sendJson(exchange, 200, res);
        } else if ("/api/admin/transactions".equals(path) && "GET".equalsIgnoreCase(method)) {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String type = query.get("type");
            String search = query.get("search");
            Double min = query.containsKey("min") ? Double.parseDouble(query.get("min")) : null;
            Double max = query.containsKey("max") ? Double.parseDouble(query.get("max")) : null;
            List<Map<String, Object>> res = transactionService.filterTransactions(null, type, search, min, max);
            sendJson(exchange, 200, res);
        } else if ("/api/admin/loans".equals(path) && "GET".equalsIgnoreCase(method)) {
            List<Map<String, Object>> res = new ArrayList<>();
            for (var l : com.bank.repository.DataStore.getInstance().getAllLoans()) {
                res.add(l.toMap());
            }
            sendJson(exchange, 200, res);
        } else if ("/api/admin/loans/review".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            String loanId = (String) req.get("loanId");
            String action = (String) req.get("action");
            String remarks = (String) req.get("remarks");
            String targetAcc = (String) req.get("targetAccountNumber");
            Object res = bankingService.reviewLoan(user.getId(), loanId, action, remarks, targetAcc).toMap();
            sendJson(exchange, 200, res);
        } else if ("/api/admin/investments".equals(path) && "GET".equalsIgnoreCase(method)) {
            List<Map<String, Object>> res = new ArrayList<>();
            for (var inv : com.bank.repository.DataStore.getInstance().getAllInvestments()) {
                res.add(inv.toMap());
            }
            sendJson(exchange, 200, res);
        } else if ("/api/admin/audit-logs".equals(path) && "GET".equalsIgnoreCase(method)) {
            List<Map<String, Object>> res = adminService.getAllAuditLogs();
            sendJson(exchange, 200, res);
        } else if ("/api/admin/settings".equals(path) && "GET".equalsIgnoreCase(method)) {
            sendJson(exchange, 200, com.bank.repository.DataStore.getInstance().getSystemSettings().toMap());
        } else if ("/api/admin/settings/update".equals(path) && "POST".equalsIgnoreCase(method)) {
            Map<String, Object> req = readJsonBody(exchange);
            adminService.updateSystemSettings(user.getId(), req);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "SUCCESS");
            res.put("settings", com.bank.repository.DataStore.getInstance().getSystemSettings().toMap());
            sendJson(exchange, 200, res);
        } else {
            sendError(exchange, 404, "Unknown admin endpoint: " + path);
        }
    }

    private User requireUser(HttpExchange exchange) {
        String token = extractToken(exchange);
        return authService.authenticateToken(token);
    }

    private String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return exchange.getRequestHeaders().getFirst("X-Auth-Token");
    }

    private Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return JsonUtil.parseObject(json);
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object data) throws IOException {
        byte[] bytes = JsonUtil.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = new HashMap<>();
        err.put("error", true);
        err.put("statusCode", statusCode);
        err.put("message", message);
        sendJson(exchange, statusCode, err);
    }

    private Map<String, String> parseQueryParams(String queryString) {
        Map<String, String> map = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return map;
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
                } else if (idx == -1) {
                    map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            } catch (Exception ignored) {}
        }
        return map;
    }
}
