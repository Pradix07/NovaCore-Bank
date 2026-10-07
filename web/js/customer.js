// Customer Dashboard Engine
let currentOverviewData = null;

document.addEventListener('DOMContentLoaded', () => {
  const user = Auth.requireAuth('CUSTOMER');
  if (!user) return;

  // Initialize UI
  document.getElementById('nav-user-name').innerText = user.fullName;
  document.getElementById('user-avatar-text').innerText = user.fullName.charAt(0).toUpperCase();

  setupNavigation();
  loadCustomerOverview();
  setupEmiCalculator();
  setupForms();
});

function setupNavigation() {
  const navItems = document.querySelectorAll('.sidebar-menu .nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      navItems.forEach(n => n.classList.remove('active'));
      item.classList.add('active');

      const targetTab = item.getAttribute('data-tab');
      document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
      const activeTab = document.getElementById(targetTab);
      if (activeTab) {
        activeTab.classList.add('active');
        document.getElementById('topbar-title').innerText = item.querySelector('span').innerText;

        if (targetTab === 'tab-transactions') {
          loadCustomerTransactions();
        } else if (targetTab === 'tab-overview') {
          loadCustomerOverview();
        }
      }
    });
  });
}

async function loadCustomerOverview() {
  try {
    const res = await fetch('/api/customer/overview', {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });

    if (res.status === 401) {
      Auth.clearSession();
      window.location.href = '/index.html';
      return;
    }

    const data = await res.json();
    currentOverviewData = data;

    // Populate Overview Stats
    document.getElementById('stat-total-balance').innerText = '₹' + data.totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 });
    document.getElementById('stat-total-inflow').innerText = '+₹' + data.totalInflow.toLocaleString('en-IN', { minimumFractionDigits: 2 });
    document.getElementById('stat-total-outflow').innerText = '-₹' + data.totalOutflow.toLocaleString('en-IN', { minimumFractionDigits: 2 });
    document.getElementById('stat-active-accounts').innerText = data.accounts.length;

    // Populate Accounts Grid
    renderAccounts(data.accounts);

    // Populate Virtual Card
    if (data.accounts.length > 0) {
      renderVirtualCard(data.accounts[0], data.profile);
    }

    // Populate Recent Transactions
    renderRecentTransactions(data.recentTransactions);

    // Populate Services Summary
    renderLoansSummary(data.loans);
    renderInvestmentsSummary(data.investments);

    // Populate Form Account dropdowns
    populateAccountDropdowns(data.accounts);

    // Populate Profile Form
    populateProfileForm(data.profile);

  } catch (err) {
    console.error('Error loading overview:', err);
    Toast.error('Failed to load banking data');
  }
}

function renderAccounts(accounts) {
  const container = document.getElementById('accounts-cards-container');
  if (!container) return;

  container.innerHTML = accounts.map(acc => `
    <div class="glass-card" style="position: relative; overflow: hidden; border-left: 4px solid ${acc.accountType === 'SAVINGS' ? 'var(--primary)' : 'var(--secondary)'};">
      <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
        <div>
          <span class="badge ${acc.accountType === 'SAVINGS' ? 'badge-primary' : 'badge-info'}">${acc.accountType} ACCOUNT</span>
          <h4 style="margin-top: 6px; font-size: 1.1rem;">#${acc.accountNumber}</h4>
        </div>
        <span class="badge ${acc.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}">${acc.status}</span>
      </div>
      <div style="margin: 16px 0;">
        <div style="font-size: 0.8rem; color: var(--text-muted);">Current Balance</div>
        <div style="font-size: 1.7rem; font-weight: 800; color: #fff;">₹${acc.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
      </div>
      <div style="display: flex; justify-content: space-between; font-size: 0.8rem; color: var(--text-muted); border-top: 1px solid var(--border-color); padding-top: 10px;">
        <span>${acc.accountType === 'SAVINGS' ? 'Interest: ' + acc.interestRate + '%' : 'Overdraft: ₹' + acc.overdraftLimit}</span>
        <span>Card: •••• ${acc.cardNumber ? acc.cardNumber.slice(-4) : 'N/A'}</span>
      </div>
    </div>
  `).join('');
}

