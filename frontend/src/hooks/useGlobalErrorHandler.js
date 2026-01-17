import { useEffect } from 'react';

/**
 * Global Error Handler Hook
 * Catches all unhandled errors and logs them properly
 * Prevents "Cannot read properties of null" errors from showing as toasts
 */
export function useGlobalErrorHandler() {
    useEffect(() => {
        const handleError = (event) => {
            const error = event.error || event.reason;

            // Check if it's a null reference error
            if (error && error.message && error.message.includes('Cannot read properties of null')) {
                console.error('🔴 Null Reference Error Caught:', {
                    message: error.message,
                    stack: error.stack,
                    timestamp: new Date().toISOString()
                });

                // Prevent the error from propagating
                event.preventDefault();
                return;
            }

            // Log other errors normally
            console.error('🔴 Unhandled Error:', error);
        };

        const handleRejection = (event) => {
            const error = event.reason;

            // Check if it's a null reference error
            if (error && error.message && error.message.includes('Cannot read properties of null')) {
                console.error('🔴 Null Reference Promise Rejection:', {
                    message: error.message,
                    stack: error.stack,
                    timestamp: new Date().toISOString()
                });

                // Prevent the error from propagating
                event.preventDefault();
                return;
            }

            // Log other rejections normally
            console.error('🔴 Unhandled Promise Rejection:', error);
        };

        // Add global error handlers
        window.addEventListener('error', handleError);
        window.addEventListener('unhandledrejection', handleRejection);

        return () => {
            window.removeEventListener('error', handleError);
            window.removeEventListener('unhandledrejection', handleRejection);
        };
    }, []);
}
