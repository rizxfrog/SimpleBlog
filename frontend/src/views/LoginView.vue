<template>
  <div class="container auth-shell">
    <section class="auth-card fade-up">
      <div class="auth-header">
        <span class="eyebrow">管理员入口</span>
        <h2>登录后台</h2>
        <p>使用管理员账号进入内容管理与站点配置。</p>
      </div>
      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="••••••••" show-password />
        </el-form-item>
        <el-button type="primary" class="btn btn-primary" @click="onSubmit">登录</el-button>
      </el-form>
      <p v-if="error" class="auth-error">{{ error }}</p>
    </section>
    <aside class="card card-ghost fade-up">
      <span class="eyebrow">管理入口说明</span>
      <h3 class="section-title">你可以在这里做什么？</h3>
      <ul>
        <li>快速创建与发布文章。</li>
        <li>管理分类、标签与封面素材。</li>
        <li>查看站点数据与更新提醒。</li>
      </ul>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const error = ref('')

const form = reactive({
  username: '',
  password: ''
})

const { mutate } = useMutation(gql`
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      token
      user {
        id
        username
      }
    }
  }
`)

const onSubmit = async () => {
  error.value = ''
  try {
    const result = await mutate({ input: form })
    const token = result?.data?.login?.token
    if (token) {
      auth.setToken(token)
      const redirect = (route.query.redirect as string) || '/admin'
      router.push(redirect)
    }
  } catch (e: any) {
    error.value = e?.message || '登录失败'
  }
}
</script>