function renderVirtualCard(account, profile) {
  const cardElem = document.getElementById('primary-virtual-card');
  if (!cardElem) return;

  const cardNum = account.cardNumber || '4532 8899 1234 5678';
  const formattedNum = cardNum.replace(/(\d{4})/g, '$1 ').trim();

  document.getElementById('card-number-display').innerText = formattedNum;
  document.getElementById('card-holder-display').innerText = profile.fullName;
  document.getElementById('card-expiry-display').innerText = account.cardExpiry || '12/28';
  document.getElementById('card-cvv-display').innerText = account.cardCvv || '742';

  const freezeBtn = document.getElementById('btn-freeze-card');
  if (freezeBtn) {
    if (account.cardFrozen) {
      cardElem.classList.add('frozen');
      freezeBtn.innerText = '❄️ Unfreeze Card';
      freezeBtn.className = 'btn btn-secondary btn-sm';
    } else {
      cardElem.classList.remove('frozen');
      freezeBtn.innerText = '🔒 Freeze Card';
      freezeBtn.className = 'btn btn-danger btn-sm';
    }
  }
}

function renderRecentTransactions(transactions) {
  const container = document.getElementById('recent-transactions-list');
  if (!container) return;

  if (transactions.length === 0) {
    container.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 30px;">No transactions recorded yet.</div>';
    return;
  }

  container.innerHTML = transactions.map(t => {
    const isCredit = ['DEPOSIT', 'TRANSFER_IN', 'INVESTMENT_RETURN', 'LOAN_DISBURSEMENT'].includes(t.type);
    return `
      <div class="tx-item">
        <div style="display: flex; align-items: center; gap: 14px;">
          <div class="tx-icon-box" style="background: ${isCredit ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)'}; color: ${isCredit ? 'var(--success)' : 'var(--danger)'};">
            ${isCredit ? '↓' : '↑'}
          </div>
          <div>
            <div style="font-weight: 600; color: #fff;">${t.description || t.type}</div>
            <div style="font-size: 0.78rem; color: var(--text-muted);">${t.timestamp} • Ref: ${t.referenceNumber || t.id}</div>
          </div>
        </div>
        <div style="text-align: right;">
          <div style="font-weight: 700; color: ${isCredit ? 'var(--success)' : '#fff'};">
            ${isCredit ? '+' : '-'}₹${t.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <span class="badge badge-success" style="font-size: 0.65rem; padding: 2px 6px;">SUCCESS</span>
        </div>
      </div>
    `;
  }).join('');
}

function renderLoansSummary(loans) {
  const container = document.getElementById('customer-loans-table-body');
  if (!container) return;

  if (loans.length === 0) {
    container.innerHTML = '<tr><td colspan="6" style="text-align:center; color: var(--text-muted);">No loan applications on record. Apply below.</td></tr>';
    return;
  }

  container.innerHTML = loans.map(l => `
    <tr>
      <td><strong>${l.id}</strong></td>
      <td><span class="badge badge-primary">${l.loanType}</span></td>
      <td><strong>₹${l.amount.toLocaleString('en-IN')}</strong></td>
      <td>₹${l.monthlyEmi.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}/mo</td>
      <td>
        <span class="badge ${l.status === 'APPROVED' ? 'badge-success' : (l.status === 'PENDING' ? 'badge-warning' : 'badge-danger')}">
          ${l.status}
        </span>
      </td>
      <td>
        ${l.status === 'APPROVED' ? `
          <button class="btn btn-primary btn-sm" onclick="openPayEmiModal('${l.id}', ${l.monthlyEmi})">Pay EMI</button>
        ` : `<span style="font-size: 0.8rem; color: var(--text-muted);">${l.status === 'PENDING' ? 'Under Review' : 'Closed'}</span>`}
      </td>
    </tr>
  `).join('');
}

