import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useModeratorStore } from '../store/moderatorStore'
import { useModeratorWebSocket } from '../hooks/useModeratorWebSocket'
import DashboardHeader from '../components/moderator/DashboardHeader'
import StatsOverview from '../components/moderator/StatsOverview'
import StudentGrid from '../components/moderator/StudentGrid'
import ViolationLog from '../components/moderator/ViolationLog'
import StudentDetailModal from '../components/moderator/StudentDetailModal'
import FilterBar from '../components/moderator/FilterBar'
import { violationAPI } from '../services/api'
import toast from 'react-hot-toast'

export default function ModeratorDashboard() {
  const { examId } = useParams()
  const {
    currentExamId,
    examTitle,
    stats,
    selectedStudent,
    setCurrentExam,
    setSelectedStudent,
  } = useModeratorStore()

  const [loading, setLoading] = useState(true)

  // WebSocket connection
  const { isConnected, sendTermination, sendWarning } = useModeratorWebSocket(examId)

  // Initialize dashboard
  useEffect(() => {
    async function init() {
      try {
        // Set exam
        setCurrentExam(examId, 'Data Structures Final Exam')

        // Load initial violations
        const response = await violationAPI.getExamViolations(examId)
        // Process violations...

        setLoading(false)
      } catch (error) {
        console.error('Failed to initialize dashboard:', error)
        toast.error('Failed to load dashboard')
      }
    }

    init()
  }, [examId])

  const handleTerminateStudent = async (studentId, reason) => {
    try {
      sendTermination(studentId, reason)
      toast.success('Student terminated')
      setSelectedStudent(null)
    } catch (error) {
      toast.error('Failed to terminate student')
    }
  }

  const handleSendWarning = async (studentId, message) => {
    try {
      sendWarning(studentId, message)
      toast.success('Warning sent')
    } catch (error) {
      toast.error('Failed to send warning')
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-950">
        <div className="spinner" />
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-950">
      <DashboardHeader 
        examTitle={examTitle}
        isConnected={isConnected}
      />

      <div className="container mx-auto px-6 py-6">
        <StatsOverview stats={stats} />

        <FilterBar />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
          {/* Student Grid - 2/3 width */}
          <div className="lg:col-span-2">
            <StudentGrid 
              onSelectStudent={setSelectedStudent}
            />
          </div>

          {/* Violation Log - 1/3 width */}
          <div>
            <ViolationLog />
          </div>
        </div>
      </div>

      {/* Student Detail Modal */}
      {selectedStudent && (
        <StudentDetailModal
          student={selectedStudent}
          onClose={() => setSelectedStudent(null)}
          onTerminate={handleTerminateStudent}
          onSendWarning={handleSendWarning}
        />
      )}
    </div>
  )
}
