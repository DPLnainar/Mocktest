import { useEffect, useState } from 'react'

/**
 * useOnlineStatus Hook
 * 
 * Tracks network connectivity
 * Triggers sync when coming back online
 */
export function useOnlineStatus() {
  const [isOnline, setIsOnline] = useState(navigator.onLine)

  useEffect(() => {
    function handleOnline() {
      console.log('🟢 Network: Back online')
      setIsOnline(true)
    }

    function handleOffline() {
      console.log('🔴 Network: Offline')
      setIsOnline(false)
    }

    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [])

  return isOnline
}
