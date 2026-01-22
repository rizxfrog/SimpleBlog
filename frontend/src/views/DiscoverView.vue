<template>
  <div class="container">
    <section class="card fade-up">
      <span class="eyebrow">主题导航</span>
      <h2 class="section-title">从分类与标签开始探索</h2>
      <p class="post-meta">内容按主题与标签整理，方便定位学习路径。</p>
    </section>

    <section class="landing-grid stagger" style="margin-top: 20px;">
      <article
        v-for="(cat, index) in categories"
        :key="cat.id"
        class="card"
        :style="{ '--i': index + 1 }"
      >
        <span class="chip">分类</span>
        <h3 class="section-title">{{ cat.name }}</h3>
        <p class="post-meta">slug: {{ cat.slug }}</p>
      </article>
    </section>

    <section class="card" style="margin-top: 20px;">
      <h3 class="section-title">热门标签</h3>
      <div class="chip-list">
        <span v-for="tag in tags" :key="tag.id" class="chip">
          {{ tag.name }}
        </span>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'

const { result } = useQuery(gql`
  query DiscoverMeta {
    categories {
      id
      name
      slug
    }
    tags {
      id
      name
      slug
    }
  }
`)

const categories = computed(() => result.value?.categories ?? [])
const tags = computed(() => result.value?.tags ?? [])
</script>
