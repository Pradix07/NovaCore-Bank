package com.bank.service.interfaces;

import com.bank.model.Investment;
import com.bank.model.LoanApplication;
import java.util.Map;

/**
 * ============================================================================
 * INTERFACE: IBankingService
 * ============================================================================
 * Demonstrates:
 * - Facade Pattern: Unified interface for high-level banking operations (Loans, Investments)
 */
public interface IBankingService {

    /**
     * Submits a new loan application.
     * @param customerId Customer ID
     * @param loanType Type of loan (PERSONAL, HOME, EDUCATION, AUTO)
     * @param amount Requested loan principal
     * @param tenureMonths Loan duration in months
     * @param purpose Reason for loan
     * @return Created LoanApplication instance
     */
    LoanApplication applyForLoan(String customerId, String loanType, double amount, int tenureMonths, String purpose);

    /**
     * Approves or rejects a loan application with optional fund disbursement.
     * @param adminId Performing admin ID
     * @param loanId Target loan application ID
     * @param action "APPROVE" or "REJECT"
     * @param remarks Admin review remarks
     * @param targetAccountNumber Disbursal account
     * @return Updated LoanApplication instance
     */
    LoanApplication reviewLoan(String adminId, String loanId, String action, String remarks, String targetAccountNumber);

    /**
     * Pays a monthly EMI towards an approved active loan.
     * @param customerId Customer ID
     * @param loanId Loan ID
     * @param fromAccountNumber Source account number
     * @return Payment confirmation details
     */
    Map<String, Object> payLoanEmi(String customerId, String loanId, String fromAccountNumber);

    /**
     * Creates a new investment (Fixed Deposit / Mutual Fund / Gold Bond).
     * @param customerId Customer ID
     * @param fromAccountNumber Source account number
     * @param type Investment type
     * @param name Investment title
     * @param amount Principal investment amount
     * @param durationMonths Investment tenure in months
     * @return Created Investment instance
     */
    Investment createInvestment(String customerId, String fromAccountNumber, String type, String name, double amount, int durationMonths);

    /**
     * Liquidates/withdraws an active investment.
     * @param customerId Customer ID
     * @param investmentId Investment ID
     * @param toAccountNumber Destination payout account
     * @return Liquidation confirmation details
     */
    Map<String, Object> withdrawInvestment(String customerId, String investmentId, String toAccountNumber);
}
