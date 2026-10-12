<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import User from '@vicons/tabler/es/User'
import { BLOGGER_EMAIL } from '@/config'
import { frontApi } from '@/api/front'
import { CONTENT_MAX, EMAIL_MAX, NICKNAME_MAX, validateCommentInput } from '@/utils/comment'
import type { CommentFormModel } from '@/utils/comment'

const QQ_RE = /^[1-9]\d{4,10}$/
const QQ_QUERY_INTERVAL_MS = 15000
let lastQqQueryAt = 0

let form = defineModel<CommentFormModel>({ required: true })

withDefaults(defineProps<{
  placeholder?: string
  submitText?: string
  cancelable?: boolean
}>(), {
  placeholder: '说点什么...',
  submitText: '发送',
  cancelable: false,
})

const emit = defineEmits<{ submit: []; cancel: [] }>()
const qqLoading = ref(false)

async function onNicknameBlur() {
  const qq = (form.value.nickname || '').trim()
  if (!QQ_RE.test(qq) || qqLoading.value) return
  const now = Date.now()
  if (now - lastQqQueryAt < QQ_QUERY_INTERVAL_MS) {
    ElMessage.warning('QQ 信息查询过于频繁，请稍后再试')
    return
  }
  lastQqQueryAt = now
  qqLoading.value = true
  try {
    const info = await frontApi.qqInfo(qq)
    if (info.nickname) {
      form.value.nickname = info.nickname
    }
    if (info.avatar) {
      form.value.avatar = info.avatar
    }
    if (info.email && info.email.toLowerCase() !== BLOGGER_EMAIL.toLowerCase()) {
      form.value.email = info.email
    }
  } catch (error: unknown) {
    const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
    if (message) {
      ElMessage.warning(message)
    }
    form.value.avatar = `https://q1.qlogo.cn/g?b=qq&nk=${qq}&s=100`
    const email = `${qq}@qq.com`
    if (email.toLowerCase() !== BLOGGER_EMAIL.toLowerCase()) {
      form.value.email = email
    }
  } finally {
    qqLoading.value = false
  }
}

function onAvatarError() {
  form.value.avatar = ''
}

function onSubmit() {
  if (qqLoading.value) return
  form.value.nickname = (form.value.nickname || '').trim()
  form.value.email = (form.value.email || '').trim()
  form.value.website = (form.value.website || '').trim()
  form.value.content = (form.value.content || '').trim()
  const error = validateCommentInput(form.value)
  if (error) {
    ElMessage.warning(error)
    return
  }
  emit('submit')
}
</script>

<template>
  <form class="tk-box" :class="{ busy: qqLoading }" novalidate @submit.prevent="onSubmit">
    <textarea
      v-model="form.content"
      class="tk-textarea"
      :placeholder="placeholder"
      :maxlength="CONTENT_MAX"
      rows="5"
      required
      :readonly="qqLoading"
    />
    <div class="tk-meta">
      <div class="tk-form-avatar" aria-hidden="true">
        <img
          v-if="form.avatar"
          :src="form.avatar"
          alt=""
          referrerpolicy="no-referrer"
          @error="onAvatarError"
        />
        <User v-else class="tabler-icon" />
      </div>
      <label>
        <span>昵称</span>
        <el-tooltip content="输入QQ号自动获取昵称和头像" placement="bottom" trigger="focus">
          <input
            v-model="form.nickname"
            type="text"
            placeholder="必填"
            :maxlength="NICKNAME_MAX"
            required
            :readonly="qqLoading"
            @blur="onNicknameBlur"
          />
        </el-tooltip>
      </label>
      <label>
        <span>邮箱</span>
        <input v-model="form.email" type="email" placeholder="必填" :maxlength="EMAIL_MAX" required :readonly="qqLoading" />
      </label>
      <label>
        <span>网站</span>
        <input v-model="form.website" type="text" placeholder="选填" :readonly="qqLoading" />
      </label>
    </div>
    <div class="tk-actions">
      <button v-if="cancelable" class="tk-cancel" type="button" :disabled="qqLoading" @click="emit('cancel')">取消</button>
      <label class="tk-notice">
        <input v-model="form.notice" type="checkbox" :disabled="qqLoading" />
        有回复时邮件通知我
      </label>
      <button class="tk-send" type="submit" :disabled="qqLoading">{{ submitText }}</button>
    </div>
    <div v-if="qqLoading" class="tk-loading" aria-live="polite">
      <span class="tk-loading-ring" />
      <span>获取QQ信息中</span>
    </div>
  </form>
