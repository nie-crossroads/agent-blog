import { createRouter, createWebHistory } from 'vue-router'
import FrontLayout from '@/layouts/FrontLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      return { el: to.hash, top: 88, behavior: 'smooth' }
    }
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      component: FrontLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/views/Home.vue') },
        { path: 'article/:id', name: 'article', component: () => import('@/views/ArticleDetail.vue') },
        { path: 'category', name: 'categories', component: () => import('@/views/Category.vue') },
        { path: 'category/:id', name: 'category', component: () => import('@/views/Category.vue') },
        { path: 'tag/:id', name: 'tag', component: () => import('@/views/Tag.vue') },
        { path: 'archive', name: 'archive', component: () => import('@/views/Archive.vue') },
        { path: 'essays', name: 'essays', component: () => import('@/views/Essays.vue') },
        { path: 'records', name: 'records', component: () => import('@/views/Records.vue') },
        { path: 'friends', name: 'friends', component: () => import('@/views/Friends.vue') },
        { path: 'messages', name: 'messages', component: () => import('@/views/Messages.vue') },
        { path: 'about', name: 'about', component: () => import('@/views/About.vue') },
      ],
    },
  ],
})

router.afterEach((to) => {
  if (window._hmt) {
    window._hmt.push(['_trackPageview', to.fullPath])
  }
})

export default router
