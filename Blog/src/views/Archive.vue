<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { frontApi } from '@/api/front'
import type { Article } from '@/api/types'
import { formatDate } from '@/utils/format'
import { usePageReady } from '@/utils/pageReady'

const list = ref<Article[]>([])
const loaded = ref(false)
const beginReady = usePageReady()
const groups = computed(() => {
  const map = new Map<string, Article[]>()
  for (const item of list.value) {
    const year = item.yearTime ?? String(item.createTime || '').slice(0, 4)
    const month = item.monthTime ?? String(item.createTime || '').slice(5, 7)
    const key = `${year}年${month}月`
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(item)
  }
  return [...map.entries()]
})

onMounted(async () => {
  const pageReady = beginReady()
  try {
    list.value = await frontApi.archive()
  } finally {
    loaded.value = true
    pageReady()
  }
})
</script>

<template>
  <div>
    <h1 class="page-title">归档</h1>
    <section v-for="[key, items] in groups" :key="key">
      <h2>{{ key }}</h2>
      <RouterLink v-for="item in items" :key="item.id" :to="`/article/${item.id}`" class="row">
        <time>{{ formatDate(item.createTime) }}</time>
        <strong>{{ item.title }}</strong>
      </RouterLink>
    </section>
    <el-empty v-if="loaded && !list.length" />
  </div>
</template>

<style scoped lang="scss">
h2 { font-size: calc(1rem + 2px); color: var(--btn-content); }
.row {
  display: flex;
  gap: 1rem;
  padding: 0.65rem 0;
  border-bottom: 1px dashed var(--border);
  time { color: var(--c-text-2); width: 7.5rem; flex-shrink: 0; }
  strong {
    min-width: 0;
    transition: transform 0.2s ease;
  }
}

.row:hover strong {
  transform: translateX(0.4rem);
}
</style>
