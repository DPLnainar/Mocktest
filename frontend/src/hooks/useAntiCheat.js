import { useEffect, useCallback } from 'react';
import { useTestStore } from '../store/testStore';
import api from '../services/api';

const useAntiCheat = (attemptId, isTestActive) => {
    const { addViolation } = useTestStore();

    const logViolation = useCallback(async (type, metadata = {}) => {
        if (!isTestActive || !attemptId) return;

        console.warn(`[Anti-Cheat] Violation detected: ${type}`, metadata);

        // Update local store first for immediate feedback
        addViolation({ type, timestamp: new Date(), ...metadata });

        try {
            await api.post(`/student/proctor/attempts/${attemptId}/log`, {
                eventType: type,
                metadata
            });
        } catch (error) {
            console.error('Failed to log violation:', error);
        }
    }, [attemptId, isTestActive, addViolation]);

    // 1. Tab Switch & Window Blur Detection
    useEffect(() => {
        if (!isTestActive) return;

        const handleVisibilityChange = () => {
            if (document.hidden) {
                logViolation('TAB_SWITCH', { reason: 'visibility_hidden' });
            }
        };

        const handleBlur = () => {
            logViolation('TAB_SWITCH', { reason: 'window_blur' });
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);
        window.addEventListener('blur', handleBlur);

        return () => {
            document.removeEventListener('visibilitychange', handleVisibilityChange);
            window.removeEventListener('blur', handleBlur);
        };
    }, [isTestActive, logViolation]);

    // 2. Fullscreen Enforcement
    useEffect(() => {
        if (!isTestActive) return;

        const handleFullscreenChange = () => {
            if (!document.fullscreenElement) {
                logViolation('FULLSCREEN_EXIT');
            }
        };

        document.addEventListener('fullscreenchange', handleFullscreenChange);

        return () => {
            document.removeEventListener('fullscreenchange', handleFullscreenChange);
        };
    }, [isTestActive, logViolation]);

    // 3. Copy/Paste/Right-Click Prevention
    useEffect(() => {
        if (!isTestActive) return;

        const preventAndLog = (e, type) => {
            e.preventDefault();
            logViolation(type);
        };

        const handleCopy = (e) => preventAndLog(e, 'COPY_ATTEMPT');
        const handlePaste = (e) => preventAndLog(e, 'PASTE_ATTEMPT');
        const handleCut = (e) => preventAndLog(e, 'COPY_ATTEMPT');
        const handleContextMenu = (e) => preventAndLog(e, 'COPY_ATTEMPT'); // Block right-click

        document.addEventListener('copy', handleCopy);
        document.addEventListener('paste', handlePaste);
        document.addEventListener('cut', handleCut);
        document.addEventListener('contextmenu', handleContextMenu);

        return () => {
            document.removeEventListener('copy', handleCopy);
            document.removeEventListener('paste', handlePaste);
            document.removeEventListener('cut', handleCut);
            document.removeEventListener('contextmenu', handleContextMenu);
        };
    }, [isTestActive, logViolation]);

    return { logViolation };
};

export default useAntiCheat;
