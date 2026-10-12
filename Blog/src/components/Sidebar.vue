<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BrandGithub from '@vicons/tabler/es/BrandGithub'
import MusicIcon from '@vicons/tabler/es/Music'
import Flame from '@vicons/tabler/es/Flame'
import { frontApi } from '@/api/front'
import { siteConfig } from '@/config'
import { formatDate } from '@/utils/format'
import avatarImg from '@/assets/siteAvatar.jpg'
import MusicPlayer from '@/components/MusicPlayer.vue'

const articleTotal = ref(0)
const lastUpdate = ref('')
const recommendArticles = ref<any[]>([])

const runDays = computed(() => {
  const start = new Date(`${siteConfig.startDate}T00:00:00`).getTime()
  return Math.max(0, Math.floor((Date.now() - start) / 86400000))
})

onMounted(async () => {
  try {
    const page = await frontApi.articles({ page: 1, size: 1 })
    articleTotal.value = page.total
    lastUpdate.value = formatDate(page.list[0]?.createTime)
  } catch {
    /* keep empty widgets if the API is down */
  }
  try {
    recommendArticles.value = await frontApi.recommendArticles()
  } catch {
    /* keep empty widgets if the API is down */
  }
})
</script>

<template>
  <div class="stack">
    <section class="card-base block profile">
      <img class="avatar" :src="avatarImg" alt="" />
      <h2>{{ siteConfig.author }}</h2>
      <p class="bio">{{ siteConfig.bio }}</p>
      <a class="github" :href="siteConfig.github" target="_blank" rel="noreferrer">
        <BrandGithub class="tabler-icon" /> GitHub
      </a>
    </section>

    <section class="card-base block music">
      <h3><MusicIcon class="tabler-icon" /> 音乐</h3>
      <MusicPlayer />
    </section>

    <section class="card-base block recommend">
      <h3><Flame class="tabler-icon" /> 推荐文章</h3>
      <ul v-if="recommendArticles.length" class="recommend-list">
        <li v-for="item in recommendArticles" :key="item.id">
          <RouterLink :to="`/article/${item.id}`" class="recommend-link">{{ item.title }}</RouterLink>
        </li>
      </ul>
      <p v-else class="recommend-empty">暂无推荐</p>
    </section>

    <section class="card-base block">
      <h3>站点状态</h3>
      <div class="stat"><span>文章数目</span><b>{{ articleTotal }}</b></div>
      <div class="stat"><span>运行天数</span><b>{{ runDays }} 天</b></div>
      <div class="stat"><span>最后更新</span><b>{{ lastUpdate || '—' }}</b></div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.stack { display: flex; flex-direction: column; gap: 1rem; }
.block { padding: 0.9rem; }
.profile { text-align: center; }
.avatar {
  width: 5.5rem;
  height: 5.5rem;
  border-radius: 50%;
  object-fit: cover;
  margin: 0.25rem auto 0.75rem;
  display: block;
}
h2 { margin: 0; font-size: calc(1.2rem + 2px); color: var(--c-text-1); }
.bio { margin: 0.5rem 0 0.75rem; color: var(--c-text-2); font-size: calc(0.85rem + 2px); }
.github {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.8rem;
  border-radius: 999px;
  background: var(--btn-regular-bg);
  color: var(--btn-content);
  font-size: calc(0.85rem + 2px);
  transition: background 0.18s ease, color 0.18s ease;
}

.github:hover {
  background: var(--btn-content);
  color: #fff;
}
h3 {
  margin: 0 0 0.75rem;
  font-size: calc(0.9rem + 2px);
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--c-text-1);
}
.music { overflow: visible; }
.music h3 {
  font-size: calc(1.05rem + 2px);
  :deep(.tabler-icon) { width: 1.3rem; height: 1.3rem; }
}
.stat {
  display: flex;
  justify-content: space-between;
  padding: 0.4rem 0;
  font-size: calc(0.9rem + 2px);
  color: var(--c-text-2);
}
.recommend-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}
.recommend-list li {
  font-size: calc(0.85rem + 2px);
  line-height: 1.45;
}
.recommend-link {
  color: var(--c-text-2);
  text-decoration: none;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.18s ease;
}
.recommend-link:hover {
  color: var(--btn-content);
}
.recommend-empty {
  margin: 0;
  color: var(--c-text-3);
  font-size: calc(0.85rem + 2px);
}
</style>
