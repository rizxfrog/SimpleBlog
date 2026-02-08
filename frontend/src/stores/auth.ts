import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('simpleblog_token') || ''
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem('simpleblog_token', token)
    },
    clear() {
      this.token = ''
      localStorage.removeItem('simpleblog_token')
    }
  }
})
