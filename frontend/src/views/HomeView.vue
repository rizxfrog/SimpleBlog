<template>
  <div class="container">
    <header class="content-header">
      <div>
        <span class="eyebrow">最新发布</span>
        <h2>文章与实践笔记</h2>
        <p class="post-meta">共 {{ posts.length }} 篇内容，聚焦可复用的技术经验。</p>
      </div>
      <div class="content-controls">
        <ThemeToggle/>
        <RouterLink class="btn btn-ghost" to="/discover">浏览主题</RouterLink>
      </div>
    </header>

    <div class="content-shell">
      <section>
        <article v-if="featuredPost" class="card fade-up">
          <span class="eyebrow">编辑推荐</span>
          <h3 class="section-title">{{ featuredPost.title }}</h3>
          <p>{{ featuredPost.summary || '这是一篇值得优先阅读的内容。' }}</p>
          <RouterLink class="btn btn-primary" :to="`/post/${featuredPost.id}`">阅读本篇</RouterLink>
        </article>

        <section class="post-list">
          <PostCard
              v-for="post in listPosts"
              :key="post.id"
              :post="post"
              :category="post.category"
          />
        </section>
      </section>

      <aside class="sidebar">
        <div class="card">
          <h4>站内检索</h4>
          <input class="search-input" type="text" placeholder="输入关键词"/>
        </div>
        <div class="card">
          <h4>分类</h4>
          <div class="chip-list">
            <span v-for="cat in categories" :key="cat.id" class="chip">
              {{ cat.name }}
            </span>
          </div>
        </div>
        <div class="card">
          <h4>标签</h4>
          <div class="chip-list">
            <span v-for="tag in tags" :key="tag.id" class="chip">
              {{ tag.name }}
            </span>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {useQuery} from '@vue/apollo-composable'
import {gql} from '@apollo/client/core'
import {RouterLink} from 'vue-router'
import PostCard from '../components/PostCard.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

const {result} = useQuery(
    gql`
      query Blogs($page: Int!, $size: Int!, $publishedOnly: Boolean) {
        blogs(page: $page, size: $size, publishedOnly: $publishedOnly) {
          items {
            id
            title
            summary
            coverUrl
            createdAt
            author {
              id
              username
              displayName
            }
            category {
              id
              name
            }
          }
        }
      }
    `,
    {page: 1, size: 12, publishedOnly: true}
)

const {result: metaResult} = useQuery(gql`
  query Meta {
    categories {
      id
      name
    }
    tags {
      id
      name
    }
  }
`)

const posts = computed(() => result.value?.blogs?.items ?? [])
const featuredPost = computed(() => posts.value[0])
const listPosts = computed(() => (featuredPost.value ? posts.value.slice(1) : posts.value))
const categories = computed(() => metaResult.value?.categories ?? [])
const tags = computed(() => metaResult.value?.tags ?? [])
</script>
