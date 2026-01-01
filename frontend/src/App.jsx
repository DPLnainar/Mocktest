import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import LoginPage from './pages/LoginPage'
import ExamPage from './pages/ExamPage'
import ExamTerminated from './pages/ExamTerminated'
import ModeratorDashboard from './pages/ModeratorDashboard'
import { useAuthStore } from './store/authStore'

function App() {
  return (
    <BrowserRouter>
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
      <Routes>
        <Route path="/login" element={<LoginPage />} />
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
    </BrowserRouter>
  )
}

// Protected route wrapper
function ProtectedRoute({ children, requiredRole }) {
  const { token, user } = useAuthStore()
  
  if (!token) {
    return <Navigate to="/login" />
  }
  
  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/" />
  }
  
  return children
}

export default App
