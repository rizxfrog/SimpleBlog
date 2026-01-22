<template>
  <AdminShell title="仪表盘" subtitle="站点运行概览与快捷入口">
    <section class="stats-grid">
      <div class="stat-card">
        <span>文章总数</span>
        <strong>{{ totalPosts }}</strong>
      </div>
      <div class="stat-card">
        <span>已发布</span>
        <strong>{{ publishedPosts }}</strong>
      </div>
      <div class="stat-card">
        <span>草稿</span>
        <strong>{{ draftPosts }}</strong>
      </div>
      <div class="stat-card">
        <span>总浏览</span>
        <strong>{{ totalViews }}</strong>
      </div>
    </section>

    <section class="admin-grid">
      <div class="card">
        <h3 class="section-title">快速入口</h3>
        <div class="quick-grid">
          <RouterLink to="/admin/editor">新建文章</RouterLink>
          <RouterLink to="/admin/posts">管理文章</RouterLink>
          <RouterLink to="/blog">浏览站点</RouterLink>
          <RouterLink to="/admin/settings">系统设置</RouterLink>
        </div>
      </div>
      <div class="card">
        <div class="panel-header">
          <h3 class="section-title">通知</h3>
          <button class="btn btn-soft">查看全部</button>
        </div>
        <ul class="notice-list">
          <li>站点运行稳定，连续在线 7 天。</li>
          <li>有 3 篇草稿等待完善内容。</li>
          <li>建议更新首页推荐，提高阅读转化。</li>
        </ul>
      </div>
    </section>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import AdminShell from '../components/AdminShell.vue'

const { result } = useQuery(
  gql`
    query Blogs($page: Int!, $size: Int!, $publishedOnly: Boolean) {
      blogs(page: $page, size: $size, publishedOnly: $publishedOnly) {
        items {
          id
          published
          views
        }
      }
    }
  `,
  { page: 1, size: 50, publishedOnly: false }
)

const posts = computed(() => result.value?.blogs?.items ?? [])
const totalPosts = computed(() => posts.value.length)
const publishedPosts = computed(() => posts.value.filter((post: any) => post.published).length)
const draftPosts = computed(() => posts.value.filter((post: any) => !post.published).length)
const totalViews = computed(() => posts.value.reduce((sum: number, post: any) => sum + (post.views ?? 0), 0))
</script>
