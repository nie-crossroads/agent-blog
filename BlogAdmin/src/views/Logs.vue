<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import CrudPage from '@/components/CrudPage.vue'
import { PAGE_SIZE } from '@/utils/page'
import { tableTime } from '@/utils/format'

const logStatus = ref('')
const page = ref(1)
const data = ref({ total: 0, list: [] as any[] })

async function load() {
  data.value = await adminApi.logs({ page: page.value, size: PAGE_SIZE, logStatus: logStatus.value || undefined })
}

function resetFilters() {
  logStatus.value = ''
}

function sourceText(_row: unknown, _column: unknown, value: number) {
  return value === 0 ? '前台' : '后台'
}

onMounted(load)
</script>

<template>
  <CrudPage
    v-model:page="page"
    :rows="data.list"
    :total="data.total"
    :show-keyword="false"
    :show-create="false"
    @search="load"
    @reset="resetFilters"
    @page-change="load"
  >
    <template #filters>
      <span class="filter-label">状态</span>
      <el-select v-model="logStatus" clearable placeholder="全部" class="filter-select">
        <el-option label="成功" value="成功" />
        <el-option label="失败" value="失败" />
      </el-select>
    </template>
    <el-table-column prop="event" label="事件" />
    <el-table-column prop="consumer" label="来源" :formatter="sourceText" />
    <el-table-column prop="status" label="状态" />
    <el-table-column prop="detail" label="详情" />
    <el-table-column prop="createTime" label="时间" :formatter="tableTime" />
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
