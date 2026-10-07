-- ============================================================================
-- NOVACORE BANKING SYSTEM - RELATIONAL DATABASE SCHEMA (JDBC / SQL)
-- ============================================================================
-- Compatible with MySQL 8+, PostgreSQL, SQLite, and H2 Database
-- ============================================================================

-- 1. USERS TABLE (Handles Customer and Admin entities)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL,
    phone_number VARCHAR(32),
    address TEXT,
    pan_or_ssn VARCHAR(64),
    transaction_pin VARCHAR(64),
    occupation VARCHAR(128),
    monthly_income DOUBLE DEFAULT 0.0,
    department VARCHAR(128),
    clearance_level INT DEFAULT 0,
    is_frozen BOOLEAN DEFAULT FALSE,
    created_at VARCHAR(64) NOT NULL
);

-- 2. ACCOUNTS TABLE (Polymorphic: SavingsAccount and CheckingAccount)
CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    account_type VARCHAR(32) NOT NULL,
    balance DOUBLE NOT NULL DEFAULT 0.0,
    currency VARCHAR(16) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    interest_rate DOUBLE DEFAULT 0.0,
    minimum_balance DOUBLE DEFAULT 0.0,
    overdraft_limit DOUBLE DEFAULT 0.0,
    card_number VARCHAR(32),
    card_expiry VARCHAR(16),
    card_cvv VARCHAR(8),
    card_frozen BOOLEAN DEFAULT FALSE,
    created_at VARCHAR(64) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. TRANSACTIONS TABLE (ACID Financial Audit Trail)
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(64) PRIMARY KEY,
    source_account VARCHAR(64),
    destination_account VARCHAR(64),
    customer_id VARCHAR(64),
    amount DOUBLE NOT NULL,
    currency VARCHAR(16) DEFAULT 'INR',
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    description TEXT,
    timestamp VARCHAR(64) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 4. LOAN APPLICATIONS TABLE (Credit assessment & workflow)
CREATE TABLE IF NOT EXISTS loans (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    applicant_name VARCHAR(128) NOT NULL,
    loan_type VARCHAR(64) NOT NULL,
    amount DOUBLE NOT NULL,
    tenure_months INT NOT NULL,
    interest_rate DOUBLE NOT NULL,
    monthly_emi DOUBLE NOT NULL,
    total_payable DOUBLE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    purpose TEXT,
    admin_remarks TEXT,
    applied_at VARCHAR(64) NOT NULL,
    reviewed_at VARCHAR(64),
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. AUDIT LOGS TABLE (Compliance, Access & Security Logs)
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64),
    username VARCHAR(128),
    role VARCHAR(32),
    action VARCHAR(128) NOT NULL,
    details TEXT,
    ip_address VARCHAR(64),
    timestamp VARCHAR(64) NOT NULL
);

-- INDEXES FOR QUERY OPTIMIZATION
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_tx_source ON transactions(source_account);
CREATE INDEX IF NOT EXISTS idx_tx_dest ON transactions(destination_account);
CREATE INDEX IF NOT EXISTS idx_loans_customer ON loans(customer_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
