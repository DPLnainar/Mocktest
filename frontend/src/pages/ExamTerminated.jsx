import { useExamStore } from '../store/examStore'
import { useAuthStore } from '../store/authStore'

export default function ExamTerminated() {
  const { terminationReason, strikeCount } = useExamStore()
  const { logout } = useAuthStore()

  const handleExit = () => {
    logout()
    window.location.href = '/login'
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-red-900 to-black">
      <div className="bg-gray-800 p-8 rounded-lg shadow-2xl max-w-md text-center">
        <div className="text-6xl mb-4">🚫</div>
        
        <h1 className="text-3xl font-bold text-red-400 mb-4">
          Exam Terminated
        </h1>

        <p className="text-gray-300 mb-6">
          {terminationReason || 'Your exam has been terminated due to violations.'}
        </p>

        <div className="bg-gray-700 p-4 rounded-lg mb-6">
          <p className="text-sm text-gray-400">Strike Count</p>
          <p className="text-4xl font-bold text-red-400">{strikeCount} / 5</p>
        </div>

        <p className="text-sm text-gray-400 mb-6">
          Please contact your exam coordinator for further instructions.
        </p>

        <button
          onClick={handleExit}
          className="w-full py-3 bg-red-600 hover:bg-red-700 rounded-lg font-semibold transition"
        >
          Exit
        </button>
      </div>
    </div>
  )
}
