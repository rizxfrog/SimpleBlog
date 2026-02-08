<template>
  <header class="site-header">
    <div class="container nav">
      <RouterLink class="brand" to="/">
        <div class="brand-mark">S</div>
        <span>SimpleBlog Studio</span>
      </RouterLink>

      <nav class="nav-links">
        <RouterLink to="/">主页</RouterLink>
        <RouterLink to="/blog">文章</RouterLink>
        <RouterLink to="/docs">Docs</RouterLink>
        <RouterLink to="/discover">主题</RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/admin">后台</RouterLink>
      </nav>

      <div class="nav-actions">
        <div class="theme-select-wrapper">
          <n-config-provider :theme="naiveTheme">
            <n-select v-model:value="themeMode" :options="themeOptions" size="small" class="theme-picker"/>
          </n-config-provider>
        </div>

        <!--        <RouterLink v-if="!auth.isAuthenticated" class="btn btn-primary" to="/login">登录</RouterLink>-->
        <!--        <button v-else class="avatar-chip" @click="onLogout">退�?/button>-->
        <n-button v-if="!auth.isAuthenticated" type="primary" round ghost @click="router.push('/login')">登录</n-button>
        <n-button v-else type="error" round secondary strong @click="onLogout">Logout</n-button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {RouterLink, useRouter} from 'vue-router'
import {darkTheme} from 'naive-ui'
import {useAuthStore} from '@/stores/auth'
import {useUiStore} from '@/stores/ui'

const auth = useAuthStore()
const router = useRouter()
const ui = useUiStore()
// 映射主题选项
const themeOptions = [
  {label: '浅色模式', value: 'light'},
  {label: '深色模式', value: 'dark'},
  {label: '跟随系统', value: 'system'}
]
const themeMode = computed({
  get: () => ui.theme,
  set: (value) => ui.setTheme(value as 'light' | 'dark' | 'system')
})

const naiveTheme = computed(() => {
  if (ui.theme === 'dark') {
    return darkTheme
  }
  if (ui.theme === 'system') {
    const prefersDark = typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches
    return prefersDark ? darkTheme : null
  }
  return null
})

const onLogout = () => {
  auth.clear()
  router.push('/login')
}
</script>

<style scoped>
.theme-select-wrapper {
  width: 120px;
  margin-right: 1rem;
}

/* 这里的样式可以根据你的导航栏高度微调 */
.theme-picker {
  --n-bezier: cubic-bezier(0.4, 0, 0.2, 1);
}
</style>


