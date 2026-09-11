<template>
  <div class="module-shell customer-detail">

    <!-- ============ 详情 Hero ============ -->
    <div class="panel-card">
      <div class="panel-card__body">
        <div class="detail-hero">
          <div class="detail-hero__avatar">{{ avatarText }}</div>

          <div class="detail-hero__main">
            <h3 class="detail-hero__name">
              {{ customer.clueDO?.fullName || '未命名客户' }}
              <span v-if="customer.appellationDO?.typeValue" class="hero-appellation">
                {{ customer.appellationDO.typeValue }}
              </span>
            </h3>

            <div class="detail-hero__meta">
              <span>
                <el-icon :size="12"><User /></el-icon>
                负责人：{{ customer.ownerDO?.name || '—' }}
              </span>
              <span>
                <el-icon :size="12"><ShoppingBag /></el-icon>
                活动：{{ customer.activityDO?.name || '—' }}
              </span>
              <span>
                <el-icon :size="12"><Iphone /></el-icon>
                {{ customer.clueDO?.phone || '—' }}
              </span>
              <span>
                <el-icon :size="12"><Money /></el-icon>
                交易：{{ tranTotal }} 笔 / {{ tranAmountTotal }}
              </span>
            </div>

            <div class="hero-tags">
              <span v-if="customer.intentionProductDO?.name" class="state-tag state-tag--green">
                <el-icon :size="11"><Goods /></el-icon>{{ customer.intentionProductDO.name }}
              </span>
              <span v-if="customer.intentionStateDO?.typeValue"
                    class="state-tag" :class="intentionTagByText(customer.intentionStateDO.typeValue)">
                意向：{{ customer.intentionStateDO.typeValue }}
              </span>
              <span v-if="customer.needLoanDO?.typeValue"
                    class="state-tag" :class="needLoanTagByText(customer.needLoanDO.typeValue)">
                贷款：{{ customer.needLoanDO.typeValue }}
              </span>
              <span v-if="customer.sourceDO?.typeValue" class="state-tag state-tag--neutral">
                来源：{{ customer.sourceDO.typeValue }}
              </span>
              <span v-if="customer.nextContactTime"
                    class="state-tag" :class="isOverdue(customer.nextContactTime) ? 'state-tag--red' : 'state-tag--gold'">
                <el-icon :size="11"><Clock /></el-icon>
                {{ isOverdue(customer.nextContactTime) ? '已逾期：' : '待联系：' }}
                {{ customer.nextContactTime }}
              </span>
            </div>
          </div>

          <div class="detail-hero__actions">
            <el-button type="primary" :icon="Plus" @click="addTran" v-hasPermission="'tran:add'">
              为该客户创建交易
            </el-button>
            <el-button type="danger" :icon="Delete" @click="del" v-hasPermission="'customer:delete'">
              删除客户
            </el-button>
            <el-button :icon="Back" @click="goBack">返回列表</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 客户档案：按认知分组，降低阅读负荷 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Document /></el-icon>
          <span>客户档案</span>
        </div>
        <span class="panel-card__hint">基础资料、联系方式与商业画像</span>
      </div>

      <div class="panel-card__body">
        <div class="info-group">
          <div class="info-group__label"><em></em>基础资料</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">姓名</span>
              <span class="detail-item__value">{{ customer.clueDO?.fullName || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">称呼</span>
              <span class="detail-item__value">{{ customer.appellationDO?.typeValue || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">年龄</span>
              <span class="detail-item__value">{{ customer.clueDO?.age || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">职业</span>
              <span class="detail-item__value">{{ customer.clueDO?.job || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">年收入</span>
              <span class="detail-item__value">{{ formatIncome(customer.clueDO?.yearIncome) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">住址</span>
              <span class="detail-item__value">{{ customer.clueDO?.address || '—' }}</span>
            </div>
          </div>
        </div>

        <div class="info-group">
          <div class="info-group__label"><em></em>联系方式</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">手机</span>
              <span class="detail-item__value">{{ customer.clueDO?.phone || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">微信</span>
              <span class="detail-item__value">{{ customer.clueDO?.weixin || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">QQ</span>
              <span class="detail-item__value">{{ customer.clueDO?.qq || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">邮箱</span>
              <span class="detail-item__value">{{ customer.clueDO?.email || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">下次联系</span>
              <span class="detail-item__value"
                    :class="{ 'overdue-text': isOverdue(customer.nextContactTime) }">
                {{ customer.nextContactTime || '未设置' }}
              </span>
            </div>
          </div>
        </div>

        <div class="info-group">
          <div class="info-group__label"><em></em>商业画像</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">负责人</span>
              <span class="detail-item__value">{{ customer.ownerDO?.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">所属活动</span>
              <span class="detail-item__value">{{ customer.activityDO?.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">选购产品</span>
              <span class="detail-item__value">{{ customer.intentionProductDO?.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">线索来源</span>
              <span class="detail-item__value">{{ customer.sourceDO?.typeValue || '—' }}</span>
            </div>
            <div class="detail-item detail-item--full">
              <span class="detail-item__label">客户描述</span>
              <span class="detail-item__value">{{ customer.description || '—' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- ============ 交易记录：独立卡片 ============ -->
    <div class="panel-card forest-table">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Wallet /></el-icon>
          <span>交易记录</span>
          <el-tag v-if="tranTotal > 0" size="small" type="success" effect="plain" round>
            {{ tranTotal }} 笔 · {{ tranAmountTotal }}
          </el-tag>
        </div>
        <el-button type="primary" :icon="Plus" @click="addTran" v-hasPermission="'tran:add'">
          新增一笔交易
        </el-button>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table v-loading="tranLoading" :data="tranList" style="width: 100%;">
          <el-table-column type="index" label="#" width="54"/>
          <el-table-column label="交易流水号" min-width="150">
            <template #default="scope">
              <a href="javascript:" class="forest-link" @click="viewTran(scope.row.id)">
                {{ scope.row.tranNo || '—' }}
              </a>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="128" align="right">
            <template #default="scope">
              <span v-if="scope.row.money != null" class="money-cell">{{ formatMoney(scope.row.money) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="所处阶段" width="140">
            <template #default="scope">
              <span v-if="scope.row.stageDO?.typeValue" class="state-tag state-tag--blue">
                {{ scope.row.stageDO.typeValue }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="预计成交日期" width="120">
            <template #default="scope">{{ scope.row.expectedDate ? String(scope.row.expectedDate).slice(0,10) : '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="scope">
              <el-button type="primary" link size="small" @click="viewTran(scope.row.id)" v-hasPermission="'tran:view'">详情</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <el-icon :size="40"><Wallet /></el-icon>
              <p>该客户暂无交易记录</p>
              <el-button size="small" type="primary" @click="addTran" v-hasPermission="'tran:add'">创建第一笔交易</el-button>
            </div>
          </template>
        </el-table>
      </div>

      <div class="pager-bar">
        <el-pagination
            background
            layout="total, prev, pager, next"
            :page-size="tranPageSize"
            :total="tranTotal"
            @prev-click="toTranPage"
            @next-click="toTranPage"
            @current-change="toTranPage"/>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { doGet, doDelete } from "../http/httpRequest.js"
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, ShoppingBag, Iphone, Money, Clock, Document, Goods,
  Wallet, Plus, Delete, Back
} from '@element-plus/icons-vue'

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

// 交易表格加载态
const tranLoading = ref(false)

// ============ 详情页派生展示逻辑 ============

/** Hero 头像占位：取姓名首字（空值时用“客”兜底，避免圆形头像完全空白） */
const avatarText = computed(() => {
  const name = customer.value.clueDO?.fullName
  return name ? String(name).charAt(0).toUpperCase() : '客'
})

/** 本页交易金额汇总（千分位），体现客户价值 */
const tranAmountTotal = computed(() => {
  const sum = tranList.value.reduce((acc, t) => acc + Number(t.money || 0), 0)
  return formatMoney(sum)
})

/**
 * 意向状态标签配色：基于后端直接下发的 typeValue 文本推导。
 * 不依赖字典 ID——详情页未额外加载意向字典列表，用文本判定即可且无需多发请求。
 */
const intentionTagByText = (text) => {
  const name = text || ''
  if (name.includes('高')) return 'state-tag--green'
  if (name.includes('中')) return 'state-tag--gold'
  if (name.includes('低')) return 'state-tag--neutral'
  return 'state-tag--blue'
}

/** 贷款需求标签配色：“需要”类高亮为金色（商机信号） */
const needLoanTagByText = (text) => {
  const name = text || ''
  if (name.includes('否') || name.includes('不')) return 'state-tag--neutral'
  return 'state-tag--gold'
}

/** 是否已逾期：下次联系时间早于当前时刻 */
const isOverdue = (val) => {
  if (!val) return false
  const t = new Date(String(val).replace(' ', 'T'))
  return !isNaN(t.getTime()) && t.getTime() < Date.now()
}

/** 年收入格式化：千分位（空值给占位符而非空白） */
const formatIncome = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (isNaN(n)) return String(val)
  return '¥' + n.toLocaleString('zh-CN')
}

/** 交易金额格式化：千分位 + 2 位小数，与看板/交易列表口径一致 */
const formatMoney = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (isNaN(n)) return String(val)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

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
  tranLoading.value = true
  doGet(`/api/customer/${id}/trans`, { current }).then(resp => {
    if (resp.data.code === 200) {
      tranList.value = resp.data.data.list
      tranPageSize.value = resp.data.data.pageSize
      tranTotal.value = resp.data.data.total
    }
  }).finally(() => {
    tranLoading.value = false
  })
}

const toTranPage = (current) => { loadTrans(current) }

/**
 * 为该客户创建交易。
 *
 * 【修复的功能缺陷】旧版直接跳通用新增页并丢弃当前客户身份，
 * 用户必须在下拉里把刚才的客户再找一遍。现在携带 customerId，
 * 由 TranRecordView.applyCustomerPrefill() 预填下拉，实现“从这个客户出发”的自然动线。
 */
const addTran = () => {
  router.push({ path: '/dashboard/tran/add', query: { customerId: route.params.id } })
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
@import "@/assets/module-theme.css";

.customer-detail { min-height: 100%; box-sizing: border-box; }

/* Hero 内的称呼小字 */
.hero-appellation {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 500;
  color: #6d8272;
  background: #eef2f0;
  border: 1px solid #dfe7e2;
  font-family: -apple-system, 'PingFang SC', sans-serif;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 11px;
}

/* 信息分组 */
.info-group { margin-bottom: 22px; }
.info-group:last-child { margin-bottom: 0; }

.info-group__label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #4b7551;
  letter-spacing: 0.6px;
}

.info-group__label em {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #5f936b, #a8cdb1);
}

/* 金额单元格：等宽字体 + 右对齐 */
.money-cell {
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-weight: 600;
  color: #98704a;
}

.overdue-text { color: #c2564e; font-weight: 600; }

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 0;
  color: #93a89b;
}
.empty-state p { margin: 0; font-size: 13px; }
</style>
