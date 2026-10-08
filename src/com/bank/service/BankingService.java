package com.bank.service;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.BankingException;
import com.bank.exceptions.ValidationException;
import com.bank.model.*;
import com.bank.repository.DataStore;
import com.bank.util.SecurityUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.bank.service.interfaces.IBankingService;

/**
 * ============================================================================
 * SERVICE: BankingService
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements IBankingService)
 * - Facade Pattern: Coordinates Loans, Investments, and Transactions
 * - Input validation and business constraint enforcement
 */
public class BankingService implements IBankingService {

    private final DataStore dataStore = DataStore.getInstance();

    public LoanApplication applyForLoan(String customerId, String loanType, double amount, int tenureMonths, String purpose) {
        if (amount < 1000.0) {
            throw new ValidationException("Minimum loan application amount is $1,000.00.");
        }
        if (tenureMonths < 3 || tenureMonths > 360) {
            throw new ValidationException("Loan tenure must be between 3 and 360 months.");
        }

        User user = dataStore.getUserById(customerId);
        if (!(user instanceof Customer)) {
            throw new ValidationException("Invalid customer.");
        }
        Customer customer = (Customer) user;

        double baseRate = dataStore.getSystemSettings().getDefaultLoanInterestRate();
        if ("HOME".equalsIgnoreCase(loanType)) baseRate -= 0.5;
        if ("EDUCATION".equalsIgnoreCase(loanType)) baseRate -= 1.0;
        if ("PERSONAL".equalsIgnoreCase(loanType)) baseRate += 1.5;

        String loanId = SecurityUtil.generateId("LOAN");
        LoanApplication loan = new LoanApplication(
                loanId,
                customerId,
                customer.getFullName(),
                loanType.toUpperCase(),
                amount,
                tenureMonths,
                baseRate,
                purpose != null ? purpose.trim() : "Personal financial needs"
        );

        dataStore.addLoan(loan);
        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                customerId,
                customer.getFullName(),
                "CUSTOMER",
                "LOAN_APPLICATION_SUBMITTED",
                "Applied for " + loanType + " loan of $" + amount + " for " + tenureMonths + " months.",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
        return loan;
    }

    public LoanApplication reviewLoan(String adminId, String loanId, String action, String remarks, String targetAccountNumber) {
        LoanApplication loan = dataStore.getLoanById(loanId);
        if (loan == null) {
            throw new ValidationException("Loan application #" + loanId + " not found.");
        }

        User admin = dataStore.getUserById(adminId);
        String adminName = admin != null ? admin.getFullName() : "Administrator";

        loan.setDecidedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        loan.setRemarks(remarks);

        if ("APPROVE".equalsIgnoreCase(action)) {
            loan.setStatus("APPROVED");

            // Disburse loan funds into the customer's account if available
            List<Account> accounts = dataStore.getAccountsByCustomerId(loan.getCustomerId());
            Account disburseAccount = null;
            if (targetAccountNumber != null && !targetAccountNumber.isEmpty()) {
                disburseAccount = dataStore.getAccountByNumber(targetAccountNumber);
            } else if (!accounts.isEmpty()) {
                disburseAccount = accounts.get(0);
            }

            if (disburseAccount != null) {
                disburseAccount.deposit(loan.getAmount());
                dataStore.updateAccount(disburseAccount);
                Transaction tx = new Transaction(
                        SecurityUtil.generateId("TXN"),
                        TransactionType.LOAN_DISBURSEMENT,
                        loan.getAmount(),
                        "NOVA_LOAN_DESK",
                        disburseAccount.getAccountNumber(),
                        "Approved Loan Disbursal - " + loan.getLoanType() + " Loan (" + loan.getId() + ")",
                        disburseAccount.getBalance(),
                        loan.getCustomerId(),
                        SecurityUtil.generateReferenceNumber()
                );
                dataStore.addTransaction(tx);
            }

            dataStore.addAuditLog(new AuditLog(
                    SecurityUtil.generateId("LOG"),
                    adminId,
                    adminName,
                    "ADMIN",
                    "LOAN_APPROVED",
                    "Approved loan #" + loanId + " ($" + loan.getAmount() + ") for customer " + loan.getCustomerName(),
                    "127.0.0.1"
            ));
        } else {
            loan.setStatus("REJECTED");
            dataStore.addAuditLog(new AuditLog(
                    SecurityUtil.generateId("LOG"),
                    adminId,
                    adminName,
                    "ADMIN",
                    "LOAN_REJECTED",
                    "Rejected loan #" + loanId + " for customer " + loan.getCustomerName() + ". Reason: " + remarks,
                    "127.0.0.1"
            ));
        }

        dataStore.updateLoan(loan);
        dataStore.saveToFile();
        return loan;
    }