</template>

<style scoped lang="scss">
.tk-box {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.tk-box.busy {
  pointer-events: none;
}
.tk-loading {
  position: absolute;
  inset: 0;
  z-index: 6;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  border-radius: var(--radius-large);
  background: color-mix(in oklab, var(--card-bg) 84%, transparent);
  color: var(--c-text-1);
  font-size: calc(0.9rem + 2px);
}
.tk-loading-ring {
  width: 1.15rem;
  height: 1.15rem;
  border: 2px solid var(--line-divider);
  border-top-color: var(--btn-content);
  border-radius: 50%;
  animation: tk-spin 0.7s linear infinite;
}
@keyframes tk-spin {
  to { transform: rotate(360deg); }
}
.tk-textarea {
  width: 100%;
  min-height: 9.5rem;
  padding: 1rem 1.25rem;
  border: 1px solid var(--line-divider);
  border-radius: var(--radius-large);
  background: var(--card-bg);
  color: var(--c-text-1);
  resize: vertical;
  outline: none;
}
.tk-textarea:focus {
  border-color: var(--primary);
}
.tk-meta {
  display: grid;
  grid-template-columns: 2.5rem repeat(3, minmax(0, 1fr));
  gap: 0.6rem;
  align-items: stretch;
}
.tk-form-avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--btn-regular-bg);
  color: var(--btn-content);
}
.tk-form-avatar .tabler-icon {
  width: 1.35rem;
  height: 1.35rem;
}
.tk-form-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.tk-meta > label {
  display: flex;
  min-height: 2.5rem;
  min-width: 0;
  border: 1px solid var(--line-divider);
  border-radius: 0.7rem;
  overflow: hidden;
  background: var(--btn-regular-bg);
  transition: border-color 0.15s ease;
}
.tk-meta > label:focus-within {
  border-color: var(--primary);
}
.tk-meta span {
  flex: 0 0 3.2rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: calc(0.8rem + 2px);
  color: var(--c-text-2);
}
.tk-meta input {
  flex: 1;
  min-width: 0;
  border: none;
  background: var(--card-bg);
  color: var(--c-text-1);
  padding: 0 0.75rem;
  outline: none;
}
.tk-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.5rem;
}
.tk-notice {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  margin-right: 0.45rem;
  font-size: calc(0.85rem + 2px);
  color: var(--c-text-2);
  cursor: pointer;
  user-select: none;
}
.tk-notice input {
  width: 0.95rem;
  height: 0.95rem;
  accent-color: var(--primary);
  cursor: pointer;
}
.tk-send,
.tk-cancel {
  height: 2rem;
  padding: 0 0.9rem;
  cursor: pointer;
  font-size: calc(0.875rem + 2px);
  border-radius: 0.55rem;
}
.tk-send {
  border: none;
  background: var(--btn-regular-bg);
  color: var(--btn-content);
}
.tk-send:hover { background: var(--btn-plain-bg-hover); }
.tk-cancel {
  border: 1px solid var(--line-divider);
  background: transparent;
  color: var(--c-text-2);
}
.tk-cancel:hover {
  border-color: var(--btn-content);
  color: var(--btn-content);
}
@media (max-width: 700px) {
  .tk-meta {
    grid-template-columns: 2.5rem minmax(0, 1fr);
  }
  .tk-meta > label:not(:first-of-type),
  .tk-meta > :deep(.el-tooltip) ~ label {
    grid-column: 1 / -1;
  }
}
</style>
