<template>
  <div class="auth-page">
    <section class="auth-card fade-up">
      <div class="auth-header">
        <span class="eyebrow">后台入口</span>
        <h2>登录</h2>
        <p>仅管理员可用</p>
      </div>
      <n-form label-placement="top">
        <n-form-item label="用户名">
          <n-input v-model:value="form.username" placeholder="admin" />
        </n-form-item>
        <n-form-item label="密码">
          <n-input v-model:value="form.password" type="password" placeholder="••••••••" />
        </n-form-item>
        <n-button type="primary" class="btn btn-primary" @click="onSubmit">进入</n-button>
      </n-form>
      <p v-if="error" class="auth-error">{{ error }}</p>
    </section>
    <aside class="visual fade-up" aria-hidden="true">
      <svg viewBox="0 0 360 360" role="img">
        <defs>
          <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stop-color="#ffb457" />
            <stop offset="100%" stop-color="#ff6b6b" />
          </linearGradient>
        </defs>
        <circle cx="180" cy="180" r="140" fill="url(#glow)" opacity="0.12" />
        <path
          d="M60 200 C120 120 220 120 300 200"
          fill="none"
          stroke="url(#glow)"
          stroke-width="10"
          stroke-linecap="round"
        />
        <path
          d="M90 230 C150 170 210 170 270 230"
          fill="none"
          stroke="#1f2937"
          stroke-width="8"
          stroke-linecap="round"
        />
        <circle cx="120" cy="150" r="8" fill="#1f2937" />
        <circle cx="240" cy="150" r="8" fill="#1f2937" />
        <rect x="120" y="240" width="120" height="12" rx="6" fill="#1f2937" />
      </svg>
      <p class="visual-text">清爽、快速、专注写作</p>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation } from '@vue/apollo-composable'
import { gql } from '@apollo/client/core'
import { useAuthStore } from '@/stores/auth'

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

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;600&display=swap');

.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(240px, 1fr);
  gap: 32px;
  align-items: center;
  padding: 48px 8vw;
  background: radial-gradient(circle at 10% 20%, #fff4e3 0%, #fff 45%, #f2f7ff 100%);
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
}

.auth-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px 28px 24px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.12);
}

.auth-header {
  display: grid;
  gap: 6px;
  margin-bottom: 20px;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #f97316;
}

.auth-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #0f172a;
}

.auth-header p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.btn-primary {
  width: 100%;
  height: 44px;
  border-radius: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.auth-error {
  margin-top: 12px;
  color: #dc2626;
  font-size: 13px;
}

.visual {
  display: grid;
  place-items: center;
  gap: 12px;
  padding: 24px;
  border-radius: 24px;
  background: linear-gradient(135deg, #fff7ed 0%, #eef2ff 100%);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.visual svg {
  width: min(280px, 70vw);
  height: auto;
}

.visual-text {
  margin: 0;
  color: #1f2937;
  font-size: 14px;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
    padding: 32px 6vw;
  }

  .visual {
    order: -1;
  }
}
</style>
