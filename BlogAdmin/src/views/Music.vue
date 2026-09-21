<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import CrudPage from '@/components/CrudPage.vue'
import { PAGE_SIZE } from '@/utils/page'
import { tableTime } from '@/utils/format'

const all = ref<any[]>([])
const keyword = ref('')
const author = ref('')
const appliedName = ref('')
const appliedAuthor = ref('')
const page = ref(1)
const visible = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  author: '',
  url: '',
  cover: '',
  lrc: '',
  status: 1,
})
const view = computed(() => {
  const nameKw = appliedName.value.trim().toLowerCase()
  const authorKw = appliedAuthor.value.trim().toLowerCase()
  const filtered = all.value.filter((item) => {
    if (nameKw && !String(item.name ?? '').toLowerCase().includes(nameKw)) return false
    if (authorKw && !String(item.author ?? '').toLowerCase().includes(authorKw)) return false
    return true
  })
  const start = (Math.max(page.value, 1) - 1) * PAGE_SIZE
  return {
    total: filtered.length,
    rows: filtered.slice(start, start + PAGE_SIZE),
  }
})

async function load() {
  all.value = await adminApi.music()
}

function applySearch() {
  appliedName.value = keyword.value
  appliedAuthor.value = author.value
}

function resetFilters() {
  author.value = ''
}

function open(row?: any) {
  form.id = row?.id
  form.name = row?.name || ''
  form.author = row?.author || ''
  form.url = row?.url || ''
  form.cover = row?.cover || ''
  form.lrc = row?.lrc || ''
  form.status = row?.status ?? 1
  visible.value = true
}

async function save() {
  const name = form.name.trim()
  const url = form.url.trim()
  if (!name) {
    ElMessage.warning('请填写歌名')
    return
  }
  if (!url) {
    ElMessage.warning('请填写播放地址')
    return
  }
  try {
    await adminApi.saveMusic({
      ...form,
      name,
      author: form.author.trim(),
      url,
      cover: form.cover.trim(),
      lrc: form.lrc.trim(),
      status: form.status,
    })
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch {
    // 错误已由请求拦截器提示
  }
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该音乐？', '提示', { type: 'warning' })
  try {
    await adminApi.deleteMusic(id)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 错误已由请求拦截器提示
  }
}

onMounted(load)
</script>

<template>
  <CrudPage
    v-model:keyword="keyword"
    v-model:page="page"
    :rows="view.rows"
    :total="view.total"
    search-label="歌名"
    search-placeholder="请输入歌名"
    @search="applySearch"
    @reset="resetFilters"
    @create="open()"
  >
    <template #filters>
      <span class="filter-label">作者</span>
      <el-input
        v-model="author"
        clearable
        placeholder="请输入作者"
        class="author-input"
        @keyup.enter="applySearch"
      />
    </template>
    <el-table-column prop="name" label="歌名" />
    <el-table-column prop="author" label="作者" />
    <el-table-column prop="url" label="地址" show-overflow-tooltip />
    <el-table-column label="状态">
      <template #default="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'">
          {{ row.status === 1 ? '前台显示' : '前台不显示' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="createTime" label="创建时间" :formatter="tableTime" />
    <el-table-column label="操作">
      <template #default="{ row }">
        <el-button link type="primary" @click="open(row)">编辑</el-button>
        <el-button link type="danger" @click="remove(row.id)">删除</el-button>
      </template>
    </el-table-column>
    <template #extra>
      <el-dialog v-model="visible" :title="form.id ? '编辑音乐' : '新建音乐'" width="520px" align-center>
        <el-form label-width="80px">
          <el-form-item label="歌名"><el-input v-model="form.name" maxlength="80" /></el-form-item>
          <el-form-item label="作者"><el-input v-model="form.author" maxlength="80" /></el-form-item>
          <el-form-item label="地址"><el-input v-model="form.url" placeholder="音频 URL" /></el-form-item>
          <el-form-item label="封面"><el-input v-model="form.cover" placeholder="封面 URL" /></el-form-item>
          <el-form-item label="歌词">
            <el-input v-model="form.lrc" type="textarea" :rows="6" placeholder="可选，LRC 格式" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch
              v-model="form.status"
              :active-value="1"
              :inactive-value="0"
              active-text="前台显示"
              inactive-text="前台不显示"
            />
          </el-form-item>
        </el-form>
        <template #footer><el-button type="primary" @click="save">保存</el-button></template>
      </el-dialog>
    </template>
  </CrudPage>
</template>

<style scoped>
.filter-label {
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.author-input {
  width: 220px;
}
</style>
