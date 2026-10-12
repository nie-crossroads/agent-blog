<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'
import CrudPage from '@/components/CrudPage.vue'
import { PAGE_SIZE } from '@/utils/page'
import { tableTime } from '@/utils/format'

const router = useRouter()
const keyword = ref('')
const page = ref(1)
const categoryId = ref<number | undefined>()
const status = ref<number | undefined>()
const recommend = ref<number | undefined>()
const categories = ref<any[]>([])
const data = ref({ total: 0, list: [] as any[] })

async function load() {
  data.value = await adminApi.articles({
    page: page.value,
    size: PAGE_SIZE,
    keyword: keyword.value,
    categoryId: categoryId.value,
    status: status.value,
    recommend: recommend.value,
  })
}

function resetFilters() {
  categoryId.value = undefined
  status.value = undefined
  recommend.value = undefined
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该文章？', '提示', { type: 'warning' })
  await adminApi.deleteArticle(id)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  categories.value = await adminApi.categories()
  await load()
})
</script>

<template>
  <CrudPage
    v-model:keyword="keyword"
    v-model:page="page"
    :rows="data.list"
    :total="data.total"
    search-label="标题"
    search-placeholder="请输入标题"
    @search="load"
    @reset="resetFilters"
    @page-change="load"
    @create="router.push('/articles/edit')"
  >
    <template #filters>
      <span class="filter-label">分类</span>
      <el-select v-model="categoryId" clearable placeholder="全部" class="filter-select">
        <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <span class="filter-label">状态</span>
      <el-select v-model="status" clearable placeholder="全部" class="filter-select">
        <el-option label="发布" :value="1" />
        <el-option label="草稿" :value="0" />
      </el-select>
      <span class="filter-label">是否推荐</span>
      <el-select v-model="recommend" clearable placeholder="全部" class="filter-select">
        <el-option label="是" :value="1" />
        <el-option label="否" :value="0" />
      </el-select>
    </template>
    <el-table-column prop="title" label="标题" />
    <el-table-column prop="categoryName" label="分类" />
    <el-table-column label="状态">
      <template #default="{ row }">{{ row.status === 1 ? '发布' : '草稿' }}</template>
    </el-table-column>
    <el-table-column label="推荐">
      <template #default="{ row }">{{ row.recommend === 1 ? '是' : '否' }}</template>
    </el-table-column>
    <el-table-column prop="pv" label="阅读" />
    <el-table-column prop="createTime" label="创建时间" :formatter="tableTime" />
    <el-table-column label="操作">
      <template #default="{ row }">
        <el-button link type="primary" @click="router.push(`/articles/edit/${row.id}`)">编辑</el-button>
        <el-button link type="primary" @click="router.push(`/articles/edit/${row.id}?simple=1`)">简易编辑</el-button>
        <el-button link type="danger" @click="remove(row.id)">删除</el-button>
      </template>
    </el-table-column>
  </CrudPage>
</template>

<style scoped>
.filter-label {
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.filter-select {
  width: 220px;
}
</style>
