<template>
  <div class="container auth-shell">
    <section class="auth-card">
      <div class="auth-header">
        <h2>后台登录</h2>
        <p>使用管理员账号进入内容管理后台。</p>
      </div>
      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="••••••" show-password />
        </el-form-item>
        <el-button type="primary" class="btn btn-primary" @click="onSubmit">登录</el-button>
      </el-form>
      <p v-if="error" class="auth-error">{{ error }}</p>
    </section>
    <aside class="auth-side">
      <h3>欢迎回来</h3>
      <p>查看数据概览、管理文章、维护站点配置，一站式完成运营。</p>
      <ul>
        <li>仪表盘数据看板</li>
        <li>文章与评论管理</li>
        <li>主题与外观设置</li>
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
