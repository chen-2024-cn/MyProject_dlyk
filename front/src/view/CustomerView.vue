<template>
  <div class="module-shell customer-module">

    <!-- ============ 页面 Hero ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">JOURNEY · CUSTOMER BASE</span>
        <h2>客户管理</h2>
        <p>由线索转化而来的正式客户：查看完整画像、跟进交易进展，并支持批量导出</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ isFiltering ? '已启用条件筛选' : '客户库实时同步中' }}
      </div>
    </section>

    <!-- ============ 概览指标 ============ -->
    <div class="module-metrics">
      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><Avatar /></el-icon></div>
        <div>
          <div class="metric-tile__label">{{ isFiltering ? '匹配客户' : '客户总数' }}</div>
          <div class="metric-tile__value">{{ total }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><Goods /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页意向产品数</div>
          <div class="metric-tile__value">{{ productCount }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><AlarmClock /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页待联系</div>
          <div class="metric-tile__value">{{ followUpCount }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
        <div class="metric-tile__icon"><el-icon :size="18"><Select /></el-icon></div>
        <div>
          <div class="metric-tile__label">已选中（待导出）</div>
          <div class="metric-tile__value">{{ customerIdArray.length }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 筛选面板 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Filter /></el-icon>
          <span>客户筛选</span>
        </div>
        <span class="panel-card__hint">姓名 / 手机号模糊搜索，条件可任意组合</span>
      </div>

      <div class="panel-card__body">
        <div class="filter-grid">
          <div class="filter-item">
            <label class="filter-item__label">客户姓名</label>
            <el-input v-model="filters.fullName" placeholder="支持模糊匹配" clearable
                      :prefix-icon="Search" @keyup.enter="doSearch" />
          </div>

          <div class="filter-item">
            <label class="filter-item__label">手机号码</label>
            <el-input v-model="filters.phone" placeholder="支持模糊匹配" clearable
                      :prefix-icon="Iphone" @keyup.enter="doSearch" />
          </div>

          <div class="filter-item">
            <label class="filter-item__label">负责人</label>
            <el-select v-model="filters.ownerId" placeholder="全部负责人" clearable filterable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="o in ownerOptions" :key="o.id" :label="o.name" :value="o.id" />
            </el-select>
          </div>

          <div class="filter-item">
            <label class="filter-item__label">选购产品</label>
            <el-select v-model="filters.product" placeholder="全部产品" clearable filterable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
          </div>

          <div class="filter-item">
            <label class="filter-item__label">线索来源</label>
            <el-select v-model="filters.source" placeholder="全部来源" clearable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="d in sourceOptions" :key="d.id" :label="d.typeValue" :value="d.id" />
            </el-select>
          </div>

          <div class="filter-item">
            <label class="filter-item__label">意向状态</label>
            <el-select v-model="filters.intentionState" placeholder="全部意向" clearable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="d in intentionOptions" :key="d.id" :label="d.typeValue" :value="d.id" />
            </el-select>
          </div>
        </div>

        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          <span v-if="isFiltering" class="toolbar__selected">
            已生效 {{ activeFilterCount }} 项条件
          </span>
        </div>
      </div>
    </div>

    <!-- ============ 数据表格卡片 ============ -->
    <div class="panel-card forest-table">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><User /></el-icon>
          <span>客户清单</span>
        </div>

        <div class="toolbar">
          <el-button type="primary" :icon="Download" @click="batchExportExcel" v-hasPermission="'customer:export'">
            导出全部
          </el-button>
          <el-button type="success" :icon="Download" @click="chooseExportExcel" v-hasPermission="'customer:export'">
            导出选中
          </el-button>
        </div>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table
            v-loading="loading"
            :data="customerList"
            style="width: 100%"
            @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" label="#" width="54" :index="rowIndex" />
          <el-table-column label="姓名" min-width="96" fixed>
            <template #default="scope">
              <a href="javascript:" class="forest-link" @click="view(scope.row.id)">
                {{ scope.row.clueDO?.fullName || '—' }}
              </a>
            </template>
          </el-table-column>
          <el-table-column label="称呼" width="70">
            <template #default="scope">{{ scope.row.appellationDO?.typeValue || '—' }}</template>
          </el-table-column>
          <el-table-column label="手机" width="116">
            <template #default="scope">{{ scope.row.clueDO?.phone || '—' }}</template>
          </el-table-column>
          <el-table-column label="负责人" width="90">
            <template #default="scope">{{ scope.row.ownerDO?.name || '—' }}</template>
          </el-table-column>
          <el-table-column label="所属活动" min-width="128" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.activityDO?.name || '—' }}</template>
          </el-table-column>
          <el-table-column label="选购产品" min-width="108" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="scope.row.intentionProductDO?.name" class="state-tag state-tag--green">
                {{ scope.row.intentionProductDO.name }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="意向状态" width="102">
            <template #default="scope">
              <span v-if="scope.row.intentionStateDO?.typeValue"
                    class="state-tag" :class="intentionTagByText(scope.row.intentionStateDO.typeValue)">
                {{ scope.row.intentionStateDO.typeValue }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="贷款需求" width="92">
            <template #default="scope">
              <span v-if="scope.row.needLoanDO?.typeValue"
                    class="state-tag" :class="needLoanTagByText(scope.row.needLoanDO.typeValue)">
                {{ scope.row.needLoanDO.typeValue }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="90">
            <template #default="scope">{{ scope.row.sourceDO?.typeValue || '—' }}</template>
          </el-table-column>
          <el-table-column label="下次联系" width="152">
            <template #default="scope">
              <span v-if="scope.row.nextContactTime">
                <span :class="{ 'overdue-text': isOverdue(scope.row.nextContactTime) }">
                  {{ formatDate(scope.row.nextContactTime) }}
                </span>
                <el-tag v-if="isOverdue(scope.row.nextContactTime)" size="small" type="danger" effect="plain">逾期</el-tag>
              </span>
              <span v-else class="empty-dash">未设置</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="scope">
              <el-button type="primary" link size="small" :icon="View"
                         @click="view(scope.row.id)" v-hasPermission="'customer:view'">详情</el-button>
            </template>
          </el-table-column>

          <!-- 空状态：区分“没有客户”（需去线索转化）与“筛选无结果” -->
          <template #empty>
            <div class="empty-state">
              <el-icon :size="44"><FolderOpened /></el-icon>
              <p v-if="isFiltering">没有符合当前条件的客户</p>
              <p v-else>还没有客户，可从线索详情页将成熟线索转化为客户</p>
              <el-button v-if="isFiltering" size="small" @click="resetFilters">清除筛选条件</el-button>
              <el-button v-else size="small" type="primary" @click="goClueList">前往线索管理</el-button>
            </div>
          </template>
        </el-table>
      </div>

      <div class="pager-bar">
        <!-- 【严谨性说明】同线索页：后端 PAGE_SIZE 固定为 10，故不提供 size 选择器避免欺骗性体验 -->
        <el-pagination
            background
            layout="total, prev, pager, next, jumper"
            :page-size="pageSize"
            :total="total"
            :current-page="currentPage"
            @prev-click="page"
            @next-click="page"
            @current-change="page" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { doGet } from "../http/httpRequest.js"
import axios from "axios"
import { getToken, messageFrame } from "../util/util.js"
import router from "@/router/router.js";
import { ElMessage } from 'element-plus'
import {
  Search, Refresh, Download, View, Filter, User, Goods, Avatar,
  AlarmClock, Select, FolderOpened, Iphone
} from '@element-plus/icons-vue'

// 组件名称（用于调试）
defineOptions({ name: "CustomerView" })

// 响应式数据
const customerList = ref([])           // 客户列表
const pageSize = ref(10)               // 每页条数
const total = ref(0)                   // 总记录数
const currentPage = ref(1)             // 当前页码
const loading = ref(false)             // 表格加载态
const customerIdArray = ref([])        // 已选中的客户ID数组

// 筛选条件（全部可选；不传即等价于原全量分页）
const filters = reactive({
  fullName: '',
  phone: '',
  ownerId: '',
  product: '',
  source: '',
  intentionState: ''
})

// 筛选下拉数据源
const ownerOptions = ref([])
const productOptions = ref([])
const sourceOptions = ref([])
const intentionOptions = ref([])

const isFiltering = computed(() => !!(filters.fullName || filters.phone || filters.ownerId
    || filters.product || filters.source || filters.intentionState))

const activeFilterCount = computed(() => [filters.fullName, filters.phone, filters.ownerId,
  filters.product, filters.source, filters.intentionState].filter(v => v !== '' && v != null).length)

// 本页意向产品数（去重统计，体现客户意向分布）
const productCount = computed(() => new Set(customerList.value
  .map(c => c.intentionProductDO?.name).filter(Boolean)).size)

// 意向字典仍用于筛选下拉；配色不再依赖它（改由 typeValue 文本推导）

// 本页已设置下次联系且已逾期的客户数——销售最需要的“该联系了”提醒
const followUpCount = computed(() => customerList.value
  .filter(c => isOverdue(c.nextContactTime)).length)

// 组装查询参数：仅携带实际填写的条件
const buildParams = (current) => {
  const params = { current }
  if (filters.fullName) params.fullName = filters.fullName.trim()
  if (filters.phone) params.phone = filters.phone.trim()
  if (filters.ownerId) params.ownerId = filters.ownerId
  if (filters.product) params.product = filters.product
  if (filters.source) params.source = filters.source
  if (filters.intentionState) params.intentionState = filters.intentionState
  return params
}

// 获取客户分页列表数据
const getData = async (current) => {
  currentPage.value = current
  loading.value = true
  try {
    const resp = await doGet("/api/customers", buildParams(current))
    if (resp.data.code === 200) {
      customerList.value = resp.data.data.list
      pageSize.value = resp.data.data.pageSize || pageSize.value
      total.value = resp.data.data.total
      currentPage.value = resp.data.data.pageNum || current
    }
  } finally {
    loading.value = false
  }
}

// 加载筛选下拉数据源
const loadFilterOptions = () => {
  doGet('/api/owner').then(resp => {
    if (resp.data.code === 200) ownerOptions.value = resp.data.data || []
  })
  doGet('/api/dicvalue/product').then(resp => {
    if (resp.data.code === 200) productOptions.value = resp.data.data || []
  })
  doGet('/api/dicvalue/source').then(resp => {
    if (resp.data.code === 200) sourceOptions.value = resp.data.data || []
  })
  doGet('/api/dicvalue/intentionState').then(resp => {
    if (resp.data.code === 200) intentionOptions.value = resp.data.data || []
  })
}

const doSearch = () => { getData(1) }

const resetFilters = () => {
  filters.fullName = ''
  filters.phone = ''
  filters.ownerId = ''
  filters.product = ''
  filters.source = ''
  filters.intentionState = ''
  getData(1)
}

// 分页跳转
const page = (number) => {
  getData(number)
}

// 连续序号：跳页保持递增
const rowIndex = (index) => (currentPage.value - 1) * pageSize.value + index + 1

// 日期格式化：列表只保留到分
const formatDate = (val) => {
  if (!val) return ''
  return String(val).slice(0, 16)
}

// 是否已逾期
const isOverdue = (val) => {
  if (!val) return false
  const t = new Date(String(val).replace(' ', 'T'))
  return !isNaN(t.getTime()) && t.getTime() < Date.now()
}

/**
 * 意向状态标签配色。
 *
 * 【为何基于文本而非 ID】客户列表的 clueDO 关联在 Mapper 中只映射了
 * fullName/phone/weixin/qq/email/age/job/yearIncome/address，并未映射
 * needLoan 与 intentionState 的原始 ID（读它永远是 undefined）。
 * 后端确实下发的是 needLoanDO.typeValue / intentionStateDO.typeValue 文本，
 * 故配色一律基于文本语义推导，不依赖不存在的字段。
 */
const intentionTagByText = (text) => {
  const name = text || ''
  if (name.includes('高')) return 'state-tag--green'
  if (name.includes('中')) return 'state-tag--gold'
  if (name.includes('低')) return 'state-tag--neutral'
  return 'state-tag--blue'
}

// 贷款需求标签配色：“需要/是”类高亮为金色（商机信号），其余中性感
const needLoanTagByText = (text) => {
  const name = text || ''
  if (name.includes('否') || name.includes('不')) return 'state-tag--neutral'
  return 'state-tag--gold'
}

// 处理表格选中变化
const handleSelectionChange = (selectionDataArray) => {
  customerIdArray.value = selectionDataArray.map(data => data.id)
}

/**
 * 导出 Excel 核心逻辑。
 *
 * 【修复】旧版创建的隐藏 iframe 永不从 DOM 移除，每导一次就往 body 里永久堆一个节点
 * （长时间使用的内存泄露）。现在下载触发后延时移除 iframe。
 * 【体验】导出无 loading/结果反馈，用户点了不知是否成功；补上轻量提示。
 */
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
  // 浏览器接管下载后移除节点，防止 DOM 节点持续堆积
  setTimeout(() => iframe.remove(), 3000)
}

// 批量导出（当前筛选条件下的全部数据）
const batchExportExcel = () => {
  ElMessage.success('导出任务已开始，请留意浏览器下载')
  exportExcel(null)
}

// 选择导出（仅导出勾选数据）
const chooseExportExcel = () => {
  if (customerIdArray.value.length <= 0) {
    messageFrame("请先勾选要导出的客户", "warning")
    return
  }
  ElMessage.success(`已提交 ${customerIdArray.value.length} 条客户的导出任务`)
  const ids = customerIdArray.value.join(",")
  exportExcel(ids)
}

// 查看详情
const view = (id) => {
  router.push(`/dashboard/customer/${id}`)
}

// 空客态引导：客户由线索转化而来，直接带到线索管理页
const goClueList = () => {
  router.push('/dashboard/clue')
}

// 组件挂载时加载第一页数据
onMounted(() => {
  getData(1)
  loadFilterOptions()
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.customer-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

/* 逾期时间高亮 */
.overdue-text { color: #c2564e; font-weight: 600; }

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 44px 0;
  color: #93a89b;
  text-align: center;
}
.empty-state p { margin: 0; font-size: 13px; letter-spacing: 0.4px; max-width: 420px; }
</style>