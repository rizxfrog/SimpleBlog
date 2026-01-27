<template>
  <div class="container">
    <section v-if="post" class="blog-article">
      <div class="article-hero">
        <span class="eyebrow">文章详情</span>
        <h1>{{ post.title }}</h1>
        <div class="article-meta">
          <span>{{ authorName }}</span>
          <span v-if="formattedDate">· {{ formattedDate }}</span>
          <span v-if="post.category">· {{ post.category.name }}</span>
        </div>
      </div>

      <div v-if="post.coverUrl" class="article-cover" :style="coverStyle"></div>

      <div class="article-body">
        <div class="chip-list" v-if="post.tags?.length">
          <span v-for="tag in post.tags" :key="tag.id" class="chip">{{ tag.name }}</span>
        </div>
        <div class="markdown" ref="markdownEl" v-html="html"></div>
      </div>
    </section>

    <section class="card" style="margin-top: 24px;" v-if="comments.length">
      <h3 class="section-title">评论</h3>
      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <strong>{{ comment.user?.displayName || comment.user?.username || '访客' }}</strong>
          <p>{{ comment.content }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { marked } from 'marked'
import hljs from 'highlight.js'
import dayjs from 'dayjs'

const route = useRoute()
const postId = Number(route.params.id)

const { result } = useQuery(
  gql`
    query Blog($id: ID!) {
      blog(id: $id) {
        id
        title
        content
        coverUrl
        createdAt
        category {
          id
          name
        }
        tags {
          id
          name
        }
        author {
          id
          username
          displayName
        }
      }
      comments(blogId: $id) {
        id
        content
        user {
          id
          username
          displayName
        }
      }
    }
  `,
  { id: postId }
)

const post = computed(() => result.value?.blog)
const comments = computed(() => result.value?.comments ?? [])
const html = computed(() => (post.value?.content ? marked.parse(post.value.content) : ''))
const markdownEl = ref<HTMLElement | null>(null)

const authorName = computed(() => {
  return post.value?.author?.displayName || post.value?.author?.username || '匿名作者'
})

const formattedDate = computed(() => {
  return post.value?.createdAt ? dayjs(post.value.createdAt).format('YYYY/MM/DD') : ''
})

const coverStyle = computed(() => ({
  backgroundImage: `url(${post.value?.coverUrl})`
}))

const highlightBlocks = async () => {
  await nextTick()
  const blocks = markdownEl.value?.querySelectorAll('pre code') ?? []
  blocks.forEach((block) => {
    hljs.highlightElement(block as HTMLElement)
  })
}

watch(html, () => {
  highlightBlocks()
})

onMounted(() => {
  highlightBlocks()
})
</script>
