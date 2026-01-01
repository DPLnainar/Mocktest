import { useModeratorStore } from '../../store/moderatorStore'
import StudentCard from './StudentCard'

export default function StudentGrid({ onSelectStudent }) {
  const { getFilteredStudents } = useModeratorStore()
  const students = getFilteredStudents()

  if (students.length === 0) {
    return (
      <div className="bg-gray-900 rounded-lg p-12 text-center border border-gray-800">
        <p className="text-gray-400">No students found</p>
      </div>
    )
  }

  return (
    <div className="bg-gray-900 rounded-lg p-4 border border-gray-800">
      <h2 className="text-lg font-semibold mb-4">
        Students ({students.length})
      </h2>

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 max-h-[600px] overflow-y-auto pr-2">
        {students.map((student) => (
          <StudentCard
            key={student.studentId}
            student={student}
            onClick={() => onSelectStudent(student)}
          />
        ))}
      </div>
    </div>
  )
}
