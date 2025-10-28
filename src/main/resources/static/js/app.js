// Employee Portal JavaScript Application
class EmployeePortalApp {
    constructor() {
        this.apiBaseUrl = '/api';
        this.token = localStorage.getItem('jwt_token');
        this.init();
    }

    init() {
        // Initialize application
        this.setupEventListeners();
        this.checkAuthentication();
    }

    setupEventListeners() {
        // Global event listeners
        document.addEventListener('DOMContentLoaded', () => {
            console.log('Employee Portal App initialized');
        });
    }

    checkAuthentication() {
        // Skip token validation - let API calls handle authentication
        // If token is invalid, API calls will fail and redirect to login
    }

    async apiCall(endpoint, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                ...(this.token && { 'Authorization': `Bearer ${this.token}` })
            }
        };

        const mergedOptions = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        };

        try {
            const response = await fetch(`${this.apiBaseUrl}${endpoint}`, mergedOptions);
            
            if (response.status === 401) {
                this.logout();
                return null;
            }

            return response;
        } catch (error) {
            console.error('API call failed:', error);
            throw error;
        }
    }

    setToken(token) {
        this.token = token;
        localStorage.setItem('jwt_token', token);
    }

    logout() {
        this.token = null;
        localStorage.removeItem('jwt_token');
        window.location.href = '/login';
    }

    showAlert(message, type = 'info') {
        // Create and show bootstrap alert
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        const container = document.querySelector('.main-content') || document.body;
        container.insertBefore(alertDiv, container.firstChild);

        // Auto-dismiss after 5 seconds
        setTimeout(() => {
            if (alertDiv.parentNode) {
                alertDiv.remove();
            }
        }, 5000);
    }
}

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.employeePortalApp = new EmployeePortalApp();
});