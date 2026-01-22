<template>
  <AdminShell title="文章管理" subtitle="维护站点内容与发布状态">
    <template #actions>
      <RouterLink class="btn btn-primary" to="/admin/editor">新建</RouterLink>
    </template>

    <section class="card">
      <div class="table-toolbar">
        <input type="text" placeholder="输入关键词搜索" />
        <div class="table-filters">
          <button class="btn btn-soft">状态：全部</button>
          <button class="btn btn-soft">可见性：全部</button>
          <button class="btn btn-soft">排序：默认</button>
        </div>
      </div>
      <el-table :data="posts" style="width: 100%; margin-top: 16px;">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="published" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.published ? 'success' : 'info'">
              {{ scope.row.published ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="scope">
            <RouterLink :to="`/admin/editor/${scope.row.id}`">编辑</RouterLink>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </AdminShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useQuery } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import AdminShell from '../components/AdminShell.vue'

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
