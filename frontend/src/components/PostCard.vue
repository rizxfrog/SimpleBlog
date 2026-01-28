<template>
  <article class="post-card">
    <div class="post-cover" :style="coverStyle">
      <span v-if="category" class="post-tag">{{ category.name }}</span>
    </div>
    <div class="post-body">
      <h3 v-if="titleHighlight" v-html="titleHighlight"></h3>
      <h3 v-else>{{ post.title }}</h3>
      <div class="post-meta">
        <span>{{ authorName }}</span>
        <span v-if="formattedDate">· {{ formattedDate }}</span>
      </div>
      <p v-if="summaryHighlight" v-html="summaryHighlight"></p>
      <p v-else>{{ post.summary || 'No summary yet.' }}</p>
      <RouterLink class="post-link" :to="`/post/${post.id}`">Read more →</RouterLink>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import dayjs from 'dayjs'

const props = defineProps<{ post: any; category?: any }>()

const titleHighlight = computed(() => props.post?.titleHighlight)
const summaryHighlight = computed(() => props.post?.summaryHighlight)

const coverStyle = computed(() => {
  const cover = props.post?.coverUrl
  if (cover) {
    return { backgroundImage: `url(${cover})` }
  }
  return { backgroundImage: 'linear-gradient(135deg, #ffd3b1, #ffe7c7)' }
})

const authorName = computed(() => {
  return props.post?.author?.displayName || props.post?.author?.username || 'Anonymous'
})

const formattedDate = computed(() => {
  return props.post?.createdAt ? dayjs(props.post.createdAt).format('YYYY/MM/DD') : ''
})
</script>
