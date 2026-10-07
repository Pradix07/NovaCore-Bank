package com.bank.service;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InsufficientFundsException;
import com.bank.exceptions.ValidationException;
import com.bank.model.*;
import com.bank.repository.DataStore;
import com.bank.util.SecurityUtil;

import java.util.*;
import java.util.stream.Collectors;
import com.bank.service.interfaces.ITransactionService;

/**
 * ============================================================================
 * SERVICE: TransactionService (Indian Banking System Rails)
 * ============================================================================
 * Demonstrates:
 * - Multithreading & Synchronization (Deterministic Lock Ordering to prevent Deadlocks)
 * - Indian Payment Modes: UPI, IMPS (24x7 Instant), NEFT, RTGS (High Value >= ₹2,00,000)
 * - IFSC Code and UPI ID Resolution
 * - Java 8 Streams & Lambdas for filtering & auditing
 * - ACID Transaction Atomicity
 */
public class TransactionService implements ITransactionService {

    private final DataStore dataStore = DataStore.getInstance();

    public Map<String, Object> transferFunds(String customerId, String fromAccNumber, String toAccNumberOrUpi, double amount, String description, String securityPin) {
        return transferFundsDetailed(customerId, fromAccNumber, toAccNumberOrUpi, amount, "UPI/IMPS", null, description, securityPin);
    }

