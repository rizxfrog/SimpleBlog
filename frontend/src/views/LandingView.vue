<template>
  <div class="landing container">
    <section class="hero-grid">
      <div class="hero-copy fade-up">
        <span class="eyebrow">SimpleBlog · GraphQL</span>
        <h1>SimpleBlog</h1>
        <p>专注Java、Go、Python编程语言于与计算机基础、计算机网络，分享技术干货</p>
        <div class="hero-actions">
          <RouterLink class="btn btn-primary" to="/blog">开始阅读</RouterLink>
          <RouterLink class="btn btn-ghost" to="/discover">探索</RouterLink>
        </div>
        <div class="hero-metrics stagger" style="--i: 0;">
          <div class="metric-card" style="--i: 1;">
            <strong>120+</strong>
            <span>精选文章</span>
          </div>
          <div class="metric-card" style="--i: 2;">
            <strong>18</strong>
            <span>主题路径</span>
          </div>
          <div class="metric-card" style="--i: 3;">
            <strong>7d</strong>
            <span>周更新节奏</span>
          </div>
        </div>
      </div>
      <div class="hero-panel">
        <div class="card fade-up">
          <h3 class="section-title">主题地图</h3>
          <div class="topic-grid">
            <RouterLink v-for="item in topics" :key="item.label" class="topic-item" :to="item.to">
              {{ item.label }}
            </RouterLink>
          </div>
        </div>
        <!--        <div class="card card-ghost fade-up">
                  <span class="eyebrow">编辑计划</span>
                  <p>本周聚焦：工程化写作、API 设计与前端体验优化。</p>
                  <RouterLink class="btn btn-soft" to="/discover">查看内容计划</RouterLink>
                </div>-->
      </div>
    </section>

    <!--    <section class="landing-grid stagger" style="&#45;&#45;i: 0;">
          <article class="card" style="&#45;&#45;i: 1;">
            <span class="eyebrow">精选专题</span>
            <h3 class="section-title">安全与工程实践</h3>
            <p>围绕真实项目拆解思路与技术细节，适合进阶阅读。</p>
          </article>
          <article class="card" style="&#45;&#45;i: 2;">
            <span class="eyebrow">工具箱</span>
            <h3 class="section-title">效率与自动化</h3>
            <p>整理日常开发必备的脚本、模板与工具链升级方案。</p>
          </article>
          <article class="card" style="&#45;&#45;i: 3;">
            <span class="eyebrow">笔记</span>
            <h3 class="section-title">长期知识管理</h3>
            <p>以标签与分类构建个人知识体系，快速定位内容。</p>
          </article>
        </section>-->
  </div>
</template>

<script setup lang="ts">
import {RouterLink} from 'vue-router'

const topics = [
  {label: 'Java', to: '/discover'},
  {label: 'Go', to: '/discover'},
  {label: 'Python', to: '/discover'},
  {label: 'GraphQL', to: '/discover'},
  {label: '计算机网络', to: '/discover'},
  {label: '计算机基础', to: '/discover'},
]
</script>
