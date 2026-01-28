<template>
  <div class="container">
    <div v-if="post" class="blog-shell">
      <section class="blog-article">
        <div class="article-hero">
          <span class="eyebrow">Article</span>
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

      <aside v-if="showToc" class="article-toc card">
        <h4>Contents</h4>
        <nav class="toc-list" aria-label="Article table of contents">
          <button
            v-for="item in tocItems"
            :key="item.id"
            type="button"
            class="toc-link"
            :class="[`level-${item.level}`, { active: item.id === activeHeadingId }]"
            @click="scrollToHeading(item.id)"
          >
            <span class="toc-dot" aria-hidden="true"></span>
            <span class="toc-text">{{ item.text }}</span>
          </button>
        </nav>
      </aside>
    </div>

    <section class="card" style="margin-top: 24px;" v-if="comments.length">
      <h3 class="section-title">Comments</h3>
      <div class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <strong>{{ comment.user?.displayName || comment.user?.username || 'Guest' }}</strong>
          <p>{{ comment.content }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { marked, type Tokens } from 'marked'
import hljs from 'highlight.js'
import dayjs from 'dayjs'

type TocItem = {
  id: string
  text: string
  level: number
}

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
const html = ref('')
const markdownEl = ref<HTMLElement | null>(null)
const tocItems = ref<TocItem[]>([])
const activeHeadingId = ref('')
let headingObserver: IntersectionObserver | null = null
let signTimer: number | null = null

const TOC_MIN_HEADINGS = 4
const TOC_MIN_CONTENT_LENGTH = 1200

const showToc = computed(() => {
  const headingCount = tocItems.value.length
  if (headingCount < 2) return false
  if (headingCount >= TOC_MIN_HEADINGS) return true
  const contentLength = post.value?.content?.length ?? 0
  return contentLength >= TOC_MIN_CONTENT_LENGTH
})

const authorName = computed(() => {
  return post.value?.author?.displayName || post.value?.author?.username || 'Anonymous'
})

const formattedDate = computed(() => {
  return post.value?.createdAt ? dayjs(post.value.createdAt).format('YYYY/MM/DD') : ''
})

const coverStyle = computed(() => ({
  backgroundImage: `url(${post.value?.coverUrl})`
}))

const slugify = (value: string) => {
  const normalized = value
    .replace(/<[^>]+>/g, '')
    .trim()
    .toLowerCase()
    .replace(/[\u0000-\u001f]/g, '')
    .replace(/[^a-z0-9\u4e00-\u9fa5\s-]/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
  return normalized || 'section'
}

const disconnectObserver = () => {
  if (headingObserver) {
    headingObserver.disconnect()
    headingObserver = null
  }
}

const setupHeadingObserver = () => {
  disconnectObserver()
  const root = markdownEl.value
  if (!root) return

  const headings = Array.from(root.querySelectorAll<HTMLElement>('h2[id], h3[id], h4[id]'))
  if (!headings.length) return

  headingObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)
      if (visible[0]?.target) {
        activeHeadingId.value = (visible[0].target as HTMLElement).id
      }
    },
    {
      root: null,
      rootMargin: '-96px 0px -70% 0px',
      threshold: [0, 1]
    }
  )

  headings.forEach((heading) => headingObserver?.observe(heading))
  activeHeadingId.value = headings[0].id
}

const highlightBlocks = async () => {
  await nextTick()
  const blocks = markdownEl.value?.querySelectorAll('pre code') ?? []
  blocks.forEach((block) => {
    hljs.highlightElement(block as HTMLElement)
  })
}

const renderMarkdown = async (content: string | undefined) => {
  if (!content) {
    html.value = ''
    tocItems.value = []
    activeHeadingId.value = ''
    disconnectObserver()
    return
  }

  const renderer = new marked.Renderer()
  const slugCounts = new Map<string, number>()
  const nextToc: TocItem[] = []

  renderer.image = (token) => {
    const src = token.href ?? ''
    const title = token.title ? ` title="${escapeHtmlAttr(token.title)}"` : ''
    const alt = token.text ? escapeHtmlAttr(token.text) : 'image'
    if (isVideoUrl(src)) {
      return `<video controls preload="metadata"${title}><source src="${escapeHtmlAttr(src)}"></video>`
    }
    return `<img src="${escapeHtmlAttr(src)}" alt="${alt}" loading="lazy"${title} />`
  }

  renderer.heading = (token: Tokens.Heading) => {
    const level = token.depth
    const plainText = token.text
    const headingHtml = marked.parseInline(token.text) as string
    const base = slugify(plainText)
    const count = (slugCounts.get(base) ?? 0) + 1
    slugCounts.set(base, count)
    const id = count === 1 ? base : `${base}-${count}`

    if (level >= 2 && level <= 4) {
      nextToc.push({ id, text: plainText, level })
    }

    return `<h${level} id="${id}">${headingHtml}</h${level}>`
  }

  html.value = marked.parse(content, { renderer }) as string
  tocItems.value = nextToc

  await nextTick()
  scheduleSignMedia()
  await highlightBlocks()
  setupHeadingObserver()
}

const isVideoUrl = (value: string) => {
  const clean = value.split('?')[0].split('#')[0].toLowerCase()
  return (
    clean.endsWith('.mp4') ||
    clean.endsWith('.webm') ||
    clean.endsWith('.ogg') ||
    clean.endsWith('.mov') ||
    clean.endsWith('.m4v')
  )
}

const escapeHtmlAttr = (value: string) => {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

const scheduleSignMedia = () => {
  if (signTimer) {
    window.clearTimeout(signTimer)
  }
  signTimer = window.setTimeout(() => {
    signMediaSources()
  }, 120)
}

const signMediaSources = async () => {
  const root = markdownEl.value
  if (!root) return
  const targets = Array.from(root.querySelectorAll<HTMLImageElement | HTMLSourceElement>('img, video source'))
  const urls = Array.from(
    new Set(
      targets
        .map((node) => node.getAttribute('src'))
        .filter((value): value is string => Boolean(value))
    )
  )
  if (!urls.length) return
  try {
    const signedUrls = await fetchSignedUrls(urls)
    if (!signedUrls) return
    targets.forEach((node) => {
      const src = node.getAttribute('src')
      if (!src) return
      const signed = signedUrls[src]
      if (!signed) return
      node.setAttribute('src', signed)
      if (node instanceof HTMLSourceElement) {
        const parent = node.parentElement as HTMLVideoElement | null
        parent?.load()
      }
    })
  } catch {
    // ignore signing failures to avoid breaking render
  }
}

const fetchSignedUrls = async (urls: string[]) => {
  const api = import.meta.env.VITE_API_URL ?? 'http://localhost:8888/graphql'
  const signUrl = api.replace(/\/graphql\/?$/, '') + '/api/media/sign'
  const response = await fetch(signUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ urls })
  })
  if (!response.ok) return null
  const data = await response.json()
  return data?.signedUrls as Record<string, string> | null
}

const scrollToHeading = (id: string) => {
  const selector = `#${CSS.escape(id)}`
  const target = markdownEl.value?.querySelector<HTMLElement>(selector)
  if (!target) return
  target.scrollIntoView({ behavior: 'smooth', block: 'start' })
  activeHeadingId.value = id
}

watch(
  () => post.value?.content,
  (content) => {
    renderMarkdown(content)
  },
  { immediate: true }
)

onMounted(() => {
  setupHeadingObserver()
})

onBeforeUnmount(() => {
  if (signTimer) {
    window.clearTimeout(signTimer)
    signTimer = null
  }
  disconnectObserver()
})
</script>
