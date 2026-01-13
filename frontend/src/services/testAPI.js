import api from './api';

// Test APIs
export const testAPI = {
    // Moderator APIs
    createTest: (data) => api.post('/tests', data),
    getAllTests: () => api.get('/tests'),
    getTestById: (id) => api.get(`/tests/${id}`),
    updateTest: (id, data) => api.put(`/tests/${id}`, data),
    deleteTest: (id) => api.delete(`/tests/${id}`),
    publishTest: (id, status) => api.patch(`/tests/${id}/status`, { status }), // Use dedicated PATCH endpoint

    // Student APIs
    getAvailableTests: () => api.get('/student/tests'),
    getStudentTest: (id) => api.get(`/student/tests/${id}`),
    startTest: (testId) => api.post(`/student/tests/${testId}/start`),
    getAttempt: (testId) => api.get(`/student/tests/${testId}/attempt`),
    getAttemptById: (attemptId) => api.get(`/student/attempts/${attemptId}`),
    submitAnswer: (attemptId, data) => api.post(`/student/attempts/${attemptId}/answer`, data),
    executeCode: (attemptId, data) => api.post(`/student/attempts/${attemptId}/execute`, data),
    getExecutionResult: (attemptId, questionId) => api.get(`/student/attempts/${attemptId}/questions/${questionId}/result`),
    getQueuePosition: (attemptId) => api.get(`/student/attempts/${attemptId}/queue-position`),
    submitTest: (attemptId) => api.post(`/student/attempts/${attemptId}/submit`),

    // Admin APIs
    getQueueStats: () => api.get('/admin/queue/stats'),
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
    pasteQuestions: (text) => api.post('/questions/paste-upload', { text }),
    bulkCreate: (questions) => api.post('/questions/bulk-create', { questions }),
    cloneQuestion: (id) => api.post(`/questions/${id}/clone`),
};

// Moderator APIs (Smart Assessment Engine)
export const moderatorAPI = {
    // MinIO file upload
    uploadTestCase: (file, questionId) => {
        const formData = new FormData();
        formData.append('file', file);
        if (questionId) formData.append('questionId', questionId);
        return api.post('/moderator/upload/test-case', formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
    },

    // Async verification
    verifySolution: (request) => api.post('/moderator/verify-solution', request),
    getVerificationStatus: (verificationId) => api.get(`/moderator/verify-status/${verificationId}`),
};

export default api;
