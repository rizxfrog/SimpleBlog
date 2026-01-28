<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <RouterLink class="admin-brand" to="/">SimpleBlog</RouterLink>
      <div class="admin-search">
        <input type="text" placeholder="搜索内容" />
        <span>Ctrl + K</span>
      </div>
      <nav class="admin-menu">
        <RouterLink to="/admin">仪表盘</RouterLink>
        <RouterLink to="/admin/posts">文章</RouterLink>
        <RouterLink to="/admin/editor">新建文章</RouterLink>
        <RouterLink to="/admin/settings">设置</RouterLink>
      </nav>
      <div class="admin-profile">
        <div class="avatar"></div>
        <div>
          <strong>Administrator</strong>
          <span>站点管理</span>
        </div>
      </div>
    </aside>

    <section class="admin-main">
      <header class="admin-topbar">
        <div>
          <h1>{{ title }}</h1>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>
        <div class="admin-actions">
          <slot name="actions" />
        </div>
      </header>
      <div class="admin-content">
        <slot />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{ title: string; subtitle?: string }>()
</script>
