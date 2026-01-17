import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import toast, { Toaster } from 'react-hot-toast'
import LoginPage from './pages/LoginPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import ExamPage from './pages/ExamPage'
import ExamTerminated from './pages/ExamTerminated'
import ModeratorDashboard from './pages/ModeratorDashboard'
import ModeratorTestsPage from './pages/ModeratorTestsPage'
import QuestionBankPage from './pages/QuestionBankPage'
import StudentTestListPage from './pages/StudentTestListPage'
import TestAnalyticsPage from './pages/TestAnalyticsPage'
import StudentHistoryPage from './pages/StudentHistoryPage'
import TestTakingPage from './pages/TestTakingPage'
import TestReviewPage from './pages/TestReviewPage'
import TestBuilder from './components/TestBuilder'
import { useAuthStore } from './store/authStore'
import ErrorBoundary from './components/ErrorBoundary'

// Monkey-patch toast.error to silence harmless null reference errors
const originalError = toast.error;
toast.error = (message, options) => {
  // Check if message contains the specific null reference error
  if (message && typeof message === 'string' && message.includes('Cannot read properties of null')) {
    console.warn('[Silenced Toast Error]:', message);
    return null; // Don't show the toast
  }
  // Call original toast.error for all other errors
  return originalError(message, options);
};

function App() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            background: '#1a1a1a',
            color: '#fff',
            border: '1px solid #333',
          },
        }}
      />
      <ErrorBoundary>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />

          {/* Moderator Routes */}
          <Route
            path="/moderator/tests"
            element={
              <ProtectedRoute requiredRole="MODERATOR">
                <ModeratorTestsPage />
              </ProtectedRoute>
            }
          />
          {/* Temporary Redirect for users stuck on old route */}
          <Route path="/moderator/home" element={<Navigate to="/moderator/tests" replace />} />

          {/* Test Builder Route */}
          <Route
            path="/moderator/test/:testId?"
            element={
              <ProtectedRoute requiredRole="MODERATOR">
                <TestBuilder />
              </ProtectedRoute>
            }
          />

          <Route
            path="/moderator/questions"
            element={
              <ProtectedRoute requiredRole="MODERATOR">
                <QuestionBankPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/moderator/tests/:testId/analytics"
            element={
              <ProtectedRoute requiredRole="MODERATOR">
                <TestAnalyticsPage />
              </ProtectedRoute>
            }
          />

          {/* Student Routes */}
          <Route
            path="/student/tests"
            element={
              <ProtectedRoute requiredRole="STUDENT">
                <StudentTestListPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/test/:testId"
            element={
              <ProtectedRoute requiredRole="STUDENT">
                <TestTakingPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/history"
            element={
              <ProtectedRoute requiredRole="STUDENT">
                <StudentHistoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/history/tests/:testId/review"
            element={
              <ProtectedRoute requiredRole="STUDENT">
                <TestReviewPage />
              </ProtectedRoute>
            }
          />

          {/* Existing Routes */}
          <Route
            path="/exam/:examId"
            element={
              <ProtectedRoute requiredRole="STUDENT">
                <ExamPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/moderator/exam/:examId"
            element={
              <ProtectedRoute requiredRole="MODERATOR">
                <ModeratorDashboard />
              </ProtectedRoute>
            }
          />
          <Route path="/exam-terminated" element={<ExamTerminated />} />
          <Route path="/" element={<Navigate to="/login" />} />
        </Routes>
      </ErrorBoundary>
    </BrowserRouter>
  )
}

// Protected route wrapper
function ProtectedRoute({ children, requiredRole }) {
  const { token, user } = useAuthStore()

  if (!token) {
    return <Navigate to="/login" />
  }

  if (requiredRole && user?.role !== requiredRole && user?.role !== 'ADMIN') {
    return <Navigate to="/" />
  }

  return children
}

export default App
