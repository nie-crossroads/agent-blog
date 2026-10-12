import { del, get, post, put } from './http'

export const authApi = {
  login: (data: { username: string; password: string }) =>
    post<{ token: string; username: string; role: number }>('/api/auth/login', data),
  session: () => get<void>('/api/admin/session'),
  updateUsername: (data: { username: string; password: string }) =>
    put<{ token: string; username: string; role: number }>('/api/admin/account/username', data),
  updatePassword: (data: { oldPassword: string; newPassword: string }) =>
    put('/api/admin/account/password', data),
}

export const adminApi = {
  dashboard: () =>
    get<{
      articleCount: number
      friendCount: number
      messageCount: number
      commentCount: number
      blackCount: number
      errorLogCount: number
      hotArticles: any[]
      recentBlacks: any[]
    }>('/api/admin/dashboard'),
  articles: (params: object) => get<{ total: number; list: any[] }>('/api/admin/articles', params),
  article: (id: number | string) => get<any>(`/api/admin/articles/${id}`),
  articleSimple: (id: number | string) => get<any>(`/api/admin/articles/${id}/simple`),
  saveArticle: (data: any) =>
    data.id ? put<number>(`/api/admin/articles/${data.id}`, data) : post<number>('/api/admin/articles', data),
  deleteArticle: (id: number) => del(`/api/admin/articles/${id}`),
  categories: () => get<any[]>('/api/admin/categories'),
  saveCategory: (data: any) => post<number>('/api/admin/categories', data),
  deleteCategory: (id: number) => del(`/api/admin/categories/${id}`),
  tags: () => get<any[]>('/api/admin/tags'),
  saveTag: (data: any) => post<number>('/api/admin/tags', data),
  deleteTag: (id: number) => del(`/api/admin/tags/${id}`),
  comments: (params: object) => get<{ total: number; list: any[] }>('/api/admin/comments', params),
  replyComment: (data: object) => post('/api/admin/comments', data),
  handleComment: (id: number, handle: number) => put(`/api/admin/comments/${id}/handle`, { handle }),
  reviewComment: (id: number, approved: boolean) => put(`/api/admin/comments/${id}/review`, { approved }),
  visibleComment: (id: number, visible: number) => put(`/api/admin/comments/${id}/visible`, { visible }),
  deleteComment: (id: number) => del(`/api/admin/comments/${id}`),
  messages: (params: object) => get<{ total: number; list: any[] }>('/api/admin/messages', params),
  replyMessage: (data: object) => post('/api/admin/messages', data),
  handleMessage: (id: number, handle: number) => put(`/api/admin/messages/${id}/handle`, { handle }),
  reviewMessage: (id: number, approved: boolean) => put(`/api/admin/messages/${id}/review`, { approved }),
  visibleMessage: (id: number, visible: number) => put(`/api/admin/messages/${id}/visible`, { visible }),
  deleteMessage: (id: number) => del(`/api/admin/messages/${id}`),
  essays: (params: object) => get<{ total: number; list: any[] }>('/api/admin/essays', params),
  saveEssay: (data: object) => post('/api/admin/essays', data),
  deleteEssay: (id: number) => del(`/api/admin/essays/${id}`),
  records: (params: object) => get<{ total: number; list: any[] }>('/api/admin/records', params),
  saveRecord: (data: object) => post('/api/admin/records', data),
  deleteRecord: (id: number) => del(`/api/admin/records/${id}`),
  recordCategories: () => get<any[]>('/api/admin/record-categories'),
  saveRecordCategory: (data: object) => post('/api/admin/record-categories', data),
  deleteRecordCategory: (id: number) => del(`/api/admin/record-categories/${id}`),
  friends: () => get<any[]>('/api/admin/friends'),
  saveFriend: (data: object) => post('/api/admin/friends', data),
  deleteFriend: (id: number) => del(`/api/admin/friends/${id}`),
  friendCategories: () => get<any[]>('/api/admin/friend-categories'),
  saveFriendCategory: (data: object) => post('/api/admin/friend-categories', data),
  deleteFriendCategory: (id: number) => del(`/api/admin/friend-categories/${id}`),
  music: () => get<any[]>('/api/admin/music'),
  saveMusic: (data: object) => post('/api/admin/music', data),
  deleteMusic: (id: number) => del(`/api/admin/music/${id}`),
  blacks: () => get<any[]>('/api/admin/blacks'),
  saveBlack: (data: object) => post('/api/admin/blacks', data),
  deleteBlack: (id: number) => del(`/api/admin/blacks/${id}`),
  logs: (params: object) => get<{ total: number; list: any[] }>('/api/admin/logs', params),
  webUpdateLogs: () => get<any[]>('/api/admin/web-update-logs'),
  saveWebUpdateLog: (data: object) => post('/api/admin/web-update-logs', data),
  deleteWebUpdateLog: (id: number) => del(`/api/admin/web-update-logs/${id}`),
  emails: (params: object) => get<{ total: number; list: any[] }>('/api/admin/emails', params),
  emailFails: (params: object) => get<{ total: number; list: any[] }>('/api/admin/email-fails', params),
  resendEmailFail: (id: number) => post(`/api/admin/email-fails/${id}/resend`),
}
