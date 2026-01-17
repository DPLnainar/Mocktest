import { useEffect, useRef } from 'react';
import api from '../services/api';
import toast from 'react-hot-toast';

export const useKeystrokeAnalysis = (attemptId, questionId, isActive = false) => {
    const keystrokeData = useRef({
        copyPasteCount: 0,
        rapidChanges: 0,
        typingSpeed: [],
        lastKeyTime: null,
        answerLength: 0
    });

    const violationReported = useRef(false);

    useEffect(() => {
        if (!isActive || !attemptId || !questionId) return;

        // Detect copy events
        const handleCopy = (e) => {
            keystrokeData.current.copyPasteCount++;
            console.log('Copy detected');
        };

        // Detect paste events
        const handlePaste = (e) => {
            keystrokeData.current.copyPasteCount++;

            const pastedText = e.clipboardData?.getData('text') || '';

            // If pasting a lot of text, it's suspicious
            if (pastedText.length > 50) {
                logKeystrokeViolation('LARGE_PASTE', {
                    length: pastedText.length,
                    questionId
                });
            }

            console.log('Paste detected:', pastedText.length, 'characters');
        };

        // Detect typing patterns
        const handleKeyDown = (e) => {
            const now = Date.now();

            if (keystrokeData.current.lastKeyTime) {
                const timeDiff = now - keystrokeData.current.lastKeyTime;
                keystrokeData.current.typingSpeed.push(timeDiff);

                // Keep only last 50 keystrokes
                if (keystrokeData.current.typingSpeed.length > 50) {
                    keystrokeData.current.typingSpeed.shift();
                }

                // Detect unusually fast typing (AI-generated or copy-paste)
                const avgSpeed = keystrokeData.current.typingSpeed.reduce((a, b) => a + b, 0) / keystrokeData.current.typingSpeed.length;

                if (avgSpeed < 30 && keystrokeData.current.typingSpeed.length > 20) {
                    // Typing faster than 30ms per key is suspicious
                    if (!violationReported.current) {
                        logKeystrokeViolation('SUSPICIOUS_TYPING_SPEED', {
                            avgSpeed,
                            questionId
                        });
                        violationReported.current = true;
                    }
                }
            }

            keystrokeData.current.lastKeyTime = now;
        };

        // Detect rapid answer changes (AI assistance)
        const handleInput = (e) => {
            const currentLength = e.target.value?.length || 0;
            const previousLength = keystrokeData.current.answerLength;

            // If answer length changes dramatically in short time
            if (Math.abs(currentLength - previousLength) > 100) {
                keystrokeData.current.rapidChanges++;

                if (keystrokeData.current.rapidChanges > 2) {
                    logKeystrokeViolation('RAPID_ANSWER_CHANGES', {
                        changes: keystrokeData.current.rapidChanges,
                        questionId
                    });
                }
            }

            keystrokeData.current.answerLength = currentLength;
        };

        // Add event listeners
        document.addEventListener('copy', handleCopy);
        document.addEventListener('paste', handlePaste);
        document.addEventListener('keydown', handleKeyDown);
        document.addEventListener('input', handleInput);

        return () => {
            document.removeEventListener('copy', handleCopy);
            document.removeEventListener('paste', handlePaste);
            document.removeEventListener('keydown', handleKeyDown);
            document.removeEventListener('input', handleInput);
        };
    }, [attemptId, questionId, isActive]);

    const logKeystrokeViolation = async (type, metadata) => {
        try {
            await api.post('/violations/report', {
                sessionId: attemptId,
                examId: null,
                violationType: type,
                severity: 'MAJOR',
                message: getViolationMessage(type),
                evidence: metadata,
                consecutiveFrames: 1,
                confidence: 1.0,
                confirmed: true
            });

            const messages = {
                'LARGE_PASTE': '⚠️ Large paste detected - flagged as suspicious',
                'SUSPICIOUS_TYPING_SPEED': '⚠️ Unusual typing pattern detected',
                'RAPID_ANSWER_CHANGES': '⚠️ Rapid answer changes detected'
            };

            toast.warning(messages[type] || 'Suspicious activity detected');
        } catch (error) {
            console.error('Failed to log keystroke violation:', error);
        }
    };

    const getViolationMessage = (type) => {
        const messages = {
            'LARGE_PASTE': 'Large paste detected',
            'SUSPICIOUS_TYPING_SPEED': 'Unusual typing pattern detected',
            'RAPID_ANSWER_CHANGES': 'Rapid answer changes detected'
        };
        return messages[type] || 'Suspicious activity detected';
    };

    return {
        copyPasteCount: keystrokeData.current.copyPasteCount,
        rapidChanges: keystrokeData.current.rapidChanges
    };
};
