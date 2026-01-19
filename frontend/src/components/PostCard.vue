<template>
  <article class="post-card">
    <div class="post-cover" :style="coverStyle">
      <span v-if="category" class="post-tag">{{ category.name }}</span>
    </div>
    <div class="post-body">
      <h3>{{ post.title }}</h3>
      <p>{{ post.summary || '暂无摘要' }}</p>
      <RouterLink class="post-link" :to="`/post/${post.id}`">阅读更多 →</RouterLink>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps<{ post: any; category?: any }>()

const coverStyle = computed(() => {
  const cover = props.post?.coverUrl
  if (cover) {
    return { backgroundImage: `url(${cover})` }
  }
  return { backgroundImage: 'linear-gradient(135deg, #c2d8ff, #ffe7c7)' }
})
</script>
