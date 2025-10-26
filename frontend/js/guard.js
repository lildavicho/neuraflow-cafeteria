// Route Guard Module - Role-based access control

class RouteGuard {
    constructor() {
        this.protectedRoutes = {
            // Admin only pages
            'dashboard.html': ['ADMIN'],
            'inventory.html': ['ADMIN'],
            'reports.html': ['ADMIN'],
            'users.html': ['ADMIN'],
            'settings.html': ['ADMIN'],
            'cameras.html': ['ADMIN'],
            
            // Admin and Comprador pages
            'pos.html': ['ADMIN', 'COMPRADOR'],
            'profile.html': ['ADMIN', 'COMPRADOR'],
            'my-orders.html': ['ADMIN', 'COMPRADOR'],
            
            // Public pages
            'login.html': null,
            'register.html': null,
            'forgot-password.html': null,
            'reset-password.html': null
        };
    }

    /**
     * Check if user is authenticated
     */
    isAuthenticated() {
        const token = localStorage.getItem('token');
        const user = this.getCurrentUser();
        return !!(token && user);
    }

    /**
     * Get current user from localStorage
     */
    getCurrentUser() {
        const userStr = localStorage.getItem('user');
        if (!userStr) return null;
        
        try {
            return JSON.parse(userStr);
        } catch (e) {
            console.error('Error parsing user data:', e);
            return null;
        }
    }

    /**
     * Check if user has specific role
     */
    hasRole(role) {
        const user = this.getCurrentUser();
        return user && user.role === role;
    }

    /**
     * Check if user has any of the specified roles
     */
    hasAnyRole(roles) {
        const user = this.getCurrentUser();
        return user && roles.includes(user.role);
    }

    /**
     * Check if current page is accessible
     */
    canAccessCurrentPage() {
        const currentPage = window.location.pathname.split('/').pop() || 'index.html';
        const allowedRoles = this.protectedRoutes[currentPage];
        
        // Public page
        if (allowedRoles === null) {
            return true;
        }
        
        // Protected page
        if (!this.isAuthenticated()) {
            return false;
        }
        
        // Check role permission
        return this.hasAnyRole(allowedRoles);
    }

    /**
     * Initialize guard for current page
     */
    init() {
        const currentPage = window.location.pathname.split('/').pop() || 'index.html';
        const allowedRoles = this.protectedRoutes[currentPage];
        
        // Skip check for public pages
        if (allowedRoles === null) {
            return true;
        }
        
        // Check authentication
        if (!this.isAuthenticated()) {
            this.redirectToLogin();
            return false;
        }
        
        // Check role permission
        if (!this.hasAnyRole(allowedRoles)) {
            this.redirectToUnauthorized();
            return false;
        }
        
        // Check token expiration
        this.checkTokenExpiration();
        
        // Set up auto logout on token expiration
        this.setupAutoLogout();
        
        return true;
    }

    /**
     * Redirect to login page
     */
    redirectToLogin() {
        const currentUrl = window.location.href;
        localStorage.setItem('redirectUrl', currentUrl);
        window.location.href = '/pages/login.html';
    }

    /**
     * Redirect to unauthorized page or appropriate dashboard
     */
    redirectToUnauthorized() {
        const user = this.getCurrentUser();
        
        if (user) {
            if (user.role === 'ADMIN') {
                window.location.href = '/pages/dashboard.html';
            } else if (user.role === 'COMPRADOR') {
                window.location.href = '/pages/pos.html';
            }
        } else {
            this.redirectToLogin();
        }
    }

    /**
     * Check if token is expired
     */
    checkTokenExpiration() {
        const token = localStorage.getItem('token');
        if (!token) return false;
        
        try {
            // Decode JWT payload
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expirationTime = payload.exp * 1000; // Convert to milliseconds
            const currentTime = Date.now();
            
            if (currentTime >= expirationTime) {
                this.handleTokenExpired();
                return false;
            }
            
            return true;
        } catch (e) {
            console.error('Error checking token expiration:', e);
            return false;
        }
    }

    /**
     * Handle expired token
     */
    async handleTokenExpired() {
        const refreshToken = localStorage.getItem('refreshToken');
        
        if (refreshToken) {
            try {
                const response = await fetch('/api/auth/refresh', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ refreshToken })
                });
                
