import { createApp, h, provide } from 'vue'
import { DefaultApolloClient } from '@vue/apollo-composable'
import { createPinia } from 'pinia'
import { create, NButton, NConfigProvider, NDataTable, NForm, NFormItem, NInput, NInputNumber, NSelect, NSwitch, NTag, NTree } from 'naive-ui'
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
const naive = create({
  components: [NButton, NConfigProvider, NDataTable, NForm, NFormItem, NInput, NInputNumber, NSelect, NSwitch, NTag, NTree]
})

app.use(pinia)
app.use(router)
app.use(naive)

const ui = useUiStore(pinia)
ui.applyTheme()

app.mount('#app')
