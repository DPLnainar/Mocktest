import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'
import { useExamStore } from '../store/examStore'
import toast from 'react-hot-toast'

/**
 * useWebSocket Hook
 * 
 * Manages WebSocket connection for real-time monitoring
 * Handles reconnection and heartbeat
 */
export function useWebSocket(sessionId, examId) {
  const clientRef = useRef(null)
  const heartbeatIntervalRef = useRef(null)
  const { token } = useAuthStore()
  const { terminate } = useExamStore()

  const connect = useCallback(() => {
    if (!sessionId || !examId || !token) return

    const socket = new SockJS('http://localhost:8080/ws')
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        console.log('WebSocket:', str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    client.onConnect = () => {
      console.log('🟢 WebSocket connected')
      
      // Subscribe to personal messages
      client.subscribe('/user/queue/messages', (message) => {
        const notification = JSON.parse(message.body)
        handlePersonalMessage(notification)
      })

      // Subscribe to exam monitoring
      client.subscribe(`/topic/exam/${examId}/monitoring`, (message) => {
        const update = JSON.parse(message.body)
        handleMonitoringUpdate(update)
      })

      // Send connect message
      client.publish({
        destination: '/app/monitoring/connect',
        body: JSON.stringify({
          sessionId,
          examId,
          timestamp: Date.now(),
        }),
      })

      // Start heartbeat
      startHeartbeat(client)
    }

    client.onStompError = (frame) => {
      console.error('WebSocket error:', frame)
      toast.error('Connection error. Retrying...')
    }

    client.activate()
    clientRef.current = client

    return client
  }, [sessionId, examId, token])

  const disconnect = useCallback(() => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current)
    }

    if (clientRef.current) {
      clientRef.current.deactivate()
      clientRef.current = null
    }
  }, [])

  const startHeartbeat = (client) => {
    // Send heartbeat every 10 seconds
    heartbeatIntervalRef.current = setInterval(() => {
      if (client && client.connected) {
        client.publish({
          destination: '/app/monitoring/heartbeat',
          body: JSON.stringify({
            sessionId,
            timestamp: Date.now(),
          }),
        })
      }
    }, 10000)
  }

  const handlePersonalMessage = (notification) => {
    console.log('📩 Personal message:', notification)

    if (notification.type === 'termination') {
      const payload = notification.payload
      terminate(payload.reason)
      
      toast.error(`Exam terminated: ${payload.reason}`, {
        duration: Infinity,
      })

      // Redirect after 3 seconds
      setTimeout(() => {
        window.location.href = '/exam-terminated'
      }, 3000)
    } else if (notification.type === 'warning') {
      toast.warning(notification.payload.message)
    }
  }

  const handleMonitoringUpdate = (update) => {
    console.log('📡 Monitoring update:', update)
    
    if (update.type === 'violation_alert') {
      // Could show notification to student
      // toast.warning('Violation detected')
    }
  }

  const sendActivity = (activityType, metadata = {}) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/monitoring/activity',
        body: JSON.stringify({
          sessionId,
          activityType,
          metadata,
          timestamp: Date.now(),
        }),
      })
    }
  }

  useEffect(() => {
    const client = connect()

    return () => {
      disconnect()
    }
  }, [connect, disconnect])

  return {
    isConnected: clientRef.current?.connected || false,
    sendActivity,
  }
}
