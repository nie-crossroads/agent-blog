<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor, config } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import Plus from '@vicons/tabler/es/Plus'
import Trash from '@vicons/tabler/es/Trash'
import ZoomIn from '@vicons/tabler/es/ZoomIn'
import { adminApi } from '@/api/admin'
import { upload, deleteUpload } from '@/api/http'
import { mediaUrl } from '@/utils/format'
import { useThemeStore } from '@/stores/theme'

config({
  codeMirrorExtensions(extensions) {
    return extensions.filter((item) => item.type !== 'linkShortener')
  },
})

const route = useRoute()
const router = useRouter()
const theme = useThemeStore()
const editorTheme = computed(() => (theme.dark ? 'dark' : 'light'))
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const previewCover = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  title: '',
  description: '',
  categoryId: undefined as number | undefined,
  tagIds: [] as Array<number | string>,
  cover: '',
  thumbnail: '',
  comment: 1,
  recommend: 0,
  status: 1,
  content: '',
})

const coverSrc = computed(() => mediaUrl(form.thumbnail || form.cover))
const contentImages = ref<{ imgUrl: string; thumbnailUrl?: string }[]>([])

async function onUploadImg(files: File[], callback: (urls: string[], names?: string[]) => void) {
  const urls: string[] = []
  const names: string[] = []
  for (const file of files) {
    const res = await upload(file)
    urls.push(res.thumbnailUrl || res.url)
    names.push(file.name)
    contentImages.value.push({ imgUrl: res.url, thumbnailUrl: res.thumbnailUrl || res.url })
  }
  callback(urls, names)
}

async function uploadCover(file: File) {
  const res = await upload(file)
  form.cover = res.url
  form.thumbnail = res.thumbnailUrl || res.url
  return false
}

async function removeCover() {
  if (!form.cover && !form.thumbnail) return
  try {
    await deleteUpload(form.cover, form.thumbnail)
    form.cover = ''
    form.thumbnail = ''
  } catch {
    // 错误已由请求拦截器提示
  }
}

async function resolveTagIds() {
  const ids: number[] = []
  for (const item of form.tagIds) {
    if (typeof item === 'number') {
      ids.push(item)
      continue
    }
    const name = String(item).trim()
    if (!name) continue
    const existed = tags.value.find((tag) => tag.name === name)
    if (existed) {
      ids.push(existed.id)
      continue
    }
    const id = await adminApi.saveTag({ name })
    tags.value.push({ id, name })
    ids.push(id)
  }
  return ids
}

async function save() {
  if (!form.title) {
    ElMessage.warning('请填写标题')
    return
  }
  const tagIds = await resolveTagIds()
  form.tagIds = tagIds
  await adminApi.saveArticle({ ...form, tagIds, images: contentImages.value })
  ElMessage.success('已保存')
  router.push('/articles')
}

onMounted(async () => {
  categories.value = await adminApi.categories()
  tags.value = await adminApi.tags()
  const id = route.params.id
  if (id) {
    const article = await adminApi.article(String(id))
    Object.assign(form, {
      id: article.id,
      title: article.title,
      description: article.description,
      categoryId: article.categoryId,
      tagIds: article.tagIds || [],
      cover: article.cover,
      thumbnail: article.thumbnail,
      comment: article.comment ?? 1,
      recommend: article.recommend ?? 0,
      status: article.status ?? 1,
      content: article.content || '',
    })
    contentImages.value = (article.images || []).map((item: any) => ({
      imgUrl: item.imgUrl,
      thumbnailUrl: item.thumbnailUrl || item.imgUrl,
    }))
  }
})
</script>

<template>
  <el-card shadow="never">
    <template #header>{{ form.id ? '编辑文章' : '写文章' }}</template>
    <el-form label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.description" maxlength="80" show-word-limit placeholder="不填则取正文开头" />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" placeholder="选择分类" class="full">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="标签">
            <el-select
              v-model="form.tagIds"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入标签"
              class="full"
            >
              <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="选项">
        <div class="option-bar">
          <el-switch v-model="form.comment" :active-value="1" :inactive-value="0" active-text="允许评论" />
          <el-switch v-model="form.recommend" :active-value="1" :inactive-value="0" active-text="推荐" />
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="发布" />
        </div>
      </el-form-item>
      <el-form-item label="封面">
        <el-upload v-if="!form.cover" :show-file-list="false" accept="image/*" :before-upload="uploadCover">
          <div class="cover-box">
            <Plus class="cover-icon" />
            <span>上传封面</span>
          </div>
        </el-upload>
        <div v-else class="cover-box has-image">
          <img :src="coverSrc" alt="封面" />
          <div class="cover-mask">
            <button type="button" title="预览" @click="previewCover = true"><ZoomIn /></button>
            <button type="button" title="删除" @click="removeCover"><Trash /></button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="正文">
        <MdEditor
          :key="editorTheme"
          v-model="form.content"
          :theme="editorTheme"
          :onUploadImg="onUploadImg"
          :codeFoldable="false"
          style="height: 520px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="router.back()">返回</el-button>
      </el-form-item>
    </el-form>
    <el-image-viewer v-if="previewCover" :url-list="[coverSrc]" @close="previewCover = false" />
  </el-card>
</template>

<style scoped lang="scss">
.full {
  width: 100%;
}

.option-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 32px;
}

.cover-box {
  width: 148px;
  height: 148px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  overflow: hidden;
  position: relative;
}

.cover-box.has-image {
  border-style: solid;
  padding: 0;
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-icon {
  width: 28px;
  height: 28px;
}

.cover-mask {
  position: absolute;
  inset: 0;
  display: none;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: rgba(0, 0, 0, 0.45);
}

.cover-box.has-image:hover .cover-mask {
  display: flex;
}

.cover-mask button {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #fff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cover-mask :deep(svg) {
  width: 18px;
  height: 18px;
}

:deep(.md-editor-input-wrapper .cm-content),
:deep(.cm-line) {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
