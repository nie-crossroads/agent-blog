<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, provide, ref } from 'vue'
import { onBeforeRouteUpdate, useRoute } from 'vue-router'
import ArrowUp from '@vicons/tabler/es/ArrowUp'
import Navbar from '@/components/Navbar.vue'
import Sidebar from '@/components/Sidebar.vue'
import ArticleToc from '@/components/ArticleToc.vue'
import SiteRuntime from '@/components/SiteRuntime.vue'
import type { Heading } from '@/utils/markdown'
import { PAGE_READY_KEY } from '@/utils/pageReady'
import bannerImg from '@/assets/banner.jpg'

const route = useRoute()
const showTop = ref(false)
const hovering = ref(false)
const scrollPercent = ref(0)
const tocHeadings = ref<Heading[]>([])
const pageRef = ref<HTMLElement | null>(null)
const lockHeight = ref<number | null>(null)
const pageLoading = ref(false)
let unlockTimer = 0
let hideTimer = 0
let loadingSince = 0
const MIN_LOADING_MS = 240

provide('tocHeadings', tocHeadings)

const listPage = computed(() => ['home', 'category', 'categories', 'tag', 'essays'].includes(String(route.name)))
const isArticle = computed(() => route.name === 'article')

function onScroll() {
  showTop.value = window.scrollY > 400
  const doc = document.documentElement
  const max = doc.scrollHeight - doc.clientHeight
  scrollPercent.value = max > 0 ? Math.round((window.scrollY / max) * 100) : 0
}

function toTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function finishPageLoad() {
  pageLoading.value = false
  lockHeight.value = null
}

function pageReady() {
  window.clearTimeout(unlockTimer)
  window.clearTimeout(hideTimer)
  const wait = pageLoading.value
    ? Math.max(0, MIN_LOADING_MS - (Date.now() - loadingSince))
    : 0
  hideTimer = window.setTimeout(() => {
    nextTick(() => {
      requestAnimationFrame(finishPageLoad)
    })
  }, wait)
}

provide(PAGE_READY_KEY, pageReady)

onBeforeRouteUpdate((to, from) => {
  if (to.path === from.path) return
  const el = pageRef.value
  if (el) {
    const height = el.offsetHeight
    if (height > 0) lockHeight.value = height
  }
  tocHeadings.value = []
  pageLoading.value = true
  loadingSince = Date.now()
  window.clearTimeout(hideTimer)
  window.clearTimeout(unlockTimer)
  unlockTimer = window.setTimeout(pageReady, 3000)
})

function imageViewerOpen() {
  return Boolean(document.querySelector('.el-image-viewer__wrapper'))
}

function stopPageScroll(event: Event) {
  if (!imageViewerOpen()) return
  event.preventDefault()
}

function stopPageScrollKeys(event: KeyboardEvent) {
  if (!imageViewerOpen()) return
  if ([' ', 'PageUp', 'PageDown', 'Home', 'End', 'ArrowUp', 'ArrowDown'].includes(event.key)) {
    event.preventDefault()
  }
}

const viewerScrollLock: AddEventListenerOptions = { passive: false, capture: true }

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('wheel', stopPageScroll, viewerScrollLock)
  window.addEventListener('touchmove', stopPageScroll, viewerScrollLock)
  window.addEventListener('keydown', stopPageScrollKeys, viewerScrollLock)
  onScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('wheel', stopPageScroll, viewerScrollLock)
  window.removeEventListener('touchmove', stopPageScroll, viewerScrollLock)
  window.removeEventListener('keydown', stopPageScrollKeys, viewerScrollLock)
  window.clearTimeout(unlockTimer)
  window.clearTimeout(hideTimer)
})
</script>

