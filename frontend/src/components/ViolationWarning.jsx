import { AlertTriangle } from 'lucide-react'

export default function ViolationWarning({ strikeCount }) {
  const getColor = () => {
    if (strikeCount >= 4) return 'from-red-600 to-red-800'
    if (strikeCount >= 2) return 'from-yellow-600 to-yellow-800'
    return 'from-green-600 to-green-800'
  }

  const getMessage = () => {
    if (strikeCount >= 4) return 'Critical: One more strike will terminate exam'
    if (strikeCount >= 2) return 'Warning: Multiple violations detected'
    return 'Caution: Violation detected'
  }

  return (
    <div
      className={`mt-4 p-4 rounded-lg bg-gradient-to-r ${getColor()} violation-warning`}
    >
      <div className="flex items-center space-x-3">
        <AlertTriangle size={24} />
        <div className="flex-1">
          <p className="font-semibold">{getMessage()}</p>
          <p className="text-sm opacity-90 mt-1">
            Strikes: {strikeCount} / 5
          </p>
        </div>
      </div>
    </div>
  )
}
