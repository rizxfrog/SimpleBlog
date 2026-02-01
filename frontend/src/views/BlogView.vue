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
          <div class="article-actions">
            <button
              class="article-action vote-btn"
              type="button"
              :class="{ 'is-active': post.userVote === 1 }"
              aria-label="Like"
              @click="voteBlog(1)"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M7 11V21h8.28a1 1 0 00.98-.804l1.5-7.5A1 1 0 0016.78 11H13V7.5A2.5 2.5 0 0010.5 5H10a1 1 0 00-1 1v5H7z"
                />
              </svg>
              <span>{{ post.likes || 0 }}</span>
            </button>
            <button
              class="article-action vote-btn"
              type="button"
              :class="{ 'is-active': post.userVote === -1 }"
              aria-label="Dislike"
              @click="voteBlog(-1)"
            >
              <svg class="is-down" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M7 11V21h8.28a1 1 0 00.98-.804l1.5-7.5A1 1 0 0016.78 11H13V7.5A2.5 2.5 0 0010.5 5H10a1 1 0 00-1 1v5H7z"
                />
              </svg>
              <span>{{ post.dislikes || 0 }}</span>
            </button>
            <span class="article-action meta">Views {{ post.views || 0 }}</span>
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

    <section class="card comment-form" style="margin-top: 24px;">
      <h3 class="section-title">Comments</h3>
      <div class="comment-form-body">
        <div v-if="!auth.isAuthenticated" class="comment-form-row">
          <input v-model="commentAuthorName" type="text" placeholder="你的昵称" />
        </div>
        <div class="comment-form-row">
          <textarea v-model="commentContent" rows="4" placeholder="写下你的评论..."></textarea>
        </div>
        <div class="comment-form-actions">
          <span class="comment-form-hint">{{ submitHint }}</span>
          <button class="btn btn-primary" :disabled="submitting" @click="submitComment">
            {{ submitting ? '提交中...' : '提交评论' }}
          </button>
        </div>
      </div>
    </section>

    <section class="card" style="margin-top: 16px;">
      <div v-if="comments.length" class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <div class="comment-header">
            <strong>{{ comment.user?.displayName || comment.user?.username || comment.authorName || '匿名用户' }}</strong>
          </div>
          <p>{{ comment.content }}</p>
          <div class="comment-actions">
            <button class="comment-action" type="button" @click="voteComment(comment.id, 1)">
              👍 {{ comment.upvotes || 0 }}
            </button>
            <button class="comment-action" type="button" @click="voteComment(comment.id, -1)">
              👎 {{ comment.downvotes || 0 }}
            </button>
            <button class="comment-action" type="button" @click="toggleReplyForm(comment.id)">
              回复
            </button>
          </div>
          <div v-if="showReplyForm[comment.id]" class="comment-reply-form">
            <textarea
              v-model="replyContent[comment.id]"
              rows="3"
              placeholder="写下你的回复..."
            ></textarea>
            <div class="comment-reply-actions">
              <button class="btn btn-soft" type="button" @click="toggleReplyForm(comment.id)">取消</button>
              <button class="btn btn-primary" type="button" @click="submitReply(comment.id)">提交回复</button>
            </div>
          </div>
          <div v-if="comment.replies?.length" class="comment-replies">
            <div v-for="reply in comment.replies" :key="reply.id" class="comment-item reply-item">
              <div class="comment-header">
                <strong>{{ reply.user?.displayName || reply.user?.username || reply.authorName || '匿名用户' }}</strong>
              </div>
              <p>{{ reply.content }}</p>
              <div class="comment-actions">
                <button class="comment-action" type="button" @click="voteComment(reply.id, 1)">
                  👍 {{ reply.upvotes || 0 }}
                </button>
                <button class="comment-action" type="button" @click="voteComment(reply.id, -1)">
                  👎 {{ reply.downvotes || 0 }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <p v-else class="comment-empty">暂无评论，成为第一个留言的人吧。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMutation, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { marked, type Tokens } from 'marked'
import hljs from 'highlight.js'
import dayjs from 'dayjs'
import { useAuthStore } from '../stores/auth'

type TocItem = {
  id: string
  text: string
  level: number
}

const route = useRoute()
const postId = Number(route.params.id)

const auth = useAuthStore()
const { result, refetch } = useQuery(
  gql`
    query Blog($id: ID!) {
      blog(id: $id) {
        id
        title
        content
        coverUrl
        createdAt
        views
        likes
        dislikes
        userVote
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
        authorName
        upvotes
        downvotes
        replies {
          id
          content
          authorName
          upvotes
          downvotes
          user {
            id
            username
            displayName
          }
        }
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

const commentContent = ref('')
const commentAuthorName = ref('')
const submitting = ref(false)
const submitHint = ref('提交后需要审核，通过后显示。')

const { mutate: createComment } = useMutation(
  gql`
    mutation CreateComment($input: CommentInput!) {
      createComment(input: $input) {
        id
        status
      }
    }
  `
)

const { mutate: vote } = useMutation(
  gql`
    mutation VoteComment($id: ID!, $value: Int!) {
      voteComment(id: $id, value: $value) {
        id
        upvotes
        downvotes
      }
    }
  `
)

const { mutate: recordView } = useMutation(
  gql`
    mutation RecordBlogView($id: ID!) {
      recordBlogView(id: $id)
    }
  `
)

const { mutate: voteBlogMutation } = useMutation(
  gql`
    mutation VoteBlog($id: ID!, $value: Int!) {
      voteBlog(id: $id, value: $value) {
        id
        likes
        dislikes
        views
      }
    }
  `
)

const { mutate: createReply } = useMutation(
  gql`
    mutation CreateReply($input: ReplyInput!) {
      createReply(input: $input) {
        id
        status
      }
    }
  `
)

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

const submitComment = async () => {
  if (!commentContent.value.trim()) {
    submitHint.value = '评论内容不能为空。'
    return
  }
  if (!auth.isAuthenticated && !commentAuthorName.value.trim()) {
    submitHint.value = '匿名评论请填写昵称。'
    return
  }
  submitting.value = true
  submitHint.value = '提交中...'
  try {
    await createComment({
      input: {
        blogId: postId,
        content: commentContent.value.trim(),
        authorName: auth.isAuthenticated ? null : commentAuthorName.value.trim()
      }
    })
    commentContent.value = ''
    submitHint.value = '已提交，等待审核通过后显示。'
    await refetch()
  } catch (error: any) {
    submitHint.value = error?.message || '提交失败，请稍后重试。'
  } finally {
    submitting.value = false
  }
}

const voteBlog = async (value: number) => {
  try {
    await voteBlogMutation({ id: postId, value })
    await refetch()
  } catch {
    // ignore
  }
}

const voteComment = async (id: number, value: number) => {
  try {
    await vote({ id, value })
    await refetch()
  } catch {
    // ignore
  }
}

const replyContent = ref<Record<number, string>>({})
const showReplyForm = ref<Record<number, boolean>>({})

const toggleReplyForm = (id: number) => {
  showReplyForm.value = { ...showReplyForm.value, [id]: !showReplyForm.value[id] }
}

const submitReply = async (commentId: number) => {
  const content = (replyContent.value[commentId] || '').trim()
  if (!content) return
  try {
    await createReply({
      input: {
        commentId,
        content,
        authorName: auth.isAuthenticated ? null : commentAuthorName.value.trim()
      }
    })
    replyContent.value = { ...replyContent.value, [commentId]: '' }
    showReplyForm.value = { ...showReplyForm.value, [commentId]: false }
    await refetch()
  } catch {
    // ignore
  }
}

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
  recordView({ id: postId }).catch(() => null)
})

onBeforeUnmount(() => {
  if (signTimer) {
    window.clearTimeout(signTimer)
    signTimer = null
  }
  disconnectObserver()
})
</script>
