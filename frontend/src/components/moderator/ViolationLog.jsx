import { useModeratorStore } from '../../store/moderatorStore'
import { AlertTriangle, Clock } from 'lucide-react'

export default function ViolationLog() {
  const { recentViolations } = useModeratorStore()

  const formatTime = (timestamp) => {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      second: '2-digit',
    })
  }

  const getViolationColor = (type) => {
    if (type === 'PHONE_DETECTED' || type === 'MULTIPLE_FACES') {
      return 'text-red-400'
    }
    if (type === 'TAB_SWITCH' || type === 'WINDOW_BLUR') {
      return 'text-yellow-400'
    }
    return 'text-orange-400'
  }

  return (
    <div className="bg-gray-900 rounded-lg p-4 border border-gray-800 h-[600px] flex flex-col">
      <h2 className="text-lg font-semibold mb-4 flex items-center space-x-2">
        <AlertTriangle size={20} className="text-red-400" />
        <span>Live Violations</span>
      </h2>

      <div className="flex-1 overflow-y-auto space-y-3">
        {recentViolations.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <p>No violations detected</p>
            <p className="text-sm mt-2">All students behaving well 👍</p>
          </div>
        ) : (
          recentViolations.map((violation, index) => (
            <div
              key={index}
              className="bg-gray-800 rounded-lg p-3 border border-gray-700 hover:border-gray-600 transition"
            >
              <div className="flex items-start justify-between mb-2">
                <div>
                  <p className="font-semibold text-white text-sm">
                    {violation.studentName || `Student ${violation.studentId}`}
                  </p>
                  <p className={`text-xs ${getViolationColor(violation.violationType)}`}>
                    {violation.violationType?.replace(/_/g, ' ')}
                  </p>
                </div>
                <div className="flex items-center space-x-1 text-xs text-gray-400">
                  <Clock size={12} />
                  <span>{formatTime(violation.timestamp)}</span>
                </div>
              </div>

              <p className="text-sm text-gray-400">
                {violation.message}
              </p>

              {violation.totalStrikes && (
                <div className="mt-2 pt-2 border-t border-gray-700">
                  <span className="text-xs text-gray-500">
                    Total Strikes: 
                    <span className={`ml-1 font-bold ${
                      violation.totalStrikes >= 4 ? 'text-red-400' :
                      violation.totalStrikes >= 2 ? 'text-yellow-400' :
                      'text-green-400'
                    }`}>
                      {violation.totalStrikes}/5
                    </span>
                  </span>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  )
}
