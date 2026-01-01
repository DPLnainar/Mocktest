import { Wifi, WifiOff, RefreshCw, Check } from 'lucide-react'
import { useExamStore } from '../store/examStore'

export default function StatusBar({ isOnline }) {
  const { isSyncing, lastSyncTime } = useExamStore()

  const formatSyncTime = (timestamp) => {
    if (!timestamp) return 'Never'
    const seconds = Math.floor((Date.now() - timestamp) / 1000)
    if (seconds < 60) return `${seconds}s ago`
    const minutes = Math.floor(seconds / 60)
    return `${minutes}m ago`
  }

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-gray-900 border-t border-gray-800 px-6 py-2">
      <div className="flex items-center justify-between text-sm">
        {/* Connection Status */}
        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            {isOnline ? (
              <>
                <Wifi size={16} className="text-green-400" />
                <span className="text-green-400">Online</span>
              </>
            ) : (
              <>
                <WifiOff size={16} className="text-red-400" />
                <span className="text-red-400">Offline</span>
              </>
            )}
          </div>

          {/* Sync Status */}
          <div className="flex items-center space-x-2">
            {isSyncing ? (
              <>
                <RefreshCw size={16} className="text-yellow-400 animate-spin" />
                <span className="text-yellow-400">Syncing...</span>
              </>
            ) : (
              <>
                <Check size={16} className="text-green-400" />
                <span className="text-gray-400">
                  Saved {formatSyncTime(lastSyncTime)}
                </span>
              </>
            )}
          </div>
        </div>

        {/* Info */}
        <div className="text-gray-500 text-xs">
          {isOnline
            ? 'Changes sync automatically'
            : 'Working offline - changes saved locally'}
        </div>
      </div>
    </div>
  )
}
