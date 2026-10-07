// Admin Dashboard Engine
let currentMetrics = null;

document.addEventListener('DOMContentLoaded', () => {
  const user = Auth.requireAuth('ADMIN');
  if (!user) return;

  // Initialize UI
  document.getElementById('admin-nav-name').innerText = user.fullName;
  document.getElementById('admin-avatar-text').innerText = user.fullName.charAt(0).toUpperCase();

  setupAdminNavigation();
  loadAdminMetrics();
  setupAdminForms();
});

function setupAdminNavigation() {
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
        document.getElementById('admin-topbar-title').innerText = item.querySelector('span').innerText;

        if (targetTab === 'tab-users') loadAllUsers();
        else if (targetTab === 'tab-transactions') loadAllTransactions();
        else if (targetTab === 'tab-loans') loadAllLoans();
        else if (targetTab === 'tab-settings') loadSystemSettings();
        else if (targetTab === 'tab-audit') loadAuditLogs();
        else if (targetTab === 'tab-metrics') loadAdminMetrics();
      }
    });
  });
}

async function loadAdminMetrics() {
  try {
    const res = await fetch('/api/admin/metrics', {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });

    if (res.status === 401 || res.status === 403) {
      Auth.clearSession();
      window.location.href = '/index.html';
      return;
    }

    const data = await res.json();
    currentMetrics = data;

    // Stat cards
    document.getElementById('stat-total-deposits').innerText = '₹' + data.totalDeposits.toLocaleString('en-IN', { minimumFractionDigits: 2 });
    document.getElementById('stat-total-customers').innerText = data.totalCustomers;
    document.getElementById('stat-total-loans').innerText = '₹' + data.totalLoanDisbursed.toLocaleString('en-IN', { minimumFractionDigits: 2 });
    document.getElementById('stat-total-volume').innerText = '₹' + data.totalTransactionVolume.toLocaleString('en-IN', { minimumFractionDigits: 2 });

    // Render Charts
    const dateLabels = Object.keys(data.volumeByDate || {});
    const dateValues = Object.values(data.volumeByDate || {});
    BankCharts.renderLineChart('admin-volume-chart', dateLabels, dateValues, '#6366f1');

    const typeLabels = Object.keys(data.countByType || {});
    const typeValues = Object.values(data.countByType || {});
    BankCharts.renderDoughnutChart('admin-type-chart', typeLabels, typeValues);

  } catch (err) {
    console.error('Error loading metrics:', err);
    Toast.error('Failed to load system metrics');
  }
}

// 1. User Management
async function loadAllUsers() {
  try {
    const res = await fetch('/api/admin/users', {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });
    const users = await res.json();

    const tbody = document.getElementById('admin-users-table-body');
    if (!tbody) return;

    tbody.innerHTML = users.map(u => `
      <tr>
        <td>
          <div style="display: flex; align-items: center; gap: 10px;">
            <div class="user-avatar" style="width: 28px; height: 28px; font-size: 0.75rem;">
              ${u.fullName.charAt(0).toUpperCase()}
            </div>
            <div>
              <strong>${u.fullName}</strong>
              <div style="font-size: 0.78rem; color: var(--text-muted);">${u.email}</div>
            </div>
          </div>
        </td>
        <td><code>${u.username}</code></td>
        <td><span class="badge ${u.role === 'ADMIN' ? 'badge-primary' : 'badge-info'}">${u.role}</span></td>
        <td>
          ${u.role === 'CUSTOMER' ? (
            u.accounts && u.accounts.length > 0
              ? u.accounts.map(a => `<span class="badge badge-success" style="font-size: 0.7rem; margin-right: 4px;">#${a.accountNumber} (₹${a.balance.toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })})</span>`).join('')
              : '<span style="color: var(--text-muted);">None</span>'
          ) : '<span style="color: var(--text-muted);">Admin Privileges</span>'}
        </td>
        <td>
          <span class="badge ${u.active ? 'badge-success' : 'badge-danger'}">
            ${u.active ? 'ACTIVE' : 'SUSPENDED'}
          </span>
        </td>
        <td>
          <div style="display: flex; gap: 6px;">
            <button class="btn btn-secondary btn-sm" onclick="toggleUserStatus('${u.id}', ${!u.active})">
              ${u.active ? 'Suspend' : 'Activate'}
            </button>
            ${u.username !== 'admin' ? `
              <button class="btn btn-danger btn-sm" onclick="deleteUserAccount('${u.id}', '${u.username}')">Delete</button>
            ` : ''}
          </div>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    Toast.error('Failed to load users');
  }
}

