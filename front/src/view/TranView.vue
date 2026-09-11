<template>
  <div class="module-shell tran-module">

    <!-- ============ 页面 Hero ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">JOURNEY · DEAL PIPELINE</span>
        <h2>交易管理</h2>
        <p>销售旅程的终点：推进交易阶段、掌握管道分布，直至成交落定</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ isFiltering ? '已启用条件筛选' : '交易管道实时同步中' }}
      </div>
    </section>

    <!-- ============ 管道概览指标 ============ -->
    <div class="module-metrics">
      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><Tickets /></el-icon></div>
        <div>
          <div class="metric-tile__label">{{ isFiltering ? '匹配交易' : '交易总数' }}</div>
          <div class="metric-tile__value">{{ total }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
        <div class="metric-tile__icon"><el-icon :size="18"><Money /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页交易金额</div>
          <div class="metric-tile__value">{{ pageAmount }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><AlarmClock /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页待跟进</div>
          <div class="metric-tile__value">{{ followUpCount }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><Select /></el-icon></div>
        <div>
          <div class="metric-tile__label">已选中</div>
          <div class="metric-tile__value">{{ tranIdArray.length }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 筛选面板 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Filter /></el-icon>
          <span>交易筛选</span>
        </div>
        <span class="panel-card__hint">可按阶段查看管道分布，支持流水号模糊搜索</span>
      </div>

      <div class="panel-card__body">
        <div class="filter-grid">
          <div class="filter-item">
            <label class="filter-item__label">所属客户</label>
            <el-select v-model="filters.customerId" placeholder="全部客户" clearable filterable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="item in customerOptions" :key="item.id"
                         :label="item.name" :value="item.id" />
            </el-select>
          </div>

          <!-- 【核心新增】按交易阶段筛选：销售管道管理最重要的维度（旧版完全缺失） -->
          <div class="filter-item">
            <label class="filter-item__label">交易阶段</label>
            <el-select v-model="filters.stage" placeholder="全部阶段" clearable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="s in stageOptions" :key="s.id"
                         :label="s.typeValue" :value="s.id" />
            </el-select>
          </div>

          <!-- 【核心新增】流水号模糊搜索：客户报单号即可定位 -->
          <div class="filter-item">
            <label class="filter-item__label">交易流水号</label>
            <el-input v-model="filters.tranNo" placeholder="如 TR20260908" clearable
                      :prefix-icon="Search" @keyup.enter="doSearch" />
          </div>

          <!-- 【语义修正】旧版后端为 money 等值匹配（几乎不可能命中），现已改为“不低于”阈值 -->
          <div class="filter-item">
            <label class="filter-item__label">金额不低于</label>
            <el-input v-model="filters.money" placeholder="最小交易金额" clearable
                      :prefix-icon="Coin" @keyup.enter="doSearch" />
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
          <el-icon :size="16"><DataLine /></el-icon>
          <span>交易清单</span>
        </div>

        <div class="toolbar">
          <span v-if="tranIdArray.length > 0" class="toolbar__selected">
            已选中 {{ tranIdArray.length }} 笔
          </span>
          <el-button type="primary" :icon="Plus" @click="addTran" v-hasPermission="'tran:add'">
            新建交易
          </el-button>
          <el-button type="danger" :icon="Delete" @click="batchDelTran" v-hasPermission="'tran:delete'">
            批量删除
          </el-button>
        </div>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table
            v-loading="loading"
            :data="tranList"
            style="width: 100%"
            @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" label="#" width="54" :index="rowIndex" />
          <el-table-column label="交易流水号" min-width="150" fixed>
            <template #default="scope">
              <a href="javascript:" class="forest-link" @click="view(scope.row.id)">
                {{ scope.row.tranNo || '—' }}
              </a>
            </template>
          </el-table-column>
          <el-table-column label="客户" min-width="96" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.customerName || '—' }}</template>
          </el-table-column>
          <el-table-column label="交易金额" width="128" align="right">
            <template #default="scope">
              <!-- 金额右对齐 + 千分位：表格金额列的国际惯例，方便纵向比对大小 -->
              <span v-if="scope.row.money != null" class="money-cell">{{ formatMoney(scope.row.money) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="所处阶段" width="140">
            <template #default="scope">
              <!-- 【核心体验】把阶段从纯文本升级为“进度点 + 阶段名”，
                   一眼就能看出这笔交易在管道中的位置 -->
              <span v-if="scope.row.stageDO?.typeValue"
                    class="state-tag" :class="stageTagType(scope.row.stage)">
                <em class="stage-dot" :class="stageDotClass(scope.row.stage)"></em>
                {{ scope.row.stageDO.typeValue }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="预计成交" width="116">
            <template #default="scope">
              <span v-if="scope.row.expectedDate">{{ formatDate(scope.row.expectedDate, 10) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="下次联系" width="152">
            <template #default="scope">
              <span v-if="scope.row.nextContactTime">
                <span :class="{ 'overdue-text': isOverdue(scope.row.nextContactTime) }">
                  {{ formatDate(scope.row.nextContactTime, 16) }}
                </span>
                <el-tag v-if="isOverdue(scope.row.nextContactTime)" size="small" type="danger" effect="plain">逾期</el-tag>
              </span>
              <span v-else class="empty-dash">未设置</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="178" fixed="right">
            <template #default="scope">
              <el-button type="primary" link size="small" :icon="View"
                         @click="view(scope.row.id)" v-hasPermission="'tran:view'">详情</el-button>
              <el-button type="success" link size="small" :icon="EditPen"
                         @click="edit(scope.row.id)" v-hasPermission="'tran:edit'">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete"
                         @click="del(scope.row.id)" v-hasPermission="'tran:delete'">删除</el-button>
            </template>
          </el-table-column>

          <!-- 空状态 -->
          <template #empty>
            <div class="empty-state">
              <el-icon :size="44"><FolderOpened /></el-icon>
              <p v-if="isFiltering">没有符合当前条件的交易</p>
              <p v-else>还没有交易记录，可为已转化的客户创建交易</p>
              <el-button v-if="isFiltering" size="small" @click="resetFilters">清除筛选条件</el-button>
              <el-button v-else size="small" type="primary" @click="addTran" v-hasPermission="'tran:add'">
                新建第一笔交易
              </el-button>
            </div>
          </template>
        </el-table>
      </div>

      <div class="pager-bar">
        <!--
          【严谨性说明】未提供“每页条数”选择器：后端 Constants.PAGE_SIZE 为固定常量（10），
          若前端给出 size 选择器会造成“选了 20 仍只返回 10 条”的欺骗性体验。
          待后端支持 pageSize 参数后再同步开放。
        -->
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
import { useRouter } from 'vue-router'
import { doGet, doDelete } from '../http/httpRequest'
import { messageFrame } from "@/util/util.js"
import {
  Search, Refresh, Plus, Delete, EditPen, View, Filter, DataLine,
  Tickets, Money, Coin, AlarmClock, Select, FolderOpened
} from '@element-plus/icons-vue'

const router = useRouter()
// 说明：不再注入 reload()——它会整页重挂 router-view，丢失筛选条件与所在页码；
// 改为 getData() 就地刷新，保留完整操作上下文。

const tranList = ref([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)
const tranIdArray = ref([])
const customerOptions = ref([])
const stageOptions = ref([])

// 筛选条件（对应后端 TranQuery 的新增能力：stage / tranNo / money 阈值）
const filters = reactive({
  customerId: '',
  stage: '',
  tranNo: '',
  money: ''
})

const isFiltering = computed(() => !!(filters.customerId || filters.stage !== ''
    || filters.tranNo || filters.money))

const activeFilterCount = computed(() => [filters.customerId, filters.stage, filters.tranNo,
  filters.money].filter(v => v !== '' && v != null).length)

// 本页交易金额汇总（千分位，体现管道价值）
const pageAmount = computed(() => {
  const sum = tranList.value.reduce((acc, t) => acc + Number(t.money || 0), 0)
  return formatMoney(sum)
})

// 本页已逾期待跟进的交易数
const followUpCount = computed(() => tranList.value.filter(t => isOverdue(t.nextContactTime)).length)

// 组装查询参数：仅携带实际填写的条件
const buildParams = (current) => {
  const params = { current }
  if (filters.customerId) params.customerId = filters.customerId
  if (filters.stage !== '') params.stage = filters.stage
  if (filters.tranNo) params.tranNo = filters.tranNo.trim()
  if (filters.money) params.money = filters.money
  return params
}

/**
 * 获取交易分页列表数据。
 * 已移除旧的页级 Map 缓存：交易阶段会被频繁推进，缓存会导致列表显示的阶段
 * 与详情页不一致（陈旧数据），一致性优先于省一次请求。
 */
const getData = (current) => {
  currentPage.value = current
  loading.value = true
  doGet('/api/trans', buildParams(current)).then(resp => {
    if (resp.data.code === 200) {
      tranList.value = resp.data.data.list
      pageSize.value = resp.data.data.pageSize || pageSize.value
      total.value = resp.data.data.total
      currentPage.value = resp.data.data.pageNum || current
    }
  }).finally(() => {
    loading.value = false
  })
}

// 客户下拉（受 tran:add/edit 权限保护，仅在有权时拉取）
const loadCustomers = () => {
  doGet('/api/customer/options').then(resp => {
    if (resp.data.code === 200) {
      customerOptions.value = resp.data.data || []
    }
  })
}

// 阶段字典（按 order 排序，确保下拉项与业务流转顺序一致）
const loadStages = () => {
  doGet('/api/dicvalue/stage').then(resp => {
    if (resp.data.code === 200) {
      stageOptions.value = (resp.data.data || []).slice()
        .sort((a, b) => (a.order || 0) - (b.order || 0))
    }
  })
}

const doSearch = () => { getData(1) }

const resetFilters = () => {
  filters.customerId = ''
  filters.stage = ''
  filters.tranNo = ''
  filters.money = ''
  getData(1)
}

const page = (number) => { getData(number) }

// 连续序号：跳页保持递增
const rowIndex = (index) => (currentPage.value - 1) * pageSize.value + index + 1

// 金额千分位格式化（保留 2 位小数，与看板 StatisticView 口径一致）
const formatMoney = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (isNaN(n)) return String(val)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 日期格式化。
 * @param val 原始值
 * @param len 截取长度：10 = 仅日期（预计成交），16 = 到分钟（下次联系）
 */
const formatDate = (val, len) => {
  if (!val) return ''
  return String(val).slice(0, len)
}

// 是否已逾期
const isOverdue = (val) => {
  if (!val) return false
  const t = new Date(String(val).replace(' ', 'T'))
  return !isNaN(t.getTime()) && t.getTime() < Date.now()
}

/**
 * 阶段标签配色：按阶段在管道中的位置渐进变色。
 * 早期阶段用中性感→中期金色（关键推进区）→成交用绿色，
 * 让“一眼看出哪些单子走到后面了”成为可能。
 */
const stageTagType = (stageId) => {
  const sorted = stageOptions.value
  if (sorted.length === 0) return 'state-tag--blue'
  const idx = sorted.findIndex(s => s.id === stageId)
  if (idx === -1) return 'state-tag--blue'
  const isLast = idx === sorted.length - 1
  if (isLast) return 'state-tag--green'                    // 成交
  if (idx >= Math.ceil(sorted.length / 2)) return 'state-tag--gold'  // 中后段
  return 'state-tag--blue'                                 // 早期
}

// 阶段进度小圆点颜色（与标签色一致）
const stageDotClass = (stageId) => {
  const type = stageTagType(stageId)
  if (type === 'state-tag--green') return 'dot-green'
  if (type === 'state-tag--gold') return 'dot-gold'
  return 'dot-blue'
}

const addTran = () => { router.push('/dashboard/tran/add') }

const edit = (id) => { router.push(`/dashboard/tran/edit/${id}`) }

const view = (id) => { router.push(`/dashboard/tran/${id}`) }

/**
 * 增删改后的刷新策略：若当前页已空且不是第 1 页，自动回退一页，
 * 避免用户停在空白页误以为数据丢失。
 */
const refreshAfterMutate = (removedCount) => {
  const remaining = total.value - removedCount
  const maxPage = Math.max(1, Math.ceil(remaining / pageSize.value))
  getData(Math.min(currentPage.value, maxPage))
}

const del = (id) => {
  messageFrame('删除后不可恢复，您确定要删除这笔交易吗？').then(() => {
    doDelete(`/api/tran/${id}`, {}).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('删除成功', 'success')
        refreshAfterMutate(1)
      } else {
        messageFrame('删除失败，原因：' + resp.data.msg, 'error')
      }
    })
  }).catch(() => {})
}

const batchDelTran = () => {
  if (tranIdArray.value.length <= 0) {
    messageFrame('请先勾选要删除的交易', 'warning')
    return
  }
  messageFrame(`删除后不可恢复，确定删除选中的 ${tranIdArray.value.length} 笔交易吗？`).then(() => {
    const count = tranIdArray.value.length
    const ids = tranIdArray.value.join(',')
    doDelete('/api/tran/batch', { ids }).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('批量删除成功', 'success')
        refreshAfterMutate(count)
      } else {
        messageFrame('批量删除失败，原因：' + resp.data.msg, 'error')
      }
    })
  }).catch(() => {})
}

const handleSelectionChange = (dataObjectArray) => {
  tranIdArray.value = dataObjectArray.map(item => item.id)
}

onMounted(() => {
  getData(1)
  loadCustomers()
  loadStages()
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.tran-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

/* 金额单元格：等宽字体 + 右对齐，方便纵向比对大小 */
.money-cell {
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-weight: 600;
  color: #98704a;
}

/* 阶段进度小圆点 */
.stage-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.stage-dot.dot-blue  { background: #6f9db8; }
.stage-dot.dot-gold  { background: #c19a63; }
.stage-dot.dot-green { background: #69a675; }

/* 逾期高亮 */
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