    /**
     * Enhanced transfer handler supporting Indian Payment Rails (UPI, IMPS, NEFT, RTGS).
     */
    public Map<String, Object> transferFundsDetailed(String customerId, String fromAccNumber, String toAccNumberOrUpi, double amount, String transferMode, String recipientIfsc, String description, String securityPin) {
        if (amount <= 0) {
            throw new ValidationException("Transfer amount must be strictly greater than ₹0.00.");
        }

        String mode = (transferMode != null && !transferMode.trim().isEmpty()) ? transferMode.toUpperCase().trim() : "IMPS";

        // RTGS Minimum Validation according to Reserve Bank of India (RBI)
        if ("RTGS".equalsIgnoreCase(mode) && amount < 200000.0) {
            throw new ValidationException("RTGS (Real Time Gross Settlement) requires a minimum transfer amount of ₹2,00,000.00. Please use IMPS, UPI, or NEFT for smaller amounts.");
        }

        SystemSettings settings = dataStore.getSystemSettings();
        if (amount > settings.getPerTransactionLimit()) {
            throw new ValidationException("Amount exceeds per-transaction limit of ₹" + String.format("%,.2f", settings.getPerTransactionLimit()));
        }

        User user = dataStore.getUserById(customerId);
        if (!(user instanceof Customer)) {
            throw new ValidationException("Invalid customer session.");
        }
        Customer customer = (Customer) user;

        if (securityPin != null && !securityPin.isEmpty()) {
            if (!securityPin.equals(customer.getSecurityPin())) {
                throw new ValidationException("Invalid 4-digit Transaction / UPI PIN.");
            }
        }

        Account fromAccount = dataStore.getAccountByNumber(fromAccNumber);
        if (fromAccount == null || !fromAccount.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Source account #" + fromAccNumber + " not found or unauthorized.");
        }

        if ("FROZEN".equalsIgnoreCase(fromAccount.getStatus()) || fromAccount.isCardFrozen()) {
            throw new ValidationException("Source account or linked debit card is currently locked/frozen.");
        }

        // Resolve destination account: check direct account number or UPI ID
        Account toAccount = dataStore.getAccountByNumber(toAccNumberOrUpi);
        if (toAccount == null) {
            // Check if toAccNumberOrUpi matches a UPI ID or customer username
            for (Account acc : dataStore.getAllAccounts()) {
                if (toAccNumberOrUpi.equalsIgnoreCase(acc.getUpiId()) ||
                    toAccNumberOrUpi.equalsIgnoreCase(acc.getAccountNumber())) {
                    toAccount = acc;
                    break;
                }
            }
        }

        if (toAccount == null) {
            throw new AccountNotFoundException("Recipient account or UPI ID '" + toAccNumberOrUpi + "' does not exist in NovaCore Bank of India.");
        }

        if (fromAccount.getAccountNumber().equalsIgnoreCase(toAccount.getAccountNumber())) {
            throw new ValidationException("Source and destination accounts cannot be identical.");
        }

        if (!"ACTIVE".equalsIgnoreCase(toAccount.getStatus())) {
            throw new ValidationException("Recipient account is not active.");
        }

        // Deadlock prevention: Lock accounts in deterministic order (by accountNumber)
        Account firstLock = fromAccount.getAccountNumber().compareTo(toAccount.getAccountNumber()) < 0 ? fromAccount : toAccount;
        Account secondLock = fromAccount.getAccountNumber().compareTo(toAccount.getAccountNumber()) < 0 ? toAccount : fromAccount;

        String refNum = mode + SecurityUtil.generateReferenceNumber().replace("-", "").substring(0, 12);
        String txIdDebit = SecurityUtil.generateId("TXN");
        String txIdCredit = SecurityUtil.generateId("TXN");

        firstLock.getLock().lock();
        secondLock.getLock().lock();
        try {
            // Execute withdrawal
            fromAccount.withdraw(amount);
            // Execute deposit
            toAccount.deposit(amount);

            String memo = (description != null && !description.trim().isEmpty()) ? description.trim() : (mode + " Transfer");

            Transaction debitTx = new Transaction(
                    txIdDebit,
                    TransactionType.TRANSFER_OUT,
                    amount,
                    fromAccount.getAccountNumber(),
                    toAccount.getAccountNumber(),
                    "[" + mode + "] " + memo + " (To: " + toAccount.getAccountNumber() + " / " + toAccount.getIfscCode() + ")",
                    fromAccount.getBalance(),
                    fromAccount.getCustomerId(),
                    refNum
            );

            Transaction creditTx = new Transaction(
                    txIdCredit,
                    TransactionType.TRANSFER_IN,
                    amount,
                    fromAccount.getAccountNumber(),
                    toAccount.getAccountNumber(),
                    "[" + mode + "] " + memo + " (From: " + fromAccount.getAccountNumber() + " / " + fromAccount.getIfscCode() + ")",
                    toAccount.getBalance(),
                    toAccount.getCustomerId(),
                    refNum
            );

            dataStore.addTransaction(debitTx);
            dataStore.addTransaction(creditTx);

            dataStore.addAuditLog(new AuditLog(
                    SecurityUtil.generateId("LOG"),
                    customerId,
                    customer.getFullName(),
                    "CUSTOMER",
                    "TRANSFER_SUCCESS",
                    "Transferred ₹" + String.format("%,.2f", amount) + " via " + mode + " from #" + fromAccount.getAccountNumber() + " to #" + toAccount.getAccountNumber() + " [Ref: " + refNum + "]",
                    "127.0.0.1"
            ));

            dataStore.saveToFile();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("transferMode", mode);
            result.put("referenceNumber", refNum);
            result.put("amount", amount);
            result.put("fromAccount", fromAccount.getAccountNumber());
            result.put("toAccount", toAccount.getAccountNumber());
            result.put("recipientIfsc", toAccount.getIfscCode());
            result.put("remainingBalance", fromAccount.getBalance());
            result.put("timestamp", debitTx.getTimestamp());
            result.put("transactionId", txIdDebit);
            return result;
        } finally {
            secondLock.getLock().unlock();
            firstLock.getLock().unlock();
        }
    }

