<template>
  <AdminShell :title="isEdit ? '编辑文章' : '新建文章'" subtitle="Markdown 编辑与预览">
    <template #actions>
      <n-button type="primary" class="btn btn-primary" @click="onSubmit">保存</n-button>
    </template>

    <section class="card editor-panel">
      <div class="editor-meta">
        <n-form label-placement="top">
          <n-form-item label="标题">
            <n-input v-model:value="form.title" placeholder="文章标题" />
          </n-form-item>
          <n-form-item label="摘要">
            <n-input
              v-model:value="form.summary"
              type="textarea"
              placeholder="一句话概括文章内容"
              :autosize="{ minRows: 2, maxRows: 4 }"
            />
          </n-form-item>
          <div class="meta-grid">
            <n-form-item label="分类">
              <n-select v-model:value="form.categoryId" :options="categoryOptions" placeholder="请选择分类" />
            </n-form-item>
            <n-form-item label="标签">
              <n-select
                v-model:value="form.tagIds"
                multiple
                :options="tagOptions"
                placeholder="请选择标签"
              />
            </n-form-item>
          </div>
          <n-form-item label="封面图">
            <n-input v-model:value="form.coverUrl" placeholder="https://example.com/cover.jpg" />
          </n-form-item>
          <n-form-item label="发布状态">
            <n-switch v-model:value="form.published">
              <template #checked>已发布</template>
              <template #unchecked>草稿</template>
            </n-switch>
          </n-form-item>
        </n-form>
      </div>

      <div class="editor-body">
        <div class="editor-toolbar">
          <button @click="appendSnippet('# 标题')">H1</button>
          <button @click="appendSnippet('## 二级标题')">H2</button>
          <button @click="appendSnippet('**加粗文本**')">B</button>
          <button @click="appendSnippet('_斜体文本_')">I</button>
          <button @click="appendSnippet('> 引用内容')">Quote</button>
          <button @click="appendSnippet('- 列表项')">List</button>
          <button @click="appendSnippet('```bash\n\n```')">Code</button>
          <button @click="appendSnippet('![图片描述](url)')">Img</button>
          <button @click="triggerUpload('image')">Upload Image</button>
          <button @click="triggerUpload('video')">Upload Video</button>
        </div>
        <div class="editor-split">
          <textarea
            ref="editorRef"
            v-model="form.content"
            placeholder="开始写下你的 Markdown 内容..."
            @paste="onPaste"
            @drop="onDrop"
            @dragover.prevent
            @dragenter.prevent
          />
          <div class="editor-preview markdown" ref="previewRef" v-html="previewHtml"></div>
        </div>
        <div v-if="uploads.length" class="upload-queue">
          <div v-for="item in uploads" :key="item.id" class="upload-item">
            <div class="upload-preview">
              <img v-if="item.kind === 'image'" :src="item.previewUrl" :alt="item.name" />
              <video v-else :src="item.previewUrl" muted></video>
            </div>
            <div class="upload-meta">
              <strong>{{ item.name }}</strong>
              <div class="upload-progress">
                <span :style="{ width: `${item.progress}%` }"></span>
              </div>
              <small>{{ item.progress }}%</small>
            </div>
          </div>
        </div>
      </div>
      <p v-if="message" class="status-text">{{ message }}</p>
    </section>
    <input
      ref="imageInputRef"
      type="file"
      accept="image/*"
      multiple
      class="file-input-hidden"
      @change="onFileChange($event)"
    />
    <input
      ref="videoInputRef"
      type="file"
      accept="video/*"
      multiple
      class="file-input-hidden"
      @change="onFileChange($event)"
    />
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { marked, type Tokens } from 'marked'
import AdminShell from '../components/AdminShell.vue'

const route = useRoute()
const router = useRouter()
const blogId = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEdit = computed(() => blogId.value !== null)
const message = ref('')
const editorRef = ref<HTMLTextAreaElement | null>(null)
const previewRef = ref<HTMLElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const isUploading = ref(false)
let signTimer: number | null = null
const uploads = ref<Array<{
  id: string
  name: string
  kind: 'image' | 'video'
  progress: number
  previewUrl: string
  done: boolean
}>>([])

