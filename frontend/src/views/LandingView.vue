<template>
  <div class="landing">
    <section class="landing-hero">
      <div class="hero-copy">
        <span class="hero-tag">前百度、奇安信、360 网络安全专家</span>
        <h1>轩辕的编程宇宙</h1>
        <p>专注网络安全、逆向工程与计算机基础，分享一线实战与高质量技术笔记。</p>
        <div class="hero-actions">
          <RouterLink class="btn btn-primary" to="/blog">浏览文章</RouterLink>
          <RouterLink class="btn btn-ghost" to="/login">登入</RouterLink>
        </div>
      </div>
      <div class="hero-visual">
        <div class="visual-panel">
          <div class="visual-grid">
            <RouterLink
              v-for="item in topics"
              :key="item.label"
              class="visual-item"
              :to="item.to"
            >
              {{ item.label }}
            </RouterLink>
          </div>
          <button class="floating-cta">进入社区</button>
        </div>
      </div>
    </section>
    <section class="landing-metrics">
      <div class="metric">
        <strong>500+</strong>
        <span>原创文章</span>
      </div>
      <div class="metric">
        <strong>1700+</strong>
        <span>学员用户</span>
      </div>
      <div class="metric">
        <strong>12+</strong>
        <span>年工作经验</span>
      </div>
      <div class="metric highlight">
        <strong>48+</strong>
        <span>专项课程</span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

const topics = [
  { label: '计算机基础', to: '/discover' },
  { label: '</> C/C++', to: '/discover' },
  { label: '操作系统', to: '/discover' },
  { label: '逆向工程', to: '/discover' },
  { label: '网络安全', to: '/discover' },
  { label: '计算机网络', to: '/discover' },
  { label: '人工智能', to: '/discover' },
  { label: '学习路线', to: '/discover' }
]
</script>
