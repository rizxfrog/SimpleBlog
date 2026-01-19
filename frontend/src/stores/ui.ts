import { defineStore } from 'pinia'

type ThemeMode = 'light' | 'dark'

export const useUiStore = defineStore('ui', {
  state: () => ({
    theme: (localStorage.getItem('simpleblog_theme') as ThemeMode) || 'light'
  }),
  actions: {
    applyTheme() {
      document.documentElement.setAttribute('data-theme', this.theme)
      localStorage.setItem('simpleblog_theme', this.theme)
    },
    setTheme(theme: ThemeMode) {
      this.theme = theme
      this.applyTheme()
    },
    toggleTheme() {
      this.theme = this.theme === 'dark' ? 'light' : 'dark'
      this.applyTheme()
    }
  }
})