                if (response.ok) {
                    const data = await response.json();
                    localStorage.setItem('token', data.token);
                    localStorage.setItem('refreshToken', data.refreshToken);
                    return true;
                }
            } catch (e) {
                console.error('Token refresh failed:', e);
            }
        }
        
        this.logout();
        return false;
    }

    /**
     * Set up auto logout
     */
    setupAutoLogout() {
        const token = localStorage.getItem('token');
        if (!token) return;
        
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const expirationTime = payload.exp * 1000;
            const currentTime = Date.now();
            const timeUntilExpiry = expirationTime - currentTime;
            
            if (timeUntilExpiry > 0) {
                // Set timeout to refresh token 5 minutes before expiry
                const refreshTime = Math.max(0, timeUntilExpiry - 5 * 60 * 1000);
                
                setTimeout(async () => {
                    await this.handleTokenExpired();
                }, refreshTime);
                
                // Set timeout for hard logout at token expiry
                setTimeout(() => {
                    this.logout();
                }, timeUntilExpiry);
            }
        } catch (e) {
            console.error('Error setting up auto logout:', e);
        }
    }

    /**
     * Logout user
     */
    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/pages/login.html';
    }

    /**
     * Show/hide elements based on role
     */
    applyRoleBasedVisibility() {
        const user = this.getCurrentUser();
        
        // Hide all role-specific elements first
        document.querySelectorAll('[data-role]').forEach(element => {
            element.style.display = 'none';
        });
        
        if (user) {
            // Show elements for user's role
            document.querySelectorAll(`[data-role="${user.role}"]`).forEach(element => {
                element.style.display = '';
            });
            
            // Show elements for "any" authenticated user
            document.querySelectorAll('[data-role="any"]').forEach(element => {
                element.style.display = '';
            });
        }
        
        // Update user info in UI
        this.updateUserInfo();
    }

    /**
     * Update user info in UI
     */
    updateUserInfo() {
        const user = this.getCurrentUser();
        
        if (user) {
            // Update username displays
            document.querySelectorAll('.user-name').forEach(element => {
                element.textContent = user.name || user.email;
            });
            
            // Update user email displays
            document.querySelectorAll('.user-email').forEach(element => {
                element.textContent = user.email;
            });
            
            // Update user role displays
            document.querySelectorAll('.user-role').forEach(element => {
                element.textContent = user.role;
            });
            
            // Add role badge color
            document.querySelectorAll('.user-role-badge').forEach(element => {
                element.className = 'user-role-badge badge';
                if (user.role === 'ADMIN') {
                    element.classList.add('bg-danger');
                } else {
                    element.classList.add('bg-info');
                }
                element.textContent = user.role;
            });
        }
    }

    /**
     * Check specific permission
     */
    canPerform(action) {
        const user = this.getCurrentUser();
        if (!user) return false;
        
        const permissions = {
            // Admin permissions
            'manage_users': ['ADMIN'],
            'manage_products': ['ADMIN'],
            'manage_inventory': ['ADMIN'],
            'view_reports': ['ADMIN'],
            'manage_settings': ['ADMIN'],
            'view_cameras': ['ADMIN'],
            'export_data': ['ADMIN'],
            
            // Shared permissions
            'create_sale': ['ADMIN', 'COMPRADOR'],
            'view_own_sales': ['ADMIN', 'COMPRADOR'],
            'update_profile': ['ADMIN', 'COMPRADOR'],
            
            // Public permissions
            'view_products': ['ADMIN', 'COMPRADOR']
        };
        
        const allowedRoles = permissions[action];
        return allowedRoles && this.hasAnyRole(allowedRoles);
    }
}

// Create singleton instance
const guard = new RouteGuard();

// Auto-initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    // Check access permission
    if (!guard.init()) {
        return; // User will be redirected
    }
    
    // Apply role-based visibility
    guard.applyRoleBasedVisibility();
    
    // Add logout handlers
    document.querySelectorAll('.logout-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            guard.logout();
        });
    });
});

// Auth helpers for pages
export const ensureAuth = () => {
    if (!guard.isAuthenticated()) {
        window.location.href = '/pages/login.html';
        return false;
    }
    return true;
};

export const ensureUnauth = () => {
    // Check auth status but don't redirect immediately to prevent flash
    const isAuth = guard.isAuthenticated();
    if (isAuth) {
        const user = guard.getCurrentUser();
        // Use setTimeout to allow current script to complete
        setTimeout(() => {
            if (user && user.role === 'ADMIN') window.location.href = '/pages/dashboard.html';
            else window.location.href = '/pages/pos.html';
        }, 0);
        return false;
    }
    return true;
};

// Export for use in other modules
export { guard };
export default guard;
