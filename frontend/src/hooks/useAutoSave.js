import { useEffect, useCallback } from 'react'
import { useExamStore } from '../store/examStore'
import {
  saveCodeSnapshot,
  clearOldSnapshots,
} from '../services/indexedDB'

/**
 * useAutoSave Hook
 * 
 * Auto-saves code to IndexedDB every 5 seconds
 * Survives Wi-Fi outages and page refreshes
 */
export function useAutoSave(sessionId, code, language, intervalMs = 5000) {
  const { setSyncStatus } = useExamStore()

  const saveCode = useCallback(async () => {
    if (!sessionId || !code) return

    try {
      setSyncStatus(true)
      
      // Save to IndexedDB
      await saveCodeSnapshot(sessionId, code, language)
      
      // Clean up old snapshots (keep last 50)
      await clearOldSnapshots(sessionId, 50)
      
      setSyncStatus(false)
      
      console.log('💾 Auto-saved to IndexedDB')
    } catch (error) {
      console.error('Auto-save failed:', error)
      setSyncStatus(false)
    }
  }, [sessionId, code, language, setSyncStatus])

  useEffect(() => {
    // Initial save
    saveCode()

    // Set up interval
    const interval = setInterval(saveCode, intervalMs)

    return () => clearInterval(interval)
  }, [saveCode, intervalMs])

  return { saveCode }
}
