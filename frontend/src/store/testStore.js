import { create } from 'zustand';
import { testAPI, questionAPI } from '../services/testAPI';
import toast from 'react-hot-toast';

export const useTestStore = create((set, get) => ({
    tests: [],
    questions: [],
    currentTest: null,
    currentAttempt: null,
    loading: false,
    violations: [],

    addViolation: (violation) => set((state) => ({
        violations: [...state.violations, violation]
    })),

    clearViolations: () => set({ violations: [] }),

    // Moderator actions
    fetchTests: async () => {
        set({ loading: true });
        try {
            const { data } = await testAPI.getAllTests();
            set({ tests: data, loading: false });
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to fetch tests');
            set({ loading: false });
        }
    },

    createTest: async (testData) => {
        try {
            const { data } = await testAPI.createTest(testData);
            set((state) => ({ tests: [data, ...state.tests] }));
            toast.success('Test created successfully');
            return data;
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to create test');
            throw error;
        }
    },

    deleteTest: async (id) => {
        try {
            await testAPI.deleteTest(id);
            set((state) => ({ tests: state.tests.filter((t) => t.id !== id) }));
            toast.success('Test deleted successfully');
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to delete test');
            throw error;
        }
    },

    updateTestQuestions: async (testId, testQuestions) => {
        try {
            const currentTest = get().tests.find(t => t.id === testId);
            const { data } = await testAPI.updateTest(testId, {
                ...currentTest,
                testQuestions
            });
            set((state) => ({
                tests: state.tests.map((t) => (t.id === testId ? data : t))
            }));
            toast.success('Questions updated successfully');
            return data;
        } catch (error) {
            toast.error('Failed to update questions');
            throw error;
        }
    },

    // Question actions
    fetchQuestions: async (type = null) => {
        set({ loading: true });
        try {
            const { data } = await questionAPI.getAllQuestions(type);
            set({ questions: data, loading: false });
        } catch (error) {
            toast.error('Failed to fetch questions');
            set({ loading: false });
        }
    },

    createQuestion: async (questionData) => {
        try {
            const { data } = await questionAPI.createQuestion(questionData);
            set((state) => ({ questions: [data, ...state.questions] }));
            toast.success('Question created successfully');
            return data;
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to create question');
            throw error;
        }
    },

    bulkUploadQuestions: async (file) => {
        try {
            const { data } = await questionAPI.bulkUpload(file);
            toast.success(`Uploaded ${data.successCount} questions. ${data.errorCount} errors.`);
            if (data.errors.length > 0) {
                console.error('Upload errors:', data.errors);
            }
            await get().fetchQuestions(); // Refresh list
            return data;
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to upload questions');
            throw error;
        }
    },

    pasteQuestions: async (text) => {
        try {
            const { data } = await questionAPI.pasteQuestions(text);
            toast.success(`Processed ${data.successCount} questions. ${data.errorCount} errors.`);
            await get().fetchQuestions();
            return data;
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to paste questions');
            throw error;
        }
    },

    // Student actions
    fetchAvailableTests: async () => {
        set({ loading: true });
        try {
            const { data } = await testAPI.getAvailableTests();
            set({ tests: data, loading: false });
        } catch (error) {
            toast.error('Failed to fetch tests');
            set({ loading: false });
        }
    },

    startTest: async (testId) => {
        try {
            const { data } = await testAPI.startTest(testId);
            set({ currentAttempt: data });
            return data;
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to start test');
            throw error;
        }
    },

    getAttempt: async (testId) => {
        try {
            const { data } = await testAPI.getAttempt(testId);
            set({ currentAttempt: data });
            return data;
        } catch (error) {
            if (error.response?.status !== 404) {
                toast.error('Failed to fetch attempt');
            }
            throw error;
        }
    },

    submitAnswer: async (attemptId, questionId, answer) => {
        try {
            await testAPI.submitAnswer(attemptId, { questionId, answer });
        } catch (error) {
            console.error('Failed to save answer:', error);
        }
    },

    executeCode: async (attemptId, questionId, code, languageId, stdin = '') => {
        try {
            const { data } = await testAPI.executeCode(attemptId, { questionId, code, languageId, stdin });
            return data;
        } catch (error) {
            toast.error('Code execution failed');
            throw error;
        }
    },

    submitTest: async (attemptId) => {
        try {
            const { data } = await testAPI.submitTest(attemptId);
            set({ currentAttempt: data });
            toast.success('Test submitted successfully!');
            return data;
        } catch (error) {
            toast.error(error.response?.data?.message || 'Failed to submit test');
            throw error;
        }
    },
}));
