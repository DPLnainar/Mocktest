import { Circle, Wifi, WifiOff } from 'lucide-react'

export default function StudentCard({ student, onClick }) {
  const getStatusColor = () => {
    if (student.activityStatus === 'TERMINATED') return 'bg-red-500'
    
    switch (student.statusColor) {
      case 'GREEN':
        return 'bg-green-500'
      case 'YELLOW':
        return 'bg-yellow-500'
      case 'RED':
        return 'bg-red-500'
      default:
        return 'bg-gray-500'
    }
  }

  const getStatusBorder = () => {
    if (student.activityStatus === 'TERMINATED') return 'border-red-500'
    
    switch (student.statusColor) {
      case 'GREEN':
        return 'border-green-500'
      case 'YELLOW':
        return 'border-yellow-500'
      case 'RED':
        return 'border-red-500'
      default:
        return 'border-gray-700'
    }
  }

  const formatLastActivity = (timestamp) => {
    if (!timestamp) return 'Never'
    const seconds = Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000)
    if (seconds < 60) return `${seconds}s ago`
    const minutes = Math.floor(seconds / 60)
    if (minutes < 60) return `${minutes}m ago`
    const hours = Math.floor(minutes / 60)
    return `${hours}h ago`
  }

  return (
    <div
      onClick={onClick}
      className={`bg-gray-800 rounded-lg p-4 cursor-pointer hover:bg-gray-750 transition border-2 ${getStatusBorder()}`}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center space-x-2">
          <Circle size={12} className={`${getStatusColor()} fill-current`} />
          <span className="font-semibold text-white">
            {student.studentName}
          </span>
        </div>

        {student.connectionStatus === 'CONNECTED' ? (
          <Wifi size={16} className="text-green-400" />
        ) : (
          <WifiOff size={16} className="text-red-400" />
        )}
      </div>

      {/* Student Info */}
      <div className="space-y-1 text-sm">
        <p className="text-gray-400">
          ID: <span className="text-white">{student.studentId}</span>
        </p>
        <p className="text-gray-400">
          Dept: <span className="text-white">{student.department}</span>
        </p>
      </div>

      {/* Strikes */}
      <div className="mt-3 pt-3 border-t border-gray-700">
        <div className="flex items-center justify-between">
          <span className="text-xs text-gray-400">Violations</span>
          <span className={`text-sm font-bold ${
            student.violationCount >= 4 ? 'text-red-400' :
            student.violationCount >= 2 ? 'text-yellow-400' :
            'text-green-400'
          }`}>
            {student.violationCount}/5
          </span>
        </div>

        {/* Strike bars */}
        <div className="flex space-x-1 mt-2">
          {[...Array(5)].map((_, i) => (
            <div
              key={i}
              className={`h-2 flex-1 rounded ${
                i < student.violationCount
                  ? student.violationCount >= 4
                    ? 'bg-red-500'
                    : student.violationCount >= 2
                    ? 'bg-yellow-500'
                    : 'bg-green-500'
                  : 'bg-gray-700'
              }`}
            />
          ))}
        </div>
      </div>

      {/* Last Activity */}
      <div className="mt-2 text-xs text-gray-500">
        {formatLastActivity(student.lastActivity)}
      </div>

      {/* Terminated Badge */}
      {student.activityStatus === 'TERMINATED' && (
        <div className="mt-2 bg-red-900/30 border border-red-500 rounded px-2 py-1 text-xs text-red-400 text-center font-semibold">
          TERMINATED
        </div>
      )}
    </div>
  )
}
