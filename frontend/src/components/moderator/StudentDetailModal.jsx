import { X, AlertTriangle, Camera, Clock, Award } from 'lucide-react'
import { useState } from 'react'

export default function StudentDetailModal({ student, onClose, onTerminate, onSendWarning }) {
  const [terminateReason, setTerminateReason] = useState('')
  const [warningMessage, setWarningMessage] = useState('')
  const [showTerminateConfirm, setShowTerminateConfirm] = useState(false)

  const handleTerminate = () => {
    if (!terminateReason.trim()) {
      alert('Please provide a reason for termination')
      return
    }
    onTerminate(student.studentId, terminateReason)
    setShowTerminateConfirm(false)
  }

  const handleWarning = () => {
    if (!warningMessage.trim()) {
      alert('Please enter a warning message')
      return
    }
    onSendWarning(student.studentId, warningMessage)
    setWarningMessage('')
  }

  const formatTime = (timestamp) => {
    if (!timestamp) return 'Never'
    return new Date(timestamp).toLocaleString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      month: 'short',
      day: 'numeric',
    })
  }

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="bg-gray-900 rounded-lg max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-gray-800">
        {/* Header */}
        <div className="sticky top-0 bg-gray-900 border-b border-gray-800 p-6 flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-white">{student.studentName}</h2>
            <p className="text-sm text-gray-400">ID: {student.studentId}</p>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-800 rounded-lg transition"
          >
            <X size={24} className="text-gray-400" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Basic Info */}
          <div className="grid grid-cols-2 gap-4">
            <div className="bg-gray-800 rounded-lg p-4">
              <p className="text-xs text-gray-400 mb-1">Department</p>
              <p className="text-lg font-semibold text-white">{student.department}</p>
            </div>
            <div className="bg-gray-800 rounded-lg p-4">
              <p className="text-xs text-gray-400 mb-1">Connection</p>
              <p className={`text-lg font-semibold ${
                student.connectionStatus === 'CONNECTED' ? 'text-green-400' : 'text-red-400'
              }`}>
                {student.connectionStatus}
              </p>
            </div>
          </div>

          {/* Violations */}
          <div className="bg-gray-800 rounded-lg p-4">
            <div className="flex items-center space-x-2 mb-3">
              <AlertTriangle size={20} className="text-yellow-400" />
              <h3 className="font-semibold text-white">Violations</h3>
            </div>

            <div className="flex items-center justify-between mb-2">
              <span className="text-gray-400">Total Strikes</span>
              <span className={`text-2xl font-bold ${
                student.violationCount >= 4 ? 'text-red-400' :
                student.violationCount >= 2 ? 'text-yellow-400' :
                'text-green-400'
              }`}>
                {student.violationCount}/5
              </span>
            </div>

            {/* Strike bars */}
            <div className="flex space-x-1">
              {[...Array(5)].map((_, i) => (
                <div
                  key={i}
                  className={`h-3 flex-1 rounded ${
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

          {/* Activity Timeline */}
          <div className="bg-gray-800 rounded-lg p-4">
            <div className="flex items-center space-x-2 mb-3">
              <Clock size={20} className="text-blue-400" />
              <h3 className="font-semibold text-white">Activity</h3>
            </div>

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-400">Last Activity</span>
                <span className="text-white">{formatTime(student.lastActivity)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">Last Heartbeat</span>
                <span className="text-white">{formatTime(student.lastHeartbeat)}</span>
              </div>
            </div>
          </div>

          {/* Camera Feed (Placeholder) */}
          <div className="bg-gray-800 rounded-lg p-4">
            <div className="flex items-center space-x-2 mb-3">
              <Camera size={20} className="text-purple-400" />
              <h3 className="font-semibold text-white">Live Camera Feed</h3>
            </div>

            <div className="bg-gray-900 rounded-lg aspect-video flex items-center justify-center">
              <p className="text-gray-500">Camera feed placeholder</p>
            </div>
          </div>

          {/* Send Warning */}
          {student.activityStatus !== 'TERMINATED' && (
            <div className="bg-gray-800 rounded-lg p-4">
              <h3 className="font-semibold text-white mb-3">Send Warning</h3>
              <textarea
                value={warningMessage}
                onChange={(e) => setWarningMessage(e.target.value)}
                placeholder="Type warning message..."
                className="w-full px-3 py-2 bg-gray-900 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-yellow-500 resize-none"
                rows={3}
              />
              <button
                onClick={handleWarning}
                className="mt-2 px-4 py-2 bg-yellow-600 hover:bg-yellow-700 text-white rounded-lg transition"
              >
                Send Warning
              </button>
            </div>
          )}

          {/* Terminate */}
          {student.activityStatus !== 'TERMINATED' && (
            <div className="bg-red-900/20 border border-red-500 rounded-lg p-4">
              <h3 className="font-semibold text-red-400 mb-3">Terminate Exam</h3>

              {!showTerminateConfirm ? (
                <button
                  onClick={() => setShowTerminateConfirm(true)}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition"
                >
                  Terminate Student
                </button>
              ) : (
                <div>
                  <textarea
                    value={terminateReason}
                    onChange={(e) => setTerminateReason(e.target.value)}
                    placeholder="Reason for termination (required)..."
                    className="w-full px-3 py-2 bg-gray-900 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-red-500 resize-none mb-3"
                    rows={3}
                  />
                  <div className="flex space-x-2">
                    <button
                      onClick={handleTerminate}
                      className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition"
                    >
                      Confirm Termination
                    </button>
                    <button
                      onClick={() => {
                        setShowTerminateConfirm(false)
                        setTerminateReason('')
                      }}
                      className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {student.activityStatus === 'TERMINATED' && (
            <div className="bg-red-900/30 border border-red-500 rounded-lg p-4 text-center">
              <p className="text-red-400 font-semibold text-lg">
                STUDENT TERMINATED
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
