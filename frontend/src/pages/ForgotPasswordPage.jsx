import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { authAPI } from '../services/api'

export default function ForgotPasswordPage() {
    const navigate = useNavigate()
    const [email, setEmail] = useState('')
    const [loading, setLoading] = useState(false)
    const [emailSent, setEmailSent] = useState(false)

    const handleSubmit = async (e) => {
        e.preventDefault()
        setLoading(true)

        try {
            // Mock implementation - in production, this would call the backend
            await new Promise(resolve => setTimeout(resolve, 1500))

            // Simulate successful email send
            setEmailSent(true)
            toast.success('Password reset link sent to your email!')
        } catch (error) {
            console.error('Password reset failed:', error)
            toast.error('Failed to send reset link. Please try again.')
        } finally {
            setLoading(false)
        }
    }

    if (emailSent) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 to-black">
                <div className="bg-gray-800 p-8 rounded-lg shadow-2xl w-96">
                    <div className="text-center">
                        <div className="text-6xl mb-4">📧</div>
                        <h1 className="text-2xl font-bold mb-4 text-green-400">
                            Check Your Email
                        </h1>
                        <p className="text-gray-300 mb-6">
                            We've sent a password reset link to:
                        </p>
                        <p className="text-white font-semibold mb-6 bg-gray-700 p-3 rounded">
                            {email}
                        </p>
                        <p className="text-sm text-gray-400 mb-6">
                            Click the link in the email to reset your password. The link will expire in 1 hour.
                        </p>
                        <button
                            onClick={() => navigate('/login')}
                            className="w-full py-3 bg-green-600 hover:bg-green-700 rounded-lg font-semibold transition"
                        >
                            Back to Login
                        </button>
                        <button
                            onClick={() => setEmailSent(false)}
                            className="w-full mt-3 py-2 text-blue-400 hover:text-blue-300 transition"
                        >
                            Resend email
                        </button>
                    </div>
                </div>
            </div>
        )
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 to-black">
            <div className="bg-gray-800 p-8 rounded-lg shadow-2xl w-96">
                <h1 className="text-3xl font-bold text-center mb-2 text-green-400">
                    Forgot Password?
                </h1>
                <p className="text-gray-400 text-center text-sm mb-6">
                    No worries! Enter your email and we'll send you a reset link.
                </p>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium mb-2 text-center">
                            Email Address
                        </label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full px-4 py-2 bg-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 text-white"
                            placeholder="your.email@example.com"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3 bg-green-600 hover:bg-green-700 rounded-lg font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {loading ? 'Sending...' : 'Send Reset Link'}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <button
                        onClick={() => navigate('/login')}
                        className="text-blue-400 hover:text-blue-300 text-sm hover:underline transition"
                    >
                        ← Back to Login
                    </button>
                </div>

                <div className="mt-6 p-3 bg-blue-900/30 border border-blue-700 rounded text-xs text-blue-200">
                    <p className="font-semibold mb-1">ℹ️ Demo Mode</p>
                    <p>This is a mock implementation. In production, an actual email would be sent with a secure reset token.</p>
                </div>
            </div>
        </div>
    )
}
