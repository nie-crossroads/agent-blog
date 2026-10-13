<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import MessageCircle from '@vicons/tabler/es/MessageCircle'
import type { Comment } from '@/api/types'
import { frontApi } from '@/api/front'
import CommentForm from '@/components/CommentForm.vue'
import CommentList from '@/components/CommentList.vue'
import { clearCommentForm, copyCommentIdentity, emptyCommentForm, validateCommentInput } from '@/utils/comment'
import { useLazyVisible } from '@/utils/lazyVisible'

const props = defineProps<{ articleId: number }>()

const { target, visible } = useLazyVisible()
const comments = ref<Comment[]>([])
const loaded = ref(false)
const loading = ref(false)
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

function countVisible(items?: Comment[]): number {
  return (items || []).reduce((sum, item) => {
    if (item.visible === 0 || item.handle === 0) return sum
    return sum + 1 + countVisible(item.children)
  }, 0)
}

const total = computed(() => countVisible(comments.value))

async function loadComments() {
  if (loading.value) return
  loading.value = true
  try {
    comments.value = await frontApi.comments(props.articleId)
    loaded.value = true
  } finally {
    loading.value = false
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
    await ElMessageBox.confirm('确认发送这条评论？发送后需等待博主审核。', '提示', {
      type: 'info',
      customClass: 'tk-send-confirm',
      confirmButtonText: '确认发送',
      cancelButtonText: '再想想',
    })
  } catch {
    return
  }
  await frontApi.submitComment({
    articleId: props.articleId,
    parentId: parentId ?? undefined,
    nickname: source.nickname,
    email: source.email,
    website: source.website,
    avatar: source.avatar,
    notice: source.notice ? 1 : 0,
    content: source.content,
  })
  ElMessage.success('评论已提交')
  if (parentId != null) {
    copyCommentIdentity(replyForm, form)
    clearCommentForm(replyForm)
    replyTo.value = null
  } else {
    clearCommentForm(form)
  }
  await loadComments()
}

watch(
  visible,
  (value) => {
    if (value) loadComments()
  },
  { immediate: true },
)

watch(
  () => props.articleId,
  () => {
    comments.value = []
    loaded.value = false
    if (visible.value) loadComments()
  },
)
</script>

<template>
  <div ref="target" class="comments">
    <template v-if="loaded">
      <CommentForm v-model="form" @submit="submit()" />
      <h3 class="tk-title">
        <MessageCircle class="tabler-icon" /> 评论 {{ total }}
      </h3>
      <CommentList
        :items="comments"
        :reply-to="replyTo"
        :form="replyForm"
        @reply="startReply"
        @cancel="replyTo = null"
        @submit="submit"
      />
    </template>
    <div v-else class="lazy-placeholder">
      {{ loading || visible ? '评论加载中…' : '向下滚动加载评论' }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.comments { margin-top: 2rem; padding-top: 0.5rem; }
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
