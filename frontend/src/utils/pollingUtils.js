/**
 * Exponential Backoff Polling Utility
 * 
 * Implements smart polling with exponential backoff to reduce server load
 * while maintaining responsive user experience.
 */

/**
 * Configuration for exponential backoff polling
 */
export const POLLING_CONFIG = {
    INITIAL_DELAY: 1000,        // Start at 1 second
    BACKOFF_MULTIPLIER: 1.5,    // Increase by 1.5x each attempt
    MAX_DELAY: 5000,            // Cap at 5 seconds
    MAX_ATTEMPTS: 30,           // Maximum polling attempts
    TIMEOUT_MS: 90000           // Total timeout (90 seconds)
}

/**
 * State messages for different execution statuses
 */
export const STATUS_MESSAGES = {
    QUEUED: 'Your code is in the queue...',
    PROCESSING: 'Executing your code...',
    COMPLETED: 'Execution completed!',
    ACCEPTED: 'All test cases passed!',
    FAILED: 'Execution failed',
    INTERNAL_ERROR: 'Internal error occurred',
    TIMEOUT: 'Execution timed out - please try again'
}

/**
 * Terminal states that should stop polling
 */
const TERMINAL_STATES = [
    'COMPLETED',
    'ACCEPTED',
    'INTERNAL_ERROR',
    'REJECTED',
    'Running Error',
    'Compilation Error',
    'Time Limit Exceeded',
    'Runtime Error'
]

/**
 * Check if a status is terminal (polling should stop)
 */
export const isTerminalState = (status) => {
    if (!status) return false
    return TERMINAL_STATES.some(state =>
        status.toUpperCase().includes(state.toUpperCase())
    )
}

/**
 * Calculate next polling delay using exponential backoff
 * 
 * @param {number} attemptNumber - Current attempt number (0-indexed)
 * @returns {number} Delay in milliseconds
 */
export const calculateBackoffDelay = (attemptNumber) => {
    const delay = POLLING_CONFIG.INITIAL_DELAY *
        Math.pow(POLLING_CONFIG.BACKOFF_MULTIPLIER, attemptNumber)

    return Math.min(delay, POLLING_CONFIG.MAX_DELAY)
}

/**
 * Exponential backoff polling implementation
 * 
 * @param {Function} pollFunction - Async function to call for each poll
 * @param {Function} onUpdate - Callback for status updates (status, attempt, delay)
 * @param {Function} onComplete - Callback when terminal state reached
 * @param {Function} onTimeout - Callback when max attempts exceeded
 * @returns {Function} Cleanup function to stop polling
 */
export const exponentialBackoffPoll = async (
    pollFunction,
    onUpdate,
    onComplete,
    onTimeout
) => {
    let attemptNumber = 0
    let timeoutId = null
    let isCancelled = false
    const startTime = Date.now()

    const poll = async () => {
        if (isCancelled) return

        // Check total timeout
        if (Date.now() - startTime > POLLING_CONFIG.TIMEOUT_MS) {
            onTimeout?.()
            return
        }

        // Check max attempts
        if (attemptNumber >= POLLING_CONFIG.MAX_ATTEMPTS) {
            onTimeout?.()
            return
        }

        try {
            const result = await pollFunction()

            // Notify update callback
            const delay = calculateBackoffDelay(attemptNumber)
            onUpdate?.(result, attemptNumber + 1, delay)

            // Check if we've reached a terminal state
            if (result && isTerminalState(result.status)) {
                onComplete?.(result)
                return
            }

            // Schedule next poll with exponential backoff
            attemptNumber++
            const nextDelay = calculateBackoffDelay(attemptNumber)

            timeoutId = setTimeout(poll, nextDelay)
        } catch (error) {
            console.error('Polling error:', error)

            // Retry with backoff on error
            attemptNumber++
            if (attemptNumber < POLLING_CONFIG.MAX_ATTEMPTS) {
                const nextDelay = calculateBackoffDelay(attemptNumber)
                timeoutId = setTimeout(poll, nextDelay)
            } else {
                onTimeout?.()
            }
        }
    }

    // Start polling
    poll()

    // Return cleanup function
    return () => {
        isCancelled = true
        if (timeoutId) {
            clearTimeout(timeoutId)
        }
    }
}

/**
 * Get user-friendly message for execution status
 * 
 * @param {string} status - Execution status
 * @param {number} attemptNumber - Current polling attempt
 * @returns {string} User-friendly message
 */
export const getStatusMessage = (status, attemptNumber = 0) => {
    if (!status) return STATUS_MESSAGES.QUEUED

    const upperStatus = status.toUpperCase()

    // Check for specific status messages
    for (const [key, message] of Object.entries(STATUS_MESSAGES)) {
        if (upperStatus.includes(key)) {
            return message
        }
    }

    // Default messages based on state
    if (attemptNumber > 0) {
        return `Still processing... (attempt ${attemptNumber})`
    }

    return 'Processing your submission...'
}
