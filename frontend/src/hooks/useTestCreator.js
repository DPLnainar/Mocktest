import { useState, useCallback } from 'react';

/**
 * Custom hook to manage Test Creation state and strict validation rules.
 * 
 * Validation Rules:
 * A: MCQ_ONLY -> Disable Coding upload
 * B: CODING_ONLY -> Disable MCQ upload
 * C: HYBRID -> Enable both
 */
export const useTestCreator = (initialState) => {
    const [formData, setFormData] = useState(initialState || {
        title: '',
        description: '',
        startDateTime: '',
        endDateTime: '',
        durationMinutes: 60,
        testType: 'Practice',
        type: 'HYBRID',
        instructions: '',
        testQuestions: [],
    });

    const [activeStep, setActiveStep] = useState(1);

    // Derived state for allowed question types
    const allowedKinds = {
        mcq: formData.type === 'MCQ_ONLY' || formData.type === 'HYBRID',
        coding: formData.type === 'CODING_ONLY' || formData.type === 'HYBRID',
    };

    const updateField = (field, value) => {
        setFormData(prev => ({ ...prev, [field]: value }));
    };

    const setStructure = (structure) => {
        updateField('type', structure);
        // Optional: clear questions if type changes implies incompatibility?
        // For now, we trust the user or filter on submit.
    };

    const addQuestions = (newQuestions) => {
        setFormData(prev => ({
            ...prev,
            testQuestions: [...prev.testQuestions, ...newQuestions]
        }));
    };

    const nextStep = () => setActiveStep(prev => Math.min(3, prev + 1));
    const prevStep = () => setActiveStep(prev => Math.max(1, prev - 1));

    return {
        formData,
        activeStep,
        allowedKinds,
        updateField,
        setStructure,
        addQuestions,
        nextStep,
        prevStep,
        setActiveStep
    };
};
