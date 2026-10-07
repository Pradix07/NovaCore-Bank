package com.bank.service;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InsufficientFundsException;
import com.bank.exceptions.ValidationException;
import com.bank.model.*;
import com.bank.repository.DataStore;
import com.bank.util.SecurityUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import com.bank.service.interfaces.ITransactionService;

/**
 * ============================================================================
 * SERVICE: TransactionService
 * ============================================================================
 * Demonstrates:
 * - Interface Implementation (implements ITransactionService)
 * - Multithreading & Synchronization (Deterministic Lock Ordering to prevent Deadlocks)
 * - Java 8 Streams & Lambdas for functional filtering
 * - ACID Transaction Atomicity
 */
public class TransactionService implements ITransactionService {

    private final DataStore dataStore = DataStore.getInstance();

    public Map<String, Object> transferFunds(String customerId, String fromAccNumber, String toAccNumber, double amount, String description, String securityPin) {
        if (amount <= 0) {
            throw new ValidationException("Transfer amount must be strictly greater than $0.00.");
        }

        SystemSettings settings = dataStore.getSystemSettings();
        if (amount > settings.getPerTransactionLimit()) {
            throw new ValidationException("Amount exceeds single transaction limit of $" + String.format("%.2f", settings.getPerTransactionLimit()));
        }

        if (fromAccNumber.equalsIgnoreCase(toAccNumber)) {
            throw new ValidationException("Source and destination accounts cannot be identical.");
        }

        User user = dataStore.getUserById(customerId);
        if (!(user instanceof Customer)) {
            throw new ValidationException("Invalid customer session.");
        }
        Customer customer = (Customer) user;

        if (securityPin != null && !securityPin.isEmpty()) {
            if (!securityPin.equals(customer.getSecurityPin())) {
                throw new ValidationException("Invalid 4-digit Security PIN.");
            }
        }

        Account fromAccount = dataStore.getAccountByNumber(fromAccNumber);
        if (fromAccount == null || !fromAccount.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Source account #" + fromAccNumber + " not found or unauthorized.");
        }

        if ("FROZEN".equalsIgnoreCase(fromAccount.getStatus()) || fromAccount.isCardFrozen()) {
            throw new ValidationException("Source account or card is currently locked/frozen.");
        }

        Account toAccount = dataStore.getAccountByNumber(toAccNumber);
        if (toAccount == null) {
            throw new AccountNotFoundException("Recipient account #" + toAccNumber + " does not exist in our records.");
        }

        if (!"ACTIVE".equalsIgnoreCase(toAccount.getStatus())) {
            throw new ValidationException("Recipient account is not active.");
        }

        // Deadlock prevention: Lock accounts in deterministic order (by accountNumber)
        Account firstLock = fromAccNumber.compareTo(toAccNumber) < 0 ? fromAccount : toAccount;
        Account secondLock = fromAccNumber.compareTo(toAccNumber) < 0 ? toAccount : fromAccount;

        String refNum = SecurityUtil.generateReferenceNumber();
        String txIdDebit = SecurityUtil.generateId("TXN");
        String txIdCredit = SecurityUtil.generateId("TXN");

        firstLock.getLock().lock();
        secondLock.getLock().lock();
        try {
            // Execute withdrawal
            fromAccount.withdraw(amount);
            // Execute deposit
            toAccount.deposit(amount);

            String memo = (description != null && !description.trim().isEmpty()) ? description.trim() : "Funds Transfer";

            Transaction debitTx = new Transaction(
                    txIdDebit,
                    TransactionType.TRANSFER_OUT,
                    amount,
                    fromAccNumber,
                    toAccNumber,
                    memo + " (To: " + toAccNumber + ")",
                    fromAccount.getBalance(),
                    fromAccount.getCustomerId(),
                    refNum
            );

            Transaction creditTx = new Transaction(
                    txIdCredit,
                    TransactionType.TRANSFER_IN,
                    amount,
                    fromAccNumber,
                    toAccNumber,
                    memo + " (From: " + fromAccNumber + ")",
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
                    "Transferred $" + amount + " from #" + fromAccNumber + " to #" + toAccNumber + " [Ref: " + refNum + "]",
                    "127.0.0.1"
            ));

            dataStore.saveToFile();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("referenceNumber", refNum);
            result.put("amount", amount);
            result.put("fromAccount", fromAccNumber);
            result.put("toAccount", toAccNumber);
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
            throw new ValidationException("Deposit amount must be strictly greater than $0.00.");
        }

        Account account = dataStore.getAccountByNumber(accountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account #" + accountNumber + " not found or unauthorized.");
        }

        account.deposit(amount);
        String refNum = SecurityUtil.generateReferenceNumber();
        String txId = SecurityUtil.generateId("TXN");
        String memo = (description != null && !description.trim().isEmpty()) ? description.trim() : "Instant Online Deposit";

        Transaction tx = new Transaction(
                txId,
                TransactionType.DEPOSIT,
                amount,
                "ONLINE_GATEWAY",
                accountNumber,
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

    public Map<String, Object> withdraw(String customerId, String accountNumber, double amount, String description) {
        if (amount <= 0) {
            throw new ValidationException("Withdrawal amount must be strictly greater than $0.00.");
        }

        Account account = dataStore.getAccountByNumber(accountNumber);
        if (account == null || !account.getCustomerId().equals(customerId)) {
            throw new AccountNotFoundException("Account #" + accountNumber + " not found or unauthorized.");
        }

        account.withdraw(amount);
        String refNum = SecurityUtil.generateReferenceNumber();
        String txId = SecurityUtil.generateId("TXN");
        String memo = (description != null && !description.trim().isEmpty()) ? description.trim() : "Cash / ATM Withdrawal";

        Transaction tx = new Transaction(
                txId,
                TransactionType.WITHDRAWAL,
                amount,
                accountNumber,
                "CASH_DISPENSER",
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