<template>
  <div class="layout">
    <div class="bg-box" :style="{ backgroundImage: `url(${bannerImg})` }" />

    <div class="nav-wrap">
      <Navbar />
    </div>

    <div class="grid">
      <aside class="side">
        <Sidebar />
      </aside>
      <main class="main">
        <div
          ref="pageRef"
          class="page"
          :class="{ 'card-base panel': !listPage }"
          :style="lockHeight ? { minHeight: `${lockHeight}px` } : undefined"
        >
          <div class="page-body" :class="{ wait: pageLoading }">
            <RouterView />
          </div>
          <Transition name="page-loader">
            <div v-if="pageLoading" class="page-loader" aria-live="polite" aria-busy="true">
              <div class="page-loader-box">
                <span class="page-loader-ring" />
              </div>
            </div>
          </Transition>
        </div>
        <footer class="footer card-base">
          <SiteRuntime />
        </footer>
      </main>
    </div>

    <aside v-if="isArticle" class="toc-side">
      <ArticleToc :headings="tocHeadings" />
    </aside>

    <Transition name="fade">
      <button
        v-if="showTop"
        class="back-top"
        type="button"
        title="回到顶部"
        @click="toTop"
        @mouseenter="hovering = true"
        @mouseleave="hovering = false"
      >
        <span class="back-top-face" :class="{ up: hovering }">
          <Transition name="face">
            <span v-if="hovering" key="icon" class="back-top-icon">
              <ArrowUp />
            </span>
            <span v-else key="percent" class="progress-num">{{ scrollPercent }}</span>
          </Transition>
        </span>
      </button>
    </Transition>
  </div>
</template>

<style scoped lang="scss">
.layout {
  min-height: 100vh;
  position: relative;
}

.bg-box {
  position: fixed;
  inset: 0;
  z-index: 0;
  background-color: var(--page-bg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  pointer-events: none;
}

.nav-wrap,
.grid,
.back-top {
  position: relative;
  z-index: 1;
}

.nav-wrap {
  position: sticky;
  top: 0;
  z-index: 40;
  background: transparent;
}

.grid {
  width: min(var(--page-width), calc(100% - 2rem));
  margin: 0 auto;
  padding: 0.5rem 0 3rem;
  display: grid;
  grid-template-columns: 17.5rem minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.side {
  position: sticky;
  top: 5rem;
}

.toc-side {
  position: fixed;
  top: 5.5rem;
  left: calc(50% + min(var(--page-width), calc(100% - 2rem)) / 2 + 0.75rem);
  width: 17.5rem;
  max-height: calc(100vh - 6.5rem);
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  z-index: 6;
}

.main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.page { min-width: 0; position: relative; }

.page-body {
  transition: opacity 0.22s ease;
}

.page-body.wait {
  opacity: 0;
  pointer-events: none;
}

.page-loader {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.page-loader-box {
  position: sticky;
  top: 38vh;
  margin: 0 auto;
  width: 3.25rem;
  height: 3.25rem;
  border-radius: 1rem;
  background: var(--card-bg);
  box-shadow: var(--shadow);
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-loader-ring {
  width: 1.35rem;
  height: 1.35rem;
  border: 2px solid var(--line-divider);
  border-top-color: var(--btn-content);
  border-radius: 50%;
  animation: page-spin 0.7s linear infinite;
}

.page-loader-enter-active,
.page-loader-leave-active {
  transition: opacity 0.2s ease;
}

.page-loader-enter-from,
.page-loader-leave-to {
  opacity: 0;
}

@keyframes page-spin {
  to { transform: rotate(360deg); }
}

.panel { padding: 1.75rem 2rem; }

.footer {
  display: flex;
  justify-content: center;
  padding: 1.1rem 1.25rem;
}

.back-top {
  position: fixed;
  right: 1.5rem;
  bottom: 2rem;
  min-width: 2.6rem;
  height: 2.6rem;
  padding: 0 0.5rem;
  border: none;
  border-radius: 0.75rem;
  background: var(--float-panel-bg);
  color: var(--btn-content);
  box-shadow: var(--shadow);
  cursor: pointer;
  z-index: 40;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: background 0.28s ease, color 0.28s ease;
  :deep(svg) { width: 1.2rem; height: 1.2rem; }
}

.back-top:hover {
  background: var(--btn-content);
  color: #fff;
}

.back-top-face {
  display: grid;
  place-items: center;
  width: 1.8rem;
  height: 1.45rem;
}

.back-top-icon,
.progress-num {
  grid-area: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.progress-num {
  font-size: 1rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.face-enter-active,
.face-leave-active {
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.28s ease;
}

.face-enter-from,
.face-leave-to {
  opacity: 0;
}

.up .face-enter-from {
  transform: translateY(120%);
}

.up .face-leave-to {
  transform: translateY(-120%);
}

.back-top-face:not(.up) .face-enter-from {
  transform: translateY(-120%);
}

.back-top-face:not(.up) .face-leave-to {
  transform: translateY(120%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 114rem) {
  .toc-side { display: none; }
}

@media (max-width: 980px) {
  .grid { grid-template-columns: 1fr; }
  .side { position: static; }
  .panel { padding: 1.25rem 1rem; }
}
</style>
