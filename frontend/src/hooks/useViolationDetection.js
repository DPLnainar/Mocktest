import { useEffect, useRef } from 'react'
import { violationAPI } from '../services/api'
import api from '../services/api'
import toast from 'react-hot-toast'

/**
 * useViolationDetection Hook
 * 
 * Detects tab switches, window blur, fullscreen exit
 * Reports violations with debouncing
 */
export function useViolationDetection(sessionId, examId) {
  const lastViolationRef = useRef({})
  const tabHiddenTimeRef = useRef(null)

  const reportViolation = async (type, severity, description, evidence = {}) => {
    // Guard against null sessionId
    if (!sessionId) {
      console.warn('Cannot report violation: sessionId is null')
      return
    }

    // Debounce - Don't report same violation within 10 seconds
    const now = Date.now()
    const lastTime = lastViolationRef.current[type] || 0

    // Bypass debounce for CRITICAL violations (always report)
    if (severity !== 'CRITICAL' && now - lastTime < 10000) {
      console.log(`⏭️ Skipping duplicate ${type} violation (debounced)`)
      return
    }

    lastViolationRef.current[type] = now

    // Enhanced violation with metadata
    const violation = {
      sessionId,
      examId,
      violationType: type,
      severity,
      message: description,
      evidence: {
        ...evidence,
        timestamp: new Date().toISOString(),
        userAgent: navigator.userAgent,
      },
      consecutiveFrames: evidence.consecutiveFrames || 1,
      confidence: evidence.confidence || 1.0,
      confirmed: true,
    }

    try {
      // Report violation to backend
      const response = await violationAPI.report(violation)
      const { strikeCount, terminated } = response.data

      toast.error(`Violation detected: ${description}`)
      if (strikeCount) {
        toast.error(`Strikes: ${strikeCount}/5`, { icon: '⚠️' })
      }

      if (terminated) {
        window.location.href = '/exam-terminated'
      }
    } catch (error) {
      console.error('Failed to report violation:', error)
      // Silently fail - don't show error to user
    }
  }

  useEffect(() => {
    if (!sessionId || !examId) return

    // ===== Tab Switch / Window Blur Detection =====

    const handleVisibilityChange = () => {
      if (document.hidden) {
        tabHiddenTimeRef.current = Date.now()
      } else {
        if (tabHiddenTimeRef.current) {
          const duration = Date.now() - tabHiddenTimeRef.current

          if (duration > 2000) { // Only report if hidden > 2 seconds
            reportViolation(
              'TAB_SWITCH',
              'MAJOR',
              `Tab switched or window minimized for ${Math.round(duration / 1000)}s`,
              { duration }
            )
          }

          tabHiddenTimeRef.current = null
        }
      }
    }

    const handleBlur = () => {
      reportViolation(
        'WINDOW_BLUR',
        'MAJOR',
        'Window lost focus',
        {}
      )
    }

    // ===== Fullscreen Exit Detection =====

    const handleFullscreenChange = async () => {
      if (!document.fullscreenElement) {
        await reportViolation(
          'FULLSCREEN_EXIT',
          'CRITICAL',
          'Exited fullscreen mode - Test will be frozen',
          {}
        )
      }
    }

    const checkFreezeStatus = async (retries = 3) => {
      try {
        console.log(`Checking freeze status (retries left: ${retries})...`)
        const { data } = await api.get(`/student/attempts/${sessionId}`)

        console.log('Current status:', data.status)

        if (data.status === 'FROZEN') {
          console.log('Test IS frozen, reloading...')
          toast.error('Test has been frozen due to violations', { duration: 5000 })
          // Force immediate reload
          window.location.reload()
        } else if (retries > 0) {
          // Retry after 1 second if not frozen yet
          setTimeout(() => checkFreezeStatus(retries - 1), 1000)
        }
      } catch (error) {
        console.error('Failed to check freeze status:', error)
      }
    }

    // ===== Copy/Paste Detection =====

    const handleCopy = (e) => {
      // Allow copy from code editor (own content)
      console.log('Copy detected (allowed)')
    }

    const handlePaste = (e) => {
      const text = e.clipboardData.getData('text')

      if (text.length > 50) { // Suspicious large paste
        reportViolation(
          'COPY_PASTE_DETECTED',
          'MAJOR',
          'Large paste detected',
          { length: text.length, preview: text.substring(0, 100) }
        )
      }
    }

    // ===== Context Menu (Right Click) =====

    const handleContextMenu = (e) => {
      e.preventDefault()
      toast('Right-click disabled during exam', { icon: '🚫' })
    }

    // Attach listeners
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('blur', handleBlur)
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    document.addEventListener('copy', handleCopy)
    document.addEventListener('paste', handlePaste)
    document.addEventListener('contextmenu', handleContextMenu)

    // Note: Fullscreen is handled by TestTakingPage.jsx with user gesture
    // Don't request fullscreen here as it will fail without user interaction

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      window.removeEventListener('blur', handleBlur)
      document.removeEventListener('fullscreenchange', handleFullscreenChange)
      document.removeEventListener('copy', handleCopy)
      document.removeEventListener('paste', handlePaste)
      document.removeEventListener('contextmenu', handleContextMenu)
    }
  }, [sessionId, examId])

  return { reportViolation }
}
