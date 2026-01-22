<template>
  <article class="post-card">
    <div class="post-cover" :style="coverStyle">
      <span v-if="category" class="post-tag">{{ category.name }}</span>
    </div>
    <div class="post-body">
      <h3>{{ post.title }}</h3>
      <div class="post-meta">
        <span>{{ authorName }}</span>
        <span v-if="formattedDate">· {{ formattedDate }}</span>
      </div>
      <p>{{ post.summary || '暂无摘要，点击查看全文内容。' }}</p>
      <RouterLink class="post-link" :to="`/post/${post.id}`">继续阅读 →</RouterLink>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import dayjs from 'dayjs'

const props = defineProps<{ post: any; category?: any }>()

const coverStyle = computed(() => {
  const cover = props.post?.coverUrl
  if (cover) {
    return { backgroundImage: `url(${cover})` }
  }
  return { backgroundImage: 'linear-gradient(135deg, #ffd3b1, #ffe7c7)' }
})

const authorName = computed(() => {
  return props.post?.author?.displayName || props.post?.author?.username || '匿名作者'
})

const formattedDate = computed(() => {
  return props.post?.createdAt ? dayjs(props.post.createdAt).format('YYYY/MM/DD') : ''
})
</script>
