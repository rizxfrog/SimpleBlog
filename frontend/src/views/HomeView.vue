<template>
  <div class="container blog-shell">
    <aside class="blog-sidebar">
      <div class="profile-card">
        <div class="avatar"></div>
        <div>
          <h3>困困鱼</h3>
          <p>风霜前夕 · 安全研究员</p>
        </div>
      </div>
      <div class="sidebar-search">
        <input type="text" placeholder="站内搜索" />
        <span class="shortcut">Ctrl + K</span>
      </div>
      <div class="sidebar-icons">
        <button>专栏</button>
        <button>文章</button>
        <button>教程</button>
        <button>收藏</button>
      </div>
      <div class="sidebar-section">
        <h4>最近更新</h4>
        <ul>
          <li v-for="post in recentPosts" :key="post.id">
            <RouterLink :to="`/post/${post.id}`">{{ post.title }}</RouterLink>
          </li>
        </ul>
      </div>
    </aside>

    <section class="blog-main">
      <div class="blog-topbar">
        <div class="tabs">
          <button class="active">近期发布</button>
          <button>分类</button>
          <button>标签</button>
          <button>归档</button>
        </div>
        <ThemeToggle />
      </div>

      <div class="feature-banner">
        <div class="banner-text">
          <span>资源汇总目录（私有）</span>
          <h2>从实战到体系化的安全成长路径</h2>
          <p>整理高频学习地图、工具链与关键案例，适合从 0 到 1 的提升路线。</p>
        </div>
      </div>

      <section class="post-list">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          :category="post.category"
        />
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { RouterLink } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

const { result } = useQuery(
  gql`
    query Blogs($page: Int!, $size: Int!, $publishedOnly: Boolean) {
      blogs(page: $page, size: $size, publishedOnly: $publishedOnly) {
        items {
          id
          title
          summary
          coverUrl
          category {
            id
            name
          }
        }
      }
    }
  `,
  { page: 1, size: 9, publishedOnly: true }
)

const posts = computed(() => result.value?.blogs?.items ?? [])
const recentPosts = computed(() => posts.value.slice(0, 6))
</script>
