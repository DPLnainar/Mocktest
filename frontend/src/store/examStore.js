import { create } from 'zustand'

/**
 * Exam Store
 * Manages exam session state, violations, and sync status
 */
export const useExamStore = create((set, get) => ({
  // Session info
  sessionId: null,
  examId: null,
  examTitle: '',
  startedAt: null,
  expiresAt: null,
  
  // Code state
  code: '',
  language: 'java',
  
  // Violation tracking
  strikeCount: 0,
  violations: [],
  
  // Connection status
  isOnline: true,
  isSyncing: false,
  lastSyncTime: null,
  
  // Camera status
  cameraActive: false,
  cameraError: null,
  
  // Termination
  isTerminated: false,
  terminationReason: null,
  
  // Actions
  setSession: (session) => set({
    sessionId: session.id,
    examId: session.examId,
    examTitle: session.examTitle,
    startedAt: session.startedAt,
    expiresAt: session.expiresAt,
  }),
  
  setCode: (code) => set({ code }),
  
  setLanguage: (language) => set({ language }),
  
  addViolation: (violation) => set((state) => ({
    violations: [...state.violations, violation],
    strikeCount: state.strikeCount + violation.strikeCount,
  })),
  
  setStrikeCount: (count) => set({ strikeCount: count }),
  
  setOnlineStatus: (isOnline) => set({ isOnline }),
  
  setSyncStatus: (isSyncing) => set({ 
    isSyncing,
    lastSyncTime: isSyncing ? null : Date.now(),
  }),
  
  setCameraStatus: (active, error = null) => set({
    cameraActive: active,
    cameraError: error,
  }),
  
  terminate: (reason) => set({
    isTerminated: true,
    terminationReason: reason,
  }),
  
  reset: () => set({
    sessionId: null,
    examId: null,
    examTitle: '',
    code: '',
    strikeCount: 0,
    violations: [],
    isTerminated: false,
    terminationReason: null,
  }),
}))
