// Authentication & Session Management
const Auth = {
  TOKEN_KEY: 'novacore_bank_token',
  USER_KEY: 'novacore_bank_user',

  getToken() {
    return localStorage.getItem(this.TOKEN_KEY);
  },

  getUser() {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },

  setSession(token, user) {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  },

  clearSession() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  },

  isAuthenticated() {
    return !!this.getToken();
  },

  async login(username, password) {
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Login failed');
      }

      this.setSession(data.token, data.user);
      return data.user;
    } catch (err) {
      throw err;
    }
  },

  async register(details) {
    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(details)
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || 'Registration failed');
      }

      this.setSession(data.token, data.user);
      return data.user;
    } catch (err) {
      throw err;
    }
  },

  async logout() {
    const token = this.getToken();
    if (token) {
      try {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` }
        });
      } catch (e) {
        console.warn('Logout network notice:', e);
      }
    }
    this.clearSession();
    window.location.href = '/index.html';
  },

  requireAuth(expectedRole = null) {
    const token = this.getToken();
    const user = this.getUser();

    if (!token || !user) {
      window.location.href = '/index.html';
      return null;
    }

    if (expectedRole && user.role !== expectedRole) {
      if (user.role === 'ADMIN') {
        window.location.href = '/admin-dashboard.html';
      } else {
        window.location.href = '/customer-dashboard.html';
      }
      return null;
    }

    return user;
  }
};

// Demo quick-login helpers
function quickLogin(username, password) {
  Toast.info(`Signing in as ${username}...`);
  Auth.login(username, password)
    .then(user => {
      Toast.success(`Welcome back, ${user.fullName}!`);
      setTimeout(() => {
        if (user.role === 'ADMIN') {
          window.location.href = '/admin-dashboard.html';
        } else {
          window.location.href = '/customer-dashboard.html';
        }
      }, 500);
    })
    .catch(err => {
      Toast.error(err.message);
    });
}
