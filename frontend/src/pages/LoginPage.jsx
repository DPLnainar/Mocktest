import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { authAPI } from '../services/api'
import toast from 'react-hot-toast'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setAuth } = useAuthStore()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)

  // Load saved credentials on component mount
  useEffect(() => {
    const savedEmail = localStorage.getItem('savedEmail')
    const remembered = localStorage.getItem('rememberMe') === 'true'

    if (remembered && savedEmail) {
      setEmail(savedEmail)
      setRememberMe(true)
    }
  }, [])

  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      const response = await authAPI.login(email, password)
      // Backend returns: { token, userId, email, fullName, department, roles }
      const { token, fullName, roles, ...userData } = response.data

      // Save credentials if Remember Me is checked
      if (rememberMe) {
        localStorage.setItem('savedEmail', email)
        localStorage.setItem('rememberMe', 'true')
      } else {
        localStorage.removeItem('savedEmail')
        localStorage.removeItem('rememberMe')
      }

      // Extract primary role (handle list of roles)
      // We assume the user has one primary role for this MVP
      const primaryRole = roles && roles.length > 0 ? roles[0] : 'STUDENT'

      // Create user object for authStore
      const user = {
        name: fullName || email, // Use fullName, fallback to email
        role: primaryRole,       // Normalize to single role for frontend
        roles: roles,            // Keep original roles array just in case
        ...userData
      }

      setAuth(token, user)
      console.log('LOGIN SUCCESS! User Object:', user) // DEBUG
      toast.success(`Welcome, ${fullName || email}!`)

      // Redirect based on role
      if (primaryRole === 'MODERATOR' || primaryRole === 'ADMIN') {
        navigate('/moderator/tests')
      } else {
        navigate('/student/tests')
      }
    } catch (error) {
      console.error('Login failed:', error)
      toast.error(error.response?.data?.message || 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 to-black">
      <div className="bg-gray-800 p-8 rounded-lg shadow-2xl w-96">
        <h1 className="text-3xl font-bold text-center mb-6 text-green-400">
          Mock Test Portal
        </h1>

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2 text-center">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-2 bg-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 text-white"
              placeholder="student@example.com"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2 text-center">Password</label>
            <div className="relative">
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-4 py-2 bg-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 pr-12 text-white"
                placeholder="••••••••"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-200 text-xl transition-colors"
                title={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
          </div>

          <div className="flex items-center justify-between text-sm">
            <label className="flex items-center text-gray-400 cursor-pointer hover:text-gray-300">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="mr-2 w-4 h-4 rounded border-gray-600 bg-gray-700 text-green-600 focus:ring-green-500 focus:ring-2 cursor-pointer"
              />
              Remember me
            </label>
            <a
              href="/forgot-password"
              className="text-blue-400 hover:text-blue-300 hover:underline transition-colors"
            >
              Forgot password?
            </a>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-green-600 hover:bg-green-700 rounded-lg font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? 'Logging in...' : 'Start Exam'}
          </button>
        </form>

        <p className="text-sm text-gray-400 text-center mt-4">
          ⚠️ Camera and microphone access required
        </p>

        <div className="mt-6 p-3 bg-gray-700 rounded text-xs text-gray-300 border border-gray-600">
          <p className="font-semibold text-green-400 mb-1">Demo Credentials:</p>
          <p>📧 Email: <span className="text-white">admin@examportal.com</span></p>
          <p>🔑 Password: <span className="text-white">password</span></p>
        </div>
      </div>
    </div>
  )
}
