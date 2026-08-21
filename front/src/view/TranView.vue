<template>
  <el-button type="primary" class="btn" @click="addTran" v-hasPermission="'tran:add'">新建交易</el-button>
  <el-button type="danger" class="btn" @click="batchDelTran" v-hasPermission="'tran:delete'">批量删除</el-button>

  <div class="search-bar">
    <el-select
        v-model="searchCustomerId"
        placeholder="选择客户"
        clearable
        filterable
        style="width: 220px;">
      <el-option
          v-for="item in customerOptions"
          :key="item.id"
          :label="item.name"
          :value="item.id"/>
    </el-select>
    <el-input
        v-model="searchMoney"
        placeholder="交易金额"
        clearable
        style="width: 180px; margin-left: 10px;"/>
    <el-button type="primary" @click="doSearch" style="margin-left: 10px;">搜索</el-button>
    <el-button @click="resetSearch">重置</el-button>
  </div>

  <el-table
      :data="tranList"
      style="width: 100%; margin-top: 10px;"
      @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50"/>
    <el-table-column type="index" label="序号" width="65"/>
    <el-table-column property="tranNo" label="交易流水号"/>
    <el-table-column property="customerName" label="客户" width="100"/>
    <el-table-column property="money" label="交易金额" width="120"/>
    <el-table-column label="所处阶段" width="120">
      <template #default="scope">
        {{ scope.row.stageDO?.typeValue || '-' }}
      </template>
    </el-table-column>
    <el-table-column property="expectedDate" label="预计成交日期" width="120"/>
    <el-table-column property="nextContactTime" label="下次联系时间" width="165"/>
    <el-table-column label="操作" width="230">
      <template #default="scope">
        <el-button type="primary" @click="view(scope.row.id)" v-hasPermission="'tran:view'">详情</el-button>
        <el-button type="success" @click="edit(scope.row.id)" v-hasPermission="'tran:edit'">编辑</el-button>
        <el-button type="danger" @click="del(scope.row.id)" v-hasPermission="'tran:delete'">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
  <p>
    <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @prev-click="page"
        @next-click="page"
        @current-change="page"/>
  </p>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { doGet, doDelete } from '../http/httpRequest'
import { messageFrame } from "@/util/util.js"

const router = useRouter()
const reload = inject('reload')

const tranList = ref([])
const pageSize = ref(0)
const total = ref(0)
const tranIdArray = ref([])
const customerOptions = ref([])

const searchCustomerId = ref('')
const searchMoney = ref('')

const pageCache = ref(new Map())

const getData = (current) => {
  if (pageCache.value.has(current)) {
    tranList.value = pageCache.value.get(current)
    return
  }
  const params = { current }
  if (searchCustomerId.value) params.customerId = searchCustomerId.value
  if (searchMoney.value) params.money = searchMoney.value

  doGet('/api/trans', params).then(resp => {
    if (resp.data.code === 200) {
      const list = resp.data.data.list
      tranList.value = list
      pageSize.value = resp.data.data.pageSize
      total.value = resp.data.data.total
      pageCache.value.set(current, list)
    }
  })
}

const clearPageCache = () => { pageCache.value.clear() }

const loadCustomers = () => {
  doGet('/api/customer/options').then(resp => {
    if (resp.data.code === 200) {
      customerOptions.value = resp.data.data
    }
  })
}

const doSearch = () => {
  clearPageCache()
  getData(1)
}

const resetSearch = () => {
  searchCustomerId.value = ''
  searchMoney.value = ''
  clearPageCache()
  getData(1)
}

const page = (number) => { getData(number) }

const addTran = () => { router.push('/dashboard/tran/add') }

const edit = (id) => { router.push(`/dashboard/tran/edit/${id}`) }

const view = (id) => { router.push(`/dashboard/tran/${id}`) }

const del = (id) => {
  messageFrame('您确定要删除此交易吗？').then(() => {
    doDelete(`/api/tran/${id}`, {}).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('删除成功', 'success')
        clearPageCache()
        reload()
      } else {
        messageFrame('删除失败', 'error')
      }
    })
  }).catch(() => { messageFrame('取消删除', 'warning') })
}

const batchDelTran = () => {
  if (tranIdArray.value.length <= 0) {
    messageFrame('请选择要删除的数据', 'warning')
    return
  }
  messageFrame('您确定要删除这些交易吗？').then(() => {
    const ids = tranIdArray.value.join(',')
    doDelete('/api/tran/batch', { ids }).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('批量删除成功', 'success')
        clearPageCache()
        reload()
      } else {
        messageFrame('批量删除失败', 'error')
      }
    })
  }).catch(() => { messageFrame('取消批量删除', 'warning') })
}

const handleSelectionChange = (dataObjectArray) => {
  tranIdArray.value = dataObjectArray.map(item => item.id)
}

onMounted(() => {
  getData(1)
  loadCustomers()
})
</script>

<style scoped>
.btn { margin-bottom: 10px; }
.search-bar { display: flex; align-items: center; margin-bottom: 10px; }
</style>
