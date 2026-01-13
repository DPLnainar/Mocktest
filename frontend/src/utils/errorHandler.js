/**
 * Error handling utilities for parsing and displaying API errors
 */

/**
 * Parse error from API response
 */
export const parseError = (error) => {
    if (error.response?.data) {
        const { error: errorCode, message, retryAfter } = error.response.data;
        return {
            code: errorCode || 'UNKNOWN_ERROR',
            message: message || 'An error occurred',
            retryAfter: retryAfter || null,
            status: error.response.status
        };
    }

    if (error.request) {
        return {
            code: 'NETWORK_ERROR',
            message: 'Network error - please check your connection',
            retryAfter: null,
            status: 0
        };
    }

    return {
        code: 'UNKNOWN_ERROR',
        message: error.message || 'An unexpected error occurred',
        retryAfter: null,
        status: 0
    };
};

/**
 * Get user-friendly error message with emoji
 */
export const getErrorMessage = (parsedError) => {
    const messages = {
        'RATE_LIMIT_EXCEEDED': `⏱️ ${parsedError.message}`,
        'QUEUE_FULL': '🚫 Queue is full - please try again later',
        'EXECUTION_TIMEOUT': '⏰ Execution timed out',
        'NETWORK_ERROR': '📡 Network error - check your connection',
        'BAD_REQUEST': `❌ ${parsedError.message}`,
        'INTERNAL_SERVER_ERROR': '⚠️ Server error - please try again'
    };

    return messages[parsedError.code] || parsedError.message;
};

/**
 * Get error color for UI display
 */
export const getErrorColor = (parsedError) => {
    const colors = {
        'RATE_LIMIT_EXCEEDED': 'yellow',
        'QUEUE_FULL': 'orange',
        'EXECUTION_TIMEOUT': 'red',
        'NETWORK_ERROR': 'blue',
        'BAD_REQUEST': 'red',
        'INTERNAL_SERVER_ERROR': 'red'
    };

    return colors[parsedError.code] || 'red';
};
