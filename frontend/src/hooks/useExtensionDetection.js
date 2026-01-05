import { useEffect, useState } from 'react';
import api from '../services/api';
import toast from 'react-hot-toast';

// Known cheating extensions to detect
const SUSPICIOUS_EXTENSIONS = {
    // ChatGPT & AI Assistants
    'chatgpt': 'ChatGPT Helper',
    'openai': 'OpenAI Extension',
    'bard': 'Bard AI',
    'claude': 'Claude AI',

    // Writing Assistants
    'grammarly': 'Grammarly',
    'quillbot': 'QuillBot',
    'prowritingaid': 'ProWritingAid',

    // Answer Finders
    'chegg': 'Chegg Helper',
    'coursehero': 'Course Hero',
    'brainly': 'Brainly Helper',
    'quizlet': 'Quizlet Auto-Answer',

    // Translation (can be used to cheat)
    'translate': 'Translation Tool',
    'translator': 'Translator Extension'
};

export const useExtensionDetection = (attemptId, isActive = false) => {
    const [detectedExtensions, setDetectedExtensions] = useState([]);
    const [scanComplete, setScanComplete] = useState(false);

    useEffect(() => {
        if (!isActive || !attemptId) return;

        const detectExtensions = () => {
            const found = [];

            // Method 1: Check for extension resources in performance entries
            const resources = performance.getEntriesByType('resource');
            resources.forEach(resource => {
                const url = resource.name.toLowerCase();

                // Check for chrome-extension:// or moz-extension:// URLs
                if (url.includes('chrome-extension://') || url.includes('moz-extension://')) {
                    // Extract extension ID or name from URL
                    Object.keys(SUSPICIOUS_EXTENSIONS).forEach(key => {
                        if (url.includes(key)) {
                            found.push({
                                name: SUSPICIOUS_EXTENSIONS[key],
                                detected: 'Resource URL',
                                url: resource.name.substring(0, 100) // Truncate for privacy
                            });
                        }
                    });
                }
            });

            // Method 2: Check for extension-injected DOM elements
            Object.keys(SUSPICIOUS_EXTENSIONS).forEach(key => {
                const elements = document.querySelectorAll(`[class*="${key}"], [id*="${key}"]`);
                if (elements.length > 0) {
                    found.push({
                        name: SUSPICIOUS_EXTENSIONS[key],
                        detected: 'DOM Injection',
                        count: elements.length
                    });
                }
            });

            // Method 3: Check for global variables injected by extensions
            const suspiciousGlobals = ['grammarly', 'quillbot', 'chatgpt'];
            suspiciousGlobals.forEach(global => {
                if (window[global]) {
                    found.push({
                        name: SUSPICIOUS_EXTENSIONS[global] || global,
                        detected: 'Global Variable'
                    });
                }
            });

            // Remove duplicates
            const unique = Array.from(new Map(found.map(item => [item.name, item])).values());

            setDetectedExtensions(unique);
            setScanComplete(true);

            // Log violations if extensions found
            if (unique.length > 0) {
                logExtensionViolation(unique);
            }
        };

        // Run detection after a short delay to let page load
        const timer = setTimeout(detectExtensions, 2000);

        return () => clearTimeout(timer);
    }, [attemptId, isActive]);

    const logExtensionViolation = async (extensions) => {
        try {
            await api.post('/proctor/violation', {
                attemptId,
                eventType: 'SUSPICIOUS_EXTENSION_DETECTED',
                metadata: {
                    extensions: extensions.map(ext => ext.name),
                    count: extensions.length,
                    details: extensions
                }
            });

            // Warn user
            toast.error(`⚠️ Detected ${extensions.length} suspicious extension(s) - this has been logged`);
        } catch (error) {
            console.error('Failed to log extension violation:', error);
        }
    };

    return {
        detectedExtensions,
        scanComplete,
        hasExtensions: detectedExtensions.length > 0
    };
};
