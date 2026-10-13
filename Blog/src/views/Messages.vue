<script setup lang="ts">
import MessageReport from '@vicons/tabler/es/MessageReport'
import PageComments from '@/components/PageComments.vue'
import { PAGE_IDS } from '@/config'
import { usePageReadyOnMount } from '@/utils/pageReady'

usePageReadyOnMount()
</script>

<template>
  <div>
    <h1 class="page-title">留言板</h1>
    <section class="tips">
      <h2>
        <MessageReport class="tabler-icon" />
        留言建议
      </h2>
      <ul>
        <li>对本站有任何<mark>疑问</mark>、出现<mark>bug</mark>、做的<mark>不好</mark>、有什么<mark>改进</mark>的建议，都可以在下方留言~~~</li>
        <li>留言建议留下你的<mark>昵称</mark>与<mark>邮箱</mark>，方便及时收到博主回复</li>
        <li>请不要<mark>恶意评论</mark>，不要评论违反<mark>中国法律</mark>的内容</li>
        <li><mark>广告</mark>勿扰</li>
      </ul>
    </section>
    <PageComments :page-id="PAGE_IDS.messages" />
  </div>
</template>

<style scoped lang="scss">
.tips {
  margin: 0 0 1.75rem;
}

h2 {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin: 1.1rem 0 0.65rem;
  font-size: calc(1.35rem + 2px);
  color: var(--btn-content);
}

ul {
  margin: 0;
  padding-left: 1.6rem;
  line-height: 1.85;
  color: var(--c-text-1);
}

li {
  list-style: disc;
}

mark {
  background: transparent;
  color: var(--primary);
  font-weight: 700;
}
</style>
