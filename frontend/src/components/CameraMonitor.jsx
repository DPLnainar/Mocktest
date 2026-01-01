import { useCameraMonitor } from '../hooks/useCameraMonitor'
import { AlertCircle, Camera, CameraOff } from 'lucide-react'

export default function CameraMonitor({ onViolation, enabled }) {
  const { videoRef, canvasRef, isReady, error, detections } = useCameraMonitor(
    onViolation,
    enabled
  )

  if (error) {
    return (
      <div className="bg-red-900/20 border border-red-500 rounded-lg p-4 mt-4">
        <div className="flex items-center space-x-2 text-red-400">
          <CameraOff size={20} />
          <span className="font-semibold">Camera Error</span>
        </div>
        <p className="text-sm text-red-300 mt-2">{error}</p>
      </div>
    )
  }

  return (
    <div className="mt-4">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center space-x-2">
          <Camera size={16} className="text-green-400" />
          <span className="text-sm font-semibold">Live Monitoring</span>
        </div>
        {isReady ? (
          <span className="text-xs text-green-400">● Active</span>
        ) : (
          <span className="text-xs text-yellow-400">● Loading...</span>
        )}
      </div>

      <div className="relative camera-preview">
        {/* Video element (hidden) */}
        <video
          ref={videoRef}
          className="absolute top-0 left-0 w-full h-auto opacity-0"
          playsInline
          muted
        />

        {/* Canvas with bounding boxes */}
        <canvas
          ref={canvasRef}
          className="w-full h-auto rounded-lg bg-gray-800"
        />

        {/* Detection count */}
        {detections.length > 0 && (
          <div className="absolute top-2 right-2 bg-black/70 px-2 py-1 rounded text-xs">
            {detections.length} object{detections.length !== 1 ? 's' : ''} detected
          </div>
        )}
      </div>

      {/* Warnings */}
      {detections.some((d) => d.class === 'cell phone') && (
        <div className="flex items-center space-x-2 mt-2 text-red-400 text-sm">
          <AlertCircle size={16} />
          <span>Phone detected!</span>
        </div>
      )}

      {detections.filter((d) => d.class === 'person').length > 1 && (
        <div className="flex items-center space-x-2 mt-2 text-red-400 text-sm">
          <AlertCircle size={16} />
          <span>Multiple faces detected!</span>
        </div>
      )}
    </div>
  )
}
