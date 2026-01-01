import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '../store/authStore'
import { useModeratorStore } from '../store/moderatorStore'
import toast from 'react-hot-toast'

/**
 * useModeratorWebSocket Hook
 * 
 * WebSocket connection for moderator dashboard
 * Receives real-time student status updates
 */
export function useModeratorWebSocket(examId) {
  const clientRef = useRef(null)
  const { token } = useAuthStore()
  const { updateStudent, addViolation, setStudents } = useModeratorStore()

  const connect = useCallback(() => {
    if (!examId || !token) return

    const socket = new SockJS('http://localhost:8080/ws')
    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        console.log('Moderator WebSocket:', str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    client.onConnect = () => {
      console.log('🟢 Moderator WebSocket connected')
      
      // Subscribe to exam monitoring
      client.subscribe(`/topic/exam/${examId}/monitoring`, (message) => {
        const update = JSON.parse(message.body)
        handleMonitoringUpdate(update)
      })

      // Request initial data
      client.publish({
        destination: '/app/monitoring/moderator/connect',
        body: JSON.stringify({ examId }),
      })
    }

    client.onStompError = (frame) => {
      console.error('Moderator WebSocket error:', frame)
      toast.error('Connection error. Retrying...')
    }

    client.activate()
    clientRef.current = client

    return client
  }, [examId, token])

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate()
      clientRef.current = null
    }
  }, [])

  const handleMonitoringUpdate = (update) => {
    console.log('📡 Monitoring update:', update.type, update.payload)

    switch (update.type) {
      case 'student_status':
        // Update individual student
        const student = update.payload
        updateStudent(student.studentId, {
          connectionStatus: student.connectionStatus,
          activityStatus: student.activityStatus,
          statusColor: student.statusColor,
          violationCount: student.violationCount,
          lastActivity: student.lastActivity,
          lastHeartbeat: student.lastHeartbeat,
        })
        break

      case 'batch_status':
        // Bulk update (initial load or refresh)
        setStudents(update.payload)
        break

      case 'violation_alert':
        // New violation
        const violation = update.payload
        addViolation(violation)
        
        // Update student violation count
        updateStudent(violation.studentId, {
          violationCount: (violation.totalStrikes || 0),
          statusColor: violation.totalStrikes >= 4 ? 'RED' : 
                       violation.totalStrikes >= 2 ? 'YELLOW' : 'GREEN',
        })
        
        toast.error(
          `${violation.studentName}: ${violation.violationType}`,
          { icon: '⚠️', duration: 5000 }
        )
        break

      case 'termination':
        // Student terminated
        const termination = update.payload
        updateStudent(termination.studentId, {
          activityStatus: 'TERMINATED',
          statusColor: 'RED',
        })
        
        toast.error(
          `${termination.studentName} terminated: ${termination.reason}`,
          { icon: '🚫', duration: 8000 }
        )
        break

      case 'connection_status':
        // Connection status change
        const status = update.payload
        updateStudent(status.studentId, {
          connectionStatus: status.connectionStatus,
        })
        break
    }
  }

  const sendTermination = (studentId, reason) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/monitoring/moderator/terminate',
        body: JSON.stringify({
          studentId,
          reason,
          timestamp: Date.now(),
        }),
      })
    }
  }

  const sendWarning = (studentId, message) => {
    if (clientRef.current && clientRef.current.connected) {
      clientRef.current.publish({
        destination: '/app/monitoring/moderator/warning',
        body: JSON.stringify({
          studentId,
          message,
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
    sendTermination,
    sendWarning,
  }
}
