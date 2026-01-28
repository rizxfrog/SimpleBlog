<template>
  <header class="site-header backdrop-blur-2xl bg-white/60 dark:bg-slate-950/60">
    <div class="container nav flex items-center justify-between gap-6 py-5">
      <RouterLink class="brand group flex items-center gap-3 text-slate-900 dark:text-white" to="/">
        <div class="brand-mark shadow-lg shadow-sky-500/20 group-hover:shadow-sky-500/40">S</div>
        <span class="text-lg font-semibold tracking-wide">SimpleBlog Studio</span>
      </RouterLink>
      <nav
        class="nav-links hidden items-center gap-4 rounded-full bg-white/70 px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm shadow-slate-900/5 ring-1 ring-white/60 backdrop-blur dark:bg-slate-900/60 dark:text-slate-200 dark:ring-slate-700/40 md:flex"
      >
        <RouterLink class="px-2 py-1 transition hover:text-slate-900 dark:hover:text-white" to="/">主页</RouterLink>
        <RouterLink class="px-2 py-1 transition hover:text-slate-900 dark:hover:text-white" to="/blog">文章</RouterLink>
        <RouterLink class="px-2 py-1 transition hover:text-slate-900 dark:hover:text-white" to="/discover">主题</RouterLink>
        <RouterLink
          v-if="auth.isAuthenticated"
          class="px-2 py-1 transition hover:text-slate-900 dark:hover:text-white"
          to="/admin"
        >
          后台
        </RouterLink>
      </nav>
      <div class="nav-actions flex items-center gap-3">
        <div class="relative">
          <button
            class="inline-flex items-center gap-2 rounded-full border border-white/60 bg-white/70 px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm shadow-slate-900/5 ring-1 ring-slate-900/5 backdrop-blur transition hover:-translate-y-0.5 hover:text-slate-900 dark:border-slate-700/50 dark:bg-slate-900/70 dark:text-slate-200 dark:ring-slate-800"
            type="button"
            @click="toggleThemeMenu"
          >
            <span class="text-sm">{{ themeIcon }}</span>
            {{ themeLabel }}
          </button>
          <div
            v-if="themeMenuOpen"
            class="absolute right-0 z-20 mt-3 w-40 rounded-2xl border border-white/70 bg-white/80 p-2 text-sm font-semibold text-slate-700 shadow-xl shadow-slate-900/10 backdrop-blur dark:border-slate-700/60 dark:bg-slate-900/85 dark:text-slate-100"
          >
            <button class="flex w-full items-center gap-2 rounded-xl px-3 py-2 hover:bg-slate-100/70 dark:hover:bg-slate-800/70" @click="setTheme('light')">
              ?? Light
            </button>
            <button class="flex w-full items-center gap-2 rounded-xl px-3 py-2 hover:bg-slate-100/70 dark:hover:bg-slate-800/70" @click="setTheme('dark')">
              ?? Dark
            </button>
            <button class="flex w-full items-center gap-2 rounded-xl px-3 py-2 hover:bg-slate-100/70 dark:hover:bg-slate-800/70" @click="setTheme('system')">
              ?? System
            </button>
          </div>
        </div>
        <RouterLink v-if="!auth.isAuthenticated" class="btn btn-primary" to="/login">登录</RouterLink>
        <button v-else class="avatar-chip" @click="onLogout">退出</button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const themeMenuOpen = ref(false)
const theme = ref<'light' | 'dark' | 'system'>((localStorage.getItem('simpleblog_theme') as any) || 'system')
let systemMedia: MediaQueryList | null = null

const themeLabel = computed(() => {
  if (theme.value === 'light') return 'Light'
  if (theme.value === 'dark') return 'Dark'
  return 'System'
})

const themeIcon = computed(() => {
  if (theme.value === 'light') return '??'
  if (theme.value === 'dark') return '??'
  return '??'
})

const onLogout = () => {
  auth.clear()
  router.push('/login')
}

const toggleThemeMenu = () => {
  themeMenuOpen.value = !themeMenuOpen.value
}

const setTheme = (value: 'light' | 'dark' | 'system') => {
  theme.value = value
  localStorage.setItem('simpleblog_theme', value)
  themeMenuOpen.value = false
  applyTheme()
}

const applyTheme = () => {
  if (theme.value === 'dark') {
    document.documentElement.setAttribute('data-theme', 'dark')
    return
  }
  if (theme.value === 'light') {
    document.documentElement.removeAttribute('data-theme')
    return
  }
  const prefersDark = systemMedia?.matches
  if (prefersDark) {
    document.documentElement.setAttribute('data-theme', 'dark')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
}

const closeOnOutsideClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement | null
  if (!target) return
  if (target.closest('.nav-actions')) return
  themeMenuOpen.value = false
}

onMounted(() => {
  systemMedia = window.matchMedia('(prefers-color-scheme: dark)')
  applyTheme()
  systemMedia.addEventListener('change', applyTheme)
  window.addEventListener('click', closeOnOutsideClick)
})

onUnmounted(() => {
  systemMedia?.removeEventListener('change', applyTheme)
  window.removeEventListener('click', closeOnOutsideClick)
})
</script>
