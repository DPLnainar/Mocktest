import axios from 'axios'
import { useAuthStore } from '../store/authStore'

/**
 * API Service
 * Handles all HTTP requests to backend
 */

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  timeout: 30000,
})

// Request interceptor - Add JWT token
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor - Handle errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Token expired or invalid
      useAuthStore.getState().logout()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Auth endpoints
export const authAPI = {
  login: (email, password) =>
    api.post('/auth/login', { email, password }),

  register: (userData) =>
    api.post('/auth/register', userData),
}

// Violation endpoints
export const violationAPI = {
  report: (violation) =>
    api.post('/violations/report', violation),

  getStrikeCount: (sessionId) =>
    api.get(`/violations/session/${sessionId}/strikes`),

  getSessionViolations: (sessionId) =>
    api.get(`/violations/session/${sessionId}`),
}

// Execution endpoints
export const executionAPI = {
  execute: (submission) =>
    api.post('/execution/execute', submission),

  getResult: (submissionId) =>
    api.get(`/execution/result/${submissionId}`),
}

// Parser endpoints
export const parserAPI = {
  verify: (code, language, rules) =>
    api.post('/parser/verify', { code, language, rules }),
}

// Session endpoints
export const sessionAPI = {
  create: (examId) =>
    api.post('/monitoring/session/create', { examId }),

  getSession: (sessionId) =>
    api.get(`/monitoring/session/${sessionId}`),
}

export default api