    public Map<String, Object> payLoanEmi(String customerId, String loanId, String fromAccountNumber) {
        LoanApplication loan = dataStore.getLoanById(loanId);
        if (loan == null || !loan.getCustomerId().equals(customerId)) {
            throw new ValidationException("Loan not found or access denied.");
        }

        if (!"APPROVED".equalsIgnoreCase(loan.getStatus()) && !"ACTIVE".equalsIgnoreCase(loan.getStatus())) {
            throw new ValidationException("This loan is not active or has already been paid off.");
        }

        Account account = dataStore.getAccountByNumber(fromAccountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Debit account not found or unauthorized.");
        }

        double emiAmount = loan.getMonthlyEmi();
        account.withdraw(emiAmount);
        dataStore.updateAccount(account);

        loan.setEmisPaid(loan.getEmisPaid() + 1);
        loan.setRemainingPrincipal(Math.max(0.0, loan.getRemainingPrincipal() - (emiAmount * 0.8)));

        if (loan.getEmisPaid() >= loan.getTenureMonths() || loan.getRemainingPrincipal() <= 0) {
            loan.setStatus("PAID_OFF");
        }
        dataStore.updateLoan(loan);

        String refNum = SecurityUtil.generateReferenceNumber();
        Transaction tx = new Transaction(
                SecurityUtil.generateId("TXN"),
                TransactionType.LOAN_EMI,
                emiAmount,
                fromAccountNumber,
                "LOAN_PAYMENT_DESK",
                "Monthly EMI payment for " + loan.getLoanType() + " Loan #" + loan.getId(),
                account.getBalance(),
                customerId,
                refNum
        );
        dataStore.addTransaction(tx);
        dataStore.saveToFile();

        Map<String, Object> res = new HashMap<>();
        res.put("status", "SUCCESS");
        res.put("referenceNumber", refNum);
        res.put("emiAmount", emiAmount);
        res.put("emisPaid", loan.getEmisPaid());
        res.put("remainingPrincipal", loan.getRemainingPrincipal());
        res.put("loanStatus", loan.getStatus());
        return res;
    }

    public Investment createInvestment(String customerId, String fromAccountNumber, String type, String name, double amount, int durationMonths) {
        if (amount < 500.0) {
            throw new ValidationException("Minimum investment amount is $500.00.");
        }
        if (durationMonths < 3) {
            throw new ValidationException("Minimum investment tenure is 3 months.");
        }

        Account account = dataStore.getAccountByNumber(fromAccountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Debit account not found or unauthorized.");
        }

        account.withdraw(amount);
        dataStore.updateAccount(account);

        double rate = dataStore.getSystemSettings().getDefaultFdInterestRate();
        if ("MUTUAL_FUND".equalsIgnoreCase(type)) rate = 11.5;
        if ("GOLD_BOND".equalsIgnoreCase(type)) rate = 7.0;

        String invId = SecurityUtil.generateId("INV");
        Investment investment = new Investment(
                invId,
                customerId,
                type.toUpperCase(),
                name != null && !name.trim().isEmpty() ? name.trim() : "Smart Wealth " + type,
                amount,
                rate,
                durationMonths
        );

        dataStore.addInvestment(investment);

        Transaction tx = new Transaction(
                SecurityUtil.generateId("TXN"),
                TransactionType.INVESTMENT_DEPOSIT,
                amount,
                fromAccountNumber,
                "INVESTMENT_PORTFOLIO",
                "Investment booking: " + investment.getName() + " (" + invId + ")",
                account.getBalance(),
                customerId,
                SecurityUtil.generateReferenceNumber()
        );
        dataStore.addTransaction(tx);

        dataStore.addAuditLog(new AuditLog(
                SecurityUtil.generateId("LOG"),
                customerId,
                customerId,
                "CUSTOMER",
                "INVESTMENT_CREATED",
                "Created investment #" + invId + " of $" + amount + " at " + rate + "% for " + durationMonths + " months.",
                "127.0.0.1"
        ));

        dataStore.saveToFile();
        return investment;
    }

    public Map<String, Object> withdrawInvestment(String customerId, String investmentId, String toAccountNumber) {
        Investment inv = dataStore.getInvestmentById(investmentId);
        if (inv == null || !inv.getCustomerId().equals(customerId)) {
            throw new ValidationException("Investment not found or access denied.");
        }

        if (!"ACTIVE".equalsIgnoreCase(inv.getStatus())) {
            throw new ValidationException("Investment has already been liquidated or matured.");
        }

        Account account = dataStore.getAccountByNumber(toAccountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Deposit account not found or unauthorized.");
        }

        double payout = inv.getCurrentMaturityValue();
        account.deposit(payout);
        dataStore.updateAccount(account);
        inv.setStatus("WITHDRAWN");
        dataStore.updateInvestment(inv);

        String refNum = SecurityUtil.generateReferenceNumber();
        Transaction tx = new Transaction(
                SecurityUtil.generateId("TXN"),
                TransactionType.INVESTMENT_RETURN,
                payout,
                "INVESTMENT_LIQUIDATION",
                toAccountNumber,
                "Investment Liquidation & Payout: " + inv.getName(),
                account.getBalance(),
                customerId,
                refNum
        );
        dataStore.addTransaction(tx);
        dataStore.saveToFile();

        Map<String, Object> res = new HashMap<>();
        res.put("status", "SUCCESS");
        res.put("referenceNumber", refNum);
        res.put("payoutAmount", payout);
        res.put("accountNumber", toAccountNumber);
        res.put("newBalance", account.getBalance());
        return res;
    }
}
