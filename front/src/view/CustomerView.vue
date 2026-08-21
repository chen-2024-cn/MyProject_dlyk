<template>
  <el-button type="primary" class="btn" @click="batchExportExcel" v-hasPermission="'customer:export'">全部导出(Excel)</el-button>
  <el-button type="success" class="btn" @click="chooseExportExcel" v-hasPermission="'customer:export'">选择导出(Excel)</el-button>

  <el-table
      :data="customerList"
      style="width: 100%"
      @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50"/>
    <el-table-column type="index" label="序号" width="65"/>
    <el-table-column property="ownerDO.name" label="负责人" width="120" />
    <el-table-column property="activityDO.name" label="所属活动"/>
    <el-table-column label="姓名">
      <template #default="scope">
        <a href="javascript:" @click="view(scope.row.id)">{{ scope.row.clueDO.fullName }}</a>
      </template>
    </el-table-column>
    <el-table-column property="appellationDO.typeValue" label="称呼"/>
    <el-table-column property="clueDO.phone" label="手机" width="120"/>
    <el-table-column property="clueDO.weixin" label="微信" width="120"/>
    <el-table-column property="needLoanDO.typeValue" label="是否贷款"/>
    <el-table-column property="intentionStateDO.typeValue" label="意向状态"/>
    <el-table-column property="stateDO.typeValue" label="线索状态"/>
    <el-table-column property="sourceDO.typeValue" label="线索来源"/>
    <el-table-column property="intentionProductDO.name" label="意向产品"/>
    <el-table-column property="nextContactTime" label="下次联系时间" width="165"/>
    <el-table-column label="操作" width="85">
      <template #default="scope">
        <el-button type="primary" @click="view(scope.row.id)" v-hasPermission="'customer:view'">详情</el-button>
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
import { ref, onMounted } from 'vue'
import { doGet } from "../http/httpRequest.js"
import axios from "axios"
import { getToken, messageFrame } from "../util/util.js"
import router from "@/router/router.js";

// 组件名称（用于调试）
defineOptions({ name: "CustomerView" })

// 响应式数据
const customerList = ref([])           // 客户列表
const pageSize = ref(0)                // 每页条数
const total = ref(0)                   // 总记录数
const customerIdArray = ref([])        // 已选中的客户ID数组

// 获取线索分页列表数据
const getData = async (current) => {
  const resp = await doGet("/api/customers", { current })
  if (resp.data.code === 200) {
    customerList.value = resp.data.data.list
    pageSize.value = resp.data.data.pageSize
    total.value = resp.data.data.total
  }
}

// 分页跳转
const page = (number) => {
  getData(number)
}

// 处理表格选中变化
const handleSelectionChange = (selectionDataArray) => {
  customerIdArray.value = selectionDataArray.map(data => data.id)
}

// 导出 Excel 核心逻辑
const exportExcel = (ids) => {
  const token = getToken()
  const iframe = document.createElement("iframe")
  let url = axios.defaults.baseURL + "/api/exportExcel?Authorization=" + token
  if (ids) {
    url += "&ids=" + ids
  }
  iframe.src = url
  iframe.style.display = "none"
  document.body.appendChild(iframe)

}

// 批量导出（全部数据）
const batchExportExcel = () => {
  exportExcel(null)
}

// 选择导出（仅导出勾选数据）
const chooseExportExcel = () => {
  if (customerIdArray.value.length <= 0) {
    messageFrame("请选择要导出的数据", "warning")
    return
  }
  const ids = customerIdArray.value.join(",")
  exportExcel(ids)
}

// 查看详情
const view = (id) => {
  router.push(`/dashboard/customer/${id}`)

}

// 组件挂载时加载第一页数据
onMounted(() => {
  getData(1)
})
</script>

<style scoped>
.el-table {
  margin-top: 10px;
}
</style>