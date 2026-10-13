<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import type { Heading } from '@/utils/markdown'

const props = defineProps<{ headings: Heading[] }>()
const activeId = ref('')

function onScroll() {
  const items = props.headings
    .map((item) => document.getElementById(item.id))
    .filter((el): el is HTMLElement => !!el)
  if (!items.length) {
    activeId.value = ''
    return
  }
  const threshold = 120
  let current = items[0].id
  for (const el of items) {
    if (el.getBoundingClientRect().top <= threshold) current = el.id
  }
  activeId.value = current
}

function jump(event: MouseEvent, id: string) {
  event.preventDefault()
  const el = document.getElementById(id)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  activeId.value = id
}

watch(() => props.headings, onScroll, { deep: true })

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <section class="card-base toc">
    <h3>目录</h3>
    <nav v-if="headings.length">
      <a
        v-for="item in headings"
        :key="item.id"
        :href="`#${item.id}`"
        class="link"
        :class="{ on: activeId === item.id, [`lv-${item.level}`]: true }"
        @click="jump($event, item.id)"
      >
        {{ item.text }}
      </a>
    </nav>
    <p v-else class="empty">暂无目录</p>
  </section>
</template>

<style scoped lang="scss">
.toc {
  padding: 0.9rem;
  max-height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
h3 {
  margin: 0 0 0.75rem;
  font-size: calc(0.9rem + 2px);
  color: var(--c-text-1);
  flex: none;
}
nav {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: var(--btn-content) var(--btn-regular-bg);
}
nav::-webkit-scrollbar { width: 4px; }
nav::-webkit-scrollbar-thumb {
  background: var(--btn-content);
  border-radius: 999px;
}
nav::-webkit-scrollbar-track { background: var(--btn-regular-bg); }
.link {
  display: block;
  padding: 0.4rem 0.5rem;
  border-radius: 0.5rem;
  color: var(--c-text-2);
  font-size: calc(0.85rem + 2px);
  line-height: 1.45;
}
.link:hover,
.link.on {
  background: var(--btn-plain-bg-hover);
  color: var(--btn-content);
}
.lv-3 { padding-left: 1.45rem; }
.lv-4 { padding-left: 2.25rem; }
.empty {
  margin: 0;
  color: var(--c-text-3);
  font-size: calc(0.85rem + 2px);
}
</style>
