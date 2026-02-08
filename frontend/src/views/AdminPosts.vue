<template>
  <AdminShell title="文章管理" subtitle="维护站点内容与发布状态">
    <template #actions>
      <RouterLink class="btn btn-primary" to="/admin/editor">新建</RouterLink>
    </template>

    <section class="card">
      <div class="table-toolbar">
        <n-input type="text" placeholder="输入关键词搜索" />
        <div class="table-filters">
          <button class="btn btn-soft">状态：全部</button>
          <button class="btn btn-soft">可见性：全部</button>
          <button class="btn btn-soft">排序：默认</button>
        </div>
      </div>
      <n-data-table
        :columns="columns"
        :data="posts"
        :pagination="false"
        :row-key="rowKey"
        style="margin-top: 16px;"
      />
    </section>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { RouterLink } from 'vue-router'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { NTag } from 'naive-ui'
import AdminShell from '../components/AdminShell.vue'

type PostRow = {
  id: number
  title: string
  published: boolean
}

const rowKey = (row: PostRow) => row.id

const columns = [
  {
    type: 'selection'
  },
  {
    title: '标题',
    key: 'title'
  },
  {
    title: '状态',
    key: 'published',
    render: (row: PostRow) =>
      h(
        NTag,
        { type: row.published ? 'success' : 'info' },
        { default: () => (row.published ? '已发布' : '草稿') }
      )
  },
  {
    title: '操作',
    key: 'actions',
    render: (row: PostRow) => h(RouterLink, { to: `/admin/editor/${row.id}` }, { default: () => '编辑' })
  }
]

const { result } = useQuery(
  gql`
    query Blogs($page: Int!, $size: Int!, $publishedOnly: Boolean) {
      blogs(page: $page, size: $size, publishedOnly: $publishedOnly) {
        items {
          id
          title
          published
        }
      }
    }
  `,
  { page: 1, size: 20, publishedOnly: false }
)

const posts = computed(() => result.value?.blogs?.items ?? [])
</script>
