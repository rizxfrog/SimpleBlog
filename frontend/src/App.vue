<template>
  <div>
    <NavBar v-if="showNav" />
    <main :class="{ 'main-admin': !showNav }">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './components/NavBar.vue'

const route = useRoute()
const showNav = computed(() => !route.path.startsWith('/admin'))
</script>