function renderInvestmentsSummary(investments) {
  const container = document.getElementById('customer-investments-table-body');
  if (!container) return;

  if (investments.length === 0) {
    container.innerHTML = '<tr><td colspan="6" style="text-align:center; color: var(--text-muted);">No active wealth investments. Create one below.</td></tr>';
    return;
  }

  container.innerHTML = investments.map(inv => `
    <tr>
      <td><strong>${inv.name}</strong></td>
      <td><span class="badge badge-info">${inv.type}</span></td>
      <td>₹${inv.principalAmount.toLocaleString('en-IN')}</td>
      <td><strong style="color: var(--success);">${inv.interestRate}% p.a.</strong></td>
      <td><strong>₹${inv.currentMaturityValue.toLocaleString('en-IN')}</strong></td>
      <td>
        ${inv.status === 'ACTIVE' ? `
          <button class="btn btn-secondary btn-sm" onclick="openWithdrawInvModal('${inv.id}', ${inv.currentMaturityValue})">Withdraw / Liquidate</button>
        ` : `<span class="badge badge-warning">${inv.status}</span>`}
      </td>
    </tr>
  `).join('');
}

function populateAccountDropdowns(accounts) {
  const selects = ['transfer-from-account', 'deposit-account', 'withdraw-account', 'loan-emi-account', 'invest-from-account', 'inv-payout-account'];
  selects.forEach(id => {
    const sel = document.getElementById(id);
    if (sel) {
      sel.innerHTML = accounts.map(a => `
        <option value="${a.accountNumber}">#${a.accountNumber} (${a.accountType}) - ₹${a.balance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</option>
      `).join('');
    }
  });
}

function populateProfileForm(profile) {
  const setVal = (id, val) => { const el = document.getElementById(id); if (el) el.value = val || ''; };
  setVal('profile-fullname', profile.fullName);
  setVal('profile-email', profile.email);
  setVal('profile-phone', profile.phone);
  setVal('profile-address', profile.address);
}

