import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '../views/LandingView.vue'
import BlogHomeView from '../views/HomeView.vue'
import BlogView from '../views/BlogView.vue'
import LoginView from '../views/LoginView.vue'
import AdminDashboard from '../views/AdminDashboard.vue'
import AdminPosts from '../views/AdminPosts.vue'
import AdminEditor from '../views/AdminEditor.vue'
import DiscoverView from '../views/DiscoverView.vue'
import AdminSettings from '../views/AdminSettings.vue'
import AdminComments from '../views/AdminComments.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: LandingView },
    { path: '/blog', name: 'home', component: BlogHomeView },
    { path: '/discover', name: 'discover', component: DiscoverView },
    { path: '/post/:id', name: 'post', component: BlogView, props: true },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/admin', name: 'admin', component: AdminDashboard },
    { path: '/admin/posts', name: 'admin-posts', component: AdminPosts },
    { path: '/admin/comments', name: 'admin-comments', component: AdminComments },
    { path: '/admin/editor/:id?', name: 'editor', component: AdminEditor, props: true },
    { path: '/admin/settings', name: 'admin-settings', component: AdminSettings }
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
