<template>
  <n-config-provider :theme="naiveTheme">
    <div class="landing container">
    <section class="hero-grid">
      <div class="hero-copy">
        <n-text depth="3" class="eyebrow">SimpleBlog · GraphQL</n-text>
        <n-h1 class="hero-title">SimpleBlog</n-h1>
        <n-p depth="2" class="hero-description">
          专注 Java、Go、Python 编程语言与计算机基础、计算机网络，分享技术干货
        </n-p>

        <n-space class="hero-actions" size="large">
          <n-button type="primary" size="large" round @click="router.push('/blog')">
            开始阅读
          </n-button>
          <n-button size="large" round ghost @click="router.push('/discover')">
            探索
          </n-button>
        </n-space>

        <n-grid :x-gap="24" :cols="3" class="hero-metrics">
          <n-gi>
            <n-statistic label="精选文章">
              <span class="metric-value">120+</span>
            </n-statistic>
          </n-gi>
          <n-gi>
            <n-statistic label="主题路径">
              <span class="metric-value">18</span>
            </n-statistic>
          </n-gi>
          <n-gi>
            <n-statistic label="周更新节奏">
              <span class="metric-value">7d</span>
            </n-statistic>
          </n-gi>
        </n-grid>
      </div>

      <div class="hero-panel">
        <n-card title="主题地图" hoverable class="topic-card">
          <n-space wrap>
            <n-button
                v-for="item in topics"
                :key="item.label"
                secondary
                round
                type="info"
                @click="router.push(item.to)"
            >
              {{ item.label }}
            </n-button>
          </n-space>
        </n-card>
      </div>
    </section>
    </div>
  </n-config-provider>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { computed } from 'vue'
import { useUiStore } from '@/stores/ui'
import {
  NButton,
  NConfigProvider,
  NText,
  NH1,
  NP,
  NSpace,
  NGrid,
  NGi,
  NStatistic,
  NCard,
  darkTheme
} from 'naive-ui'

const router = useRouter()
const ui = useUiStore()

const naiveTheme = computed(() => {
  if (ui.theme === 'dark') {
    return darkTheme
  }
  if (ui.theme === 'system') {
    const prefersDark = typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches
    return prefersDark ? darkTheme : null
  }
  return null
})

const topics = [
  { label: 'Java', to: '/discover' },
  { label: 'Go', to: '/discover' },
  { label: 'Python', to: '/discover' },
  { label: 'GraphQL', to: '/discover' },
  { label: '计算机网络', to: '/discover' },
  { label: '计算机基础', to: '/discover' },
]
</script>

<style scoped>
.landing {
  padding: 60px 0;
}

.hero-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 40px;
  align-items: center;
}

.hero-title {
  font-size: 3.5rem;
  margin: 12px 0;
  font-weight: 800;
}

.eyebrow {
  letter-spacing: 2px;
  text-transform: uppercase;
  font-weight: 600;
}

.hero-description {
  font-size: 1.2rem;
  margin-bottom: 32px;
}

.hero-metrics {
  margin-top: 48px;
}

.metric-value {
  font-weight: 700;
}

.topic-card {
  border-radius: 16px;
}

/* 暗色主题适配 */
:global([data-theme="dark"]) .landing {
  background: linear-gradient(180deg, hsla(210 20% 14% / 0.6), transparent 60%);
}

:global([data-theme="dark"]) .topic-card {
  background: var(--panel-strong);
  border: 1px solid var(--line);
  box-shadow: 0 18px 40px hsla(210 10% 6% / 0.35);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
  .hero-title {
    font-size: 2.5rem;
  }
}
</style>
