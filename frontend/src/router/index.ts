import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: () => import('../views/LandingView.vue') },
    { path: '/blog', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/discover', name: 'discover', component: () => import('../views/DiscoverView.vue') },
    { path: '/docs/:id?', name: 'docs', component: () => import('../views/DocsView.vue'), props: true },
    { path: '/post/:id', name: 'post', component: () => import('../views/BlogView.vue'), props: true },
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/admin', name: 'admin', component: () => import('../views/AdminDashboard.vue') },
    { path: '/admin/posts', name: 'admin-posts', component: () => import('../views/AdminPosts.vue') },
    { path: '/admin/comments', name: 'admin-comments', component: () => import('../views/AdminComments.vue') },
    { path: '/admin/docs', name: 'admin-docs', component: () => import('../views/AdminDocs.vue') },
    { path: '/admin/editor/:id?', name: 'editor', component: () => import('../views/AdminEditor.vue'), props: true },
    { path: '/admin/settings', name: 'admin-settings', component: () => import('../views/AdminSettings.vue') }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('simpleblog_token')
  if (to.path.startsWith('/admin') && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return '/admin'
  }
})

export default router

