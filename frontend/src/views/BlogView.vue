<template>
  <div class="container">
    <section class="panel" v-if="post">
      <h1>{{ post.title }}</h1>
      <p class="tag" v-if="post.category">{{ post.category.name }}</p>
      <div class="markdown" v-html="html"></div>
    </section>

    <section class="panel" style="margin-top: 24px;" v-if="comments.length">
      <h2>评论</h2>
      <div v-for="comment in comments" :key="comment.id" style="border-bottom: 1px solid var(--line); padding: 12px 0;">
        <strong>{{ comment.user?.displayName || comment.user?.username || '访客' }}</strong>
        <p>{{ comment.content }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { marked } from 'marked'

const route = useRoute()
const postId = Number(route.params.id)

const { result } = useQuery(
  gql`
    query Blog($id: ID!) {
      blog(id: $id) {
        id
        title
        content
        category {
          id
          name
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
</script>