const form = reactive({
  title: '',
  summary: '',
  content: '',
  categoryId: null as number | null,
  coverUrl: '',
  published: false,
  tagIds: [] as number[]
})

const { result: metaResult } = useQuery(gql`
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

const categories = computed(() => metaResult.value?.categories ?? [])
const tags = computed(() => metaResult.value?.tags ?? [])
const categoryOptions = computed(() => categories.value.map((cat: any) => ({ label: cat.name, value: cat.id })))
const tagOptions = computed(() => tags.value.map((tag: any) => ({ label: tag.name, value: tag.id })))

const blogQueryOptions = computed(() => ({ enabled: isEdit.value }))

const { result: blogResult } = useQuery(
  gql`
    query Blog($id: ID!) {
      blog(id: $id) {
        id
        title
        summary
        content
        categoryId
        coverUrl
        published
        tags {
          id
        }
      }
    }
  `,
  () => ({ id: blogId.value }),
  blogQueryOptions
)

watch(
  () => blogResult.value?.blog,
  (blog) => {
    if (!blog) return
    form.title = blog.title
    form.summary = blog.summary ?? ''
    form.content = blog.content
    form.categoryId = blog.categoryId ? Number(blog.categoryId) : null
    form.coverUrl = blog.coverUrl ?? ''
    form.published = blog.published
    form.tagIds = blog.tags?.map((tag: any) => Number(tag.id)) ?? []
  },
  { immediate: true }
)

const previewHtml = computed(() => {
  if (!form.content) return ''
  const renderer = new marked.Renderer()
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
    const headingHtml = marked.parseInline(token.text) as string
    return `<h${level}>${headingHtml}</h${level}>`
  }
  return marked.parse(form.content, { renderer }) as string
})

watch(
  () => previewHtml.value,
  () => {
    scheduleSignMedia()
  }
)

const { mutate: createBlog } = useMutation(gql`
  mutation CreateBlog($input: BlogInput!) {
    createBlog(input: $input) {
      id
    }
  }
`)

const { mutate: updateBlog } = useMutation(gql`
  mutation UpdateBlog($id: ID!, $input: BlogInput!) {
    updateBlog(id: $id, input: $input) {
      id
    }
  }
`)

const appendSnippet = (snippet: string) => {
  insertAtCursor(snippet)
}

const insertAtCursor = (snippet: string) => {
  const textarea = editorRef.value
  if (!textarea) {
    const suffix = form.content ? '\n\n' : ''
    form.content += `${suffix}${snippet}`
    return
  }
  const start = textarea.selectionStart ?? form.content.length
  const end = textarea.selectionEnd ?? form.content.length
  const prefix = form.content.slice(0, start)
  const suffix = form.content.slice(end)
  const spacer = prefix && !prefix.endsWith('\n') ? '\n\n' : ''
  const trail = suffix && !suffix.startsWith('\n') ? '\n\n' : ''
  form.content = `${prefix}${spacer}${snippet}${trail}${suffix}`
}

const triggerUpload = (kind: 'image' | 'video') => {
  const input = kind === 'image' ? imageInputRef.value : videoInputRef.value
  input?.click()
}

const onFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = input.files ? Array.from(input.files) : []
  if (!files.length) return
  input.value = ''
  await handleUploads(files)
}

const onPaste = async (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items) return
  const files = Array.from(items)
    .filter((item) => item.kind === 'file')
    .map((item) => item.getAsFile())
    .filter((file): file is File => Boolean(file))
  const mediaFiles = files.filter((file) => file.type.startsWith('image/') || file.type.startsWith('video/'))
  if (!mediaFiles.length) return
  event.preventDefault()
  await handleUploads(mediaFiles)
}

const onDrop = async (event: DragEvent) => {
  const files = event.dataTransfer?.files ? Array.from(event.dataTransfer.files) : []
  const mediaFiles = files.filter((file) => file.type.startsWith('image/') || file.type.startsWith('video/'))
  if (!mediaFiles.length) return
  event.preventDefault()
  await handleUploads(mediaFiles)
}

const handleUploads = async (files: File[]) => {
  for (const file of files) {
    const isImage = file.type.startsWith('image/')
    const isVideo = file.type.startsWith('video/')
    if (!isImage && !isVideo) {
      continue
    }
    await handleUpload(file, isImage ? 'image' : 'video')
  }
}

const handleUpload = async (file: File, kind: 'image' | 'video') => {
  if (isUploading.value) return
  isUploading.value = true
  message.value = '正在上传...'
  const uploadId = `${Date.now()}-${Math.random().toString(16).slice(2)}`
  const previewUrl = URL.createObjectURL(file)
  uploads.value = [
    {
      id: uploadId,
      name: file.name,
      kind,
      progress: 0,
      previewUrl,
      done: false
    },
    ...uploads.value
  ]
  try {
    const uploadUrl = buildUploadUrl()
    const token = localStorage.getItem('simpleblog_token')
    const formData = new FormData()
    formData.append('file', file)
    if (form.categoryId) {
      formData.append('categoryId', String(form.categoryId))
    }
    if (blogId.value) {
      formData.append('blogId', String(blogId.value))
    }
    const data = await uploadWithProgress(uploadUrl, formData, token, (progress) => {
      updateUploadProgress(uploadId, progress)
    })
    const snippet = buildMediaSnippet(data)
    insertAtCursor(snippet)
    message.value = kind === 'image' ? '图片已插入' : '视频已插入'
    markUploadDone(uploadId)
  } catch (error: any) {
    message.value = error?.message || '上传失败'
    markUploadDone(uploadId, true)
  } finally {
    isUploading.value = false
  }
}

const uploadWithProgress = (
  url: string,
  formData: FormData,
  token: string | null,
  onProgress: (progress: number) => void
) => {
  return new Promise<{ url: string; name?: string; contentType?: string }>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', url)
    if (token) {
      xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    }
    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable) return
      const percent = Math.round((event.loaded / event.total) * 100)
      onProgress(percent)
    }
    xhr.onerror = () => reject(new Error('Upload failed'))
    xhr.onload = () => {
      try {
        const data = JSON.parse(xhr.responseText || '{}')
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(data)
        } else {
          reject(new Error(data?.message || 'Upload failed'))
        }
      } catch (error) {
        reject(error)
      }
    }
    xhr.send(formData)
  })
}

const updateUploadProgress = (id: string, progress: number) => {
  const target = uploads.value.find((item) => item.id === id)
  if (!target) return
  target.progress = progress
}

const markUploadDone = (id: string, failed = false) => {
  const target = uploads.value.find((item) => item.id === id)
  if (!target) return
  target.progress = failed ? target.progress : 100
  target.done = true
  setTimeout(() => {
    URL.revokeObjectURL(target.previewUrl)
  }, 2000)
}

const buildUploadUrl = () => {
  const api = import.meta.env.VITE_API_URL ?? 'http://localhost:8888/graphql'
  return api.replace(/\/graphql\/?$/, '') + '/api/uploads'
}

const scheduleSignMedia = () => {
  if (signTimer) {
    window.clearTimeout(signTimer)
  }
  signTimer = window.setTimeout(() => {
    signMediaSources()
  }, 180)
}

const signMediaSources = async () => {
  const root = previewRef.value
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
    // ignore signing failures
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
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const buildMediaSnippet = (data: { url: string; name?: string; contentType?: string }) => {
  const name = data.name || 'media'
  const contentType = data.contentType || ''
  if (contentType.startsWith('image/')) {
    return `![${name}](${data.url})`
  }
  if (contentType.startsWith('video/')) {
    return `<video controls><source src="${data.url}" type="${contentType}"></video>`
  }
  return `[${name}](${data.url})`
}

const onSubmit = async () => {
  message.value = ''
  const payload = {
    title: form.title,
    summary: form.summary,
    content: form.content,
    categoryId: form.categoryId,
    coverUrl: form.coverUrl,
    published: form.published,
    tagIds: form.tagIds
  }

  if (isEdit.value && blogId.value) {
    await updateBlog({ id: blogId.value, input: payload })
    message.value = '已更新'
  } else {
    const result = await createBlog({ input: payload })
    const id = result?.data?.createBlog?.id
    message.value = '已创建'
    if (id) {
      router.push(`/admin/editor/${id}`)
    }
  }
}
</script>
