import { useEffect, useState } from 'react'
import { Clock } from 'lucide-react'

export default function ExamHeader({ title, duration }) {
  const [timeLeft, setTimeLeft] = useState(duration * 60) // Convert to seconds

  useEffect(() => {
    const interval = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 0) {
          clearInterval(interval)
          // TODO: Auto-submit
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => clearInterval(interval)
  }, [])

  const formatTime = (seconds) => {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const secs = seconds % 60
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  const getTimeColor = () => {
    if (timeLeft < 300) return 'text-red-400' // < 5 min
    if (timeLeft < 600) return 'text-yellow-400' // < 10 min
    return 'text-green-400'
  }

  return (
    <div className="bg-gray-900 px-6 py-4 flex items-center justify-between border-b border-gray-800">
      <h1 className="text-xl font-bold">{title}</h1>

      <div className={`flex items-center space-x-2 font-mono text-lg ${getTimeColor()}`}>
        <Clock size={20} />
        <span>{formatTime(timeLeft)}</span>
      </div>
    </div>
  )
}
