<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MessageCircle from '@vicons/tabler/es/MessageCircle'
import { frontApi } from '@/api/front'
import type { Message } from '@/api/types'
import CommentForm from '@/components/CommentForm.vue'
import CommentList from '@/components/CommentList.vue'
import FramePager from '@/components/FramePager.vue'
import { clearCommentForm, copyCommentIdentity, emptyCommentForm, validateCommentInput } from '@/utils/comment'
import { useLazyVisible } from '@/utils/lazyVisible'

const PAGE_SIZE = 10
const props = defineProps<{ pageId: number }>()

const { target, visible } = useLazyVisible()
const page = ref(1)
const rootTotal = ref(0)
const list = ref<Message[]>([])
const loaded = ref(false)
const loading = ref(false)
const paging = ref<'prev' | 'next' | null>(null)
const replyTo = ref<number | null>(null)
const form = reactive(emptyCommentForm())
const replyForm = reactive(emptyCommentForm())

function startReply(id: number) {
  if (replyTo.value == null) {
    copyCommentIdentity(form, replyForm)
    replyForm.content = ''
  }
  replyTo.value = id
}

function countVisible(items?: Message[]): number {
  return (items || []).reduce((sum, item) => {
    if (item.visible === 0 || item.handle === 0) return sum
    return sum + 1 + countVisible(item.children)
  }, 0)
}

const total = computed(() => countVisible(list.value))
const hasPrev = computed(() => page.value > 1)
const hasNext = computed(() => page.value * PAGE_SIZE < rootTotal.value)

async function fetchPage() {
  const data = await frontApi.messages(props.pageId, { page: page.value, size: PAGE_SIZE })
  list.value = data.list
  rootTotal.value = data.total
  loaded.value = true
}

async function load() {
  if (loading.value) return
  loading.value = true
  try {
    await fetchPage()
  } finally {
    loading.value = false
  }
}

async function go(nextPage: number, dir: 'prev' | 'next') {
  if (paging.value || loading.value) return
  paging.value = dir
  try {
    const data = await frontApi.messages(props.pageId, { page: nextPage, size: PAGE_SIZE })
    page.value = nextPage
    list.value = data.list
    rootTotal.value = data.total
  } finally {
    paging.value = null
  }
}

async function submit(parentId?: number | null) {
  const source = parentId != null ? replyForm : form
  const error = validateCommentInput(source)
  if (error) {
    ElMessage.warning(error)
    return
  }
  try {
    await ElMessageBox.confirm('确认发送这条留言？发送后需等待博主审核。', '提示', {
      type: 'info',
      customClass: 'tk-send-confirm',
      confirmButtonText: '确认发送',
      cancelButtonText: '再想想',
    })
  } catch {
    return
  }
  await frontApi.submitMessage({
    pageId: props.pageId,
    parentId: parentId ?? undefined,
    nickname: source.nickname,
    email: source.email,
    website: source.website,
    avatar: source.avatar,
    notice: source.notice ? 1 : 0,
    content: source.content,
  })
  ElMessage.success('留言成功，请等待博主审核！')
  if (parentId != null) {
    copyCommentIdentity(replyForm, form)
    clearCommentForm(replyForm)
    replyTo.value = null
  } else {
    clearCommentForm(form)
  }
  await load()
}

watch(
  visible,
  (value) => {
    if (value) load()
  },
  { immediate: true },
)

watch(
  () => props.pageId,
  () => {
    list.value = []
    loaded.value = false
    page.value = 1
    rootTotal.value = 0
    if (visible.value) load()
  },
)
</script>

<template>
  <div ref="target" class="page-comments">
    <template v-if="loaded">
      <CommentForm v-model="form" placeholder="在昵称输入框中输入QQ号自动获取昵称和头像" @submit="submit()" />
      <h3 class="tk-title">
        <MessageCircle class="tabler-icon" /> 评论 {{ total }}
      </h3>
      <CommentList
        :items="list"
        :reply-to="replyTo"
        :form="replyForm"
        @reply="startReply"
        @cancel="replyTo = null"
        @submit="submit"
      />
      <FramePager
        :has-prev="hasPrev"
        :has-next="hasNext"
        :paging="paging"
        @prev="go(page - 1, 'prev')"
        @next="go(page + 1, 'next')"
      />
    </template>
    <div v-else class="lazy-placeholder">
      {{ loading || visible ? '评论加载中…' : '向下滚动加载评论' }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-comments {
  margin-top: 1.5rem;
}
.tk-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin: 1.5rem 0 0.9rem;
  font-size: calc(1.05rem + 2px);
  color: var(--c-text-1);
}
.lazy-placeholder {
  min-height: 8rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-text-3);
  font-size: calc(0.9rem + 2px);
}
</style>
