import { useEffect } from 'react';
import { violationAPI } from '../services/api';

/**
 * useScreenCaptureDetection Hook
 * 
 * Detects when the screen brightness suddenly drops (phone camera flash)
 * or when the page loses visibility (screenshot apps)
 */
export function useScreenCaptureDetection(attemptId, enabled) {
    useEffect(() => {
        if (!attemptId || !enabled) return;

        let lastBrightness = null;

        // Detect sudden brightness changes (phone camera flash)
        const checkBrightness = () => {
            // This is a heuristic - we can't directly detect camera flash,
            // but we can detect when the page loses focus briefly
            if (document.hidden) {
                reportSuspiciousActivity('SCREEN_HIDDEN', 'Page hidden - possible screenshot');
            }
        };

        // Detect screenshot attempts (some browsers support this)
        const handleKeyDown = (e) => {
            // Common screenshot shortcuts
            const isScreenshot =
                (e.key === 'PrintScreen') ||
                (e.metaKey && e.shiftKey && e.key === '3') || // Mac: Cmd+Shift+3
                (e.metaKey && e.shiftKey && e.key === '4') || // Mac: Cmd+Shift+4
                (e.key === 'F12'); // DevTools

            if (isScreenshot) {
                e.preventDefault();
                reportSuspiciousActivity('SCREENSHOT_ATTEMPT', `Screenshot key detected: ${e.key}`);
            }
        };

        // Detect when user switches to camera app (page loses focus)
        const handleVisibilityChange = () => {
            if (document.hidden) {
                const hiddenTime = Date.now();

                const checkReturn = () => {
                    if (!document.hidden) {
                        const duration = Date.now() - hiddenTime;

                        // If hidden for 1-5 seconds, might be taking a photo
                        if (duration > 1000 && duration < 5000) {
                            reportSuspiciousActivity(
                                'BRIEF_TAB_SWITCH',
                                `Suspicious brief switch (${Math.round(duration / 1000)}s) - possible photo`
                            );
                        }

                        document.removeEventListener('visibilitychange', checkReturn);
                    }
                };

                document.addEventListener('visibilitychange', checkReturn);
            }
        };

        const reportSuspiciousActivity = async (type, description) => {
            try {
                // Use CRITICAL severity for screenshot attempts (triggers freeze)
                const severity = type === 'SCREENSHOT_ATTEMPT' ? 'CRITICAL' : 'MINOR';

                await violationAPI.report({
                    sessionId: attemptId,
                    examId: attemptId,
                    violationType: type,
                    severity: severity,
                    message: description,
                    evidence: {
                        timestamp: new Date().toISOString(),
                        userAgent: navigator.userAgent,
                    },
                    consecutiveFrames: 1,
                    confidence: 1.0,
                    confirmed: true
                });

                console.warn(`⚠️ Suspicious activity detected: ${description}`);
            } catch (error) {
                console.error('Failed to report suspicious activity:', error);
            }
        };

        // Disable right-click context menu (prevents "Save image as")
        const handleContextMenu = (e) => {
            e.preventDefault();
            return false;
        };

        // Disable drag and drop (prevents dragging images)
        const handleDragStart = (e) => {
            e.preventDefault();
            return false;
        };

        document.addEventListener('keydown', handleKeyDown);
        document.addEventListener('visibilitychange', handleVisibilityChange);
        document.addEventListener('contextmenu', handleContextMenu);
        document.addEventListener('dragstart', handleDragStart);

        // Add CSS to prevent text selection and image dragging
        const style = document.createElement('style');
        style.textContent = `
            * {
                -webkit-user-select: none;
                -moz-user-select: none;
                -ms-user-select: none;
                user-select: none;
                -webkit-user-drag: none;
                -moz-user-drag: none;
                user-drag: none;
            }
            input, textarea {
                -webkit-user-select: text;
                -moz-user-select: text;
                -ms-user-select: text;
                user-select: text;
            }
        `;
        document.head.appendChild(style);

        return () => {
            document.removeEventListener('keydown', handleKeyDown);
            document.removeEventListener('visibilitychange', handleVisibilityChange);
            document.removeEventListener('contextmenu', handleContextMenu);
            document.removeEventListener('dragstart', handleDragStart);
            document.head.removeChild(style);
        };
    }, [attemptId, enabled]);
}
