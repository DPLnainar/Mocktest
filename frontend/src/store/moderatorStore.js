import { create } from 'zustand'

/**
 * Moderator Store
 * Manages War Room dashboard state
 */
export const useModeratorStore = create((set, get) => ({
  // Current exam being monitored
  currentExamId: null,
  examTitle: '',
  
  // Students in exam
  students: [],
  
  // Filters
  filterStatus: 'all', // all, active, flagged, terminated
  searchQuery: '',
  selectedDepartment: 'all',
  
  // Selected student for detail view
  selectedStudent: null,
  
  // Stats
  stats: {
    totalStudents: 0,
    activeStudents: 0,
    flaggedStudents: 0,
    terminatedStudents: 0,
  },
  
  // Recent violations
  recentViolations: [],
  
  // Actions
  setCurrentExam: (examId, examTitle) => set({ 
    currentExamId: examId, 
    examTitle 
  }),
  
  setStudents: (students) => {
    const stats = {
      totalStudents: students.length,
      activeStudents: students.filter(s => s.connectionStatus === 'CONNECTED').length,
      flaggedStudents: students.filter(s => s.violationCount >= 3).length,
      terminatedStudents: students.filter(s => s.activityStatus === 'TERMINATED').length,
    }
    
    set({ students, stats })
  },
  
  updateStudent: (studentId, updates) => set((state) => ({
    students: state.students.map(s => 
      s.studentId === studentId ? { ...s, ...updates } : s
    ),
  })),
  
  addViolation: (violation) => set((state) => ({
    recentViolations: [violation, ...state.recentViolations].slice(0, 50),
  })),
  
  setFilterStatus: (status) => set({ filterStatus: status }),
  
  setSearchQuery: (query) => set({ searchQuery: query }),
  
  setSelectedDepartment: (department) => set({ selectedDepartment: department }),
  
  setSelectedStudent: (student) => set({ selectedStudent: student }),
  
  getFilteredStudents: () => {
    const state = get()
    let filtered = state.students
    
    // Filter by status
    if (state.filterStatus !== 'all') {
      if (state.filterStatus === 'active') {
        filtered = filtered.filter(s => s.connectionStatus === 'CONNECTED')
      } else if (state.filterStatus === 'flagged') {
        filtered = filtered.filter(s => s.violationCount >= 3)
      } else if (state.filterStatus === 'terminated') {
        filtered = filtered.filter(s => s.activityStatus === 'TERMINATED')
      }
    }
    
    // Filter by department
    if (state.selectedDepartment !== 'all') {
      filtered = filtered.filter(s => s.department === state.selectedDepartment)
    }
    
    // Search
    if (state.searchQuery) {
      const query = state.searchQuery.toLowerCase()
      filtered = filtered.filter(s => 
        s.studentName.toLowerCase().includes(query) ||
        s.studentId.toString().includes(query)
      )
    }
    
    return filtered
  },
}))
