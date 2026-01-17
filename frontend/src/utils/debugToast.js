// Temporary toast wrapper to debug the issue
// This will log instead of showing toasts

const debugToast = {
    error: (message, options) => {
        console.log('[TOAST ERROR]:', message, options);
        // Don't call actual toast
    },
    success: (message, options) => {
        console.log('[TOAST SUCCESS]:', message, options);
        // Don't call actual toast
    },
    warning: (message, options) => {
        console.log('[TOAST WARNING]:', message, options);
        // Don't call actual toast
    },
    loading: (message, options) => {
        console.log('[TOAST LOADING]:', message, options);
        // Don't call actual toast
        return 'fake-id';
    },
    dismiss: (id) => {
        console.log('[TOAST DISMISS]:', id);
        // Don't call actual toast
    }
};

export default debugToast;
