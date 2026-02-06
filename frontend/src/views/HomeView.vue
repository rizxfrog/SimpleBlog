<template>
  <div class="container">
    <header class="content-header">
      <div>
        <span class="eyebrow">Latest</span>
        <h2>Posts And Notes</h2>
        <p class="post-meta">
          <template v-if="isSearching">
            Search: "{{ debouncedQuery }}" · {{ searchTotal }} results
          </template>
          <template v-else>
            {{ posts.length }} posts available.
          </template>
        </p>
      </div>
      <div class="content-controls">
        <ThemeToggle />
        <RouterLink class="btn btn-ghost" to="/discover">Explore</RouterLink>
      </div>
    </header>

    <div class="content-shell">
      <section>
        <article v-if="featuredPost" class="card fade-up">
          <span class="eyebrow">Featured</span>
          <h3 class="section-title">{{ featuredPost.title }}</h3>
          <p>{{ featuredPost.summary || 'A featured post worth reading first.' }}</p>
          <RouterLink class="btn btn-primary" :to="`/post/${featuredPost.id}`">Read</RouterLink>
        </article>

        <section class="post-list">
          <div v-if="isSearching && activeSearchLoading" class="card">
            Searching...
          </div>

          <div v-else-if="isSearching && !activePosts.length" class="card">
            No results.
          </div>

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
          <h4>Search</h4>
          <div class="search-mode">
            <label for="searchMode">Mode</label>
            <select id="searchMode" v-model="searchMode">
              <option value="default">Site Search</option>
              <option value="es">Full-text</option>
            </select>
          </div>
          <input
            v-model="searchInput"
            class="search-input"
            type="text"
            placeholder="Type keywords..."
          />
        </div>
        <div class="card">
          <h4>Hot Posts</h4>
          <div v-if="!hotPosts.length" class="hot-empty">No data yet.</div>
          <ol v-else class="hot-list">
            <li v-for="(item, index) in hotPosts" :key="item.id" class="hot-item">
              <RouterLink class="hot-link" :to="`/post/${item.id}`">
                <span class="hot-rank">{{ index + 1 }}</span>
                <span class="hot-title">{{ item.title }}</span>
              </RouterLink>
              <div class="hot-meta">
                Likes {{ item.likes || 0 }} · Dislikes {{ item.dislikes || 0 }} · Views {{ item.views || 0 }}
              </div>
            </li>
          </ol>
        </div>
        <div class="card">
          <h4>Categories</h4>
          <div class="chip-list">
            <span v-for="cat in categories" :key="cat.id" class="chip">
              {{ cat.name }}
            </span>
          </div>
        </div>
        <div class="card">
          <h4>Tags</h4>
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
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useLazyQuery, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { RouterLink } from 'vue-router'
import PostCard from '../components/PostCard.vue'
import ThemeToggle from '../components/ThemeToggle.vue'

const pageSize = 12

const BLOGS_QUERY = gql`
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
`

const SEARCH_BLOGS_QUERY = gql`
  query SearchBlogs($query: String!, $page: Int!, $size: Int!) {
    searchBlogs(query: $query, page: $page, size: $size) {
      total
      query
      items {
        id
        title
        summary
        titleHighlight
        summaryHighlight
        rank
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
`

const SEARCH_BLOGS_ES_QUERY = gql`
  query SearchBlogsEs($query: String!, $page: Int!, $size: Int!) {
    searchBlogsEs(query: $query, page: $page, size: $size) {
      total
      query
      items {
        id
        title
        summary
        titleHighlight
        summaryHighlight
        rank
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
`


const META_QUERY = gql`
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
`

const HOT_BLOGS_QUERY = gql`
  query HotBlogs($limit: Int) {
    hotBlogs(limit: $limit) {
      id
      title
      likes
      dislikes
      views
    }
  }
`

const { result } = useQuery(BLOGS_QUERY, {
  page: 1,
  size: pageSize,
  publishedOnly: true
})

const {
  load: loadSearch,
  result: searchResult,
  loading: searchLoading,
  refetch: refetchSearch
} = useLazyQuery(SEARCH_BLOGS_QUERY)

const {
  load: loadSearchEs,
  result: searchEsResult,
  loading: searchEsLoading,
  refetch: refetchSearchEs
} = useLazyQuery(SEARCH_BLOGS_ES_QUERY)

const { result: metaResult } = useQuery(META_QUERY)
const { result: hotResult } = useQuery(HOT_BLOGS_QUERY, { limit: 6 })

const posts = computed(() => result.value?.blogs?.items ?? [])

const searchInput = ref('')
const searchMode = ref('default')
const debouncedQuery = ref('')
const hasSearched = ref(false)
const hasSearchedEs = ref(false)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(searchInput, (value) => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    debouncedQuery.value = value.trim()
  }, 300)
})

onBeforeUnmount(() => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})

const runSearch = async (query: string) => {
  const variables = { query, page: 1, size: pageSize }
  const useEs = searchMode.value === 'es'
  if (useEs) {
    if (!hasSearchedEs.value) {
      await loadSearchEs(undefined, variables)
      hasSearchedEs.value = true
      return
    }
    if (refetchSearchEs) {
      await refetchSearchEs(variables)
    }
    return
  }
  if (!hasSearched.value) {
    await loadSearch(undefined, variables)
    hasSearched.value = true
    return
  }
  if (refetchSearch) {
    await refetchSearch(variables)
  }
}


watch(searchMode, () => {
  if (debouncedQuery.value) {
    runSearch(debouncedQuery.value)
  }
})
watch(debouncedQuery, (query) => {
  if (!query) {
    return
  }
  runSearch(query)
})

const isSearching = computed(() => debouncedQuery.value.length > 0)
const activeSearchLoading = computed(() => (searchMode.value === 'es' ? searchEsLoading.value : searchLoading.value))

const searchItems = computed(() => {
  if (!isSearching.value) {
    return []
  }
  if (searchMode.value === 'es') {
    return searchEsResult.value?.searchBlogsEs?.items ?? []
  }
  return searchResult.value?.searchBlogs?.items ?? []
})

const searchTotal = computed(() => {
  if (!isSearching.value) {
    return posts.value.length
  }
  if (searchMode.value === 'es') {
    return searchEsResult.value?.searchBlogsEs?.total ?? 0
  }
  return searchResult.value?.searchBlogs?.total ?? 0
})

const activePosts = computed(() => (isSearching.value ? searchItems.value : posts.value))

const featuredPost = computed(() => {
  if (isSearching.value) {
    return null
  }
  return activePosts.value[0]
})

const listPosts = computed(() => {
  if (featuredPost.value) {
    return activePosts.value.slice(1)
  }
  return activePosts.value
})

const categories = computed(() => metaResult.value?.categories ?? [])
const tags = computed(() => metaResult.value?.tags ?? [])
const hotPosts = computed(() => hotResult.value?.hotBlogs ?? [])
</script>
