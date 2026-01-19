import { createApp, h, provide } from 'vue'
import { DefaultApolloClient } from '@vue/apollo-composable'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { apolloClient } from './apollo'
import './assets/main.css'
import { useUiStore } from './stores/ui'

const app = createApp({
  setup() {
    provide(DefaultApolloClient, apolloClient)
  },
  render: () => h(App)
})

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)

const ui = useUiStore(pinia)
ui.applyTheme()

app.mount('#app')
