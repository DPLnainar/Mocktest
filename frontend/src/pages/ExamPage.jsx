import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useExamStore } from '../store/examStore'
import { useOnlineStatus } from '../hooks/useOnlineStatus'
import { useAutoSave } from '../hooks/useAutoSave'
import { useWebSocket } from '../hooks/useWebSocket'
import { useViolationDetection } from '../hooks/useViolationDetection'
import CodeEditor from '../components/CodeEditor'
import CameraMonitor from '../components/CameraMonitor'
import ExamHeader from '../components/ExamHeader'
import StatusBar from '../components/StatusBar'
import ViolationWarning from '../components/ViolationWarning'
import { sessionAPI } from '../services/api'
import { getLatestCodeSnapshot } from '../services/indexedDB'
import toast from 'react-hot-toast'

export default function ExamPage() {
  const { examId } = useParams()
  const navigate = useNavigate()
  
  const {
    sessionId,
    code,
    language,
    strikeCount,
    isTerminated,
    setSession,
    setCode,
    setOnlineStatus,
  } = useExamStore()

  const [loading, setLoading] = useState(true)
  const [examData, setExamData] = useState(null)

  // Network status
  const isOnline = useOnlineStatus()
  useEffect(() => {
    setOnlineStatus(isOnline)
  }, [isOnline, setOnlineStatus])

  // Initialize exam session
  useEffect(() => {
    async function initSession() {
      try {
        // Create or resume session
        const response = await sessionAPI.create(examId)
        const session = response.data

        setSession(session)
        setExamData({
          title: session.examTitle,
          duration: 120, // minutes
          questions: [
            {
              id: 1,
              title: 'Implement Bubble Sort',
              description: 'Write a function to sort an array using bubble sort algorithm.',
              constraints: ['Do not use Arrays.sort()', 'Must use nested loops'],
            },
          ],
        })

        // Restore code from IndexedDB if available
        const snapshot = await getLatestCodeSnapshot(session.id)
        if (snapshot) {
          setCode(snapshot.code)
          toast.success('Code restored from local storage')
        }

        setLoading(false)
      } catch (error) {
        console.error('Failed to initialize session:', error)
        toast.error('Failed to start exam')
        navigate('/login')
      }
    }

    initSession()
  }, [examId])

  // Auto-save
  useAutoSave(sessionId, code, language, 5000)

  // WebSocket connection
  useWebSocket(sessionId, examId)

  // Violation detection
  const { reportViolation } = useViolationDetection(sessionId, examId)

  // Handle camera violations
  const handleCameraViolation = (type, severity, description, evidence) => {
    reportViolation(type, severity, description, evidence)
  }

  // Redirect if terminated
  useEffect(() => {
    if (isTerminated) {
      navigate('/exam-terminated')
    }
  }, [isTerminated, navigate])

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="spinner" />
      </div>
    )
  }

  return (
    <div className="fullscreen-exam exam-mode">
      <ExamHeader 
        title={examData?.title || 'Exam'} 
        duration={examData?.duration}
      />

      <div className="flex h-[calc(100vh-64px)]">
        {/* Left Panel - Question */}
        <div className="w-1/3 bg-gray-900 p-6 overflow-y-auto border-r border-gray-800">
          <h2 className="text-xl font-bold mb-4">
            Question {examData?.questions[0]?.id}
          </h2>
          <h3 className="text-lg text-green-400 mb-3">
            {examData?.questions[0]?.title}
          </h3>
          <p className="text-gray-300 mb-4">
            {examData?.questions[0]?.description}
          </p>

          <div className="bg-gray-800 p-4 rounded-lg mb-4">
            <h4 className="font-semibold mb-2">Constraints:</h4>
            <ul className="list-disc list-inside text-sm text-gray-400">
              {examData?.questions[0]?.constraints.map((c, i) => (
                <li key={i}>{c}</li>
              ))}
            </ul>
          </div>

          {/* Camera Monitor */}
          <CameraMonitor 
            onViolation={handleCameraViolation}
            enabled={true}
          />

          {/* Violation Warning */}
          {strikeCount > 0 && (
            <ViolationWarning strikeCount={strikeCount} />
          )}
        </div>

        {/* Right Panel - Code Editor */}
        <div className="flex-1 bg-gray-950">
          <CodeEditor 
            sessionId={sessionId}
            examId={examId}
          />
        </div>
      </div>

      <StatusBar isOnline={isOnline} />
    </div>
  )
}
