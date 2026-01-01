import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/**
 * Authentication Store
 * Manages JWT token and user info
 */
export const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      user: null,
      
      setAuth: (token, user) => set({ token, user }),
      
      logout: () => set({ token: null, user: null }),
      
      isAuthenticated: () => {
        const state = useAuthStore.getState()
        return !!state.token
      },
    }),
    {
      name: 'exam-auth-storage',
    }
  )
)
