<template>
  <button class="theme-toggle" @click="toggleTheme">
    <span>{{ label }}</span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUiStore } from '../stores/ui'

const ui = useUiStore()
const label = computed(() => (ui.theme === 'dark' ? '亮色模式' : '暗色模式'))

const toggleTheme = () => {
  ui.toggleTheme()
}
</script>
