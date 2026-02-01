<template>
  <AdminShell title="评论管理" subtitle="审核与管理评论">
    <section class="card">
      <div class="table-toolbar">
        <n-input v-model:value="blogIdInput" type="text" placeholder="博客ID（可选）" />
        <n-input v-model:value="keyword" type="text" placeholder="关键词搜索" />
        <div class="table-filters">
          <button class="btn btn-soft" @click="setStatus(null)">状态：全部</button>
          <button class="btn btn-soft" @click="setStatus('pending')">状态：待审</button>
          <button class="btn btn-soft" @click="setStatus('approved')">状态：已通过</button>
          <button class="btn btn-soft" @click="setStatus('rejected')">状态：已拒绝</button>
        </div>
      </div>
      <div class="table-toolbar" style="margin-top: 12px;">
        <button class="btn btn-soft" @click="bulkApprove">批量通过</button>
        <button class="btn btn-soft" @click="bulkReject">批量拒绝</button>
        <button class="btn btn-soft" @click="bulkDelete">批量删除</button>
      </div>

      <n-data-table
        :columns="columns"
        :data="comments"
        :pagination="false"
        :row-key="row => row.id"
        style="margin-top: 16px;"
        @update:checked-row-keys="onSelectionChange"
      />
    </section>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useMutation, useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { NTag, NButton } from 'naive-ui'
import AdminShell from '../components/AdminShell.vue'

type CommentRow = {
  id: number
  blogId: number
  content: string
  status: 'pending' | 'approved' | 'rejected'
  upvotes: number
  downvotes: number
  authorName?: string
  user?: { displayName?: string; username?: string }
  createdAt: string
}

const blogIdInput = ref('')
const keyword = ref('')
const selectedIds = ref<number[]>([])
const statusFilter = ref<'pending' | 'approved' | 'rejected' | null>(null)

const { result, refetch } = useQuery(
  gql`
    query AdminComments($blogId: ID, $status: CommentStatus, $keyword: String) {
      adminComments(blogId: $blogId, status: $status, keyword: $keyword) {
        id
        blogId
        content
        status
        upvotes
        downvotes
        authorName
        createdAt
        user {
          username
          displayName
        }
      }
    }
  `,
  () => ({
    blogId: blogIdInput.value ? Number(blogIdInput.value) : null,
    status: statusFilter.value,
    keyword: keyword.value || null
  })
)

const { mutate: updateStatus } = useMutation(
  gql`
    mutation UpdateCommentStatus($id: ID!, $status: CommentStatus!) {
      updateCommentStatus(id: $id, status: $status) {
        id
        status
      }
    }
  `
)

const { mutate: batchUpdateStatus } = useMutation(
  gql`
    mutation BatchUpdateCommentStatus($ids: [ID!]!, $status: CommentStatus!) {
      batchUpdateCommentStatus(ids: $ids, status: $status)
    }
  `
)

const { mutate: deleteComment } = useMutation(
  gql`
    mutation DeleteComment($id: ID!) {
      deleteComment(id: $id)
    }
  `
)

const setStatus = (value: 'pending' | 'approved' | 'rejected' | null) => {
  statusFilter.value = value
  refetch()
}

const approve = async (id: number) => {
  await updateStatus({ id, status: 'approved' })
  await refetch()
}

const reject = async (id: number) => {
  await updateStatus({ id, status: 'rejected' })
  await refetch()
}

const bulkApprove = async () => {
  if (!selectedIds.value.length) return
  await batchUpdateStatus({ ids: selectedIds.value, status: 'approved' })
  await refetch()
}

const bulkReject = async () => {
  if (!selectedIds.value.length) return
  await batchUpdateStatus({ ids: selectedIds.value, status: 'rejected' })
  await refetch()
}

const bulkDelete = async () => {
  if (!selectedIds.value.length) return
  await Promise.all(selectedIds.value.map((id) => deleteComment({ id })))
  await refetch()
}

const onSelectionChange = (keys: Array<string | number>) => {
  selectedIds.value = keys.map((key) => Number(key))
}

const columns = [
  { type: 'selection', width: 48 },
  { title: 'ID', key: 'id', width: 80 },
  { title: '博客', key: 'blogId', width: 90 },
  {
    title: '作者',
    key: 'author',
    width: 140,
    render: (row: CommentRow) =>
      row.user?.displayName || row.user?.username || row.authorName || '匿名用户'
  },
  { title: '内容', key: 'content' },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render: (row: CommentRow) =>
      h(
        NTag,
        { type: row.status === 'approved' ? 'success' : row.status === 'rejected' ? 'error' : 'warning' },
        { default: () => row.status }
      )
  },
  {
    title: '票数',
    key: 'votes',
    width: 120,
    render: (row: CommentRow) => `👍 ${row.upvotes || 0} / 👎 ${row.downvotes || 0}`
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render: (row: CommentRow) =>
      h(
        'div',
        { style: 'display:flex; gap:8px;' },
        [
          h(
            NButton,
            { size: 'small', type: 'success', ghost: true, onClick: () => approve(row.id) },
            { default: () => '通过' }
          ),
          h(
            NButton,
            { size: 'small', type: 'error', ghost: true, onClick: () => reject(row.id) },
            { default: () => '拒绝' }
          )
        ]
      )
  }
]

const comments = computed(() => result.value?.adminComments ?? [])
</script>