async function toggleUserStatus(userId, newActive) {
  try {
    const res = await fetch('/api/admin/users/status', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${Auth.getToken()}`
      },
      body: JSON.stringify({ userId, active: newActive })
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.message);

    Toast.success(`User status updated to ${newActive ? 'ACTIVE' : 'SUSPENDED'}`);
    loadAllUsers();
    loadAdminMetrics();
  } catch (err) {
    Toast.error(err.message);
  }
}

async function deleteUserAccount(userId, username) {
  if (!confirm(`Are you sure you want to permanently delete user '${username}' and all associated accounts?`)) {
    return;
  }

  try {
    const res = await fetch('/api/admin/users/delete', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${Auth.getToken()}`
      },
      body: JSON.stringify({ userId })
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.message);

    Toast.success(`User '${username}' deleted successfully`);
    loadAllUsers();
    loadAdminMetrics();
  } catch (err) {
    Toast.error(err.message);
  }
}

// 2. Transaction Monitoring
async function loadAllTransactions() {
  const filterType = document.getElementById('admin-filter-tx-type')?.value || 'ALL';
  const search = document.getElementById('admin-search-tx-input')?.value || '';

  try {
    const res = await fetch(`/api/admin/transactions?type=${encodeURIComponent(filterType)}&search=${encodeURIComponent(search)}`, {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });

    const txs = await res.json();
    const tbody = document.getElementById('admin-transactions-table-body');
    if (!tbody) return;

    if (txs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted); padding: 30px;">No transactions recorded.</td></tr>';
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
          <td><strong>₹${t.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></td>
          <td><span class="badge badge-success">${t.status}</span></td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    Toast.error('Failed to load transaction monitoring stream');
  }
}

// 3. Loan Approval Management
async function loadAllLoans() {
  try {
    const res = await fetch('/api/admin/loans', {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });
    const loans = await res.json();

    const tbody = document.getElementById('admin-loans-table-body');
    if (!tbody) return;

    if (loans.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color: var(--text-muted); padding: 30px;">No loan applications found.</td></tr>';
      return;
    }

    tbody.innerHTML = loans.map(l => `
      <tr>
        <td><strong>${l.id}</strong></td>
        <td>
          <strong>${l.customerName}</strong>
          <div style="font-size: 0.75rem; color: var(--text-muted);">ID: ${l.customerId}</div>
        </td>
        <td><span class="badge badge-primary">${l.loanType}</span></td>
        <td><strong>₹${l.amount.toLocaleString('en-IN')}</strong></td>
        <td>${l.tenureMonths} Mo @ ${l.interestRate}%</td>
        <td>₹${l.monthlyEmi.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
        <td>
          <span class="badge ${l.status === 'APPROVED' ? 'badge-success' : (l.status === 'PENDING' ? 'badge-warning' : 'badge-danger')}">
            ${l.status}
          </span>
        </td>
        <td>
          ${l.status === 'PENDING' ? `
            <div style="display: flex; gap: 6px;">
              <button class="btn btn-success btn-sm" onclick="openReviewLoanModal('${l.id}', 'APPROVE', '${l.customerName}', ${l.amount})">Approve</button>
              <button class="btn btn-danger btn-sm" onclick="openReviewLoanModal('${l.id}', 'REJECT', '${l.customerName}', ${l.amount})">Reject</button>
            </div>
          ` : `<span style="font-size: 0.8rem; color: var(--text-muted);">${l.decidedAt || 'Decided'}</span>`}
        </td>
      </tr>
    `).join('');
  } catch (err) {
    Toast.error('Failed to load loan applications');
  }
}

function openReviewLoanModal(loanId, action, customerName, amount) {
  document.getElementById('review-loan-id').value = loanId;
  document.getElementById('review-loan-action').value = action;
  document.getElementById('review-loan-title').innerText = `${action} Loan Application (${loanId})`;
  document.getElementById('review-loan-desc').innerText = `Action for customer ${customerName} for requested amount of ₹${amount.toLocaleString('en-IN')}`;
  openModal('modal-review-loan');
}

async function submitReviewLoan() {
  const loanId = document.getElementById('review-loan-id').value;
  const action = document.getElementById('review-loan-action').value;
  const remarks = document.getElementById('review-loan-remarks').value;

  try {
    const res = await fetch('/api/admin/loans/review', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${Auth.getToken()}`
      },
      body: JSON.stringify({ loanId, action, remarks })
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.message);

    Toast.success(`Loan #${loanId} has been ${action === 'APPROVE' ? 'APPROVED & DISBURSED' : 'REJECTED'}!`);
    closeModal('modal-review-loan');
    loadAllLoans();
    loadAdminMetrics();
  } catch (err) {
    Toast.error(err.message);
  }
}

// 4. System Settings
async function loadSystemSettings() {
  try {
    const res = await fetch('/api/admin/settings', {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });
    const s = await res.json();

    const setVal = (id, val) => { const el = document.getElementById(id); if (el) el.value = val; };
    setVal('settings-bank-name', s.bankName);
    setVal('settings-savings-rate', s.defaultSavingsInterestRate);
    setVal('settings-loan-rate', s.defaultLoanInterestRate);
    setVal('settings-fd-rate', s.defaultFdInterestRate);
    setVal('settings-daily-limit', s.dailyTransferLimit);
    setVal('settings-single-limit', s.perTransactionLimit);
    setVal('settings-min-balance', s.minSavingsBalance);
    setVal('settings-fee-percent', s.transactionFeePercent);
    setVal('settings-support-email', s.supportEmail);
    setVal('settings-support-phone', s.supportPhone);

    const mMode = document.getElementById('settings-maintenance-mode');
    if (mMode) mMode.checked = !!s.maintenanceMode;

  } catch (err) {
    Toast.error('Failed to load system settings');
  }
}

// 5. Audit Logs
async function loadAuditLogs() {
  try {
    const res = await fetch('/api/admin/audit-logs', {
      headers: { 'Authorization': `Bearer ${Auth.getToken()}` }
    });
    const logs = await res.json();

    const tbody = document.getElementById('admin-audit-table-body');
    if (!tbody) return;

    tbody.innerHTML = logs.map(l => `
      <tr>
        <td><span style="font-size: 0.85rem; color: var(--text-muted);">${l.timestamp}</span></td>
        <td><strong>${l.actorName}</strong> (${l.actorRole})</td>
        <td><span class="badge badge-primary">${l.action}</span></td>
        <td>${l.details}</td>
        <td><code>${l.ipAddress || '127.0.0.1'}</code></td>
      </tr>
    `).join('');
  } catch (err) {
    Toast.error('Failed to load audit logs');
  }
}

function setupAdminForms() {
  // Create User Form
  const createUserForm = document.getElementById('admin-create-user-form');
  if (createUserForm) {
    createUserForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const username = document.getElementById('new-user-username').value;
      const password = document.getElementById('new-user-password').value;
      const fullName = document.getElementById('new-user-fullname').value;
      const email = document.getElementById('new-user-email').value;
      const phone = document.getElementById('new-user-phone').value;
      const role = document.getElementById('new-user-role').value;
      const accountType = document.getElementById('new-user-acctype').value;
      const initialBalance = parseFloat(document.getElementById('new-user-initbal').value || 1000);

      try {
        const res = await fetch('/api/admin/users/create', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify({ username, password, fullName, email, phone, role, accountType, initialBalance })
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'User creation failed');

        Toast.success(`User '${username}' (${role}) created successfully!`);
        closeModal('modal-create-user');
        createUserForm.reset();
        loadAllUsers();
        loadAdminMetrics();
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }

  // System Settings Form
  const settingsForm = document.getElementById('system-settings-form');
  if (settingsForm) {
    settingsForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const payload = {
        bankName: document.getElementById('settings-bank-name').value,
        defaultSavingsInterestRate: parseFloat(document.getElementById('settings-savings-rate').value),
        defaultLoanInterestRate: parseFloat(document.getElementById('settings-loan-rate').value),
        defaultFdInterestRate: parseFloat(document.getElementById('settings-fd-rate').value),
        dailyTransferLimit: parseFloat(document.getElementById('settings-daily-limit').value),
        perTransactionLimit: parseFloat(document.getElementById('settings-single-limit').value),
        minSavingsBalance: parseFloat(document.getElementById('settings-min-balance').value),
        transactionFeePercent: parseFloat(document.getElementById('settings-fee-percent').value),
        maintenanceMode: document.getElementById('settings-maintenance-mode').checked,
        supportEmail: document.getElementById('settings-support-email').value,
        supportPhone: document.getElementById('settings-support-phone').value
      };

      try {
        const res = await fetch('/api/admin/settings/update', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${Auth.getToken()}`
          },
          body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Settings update failed');

        Toast.success('System configuration saved and applied globally!');
      } catch (err) {
        Toast.error(err.message);
      }
    });
  }
}

// Export All System Transactions CSV
function exportAllTransactionsCSV() {
  fetch('/api/admin/transactions', {
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
    a.download = `admin_full_transactions_audit_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    Toast.success('Global transaction audit report exported to CSV!');
  });
}
