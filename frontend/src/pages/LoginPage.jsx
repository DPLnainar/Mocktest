import { useState } from 'react'
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

  const handleLogin = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      const response = await authAPI.login(email, password)
      const { token, user } = response.data

      setAuth(token, user)
      toast.success(`Welcome, ${user.name}!`)

      // Redirect to exam (hardcoded for demo)
      navigate('/exam/1')
    } catch (error) {
      console.error('Login failed:', error)
      toast.error('Invalid credentials')
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
              className="w-full px-4 py-2 bg-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="student@example.com"
              required
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2 text-center">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-2 bg-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              placeholder="••••••••"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-green-600 hover:bg-green-700 rounded-lg font-semibold transition disabled:opacity-50"
          >
            {loading ? 'Logging in...' : 'Start Exam'}
          </button>
        </form>

        <p className="text-sm text-gray-400 text-center mt-4">
          ⚠️ Camera and microphone access required
        </p>
      </div>
    </div>
  )
}
