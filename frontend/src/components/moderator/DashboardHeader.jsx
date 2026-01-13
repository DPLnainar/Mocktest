import { Activity, Wifi, WifiOff, Home } from 'lucide-react'
import { useNavigate } from 'react-router-dom'

export default function DashboardHeader({ examTitle, isConnected }) {
  const navigate = useNavigate()

  return (
    <div className="bg-gray-900 border-b border-gray-800 px-6 py-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Activity size={28} className="text-green-400" />
          <div>
            <h1 className="text-2xl font-bold text-white">
              War Room Dashboard
            </h1>
            <p className="text-sm text-gray-400">{examTitle}</p>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <button
            onClick={() => navigate('/moderator/tests')}
            className="flex items-center space-x-2 px-3 py-1.5 bg-gray-800 text-gray-300 rounded-md hover:bg-gray-700 hover:text-white transition-colors"
          >
            <Home size={18} />
            <span className="text-sm font-medium">Home</span>
          </button>

          <div className="h-6 w-px bg-gray-700" />

          <div className="flex items-center space-x-2">
            {isConnected ? (
              <>
                <Wifi size={20} className="text-green-400" />
                <span className="text-sm text-green-400 font-medium">
                  Live Monitoring
                </span>
              </>
            ) : (
              <>
                <WifiOff size={20} className="text-red-400" />
                <span className="text-sm text-red-400 font-medium">
                  Disconnected
                </span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
