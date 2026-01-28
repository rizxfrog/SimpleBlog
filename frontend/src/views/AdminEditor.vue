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
          />
          <div class="editor-preview markdown" v-html="previewHtml"></div>
        </div>
      </div>
      <p v-if="message" class="status-text">{{ message }}</p>
    </section>
    <input ref="imageInputRef" type="file" accept="image/*" class="file-input-hidden" @change="onFileChange('image', $event)" />
    <input ref="videoInputRef" type="file" accept="video/*" class="file-input-hidden" @change="onFileChange('video', $event)" />
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { marked } from 'marked'
import AdminShell from '../components/AdminShell.vue'

const route = useRoute()
const router = useRouter()
const blogId = computed(() => (route.params.id ? Number(route.params.id) : null))
const isEdit = computed(() => blogId.value !== null)
const message = ref('')
const editorRef = ref<HTMLTextAreaElement | null>(null)
const imageInputRef = ref<HTMLInputElement | null>(null)
const videoInputRef = ref<HTMLInputElement | null>(null)
const isUploading = ref(false)

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

const previewHtml = computed(() => (form.content ? marked.parse(form.content) : ''))

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

const onFileChange = async (kind: 'image' | 'video', event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''
  await handleUpload(file, kind)
}

const onPaste = async (event: ClipboardEvent) => {
  const items = event.clipboardData?.items
  if (!items) return
  const fileItem = Array.from(items).find((item) => item.kind === 'file')
  if (!fileItem) return
  const file = fileItem.getAsFile()
  if (!file) return
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  if (!isImage && !isVideo) return
  event.preventDefault()
  await handleUpload(file, isImage ? 'image' : 'video')
}

const handleUpload = async (file: File, kind: 'image' | 'video') => {
  if (isUploading.value) return
  isUploading.value = true
  message.value = '正在上传...'
  try {
    const uploadUrl = buildUploadUrl()
    const token = localStorage.getItem('simpleblog_token')
    const formData = new FormData()
    formData.append('file', file)
    const response = await fetch(uploadUrl, {
      method: 'POST',
      headers: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      body: formData
    })
    const data = await response.json()
    if (!response.ok) {
      throw new Error(data?.message || 'Upload failed')
    }
    const snippet = buildMediaSnippet(data)
    insertAtCursor(snippet)
    message.value = kind === 'image' ? '图片已插入' : '视频已插入'
  } catch (error: any) {
    message.value = error?.message || '上传失败'
  } finally {
    isUploading.value = false
  }
}

const buildUploadUrl = () => {
  const api = import.meta.env.VITE_API_URL ?? 'http://localhost:8888/graphql'
  return api.replace(/\/graphql\/?$/, '') + '/api/uploads'
}

const buildMediaSnippet = (data: { url: string; name?: string; contentType?: string }) => {
  const name = data.name || 'media'
  const contentType = data.contentType || ''
  if (contentType.startsWith('image/')) {
    return `![${name}](${data.url})`
  }
  if (contentType.startsWith('video/')) {
    return `<video controls src="${data.url}"></video>`
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
