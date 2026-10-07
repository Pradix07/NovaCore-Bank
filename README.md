# 🏦 NovaCore Bank of India

NovaCore Bank of India is a digital banking management system designed for the Indian banking system. It allows users to open bank accounts, transfer money using UPI and IMPS, deposit and withdraw cash, apply for loans, and manage debit cards.

You can use this system in two ways:
1. **Desktop Application** (A clean desktop window)
2. **Web Portal** (Accessible through your web browser)

---

## 🚀 How to Run the Project

### Simple One-Click Run (Windows)
1. Double-click the file named **`compile_and_run.bat`**.
2. Type one of the numbers and press Enter:
   - Type **`1`** for the **Web Banking Portal**
   - Type **`2`** for the **Desktop Application**
   - Type **`3`** to run **Both** at the same time

### Or Run via PowerShell
Open PowerShell in this folder and run:
```powershell
.\compile_and_run.ps1
```

---

## 🌐 Web Portal Links

Once the web server is running, open your web browser and visit:

| Page | Web Address | What You Can Do |
| :--- | :--- | :--- |
| **Home & Sign In** | [http://localhost:8080/](http://localhost:8080/) | Sign in, view demo logins, or create a new bank account |
| **Customer Portal** | [http://localhost:8080/customer-dashboard.html](http://localhost:8080/customer-dashboard.html) | Send money, deposit, withdraw, view statements, lock cards |
| **Admin Portal** | [http://localhost:8080/admin-dashboard.html](http://localhost:8080/admin-dashboard.html) | Approve loans, view all accounts, manage interest rates |

---

## 🔑 Demo Login Accounts

You can log in with any of these pre-created accounts:

| User Type | Username | Password | Account Details |
| :--- | :--- | :--- | :--- |
| 🛡️ **Bank Manager (Admin)** | `admin` | `admin123` | Manager account to view reports and approve loans |
| 👤 **Regular Customer** | `aarav.patel` | `customer123` | Has Savings & Business accounts with RuPay debit card |
| 👤 **Customer & Investor** | `priya.sharma` | `customer123` | Has Savings account and Gold Bond investment |
| 🎓 **College Student** | `rohan.verma` | `customer123` | Student zero-balance account (IIT Delhi) |

*(You can also register a brand new account with your own name from the home page)*

---

## 🏛️ Account Types Available

- **Savings Account:** For everyday savings with 4% annual interest. Minimum balance is ₹1,000.
- **Current Account:** For businesses and shops with GST number support and overdraft facility. Minimum balance is ₹5,000.
- **Student Account:** For school and college students with zero minimum balance and free debit card.

---

## ✨ Main Features

- **Indian Currency & Format:** All balances shown in Indian Rupees (₹) with Indian numbering (Lakhs and Crores).
- **Fast Money Transfers:** Transfer money using UPI ID, IMPS, NEFT, or RTGS with branch IFSC codes.
- **Debit Card Controls:** View card details, change security PIN, or temporarily lock/freeze your card.
- **Fixed Deposits & Gold:** Invest money in Fixed Deposits (7.5% interest) and Sovereign Gold Bonds.
- **Loans & EMI:** Apply for Personal, Home, or Education loans with monthly EMI calculation.
- **Safe & Secure:** Passwords and transactions are protected with security checks.

---

## 📋 System Requirements

- A computer running Windows, macOS, or Linux.
- **Java (JDK 17 or higher)** installed.
- Any modern web browser (Google Chrome, Microsoft Edge, Brave, Firefox).
