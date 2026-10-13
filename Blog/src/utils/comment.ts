import { BLOGGER_EMAIL } from '@/config'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const NICKNAME_MAX = 20
export const EMAIL_MAX = 30
export const CONTENT_MAX = 255

export type CommentFormModel = {
  nickname: string
  email: string
  website: string
  content: string
  avatar: string
  notice: boolean
}

export function emptyCommentForm(): CommentFormModel {
  return { nickname: '', email: '', website: '', content: '', avatar: '', notice: false }
}

export function copyCommentIdentity(from: CommentFormModel, to: CommentFormModel) {
  to.nickname = from.nickname
  to.email = from.email
  to.website = from.website
  to.avatar = from.avatar
  to.notice = from.notice
}

export function validateCommentInput(form: {
  nickname?: string
  email?: string
  content?: string
}): string | null {
  const nickname = (form.nickname || '').trim()
  const email = (form.email || '').trim()
  const content = (form.content || '').trim()
  if (!nickname) return '请填写昵称'
  if (nickname.length > NICKNAME_MAX) return '昵称不能超过20个字符'
  if (email) {
    if (email.length > EMAIL_MAX) return '邮箱不能超过30个字符'
    if (!EMAIL_RE.test(email)) return '请填写正确的邮箱格式'
    if (email.toLowerCase() === BLOGGER_EMAIL.toLowerCase()) return '不能输入博主的邮箱'
  }
  if (!content) return '请填写内容'
  if (content.length > CONTENT_MAX) return '消息内容不能超过255个字符'
  return null
}

export function clearCommentForm(form: CommentFormModel) {
  form.nickname = ''
  form.email = ''
  form.website = ''
  form.content = ''
  form.avatar = ''
  form.notice = false
}
