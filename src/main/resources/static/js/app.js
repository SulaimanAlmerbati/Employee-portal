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
        const defaultHeaders = {
            ...(this.token && { 'Authorization': `Bearer ${this.token}` })
        };

        // Only add Content-Type for non-FormData requests
        if (!(options.body instanceof FormData)) {
            defaultHeaders['Content-Type'] = 'application/json';
        }

        const mergedOptions = {
            ...options,
            headers: {
                ...defaultHeaders,
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
        this.showToast(message, type);
    }

    showToast(message, type = 'info', duration = 4000) {
        const toastContainer = document.querySelector('.toast-container');
        if (!toastContainer) {
            console.warn('Toast container not found');
            return;
        }

        // Create unique ID for this toast
        const toastId = 'toast-' + Date.now();
        
        // Map alert types to toast styles
        const typeMap = {
            'success': { bg: 'bg-success', icon: 'bi-check-circle-fill', title: 'Success' },
            'danger': { bg: 'bg-danger', icon: 'bi-exclamation-triangle-fill', title: 'Error' },
            'warning': { bg: 'bg-warning', icon: 'bi-exclamation-triangle-fill', title: 'Warning' },
            'info': { bg: 'bg-info', icon: 'bi-info-circle-fill', title: 'Info' }
        };

        const config = typeMap[type] || typeMap['info'];

        // Create toast element
        const toastElement = document.createElement('div');
        toastElement.id = toastId;
        toastElement.className = 'toast';
        toastElement.setAttribute('role', 'alert');
        toastElement.innerHTML = `
            <div class="toast-header ${config.bg} text-white">
                <i class="bi ${config.icon} me-2"></i>
                <strong class="me-auto">${config.title}</strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        `;

        // Add to container
        toastContainer.appendChild(toastElement);

        // Initialize and show toast
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: duration
        });

        toast.show();

        // Clean up after toast is hidden
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }
}

// Initialize app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.employeePortalApp = new EmployeePortalApp();
});