// Transaction History Tab
async function loadCustomerTransactions() {
  const filterType = document.getElementById('filter-tx-type')?.value || 'ALL';
  const search = document.getElementById('search-tx-input')?.value || '';

  try {
    const res = await fetch(`/api/customer/transactions?type=${encodeURIComponent(filterType)}&search=${encodeURIComponent(search)}`, {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });

    const txs = await res.json();
    const tbody = document.getElementById('full-transactions-table-body');
    if (!tbody) return;

    if (txs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted); padding: 30px;">No matching transactions found.</td></tr>';
      return;
    }

    tbody.innerHTML = txs.map(t => {
      const isCredit = ['DEPOSIT', 'TRANSFER_IN', 'INVESTMENT_RETURN', 'LOAN_DISBURSEMENT'].includes(t.type);
      return `
        <tr>
          <td><code style="color: var(--primary); font-size: 0.85rem;">${t.referenceNumber || t.id}</code></td>
          <td><span style="font-size: 0.85rem; color: var(--text-muted);">${t.timestamp}</span></td>
          <td><span class="badge ${isCredit ? 'badge-success' : 'badge-primary'}">${t.type}</span></td>
          <td>${t.description}</td>
          <td><code style="font-size: 0.85rem;">${t.fromAccount} → ${t.toAccount}</code></td>
          <td>
            <strong style="color: ${isCredit ? 'var(--success)' : '#fff'};">
              ${isCredit ? '+' : '-'}₹${t.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
            </strong>
          </td>
          <td><span class="badge badge-success">${t.status}</span></td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    Toast.error('Failed to load transaction history');
  }
}

// EMI Calculator
function setupEmiCalculator() {
  const amountInput = document.getElementById('calc-loan-amount');
  const monthsInput = document.getElementById('calc-loan-months');
  const rateInput = document.getElementById('calc-loan-rate');

  if (!amountInput || !monthsInput) return;

  const update = () => {
    const p = parseFloat(amountInput.value) || 0;
    const n = parseInt(monthsInput.value) || 12;
    const r = (parseFloat(rateInput?.value || 8.5) / 100) / 12;

    document.getElementById('display-calc-amount').innerText = '₹' + p.toLocaleString('en-IN');
    document.getElementById('display-calc-months').innerText = n + ' Months';

    let emi = 0;
    if (r === 0) {
      emi = p / n;
    } else {
      emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
    }

    const totalPayable = emi * n;
    const totalInterest = totalPayable - p;

    document.getElementById('display-calc-emi').innerText = '₹' + (isNaN(emi) ? '0.00' : emi.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
    document.getElementById('display-calc-total').innerText = '₹' + (isNaN(totalPayable) ? '0.00' : totalPayable.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
    document.getElementById('display-calc-interest').innerText = '₹' + (isNaN(totalInterest) ? '0.00' : totalInterest.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
  };

  amountInput.addEventListener('input', update);
  monthsInput.addEventListener('input', update);
  update();
}

// Setup Form Handlers
function setupForms() {
  // Transfer Form
  const transferForm = document.getElementById('transfer-funds-form');
  if (transferForm) {
    transferForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fromAccount = document.getElementById('transfer-from-account').value;
      const toAccount = document.getElementById('transfer-to-account').value;
      const amount = parseFloat(document.getElementById('transfer-amount').value);
      const description = document.getElementById('transfer-description').value;
      const securityPin = document.getElementById('transfer-pin').value;

      try {
        const res = await fetch('/api/customer/transfer', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify({ fromAccount, toAccount, amount, description, securityPin })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Transfer failed');

        Toast.success(`Transferred ₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} to #${toAccount} successfully! [Ref: ${data.referenceNumber}]`);
        closeModal('modal-transfer');
        transferForm.reset();
        loadCustomerOverview();
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }

  // Deposit Form
  const depositForm = document.getElementById('deposit-funds-form');
  if (depositForm) {
    depositForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const accountNumber = document.getElementById('deposit-account').value;
      const amount = parseFloat(document.getElementById('deposit-amount').value);
      const description = document.getElementById('deposit-description').value;

      try {
        const res = await fetch('/api/customer/deposit', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify({ accountNumber, amount, description })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Deposit failed');

        Toast.success(`Deposit of ₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} successful! [Ref: ${data.referenceNumber}]`);
        closeModal('modal-deposit');
        depositForm.reset();
        loadCustomerOverview();
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }

  // Withdraw Form
  const withdrawForm = document.getElementById('withdraw-funds-form');
  if (withdrawForm) {
    withdrawForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const accountNumber = document.getElementById('withdraw-account').value;
      const amount = parseFloat(document.getElementById('withdraw-amount').value);
      const description = document.getElementById('withdraw-description').value;

      try {
        const res = await fetch('/api/customer/withdraw', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify({ accountNumber, amount, description })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Withdrawal failed');

        Toast.success(`Withdrawal of ₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} successful! [Ref: ${data.referenceNumber}]`);
        closeModal('modal-withdraw');
        withdrawForm.reset();
        loadCustomerOverview();
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }

  // Loan Application Form
  const loanForm = document.getElementById('apply-loan-form');
  if (loanForm) {
    loanForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const loanType = document.getElementById('loan-type-select').value;
      const amount = parseFloat(document.getElementById('calc-loan-amount').value);
      const tenureMonths = parseInt(document.getElementById('calc-loan-months').value);
      const purpose = document.getElementById('loan-purpose-input').value;

      try {
        const res = await fetch('/api/customer/loans/apply', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify({ loanType, amount, tenureMonths, purpose })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Loan application failed');

        Toast.success(`Loan application submitted for ₹${amount.toLocaleString('en-IN')}! Status: PENDING`);
        loanForm.reset();
        loadCustomerOverview();
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }

  // Create Investment Form
  const investForm = document.getElementById('create-investment-form');
  if (investForm) {
    investForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fromAccount = document.getElementById('invest-from-account').value;
      const type = document.getElementById('invest-type-select').value;
      const name = document.getElementById('invest-name-input').value;
      const amount = parseFloat(document.getElementById('invest-amount-input').value);
      const durationMonths = parseInt(document.getElementById('invest-tenure-select').value);

      try {
        const res = await fetch('/api/customer/investments/create', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify({ fromAccount, type, name, amount, durationMonths })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Investment failed');

        Toast.success(`Investment '${name}' booked successfully!`);
        closeModal('modal-invest');
        investForm.reset();
        loadCustomerOverview();
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }

  // Profile Update Form
  const profileForm = document.getElementById('customer-profile-form');
  if (profileForm) {
    profileForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const fullName = document.getElementById('profile-fullname').value;
      const email = document.getElementById('profile-email').value;
      const phone = document.getElementById('profile-phone').value;
      const address = document.getElementById('profile-address').value;
      const currentPassword = document.getElementById('profile-current-pass').value;
      const newPassword = document.getElementById('profile-new-pass').value;
      const securityPin = document.getElementById('profile-pin').value;

      try {
        const res = await fetch('/api/customer/profile/update', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify({ fullName, email, phone, address, currentPassword, newPassword, securityPin })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Profile update failed');

        Toast.success('Profile details updated successfully!');
        loadCustomerOverview();
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }
}

// Toggle Card Freeze Action
async function togglePrimaryCardFreeze() {
  if (!currentOverviewData || !currentOverviewData.accounts || currentOverviewData.accounts.length === 0) return;
  const primaryAcc = currentOverviewData.accounts[0];

  try {
    const res = await fetch('/api/customer/cards/toggle-freeze', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${Auth.getToken()}`
      },
      body: JSON.stringify({ accountNumber: primaryAcc.accountNumber })
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.message);

    Toast.info(`Card is now ${data.cardFrozen ? 'FROZEN' : 'ACTIVE'}`);
    loadCustomerOverview();
  } catch (err) {
    Toast.error(err.message);
  }
}

// Open Pay EMI Modal
function openPayEmiModal(loanId, emiAmount) {
  document.getElementById('pay-emi-loan-id').value = loanId;
  document.getElementById('pay-emi-amount-display').innerText = '₹' + emiAmount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  openModal('modal-pay-emi');
}

// Submit Pay EMI
async function submitPayEmi() {
  const loanId = document.getElementById('pay-emi-loan-id').value;
  const fromAccount = document.getElementById('loan-emi-account').value;

  try {
    const res = await fetch('/api/customer/loans/pay-emi', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${Auth.getToken()}`
      },
      body: JSON.stringify({ loanId, fromAccount })
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'EMI payment failed');

    Toast.success(`EMI payment of ₹${data.emiAmount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} completed! [Ref: ${data.referenceNumber}]`);
    closeModal('modal-pay-emi');
    loadCustomerOverview();
  } catch (err) {
    Toast.error(err.message);
  }
}

// Open Withdraw Investment Modal
function openWithdrawInvModal(invId, amount) {
  document.getElementById('withdraw-inv-id').value = invId;
  document.getElementById('withdraw-inv-amount-display').innerText = '₹' + amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  openModal('modal-withdraw-inv');
}

// Submit Withdraw Investment
async function submitWithdrawInv() {
  const investmentId = document.getElementById('withdraw-inv-id').value;
  const toAccount = document.getElementById('inv-payout-account').value;

  try {
    const res = await fetch('/api/customer/investments/withdraw', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${Auth.getToken()}`
      },
      body: JSON.stringify({ investmentId, toAccount })
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Withdrawal failed');

    Toast.success(`Investment payout of ₹${data.payoutAmount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} transferred to #${toAccount}!`);
    closeModal('modal-withdraw-inv');
    loadCustomerOverview();
  } catch (err) {
    Toast.error(err.message);
  }
}

// Export Transactions to CSV
function exportTransactionsCSV() {
  fetch('/api/customer/transactions', {
    headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
  })
  .then(res => res.json())
  .then(txs => {
    let csv = 'Transaction ID,Date & Time,Type,Description,From Account,To Account,Amount,Status,Reference\n';
    txs.forEach(t => {
      csv += `"${t.id}","${t.timestamp}","${t.type}","${(t.description||'').replace(/"/g, '""')}","${t.fromAccount}","${t.toAccount}","${t.amount}","${t.status}","${t.referenceNumber}"\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `bank_statement_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    Toast.success('Statement exported to CSV!');
  });
}
