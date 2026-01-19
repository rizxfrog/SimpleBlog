<template>
  <header class="site-header">
    <div class="container nav">
      <div class="brand">
        <div class="brand-mark">✦</div>
        <span>SimpleBlog</span>
      </div>
      <nav class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/blog">博客</RouterLink>
        <RouterLink to="/discover">发现</RouterLink>
        <RouterLink v-if="auth.isAuthenticated" to="/admin">管理</RouterLink>
      </nav>
      <div class="nav-actions">
        <RouterLink v-if="!auth.isAuthenticated" class="btn btn-primary" to="/login">登录</RouterLink>
        <button v-else class="avatar-chip" @click="onLogout">退出</button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const onLogout = () => {
  auth.clear()
  router.push('/login')
}
</script>
