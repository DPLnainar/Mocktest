import { useEffect, useRef, useState } from 'react';
import api from '../services/api';
import { useAuthStore } from '../store/authStore';
import toast from 'react-hot-toast';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const useBrowserLockdown = (attemptId, onAutoSubmit) => {
    const [violationCount, setViolationCount] = useState(0);
    const [tabSwitchCount, setTabSwitchCount] = useState(0);

    const logViolation = async (eventType, metadata = {}) => {
        try {
            const response = await api.post(
                '/proctor/violation',
                {
                    attemptId,
                    eventType,
                    metadata,
                }
            );

            setViolationCount(response.data.violationCount);

            if (response.data.shouldAutoSubmit) {
                toast.error('Too many violations! Test has been suspended.');
                setTimeout(() => {
                    onAutoSubmit?.();
                }, 2000);
            }

            return response.data;
        } catch (error) {
            console.error('Failed to log violation:', error);
        }
    };

    useEffect(() => {
        if (!attemptId) return;

        // Disable right-click
        const handleContextMenu = (e) => {
            e.preventDefault();
            logViolation('COPY_ATTEMPT', { action: 'right-click' });
            toast.error('Right-click is disabled during the test');
        };

        // Disable copy
        const handleCopy = (e) => {
            e.preventDefault();
            logViolation('COPY_ATTEMPT');
            toast.error('Copying is disabled during the test');
        };

        // Disable paste
        const handlePaste = (e) => {
            e.preventDefault();
            logViolation('PASTE_ATTEMPT');
            toast.error('Pasting is disabled during the test');
        };

        // Block DevTools
        const handleKeyDown = (e) => {
            // F12, Ctrl+Shift+I, Ctrl+Shift+J, Ctrl+U
            if (
                e.key === 'F12' ||
                (e.ctrlKey && e.shiftKey && e.key === 'I') ||
                (e.ctrlKey && e.shiftKey && e.key === 'J') ||
                (e.ctrlKey && e.key === 'U')
            ) {
                e.preventDefault();
                logViolation('DEVTOOLS_OPENED', { key: e.key });
                toast.error('Developer tools are blocked');
            }
        };

        // Tab switch detection (Page Visibility API)
        const handleVisibilityChange = () => {
            if (document.hidden) {
                const newCount = tabSwitchCount + 1;
                setTabSwitchCount(newCount);
                logViolation('TAB_SWITCH', { count: newCount });

                if (newCount >= 3) {
                    toast.error('Too many tab switches! Test will be auto-submitted.');
                    setTimeout(() => {
                        onAutoSubmit?.();
                    }, 2000);
                } else {
                    toast.error(`Warning: Tab switch detected. ${newCount}/3 warnings used.`);
                }
            }
        };

        // Fullscreen exit detection
        const handleFullscreenChange = () => {
            if (!document.fullscreenElement) {
                logViolation('FULLSCREEN_EXIT');
                toast.error('Please return to fullscreen mode');
                // Try to re-enter fullscreen
                setTimeout(() => {
                    document.documentElement.requestFullscreen().catch(console.error);
                }, 1000);
            }
        };

        // Add event listeners
        document.addEventListener('contextmenu', handleContextMenu);
        document.addEventListener('copy', handleCopy);
        document.addEventListener('paste', handlePaste);
        document.addEventListener('keydown', handleKeyDown);
        document.addEventListener('visibilitychange', handleVisibilityChange);
        document.addEventListener('fullscreenchange', handleFullscreenChange);

        // Note: Fullscreen must be triggered by user gesture (button click)
        // It will be triggered from TestTakingPage when user clicks "Begin Test"

        // Cleanup
        return () => {
            document.removeEventListener('contextmenu', handleContextMenu);
            document.removeEventListener('copy', handleCopy);
            document.removeEventListener('paste', handlePaste);
            document.removeEventListener('keydown', handleKeyDown);
            document.removeEventListener('visibilitychange', handleVisibilityChange);
            document.removeEventListener('fullscreenchange', handleFullscreenChange);

            // Exit fullscreen on unmount
            if (document.fullscreenElement) {
                document.exitFullscreen().catch(console.error);
            }
        };
    }, [attemptId, tabSwitchCount, onAutoSubmit]);

    return { violationCount, tabSwitchCount };
};
