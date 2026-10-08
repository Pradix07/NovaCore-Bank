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
    aadhaar_number VARCHAR(64),
    upi_id VARCHAR(128),
    transaction_pin VARCHAR(64),
    occupation VARCHAR(128),
    monthly_income DOUBLE DEFAULT 0.0,
    department VARCHAR(128),
    clearance_level INT DEFAULT 0,
    is_frozen BOOLEAN DEFAULT FALSE,
    created_at VARCHAR(64),
    last_login VARCHAR(64)
);

-- 2. ACCOUNTS TABLE (Polymorphic: SavingsAccount, CurrentAccount, StudentAccount, CheckingAccount)
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
    max_withdraw_limit DOUBLE DEFAULT 50000.0,
    max_balance_limit DOUBLE DEFAULT 100000.0,
    ifsc_code VARCHAR(32) DEFAULT 'NOVA0001001',
    branch_name VARCHAR(128) DEFAULT 'Mumbai Fort Main Branch',
    upi_id VARCHAR(128),
    nominee_name VARCHAR(128),
    trade_license_or_gst VARCHAR(64),
    business_name VARCHAR(128),
    institution_name VARCHAR(128),
    student_id VARCHAR(64),
    card_number VARCHAR(32),
    card_expiry VARCHAR(16),
    card_cvv VARCHAR(8),
    card_frozen BOOLEAN DEFAULT FALSE,
    created_at VARCHAR(64),
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
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    description TEXT,
    balance_after DOUBLE DEFAULT 0.0,
    reference_number VARCHAR(64),
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
    remaining_principal DOUBLE DEFAULT 0.0,
    emis_paid INT DEFAULT 0,
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

-- 6. INVESTMENTS TABLE
CREATE TABLE IF NOT EXISTS investments (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    type VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    principal_amount DOUBLE NOT NULL,
    interest_rate DOUBLE NOT NULL,
    duration_months INT NOT NULL,
    expected_return DOUBLE NOT NULL,
    current_maturity_value DOUBLE NOT NULL,
    start_date VARCHAR(64),
    maturity_date VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7. SYSTEM SETTINGS TABLE
CREATE TABLE IF NOT EXISTS system_settings (
    id INT PRIMARY KEY,
    bank_name VARCHAR(128),
    default_savings_interest_rate DOUBLE,
    default_loan_interest_rate DOUBLE,
    default_fd_interest_rate DOUBLE,
    daily_transfer_limit DOUBLE,
    per_transaction_limit DOUBLE,
    min_savings_balance DOUBLE,
    transaction_fee_percent DOUBLE,
    maintenance_mode BOOLEAN DEFAULT FALSE,
    support_email VARCHAR(128),
    support_phone VARCHAR(64),
    currency_symbol VARCHAR(16)
);

-- INDEXES FOR QUERY OPTIMIZATION
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);
CREATE INDEX IF NOT EXISTS idx_tx_source ON transactions(source_account);
CREATE INDEX IF NOT EXISTS idx_tx_dest ON transactions(destination_account);
CREATE INDEX IF NOT EXISTS idx_tx_customer ON transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_loans_customer ON loans(customer_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_inv_customer ON investments(customer_id);
