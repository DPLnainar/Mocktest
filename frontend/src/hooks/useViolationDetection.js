import { useEffect, useRef } from 'react'
import { useExamStore } from '../store/examStore'
import { violationAPI } from '../services/api'
import { queueViolation } from '../services/indexedDB'
import toast from 'react-hot-toast'

/**
 * useViolationDetection Hook
 * 
 * Detects tab switches, window blur, fullscreen exit
 * Reports violations with debouncing
 */
export function useViolationDetection(sessionId, examId) {
  const { addViolation, setStrikeCount, isOnline } = useExamStore()
  const lastViolationRef = useRef({})
  const tabHiddenTimeRef = useRef(null)

  const reportViolation = async (type, severity, description, evidence = {}) => {
    // Debounce - Don't report same violation within 10 seconds
    const now = Date.now()
    const lastTime = lastViolationRef.current[type] || 0
    
    if (now - lastTime < 10000) {
      console.log(`⏭️ Skipping duplicate ${type} violation (debounced)`)
      return
    }
    
    lastViolationRef.current[type] = now

    // Phase 8: Enhanced violation with metadata
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
      // Phase 8: Add consecutive frame tracking metadata
      consecutiveFrames: evidence.consecutiveFrames || 1,
      confidence: evidence.confidence || 1.0,
      confirmed: true, // Tab switches are always confirmed
    }

    try {
      if (isOnline) {
        // Online - report immediately
        const response = await violationAPI.report(violation)
        const { strikeCount, terminated } = response.data

        addViolation({ type, strikeCount: severity === 'MAJOR' ? 2 : 1 })
        setStrikeCount(strikeCount)

        toast.error(`Violation detected: ${description}`)
        toast.error(`Strikes: ${strikeCount}/5`, { icon: '⚠️' })

        if (terminated) {
          window.location.href = '/exam-terminated'
        }
      } else {
        // Offline - queue for later
        await queueViolation(sessionId, violation)
        toast.warning('Violation queued (offline)', { icon: '📡' })
      }
    } catch (error) {
      console.error('Failed to report violation:', error)
      // Queue for later
      await queueViolation(sessionId, violation)
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
    
    const handleFullscreenChange = () => {
      if (!document.fullscreenElement) {
        reportViolation(
          'FULLSCREEN_EXIT',
          'MAJOR',
          'Exited fullscreen mode',
          {}
        )
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

    // Request fullscreen
    const enterFullscreen = async () => {
      try {
        await document.documentElement.requestFullscreen()
      } catch (error) {
        console.error('Failed to enter fullscreen:', error)
      }
    }
    
    enterFullscreen()

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange)
      window.removeEventListener('blur', handleBlur)
      document.removeEventListener('fullscreenchange', handleFullscreenChange)
      document.removeEventListener('copy', handleCopy)
      document.removeEventListener('paste', handlePaste)
      document.removeEventListener('contextmenu', handleContextMenu)
    }
  }, [sessionId, examId, isOnline])

  return { reportViolation }
}
