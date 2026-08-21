<template>
  <el-form label-width="110px" style="max-width: 95%;">

    <el-form-item label="负责人">
      <div class="desc">{{customer.ownerDO?.name || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="所属活动">
      <div class="desc">{{customer.activityDO?.name || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="姓名">
      <div class="desc">{{customer.clueDO?.fullName || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="称呼">
      <div class="desc">{{customer.appellationDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="手机">
      <div class="desc">{{customer.clueDO?.phone || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="微信">
      <div class="desc">{{customer.clueDO?.weixin || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="QQ">
      <div class="desc">{{customer.clueDO?.qq || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="邮箱">
      <div class="desc">{{customer.clueDO?.email || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="年龄">
      <div class="desc">{{customer.clueDO?.age || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="职业">
      <div class="desc">{{customer.clueDO?.job || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="年收入">
      <div class="desc">{{customer.clueDO?.yearIncome || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="住址">
      <div class="desc">{{customer.clueDO?.address || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="贷款">
      <div class="desc">{{customer.needLoanDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="意向状态">
      <div class="desc">{{customer.intentionStateDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="意向产品">
      <div class="desc">{{customer.intentionProductDO?.name || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="线索状态">
      <div class="desc">{{customer.stateDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="线索来源">
      <div class="desc">{{customer.sourceDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="客户描述">
      <div class="desc">{{customer.description || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="下次联系时间">
      <div class="desc">{{customer.nextContactTime || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item>
      <el-divider />
    </el-form-item>

    <!-- 交易记录区域 -->
    <el-form-item label="交易记录">
      <div style="width: 100%;">
        <el-button type="primary" size="small" @click="addTran" style="margin-bottom: 10px;">为该客户创建交易</el-button>
        <el-table :data="tranList" style="width: 100%;">
          <el-table-column type="index" label="序号" width="60"/>
          <el-table-column label="交易流水号">
            <template #default="scope">
              <a href="javascript:" @click="viewTran(scope.row.id)">{{ scope.row.tranNo }}</a>
            </template>
          </el-table-column>
          <el-table-column property="money" label="金额" width="120"/>
          <el-table-column label="所处阶段" width="120">
            <template #default="scope">
              {{ scope.row.stageDO?.typeValue || '-' }}
            </template>
          </el-table-column>
          <el-table-column property="expectedDate" label="预计成交日期" width="120"/>
          <el-table-column label="操作" width="85">
            <template #default="scope">
              <el-button type="primary" size="small" @click="viewTran(scope.row.id)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
            background
            layout="prev, pager, next"
            :page-size="tranPageSize"
            :total="tranTotal"
            @prev-click="toTranPage"
            @next-click="toTranPage"
            @current-change="toTranPage"/>
      </div>
    </el-form-item>

    <el-form-item>
      <el-button type="danger" @click="del">删 除</el-button>
      <el-button type="success" plain @click="goBack">返 回</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { doGet, doDelete } from "../http/httpRequest.js"
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: "CustomerDetailView" })

const route = useRoute()
const router = useRouter()

const customer = ref({
  ownerDO: {},
  activityDO: {},
  clueDO: {},
  appellationDO: {},
  needLoanDO: {},
  intentionStateDO: {},
  intentionProductDO: {},
  stateDO: {},
  sourceDO: {}
})

const loadCustomerDetail = async () => {
  const id = route.params.id
  const resp = await doGet(`/api/customer/${id}`, {})
  if (resp.data.code === 200) {
    customer.value = resp.data.data
  }
}

const goBack = () => {
  window.history.back()
}

const del = () => {
  ElMessageBox.confirm('删除后不可恢复，确定删除该客户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const id = route.params.id
    const resp = await doDelete(`/api/customer/${id}`, {})
    if (resp.data.code === 200) {
      ElMessage.success('删除成功')
      router.push('/dashboard/customer')
    } else {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 交易列表相关
const tranList = ref([])
const tranPageSize = ref(0)
const tranTotal = ref(0)

const loadTrans = (current) => {
  const id = route.params.id
  doGet(`/api/customer/${id}/trans`, { current }).then(resp => {
    if (resp.data.code === 200) {
      tranList.value = resp.data.data.list
      tranPageSize.value = resp.data.data.pageSize
      tranTotal.value = resp.data.data.total
    }
  })
}

const toTranPage = (current) => { loadTrans(current) }

const addTran = () => {
  router.push('/dashboard/tran/add')
}

const viewTran = (id) => {
  router.push(`/dashboard/tran/${id}`)
}

onMounted(() => {
  loadCustomerDetail()
  loadTrans(1)
})
</script>

<style scoped>
.desc {
  background-color: #F0FFFF;
  width: 100%;
  padding-left: 15px;
}
</style>
