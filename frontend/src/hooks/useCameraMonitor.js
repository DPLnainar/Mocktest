import { useEffect, useRef, useState } from 'react'
import * as cocoSsd from '@tensorflow-models/coco-ssd'

import { useExamStore } from '../store/examStore'
import toast from 'react-hot-toast'

/**
 * useCameraMonitor Hook (Phase 8 Enhanced)
 * 
 * Smart false-positive handling with:
 * - Consecutive frame tracking (30 frames = 3 seconds)
 * - High confidence threshold (0.85+)
 * - Evidence-only snapshots (captured only on confirmed violations)
 * 
 * Runs at 10 FPS with 3+ consecutive frame requirement
 */
export function useCameraMonitor(onViolation, enabled = true) {
  const videoRef = useRef(null)
  const canvasRef = useRef(null)
  const modelRef = useRef(null)
  const streamRef = useRef(null)
  const detectionIntervalRef = useRef(null)

  // Phase 8: Frame buffer for consecutive tracking
  const frameBufferRef = useRef([]) // Stores last 30 frames
  const violationBufferRef = useRef({
    multiple_faces: 0,
    no_face: 0,
    phone: 0,
    book: 0,
  })

  const [isReady, setIsReady] = useState(false)
  const [error, setError] = useState(null)
  const [detections, setDetections] = useState([])

  const { setCameraStatus } = useExamStore()

  // Initialize camera and model
  useEffect(() => {
    if (!enabled) return

    let mounted = true

    async function init() {
      try {
        // Load TensorFlow.js model
        console.log('Loading COCO-SSD model...')
        const model = await cocoSsd.load()
        modelRef.current = model
        console.log('✅ Model loaded')

        // Get camera stream
        console.log('Requesting camera access...')
        const stream = await navigator.mediaDevices.getUserMedia({
          video: {
            width: { ideal: 640 },
            height: { ideal: 480 },
            facingMode: 'user',
          },
          audio: false,
        })

        if (!mounted) {
          stream.getTracks().forEach((track) => track.stop())
          return
        }

        streamRef.current = stream

        // Attach to video element
        if (videoRef.current) {
          videoRef.current.srcObject = stream
          await videoRef.current.play()
        }

        setIsReady(true)
        setCameraStatus(true)
        toast.success('Camera monitoring active', { icon: '📹' })

        // Start detection loop
        startDetection()
      } catch (err) {
        console.error('Camera initialization failed:', err)
        setError(err.message)
        setCameraStatus(false, err.message)
        toast.error(`Camera error: ${err.message}`)
      }
    }

    init()

    return () => {
      mounted = false
      cleanup()
    }
  }, [enabled])

  const startDetection = () => {
    if (detectionIntervalRef.current) return

    // Run detection at 10 FPS
    detectionIntervalRef.current = setInterval(async () => {
      await detectObjects()
    }, 100) // 100ms = 10 FPS
  }

  const detectObjects = async () => {
    if (!videoRef.current || !modelRef.current) return

    const video = videoRef.current
    if (video.readyState !== 4) return // Not ready

    try {
      const predictions = await modelRef.current.detect(video)
      setDetections(predictions)

      // Phase 8: Add to frame buffer
      const frameData = {
        timestamp: Date.now(),
        predictions,
      }

      frameBufferRef.current.push(frameData)

      // Keep only last 30 frames (3 seconds at 10 FPS)
      if (frameBufferRef.current.length > 30) {
        frameBufferRef.current.shift()
      }

      // Draw bounding boxes
      drawPredictions(predictions)

      // Phase 8: Check for violations with consecutive frame validation
      checkViolationsWithConsecutiveFrames()
    } catch (error) {
      console.error('Detection error:', error)
    }
  }

  const drawPredictions = (predictions) => {
    if (!canvasRef.current || !videoRef.current) return

    const canvas = canvasRef.current
    const video = videoRef.current
    const ctx = canvas.getContext('2d')

    // Match canvas size to video
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight

    // Clear previous drawings
    ctx.clearRect(0, 0, canvas.width, canvas.height)

    // Draw each prediction
    predictions.forEach((prediction) => {
      const [x, y, width, height] = prediction.bbox

      // Box color based on object
      let color = '#10b981' // Green by default
      if (prediction.class === 'cell phone') color = '#ef4444' // Red
      if (prediction.class === 'person' && predictions.filter(p => p.class === 'person').length > 1) {
        color = '#ef4444' // Red for multiple people
      }

      // Draw bounding box
      ctx.strokeStyle = color
      ctx.lineWidth = 2
      ctx.strokeRect(x, y, width, height)

      // Draw label
      ctx.fillStyle = color
      ctx.fillRect(x, y - 20, width, 20)
      ctx.fillStyle = '#ffffff'
      ctx.font = '14px Arial'
      ctx.fillText(
        `${prediction.class} (${Math.round(prediction.score * 100)}%)`,
        x + 5,
        y - 5
      )
    })
  }

  /**
   * Phase 8: Smart violation detection with consecutive frame tracking
   * Only triggers if violation persists for 3+ consecutive frames
   */
  const checkViolationsWithConsecutiveFrames = () => {
    if (frameBufferRef.current.length < 3) return // Need at least 3 frames

    const recentFrames = frameBufferRef.current.slice(-3) // Last 3 frames

    // Check multiple faces
    const multipleFacesFrames = recentFrames.filter(frame => {
      const persons = frame.predictions.filter(p => p.class === 'person')
      if (persons.length <= 1) return false

      // Phase 8: High confidence threshold (0.85+)
      const maxConfidence = Math.max(...persons.map(p => p.score))
      return maxConfidence >= 0.85
    })

    if (multipleFacesFrames.length >= 3) {
      violationBufferRef.current.multiple_faces++

      if (violationBufferRef.current.multiple_faces === 3) {
        // Confirmed violation - capture evidence NOW
        const latestFrame = frameBufferRef.current[frameBufferRef.current.length - 1]
        const persons = latestFrame.predictions.filter(p => p.class === 'person')
        const confidence = Math.max(...persons.map(p => p.score))

        onViolation(
          'MULTIPLE_FACES',
          'MAJOR',
          `${persons.length} faces detected (confirmed over 3 frames)`,
          {
            faceCount: persons.length,
            confidence,
            consecutiveFrames: violationBufferRef.current.multiple_faces,
            screenshot: captureScreenshot(), // Evidence captured only on confirmation
          }
        )

        violationBufferRef.current.multiple_faces = 0 // Reset after trigger
      }
    } else {
      violationBufferRef.current.multiple_faces = 0
    }

    // Check no face
    const noFaceFrames = recentFrames.filter(frame => {
      const persons = frame.predictions.filter(p => p.class === 'person')
      return persons.length === 0
    })

    if (noFaceFrames.length >= 3) {
      violationBufferRef.current.no_face++

      if (violationBufferRef.current.no_face === 3) {
        onViolation(
          'NO_FACE_DETECTED',
          'MINOR',
          'No face visible (confirmed over 3 frames)',
          {
            confidence: 1.0,
            consecutiveFrames: violationBufferRef.current.no_face,
            screenshot: captureScreenshot(),
          }
        )

        violationBufferRef.current.no_face = 0
      }
    } else {
      violationBufferRef.current.no_face = 0
    }

    // Check phone detection
    const phoneFrames = recentFrames.filter(frame => {
      const phone = frame.predictions.find(p => p.class === 'cell phone')
      return phone && phone.score >= 0.85 // Phase 8: 0.85 threshold
    })

    if (phoneFrames.length >= 3) {
      violationBufferRef.current.phone++

      if (violationBufferRef.current.phone === 3) {
        const latestFrame = frameBufferRef.current[frameBufferRef.current.length - 1]
        const phone = latestFrame.predictions.find(p => p.class === 'cell phone')

        onViolation(
          'PHONE_DETECTED',
          'MAJOR',
          'Cell phone detected (confirmed over 3 frames)',
          {
            confidence: phone.score,
            consecutiveFrames: violationBufferRef.current.phone,
            boundingBox: {
              x: phone.bbox[0],
              y: phone.bbox[1],
              width: phone.bbox[2],
              height: phone.bbox[3],
            },
            screenshot: captureScreenshot(),
          }
        )

        violationBufferRef.current.phone = 0
      }
    } else {
      violationBufferRef.current.phone = 0
    }

    // Check book detection
    const bookFrames = recentFrames.filter(frame => {
      const book = frame.predictions.find(p => p.class === 'book')
      return book && book.score >= 0.85 // Phase 8: 0.85 threshold
    })

    if (bookFrames.length >= 3) {
      violationBufferRef.current.book++

      if (violationBufferRef.current.book === 3) {
        const latestFrame = frameBufferRef.current[frameBufferRef.current.length - 1]
        const book = latestFrame.predictions.find(p => p.class === 'book')

        onViolation(
          'SUSPICIOUS_ACTIVITY',
          'MAJOR',
          'Book or reference material detected (confirmed over 3 frames)',
          {
            detectedObject: 'book',
            confidence: book.score,
            consecutiveFrames: violationBufferRef.current.book,
            screenshot: captureScreenshot(),
          }
        )

        violationBufferRef.current.book = 0
      }
    } else {
      violationBufferRef.current.book = 0
    }
  }

  const captureScreenshot = () => {
    if (!videoRef.current || !canvasRef.current) return null

    const video = videoRef.current
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0)

    return canvas.toDataURL('image/jpeg', 0.8)
  }

  const cleanup = () => {
    // Stop detection
    if (detectionIntervalRef.current) {
      clearInterval(detectionIntervalRef.current)
      detectionIntervalRef.current = null
    }

    // Stop camera stream
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop())
      streamRef.current = null
    }

    setCameraStatus(false)
  }

  return {
    videoRef,
    canvasRef,
    isReady,
    error,
    detections,
    captureScreenshot,
  }
}