    public Map<String, Object> deposit(String customerId, String accountNumber, double amount, String description) {
        if (amount <= 0) {
            throw new ValidationException("Deposit amount must be strictly greater than ₹0.00.");
        }

        Account account = dataStore.getAccountByNumber(accountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account #" + accountNumber + " not found or unauthorized.");
        }

        account.deposit(amount);
        String refNum = "DEP" + SecurityUtil.generateReferenceNumber().replace("-", "").substring(0, 10);
        String txId = SecurityUtil.generateId("TXN");
        String memo = (description != null && !description.trim().isEmpty()) ? description.trim() : "Instant UPI / Cash Deposit";

        Transaction tx = new Transaction(
                txId,
                TransactionType.DEPOSIT,
                amount,
                "ONLINE_PAYMENT_GATEWAY",
                accountNumber,
                memo + " [IFSC: " + account.getIfscCode() + "]",
                account.getBalance(),
                customerId,
                refNum
        );
        dataStore.addTransaction(tx);
        dataStore.saveToFile();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("referenceNumber", refNum);
        result.put("amount", amount);
        result.put("accountNumber", accountNumber);
        result.put("ifscCode", account.getIfscCode());
        result.put("newBalance", account.getBalance());
        result.put("timestamp", tx.getTimestamp());
        return result;
    }

    public Map<String, Object> withdraw(String customerId, String accountNumber, double amount, String description) {
        if (amount <= 0) {
            throw new ValidationException("Withdrawal amount must be strictly greater than ₹0.00.");
        }

        Account account = dataStore.getAccountByNumber(accountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account #" + accountNumber + " not found or unauthorized.");
        }

        account.withdraw(amount);
        String refNum = "ATM" + SecurityUtil.generateReferenceNumber().replace("-", "").substring(0, 10);
        String txId = SecurityUtil.generateId("TXN");
        String memo = (description != null && !description.trim().isEmpty()) ? description.trim() : "RuPay Debit Card / ATM Cash Withdrawal";

        Transaction tx = new Transaction(
                txId,
                TransactionType.WITHDRAWAL,
                amount,
                accountNumber,
                "ATM_MUMBAI_FORT",
                memo,
                account.getBalance(),
                customerId,
                refNum
        );
        dataStore.addTransaction(tx);
        dataStore.saveToFile();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("referenceNumber", refNum);
        result.put("amount", amount);
        result.put("accountNumber", accountNumber);
        result.put("newBalance", account.getBalance());
        result.put("timestamp", tx.getTimestamp());
        return result;
    }

    public List<Map<String, Object>> filterTransactions(String customerId, String filterType, String searchQuery, Double minAmount, Double maxAmount) {
        List<Transaction> list = (customerId != null && !customerId.isEmpty())
                ? dataStore.getTransactionsByCustomerId(customerId)
                : dataStore.getAllTransactions();

        return list.stream()
                .filter(t -> {
                    if (filterType != null && !filterType.isEmpty() && !"ALL".equalsIgnoreCase(filterType)) {
                        if ("CREDIT".equalsIgnoreCase(filterType)) {
                            if (t.getType() != TransactionType.DEPOSIT && t.getType() != TransactionType.TRANSFER_IN && t.getType() != TransactionType.LOAN_DISBURSEMENT && t.getType() != TransactionType.INVESTMENT_RETURN) {
                                return false;
                            }
                        } else if ("DEBIT".equalsIgnoreCase(filterType)) {
                            if (t.getType() != TransactionType.WITHDRAWAL && t.getType() != TransactionType.TRANSFER_OUT && t.getType() != TransactionType.LOAN_EMI && t.getType() != TransactionType.INVESTMENT_DEPOSIT) {
                                return false;
                            }
                        } else if (!t.getType().name().equalsIgnoreCase(filterType)) {
                            return false;
                        }
                    }
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        String q = searchQuery.toLowerCase();
                        boolean matchDesc = t.getDescription() != null && t.getDescription().toLowerCase().contains(q);
                        boolean matchRef = t.getReferenceNumber() != null && t.getReferenceNumber().toLowerCase().contains(q);
                        boolean matchFrom = t.getFromAccount() != null && t.getFromAccount().toLowerCase().contains(q);
                        boolean matchTo = t.getToAccount() != null && t.getToAccount().toLowerCase().contains(q);
                        if (!matchDesc && !matchRef && !matchFrom && !matchTo) return false;
                    }
                    if (minAmount != null && t.getAmount() < minAmount) return false;
                    if (maxAmount != null && t.getAmount() > maxAmount) return false;
                    return true;
                })
                .map(Transaction::toMap)
                .collect(Collectors.toList());
    }
}
