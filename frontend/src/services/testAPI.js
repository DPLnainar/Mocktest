import api from './api';

// Test APIs
export const testAPI = {
    // Moderator APIs
    createTest: (data) => api.post('/tests', data),
    getAllTests: () => api.get('/tests'),
    getTestById: (id) => api.get(`/tests/${id}`),
    updateTest: (id, data) => api.put(`/tests/${id}`, data),
    deleteTest: (id) => api.delete(`/tests/${id}`),

    // Student APIs
    getAvailableTests: () => api.get('/student/tests'),
    getStudentTest: (id) => api.get(`/student/tests/${id}`),
    startTest: (testId) => api.post(`/student/tests/${testId}/start`),
    getAttempt: (testId) => api.get(`/student/tests/${testId}/attempt`),
    getAttemptById: (attemptId) => api.get(`/student/attempts/${attemptId}`),
    submitAnswer: (attemptId, data) => api.post(`/student/attempts/${attemptId}/answer`, data),
    executeCode: (attemptId, data) => api.post(`/student/attempts/${attemptId}/execute`, data),
    submitTest: (attemptId) => api.post(`/student/attempts/${attemptId}/submit`),
};

// Question APIs
export const questionAPI = {
    createQuestion: (data) => api.post('/questions', data),
    getAllQuestions: (type) => api.get('/questions', { params: type ? { type } : {} }),
    getQuestionById: (id) => api.get(`/questions/${id}`),
    bulkUpload: (file) => {
        const formData = new FormData();
        formData.append('file', file);
        return api.post('/questions/bulk-upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },
};

export default api;
