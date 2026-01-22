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
        </div>
        <div class="editor-split">
          <textarea v-model="form.content" placeholder="开始写下你的 Markdown 内容..." />
          <div class="editor-preview markdown" v-html="previewHtml"></div>
        </div>
      </div>
      <p v-if="message" class="status-text">{{ message }}</p>
    </section>
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
  const suffix = form.content ? '\n\n' : ''
  form.content += `${suffix}${snippet}`
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
