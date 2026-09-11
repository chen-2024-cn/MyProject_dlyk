<template>
  <div class="module-shell activity-module">

    <!-- ============ 页面 Hero ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">MARKETING · CAMPAIGN BOARD</span>
        <h2>市场活动</h2>
        <p>统筹推广活动的档期与预算：查看进行中、筹备中与已结束的活动全景</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ isFiltering ? '已启用条件筛选' : '活动清单实时同步中' }}
      </div>
    </section>

    <!-- ============ 概览指标 ============ -->
    <div class="module-metrics">
      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><Flag /></el-icon></div>
        <div>
          <div class="metric-tile__label">{{ isFiltering ? '匹配活动' : '活动总数' }}</div>
          <div class="metric-tile__value">{{ total }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><VideoPlay /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页进行中</div>
          <div class="metric-tile__value">{{ ongoingOnPage }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
        <div class="metric-tile__icon"><el-icon :size="18"><Coin /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页预算合计</div>
          <div class="metric-tile__value">{{ pageBudget }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><Select /></el-icon></div>
        <div>
          <div class="metric-tile__label">已选中</div>
          <div class="metric-tile__value">{{ selectedIds.length }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 筛选面板 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Filter /></el-icon>
          <span>活动筛选</span>
        </div>
        <span class="panel-card__hint">时间筛选按「档期重叠」匹配，跨月长活动不会漏掉</span>
      </div>

      <div class="panel-card__body">
        <div class="filter-grid">
          <div class="filter-item">
            <label class="filter-item__label">负责人</label>
            <el-select v-model="filters.ownerId" placeholder="全部负责人" clearable filterable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="item in ownerOption" :key="item.id"
                         :label="item.name" :value="item.id" />
            </el-select>
          </div>

          <div class="filter-item">
            <label class="filter-item__label">活动名称</label>
            <el-input v-model="filters.name" placeholder="名称模糊匹配" clearable
                      :prefix-icon="Search" @keyup.enter="doSearch" />
          </div>

          <!-- 【语义修正】原版"创建时间"精确到秒的等值筛选几乎不可能命中，予以移除；
               活动时间区间保留并与后端重叠口径对齐 -->
          <div class="filter-item filter-item--wide">
            <label class="filter-item__label">活动时间区间</label>
            <el-date-picker v-model="filters.dateRange" type="datetimerange"
                            range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间"
                            value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%"
                            @change="doSearch" />
          </div>

          <div class="filter-item">
            <label class="filter-item__label">预算不低于</label>
            <el-input v-model="filters.budget" placeholder="最小预算金额" clearable
                      :prefix-icon="Coin" @keyup.enter="doSearch" />
          </div>
        </div>

        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          <span v-if="isFiltering" class="toolbar__selected">已生效 {{ activeFilterCount }} 项条件</span>
        </div>
      </div>
    </div>

    <!-- ============ 活动清单 ============ -->
    <div class="panel-card forest-table">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Flag /></el-icon>
          <span>活动清单</span>
        </div>

        <div class="toolbar">
          <span v-if="selectedIds.length > 0" class="toolbar__selected">已选中 {{ selectedIds.length }} 个</span>
          <el-button type="primary" :icon="Plus" @click="openAddDialog" v-hasPermission="'activity:add'">
            添加市场活动
          </el-button>
          <el-button type="danger" :icon="Delete" :disabled="!selectedIds.length"
                     @click="deleteArr" v-hasPermission="'activity:delete'">
            批量删除
          </el-button>
        </div>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table v-loading="tableLoading" :data="activityList" style="width: 100%"
                  @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" label="#" width="54" :index="rowIndex" />
          <el-table-column label="活动名称" min-width="160" fixed show-overflow-tooltip>
            <template #default="scope">
              <a href="javascript:" class="forest-link" @click="handleDetail(scope.row)"
                 v-hasPermission="'activity:view'">
                {{ scope.row.name || '未命名活动' }}
              </a>
            </template>
          </el-table-column>
          <el-table-column label="负责人" width="110">
            <template #default="scope">
              <span v-if="scope.row.ownerDo">{{ scope.row.ownerDo.name }}</span>
              <span v-else class="state-tag state-tag--neutral">未分配</span>
            </template>
          </el-table-column>
          <!-- 【核心体验】活动状态可视化：把 start/end 时间换算成「进行中/筹备中/已结束」，
               解决旧版"只看到两个时间列却不知道活动处于什么状态"的功能模糊问题 -->
          <el-table-column label="活动状态" width="106">
            <template #default="scope">
              <span class="state-tag" :class="activityPhase(scope.row).cls">
                <em class="phase-dot" :class="activityPhase(scope.row).dot"></em>
                {{ activityPhase(scope.row).label }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="活动档期" min-width="200">
            <template #default="scope">
              <span class="date-range">
                {{ formatDate(scope.row.startTime, 10) }}
                <span class="date-sep">→</span>
                {{ formatDate(scope.row.endTime, 10) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="活动预算" width="130" align="right">
            <template #default="scope">
              <span v-if="scope.row.cost != null" class="money-cell">{{ formatMoney(scope.row.cost) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="152">
            <template #default="scope">
              <span v-if="scope.row.createTime">{{ formatDate(scope.row.createTime, 16) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="186" fixed="right">
            <template #default="scope">
              <el-button type="primary" link size="small" :icon="View"
                         @click="handleDetail(scope.row)" v-hasPermission="'activity:view'">详情</el-button>
              <el-button type="success" link size="small" :icon="EditPen"
                         @click="handleEdit(scope.row)" v-hasPermission="'activity:edit'">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete"
                         @click="handleDelete(scope.row.id)" v-hasPermission="'activity:delete'">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <el-icon :size="44"><FolderOpened /></el-icon>
              <p v-if="isFiltering">没有符合当前条件的市场活动</p>
              <p v-else>还没有市场活动，点击「添加市场活动」创建第一个</p>
              <el-button v-if="isFiltering" size="small" @click="resetFilters">清除筛选条件</el-button>
              <el-button v-else size="small" type="primary" @click="openAddDialog" v-hasPermission="'activity:add'">
                创建第一个活动
              </el-button>
            </div>
          </template>
        </el-table>
      </div>

      <div class="pager-bar">
        <el-pagination background layout="total, prev, pager, next, jumper"
                       :page-size="pageSize" :total="total" :current-page="currentPage"
                       @prev-click="toPage" @next-click="toPage" @current-change="toPage" />
      </div>
    </div>

    <!-- ============ 新增/编辑/详情 对话框 ============ -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="680px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <el-form
          ref="dialogFormRef"
          :model="activityForm"
          :rules="dialogRules"
          label-width="100px"
          :disabled="isReadOnly"
      >
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="activityForm.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="负责人" prop="ownerId">
          <el-select v-model="activityForm.ownerId" placeholder="请选择负责人" style="width: 100%">
            <el-option v-for="item in ownerOption" :key="item.id"
                       :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="activityForm.startTime" type="datetime"
                          placeholder="请选择开始时间" value-format="YYYY-MM-DD HH:mm:ss"
                          style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="activityForm.endTime" type="datetime"
                          placeholder="请选择结束时间" value-format="YYYY-MM-DD HH:mm:ss"
                          style="width: 100%" />
        </el-form-item>
        <el-form-item label="活动预算" prop="cost">
          <el-input v-model="activityForm.cost" placeholder="请输入预算金额（元）">
            <template #prefix>¥</template>
          </el-input>
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="activityForm.description" type="textarea"
                    :autosize="{ minRows: 2, maxRows: 6 }" placeholder="请输入活动描述" />
        </el-form-item>
      </el-form>

      <!-- ============ 备注模块 ============ -->
      <div v-if="activityForm.id" class="remark-section">
        <el-divider content-position="left">
          <span class="remark-divider">活动跟进备注（{{ activityRemarks.length }}）</span>
        </el-divider>

        <div v-if="canEditRemark" class="remark-input-area">
          <el-input v-model="newRemarkContent" type="textarea" :rows="2"
                    placeholder="记录活动执行过程中的关键信息…" resize="none" maxlength="500" show-word-limit />
          <el-button type="primary" size="small" :icon="Plus" :loading="remarkSubmitting"
                     :disabled="!newRemarkContent.trim()" @click="addRemark" class="remark-add-btn">
            添加备注
          </el-button>
        </div>

        <div class="remark-list" v-loading="remarkLoading">
          <div v-for="remark in activityRemarks" :key="remark.id" class="remark-item">
            <div class="remark-content">{{ remark.noteContent }}</div>
            <div class="remark-meta">
              <span class="remark-author">{{ remark.createByName || ('员工#' + remark.createBy) }}</span>
              <span>{{ remark.createTime }}</span>
              <span v-if="remark.editTime" class="remark-edited">已编辑</span>
            </div>
            <div class="remark-actions">
              <el-button link type="primary" size="small" :icon="EditPen"
                         v-if="canEditRemark" @click="editRemark(remark)">编辑</el-button>
              <el-button link type="danger" size="small" :icon="Delete"
                         v-if="canDeleteRemark" @click="deleteRemark(remark.id)">删除</el-button>
            </div>
          </div>
          <div v-if="!activityRemarks.length && !remarkLoading" class="remark-empty">
            <el-icon :size="26"><ChatDotRound /></el-icon>
            <span>暂无备注</span>
          </div>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ isReadOnly ? '关 闭' : '取 消' }}</el-button>
          <el-button v-if="!isReadOnly" type="primary" :loading="submitting" @click="submitForm">
            {{ activityForm.id ? '保存修改' : '创建活动' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { doGet, doPost, doPut, doDelete, doPostJson } from '@/http/httpRequest.js'
import { ElMessage, ElMessageBox } from 'element-plus'
import { messageFrame, getPermissionCache } from '@/util/util.js'
import {
  Search, Refresh, Plus, Delete, EditPen, View, Filter, Flag, Coin, Select,
  VideoPlay, FolderOpened, ChatDotRound
} from '@element-plus/icons-vue'

// ---------- 查询相关 ----------
const filters = reactive({
  ownerId: '',
  name: '',
  dateRange: [],
  budget: ''
})

const isFiltering = computed(() =>
    !!(filters.ownerId || filters.name || (filters.dateRange && filters.dateRange.length === 2) || filters.budget))

const activeFilterCount = computed(() => {
  let n = 0
  if (filters.ownerId) n++
  if (filters.name) n++
  if (filters.dateRange && filters.dateRange.length === 2) n++
  if (filters.budget) n++
  return n
})

const activityList = ref([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const ownerOption = ref([])
const selectedIds = ref([])
const tableLoading = ref(false)

// 权限集合（用于备注按钮的细粒度控制，与后端 @PreAuthorize 口径一致）
const permSet = computed(() => new Set(getPermissionCache()?.permissionList || []))
const canEditRemark = computed(() => !isReadOnly.value && permSet.value.has('activity:edit'))
const canDeleteRemark = computed(() => !isReadOnly.value && permSet.value.has('activity:delete'))
const currentUserId = computed(() => getPermissionCache()?.userId ?? null)

// ---------- 对话框相关 ----------
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isReadOnly = ref(false)
const submitting = ref(false)
const dialogFormRef = ref(null)

// 备注相关
const activityRemarks = ref([])
const newRemarkContent = ref('')
const remarkLoading = ref(false)
const remarkSubmitting = ref(false)

const initActivityForm = () => ({
  id: null,
  name: '',
  ownerId: '',
  startTime: '',
  endTime: '',
  cost: '',
  description: ''
})

const activityForm = reactive(initActivityForm())

const dialogRules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  ownerId: [{ required: true, message: '请选择负责人', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      // 前置拦截「结束早于开始」——后端同样校验（BusinessException），双层防御
      validator: (rule, value, callback) => {
        if (!value || !activityForm.startTime) return callback()
        if (new Date(String(value).replace(' ', 'T')) <= new Date(String(activityForm.startTime).replace(' ', 'T'))) {
          callback(new Error('结束时间必须晚于开始时间'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  description: [{ required: true, message: '请输入活动描述', trigger: 'blur' }],
  cost: [
    { required: true, message: '请输入预算金额', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) return callback(new Error('请输入预算金额'))
        if (!/^[0-9]+(\.[0-9]{1,2})?$/.test(value)) {
          callback(new Error('预算必须为非负数，最多两位小数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 对话框关闭时重置表单与备注
watch(dialogVisible, (val) => {
  if (!val) resetForm()
})

// ---------- 数据获取 ----------
onMounted(() => {
  getData(1)
  loadOwners()
})

const buildParams = (current) => {
  const params = { current }
  if (filters.ownerId) params.ownerId = filters.ownerId
  if (filters.name) params.name = filters.name.trim()
  if (filters.dateRange && filters.dateRange.length === 2) {
    // 后端按「档期重叠」匹配：startTime 传查询区间开始、endTime 传查询区间结束
    params.startTime = filters.dateRange[0]
    params.endTime = filters.dateRange[1]
  }
  if (filters.budget) params.budget = filters.budget
  return params
}

const getData = async (current) => {
  tableLoading.value = true
  currentPage.value = current
  try {
    const response = await doGet('api/activities', buildParams(current))
    if (response.data.code === 200) {
      activityList.value = response.data.data.list
      pageSize.value = response.data.data.pageSize || pageSize.value
      total.value = response.data.data.total
    } else {
      ElMessage.error(response.data.msg || '获取列表失败')
    }
  } catch (e) {
    console.error('获取市场活动失败', e)
    ElMessage.error('获取市场活动列表失败')
  } finally {
    tableLoading.value = false
  }
}

const toPage = (current) => getData(current)

const doSearch = () => getData(1)

const resetFilters = () => {
  filters.ownerId = ''
  filters.name = ''
  filters.dateRange = []
  filters.budget = ''
  getData(1)
}

// ---------- 负责人下拉（单飞防重复请求） ----------
let ownerPromise = null
const loadOwners = () => {
  if (!ownerPromise) {
    ownerPromise = doGet('/api/owner', {}).then(res => {
      if (res.data.code === 200) {
        ownerOption.value = res.data.data || []
      }
    }).catch(() => {
      ownerPromise = null
    })
  }
  return ownerPromise
}

// ---------- 表格选择 ----------
const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((row) => row.id)
}

// ---------- 展示辅助 ----------
const rowIndex = (index) => (currentPage.value - 1) * pageSize.value + index + 1

const formatDate = (val, len) => (val ? String(val).slice(0, len) : '—')

const formatMoney = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (isNaN(n)) return String(val)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 本页统计
const ongoingOnPage = computed(() =>
    activityList.value.filter(a => activityPhase(a).key === 'ongoing').length)

const pageBudget = computed(() =>
    formatMoney(activityList.value.reduce((acc, a) => acc + Number(a.cost || 0), 0)))

/**
 * 活动阶段推导（进行中 / 筹备中 / 已结束）。
 * 基于 start_time 与 end_time 相对当前时刻计算，无需后端新增字段。
 */
const activityPhase = (row) => {
  const now = Date.now()
  const start = row.startTime ? new Date(String(row.startTime).replace(' ', 'T')).getTime() : null
  const end = row.endTime ? new Date(String(row.endTime).replace(' ', 'T')).getTime() : null
  if (start == null || end == null) return { key: 'unknown', label: '档期未定', cls: 'state-tag--neutral', dot: 'dot-neutral' }
  if (now < start) return { key: 'upcoming', label: '筹备中', cls: 'state-tag--blue', dot: 'dot-blue' }
  if (now > end) return { key: 'done', label: '已结束', cls: 'state-tag--neutral', dot: 'dot-neutral' }
  return { key: 'ongoing', label: '进行中', cls: 'state-tag--green', dot: 'dot-green' }
}

// ---------- 备注 ----------
const loadRemarks = async () => {
  if (!activityForm.id) return
  remarkLoading.value = true
  try {
    const res = await doGet(`api/activities/${activityForm.id}/remarks`)
    if (res.data.code === 200) {
      activityRemarks.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '加载备注失败')
    }
  } catch (e) {
    console.error('加载备注失败', e)
  } finally {
    remarkLoading.value = false
  }
}

const addRemark = async () => {
  if (!newRemarkContent.value.trim() || remarkSubmitting.value) return
  remarkSubmitting.value = true
  try {
    const res = await doPost(`api/activities/${activityForm.id}/remarks`, {
      noteContent: newRemarkContent.value.trim()
    })
    if (res.data.code === 200) {
      ElMessage.success('备注添加成功')
      newRemarkContent.value = ''
      await loadRemarks()
    } else {
      ElMessage.error(res.data.msg || '添加失败')
    }
  } catch (e) {
    console.error('添加备注失败', e)
    ElMessage.error('添加备注失败')
  } finally {
    remarkSubmitting.value = false
  }
}

const editRemark = async (remark) => {
  try {
    const { value: newContent } = await ElMessageBox.prompt('编辑备注内容', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: remark.noteContent,
      inputType: 'textarea',
      inputValidator: (val) => (val && val.trim() ? true : '内容不能为空')
    })
    if (newContent === undefined) return
    const res = await doPut(`api/activities/remarks/${remark.id}`, { noteContent: newContent.trim() })
    if (res.data.code === 200) {
      ElMessage.success('修改成功')
      await loadRemarks()
    } else {
      // 后端规则：普通用户只能改自己创建的备注（管理员可改任意），文案直接透出
      ElMessage.error(res.data.msg || '修改失败')
    }
  } catch (e) {
    if (e !== 'cancel') console.error('修改备注失败', e)
  }
}

const deleteRemark = async (remarkId) => {
  try {
    await messageFrame('确定删除该备注吗？')
    const res = await doDelete(`api/activities/remarks/${remarkId}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      await loadRemarks()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') console.error('删除备注失败', e)
  }
}

// ---------- 详情 / 新增 / 编辑 ----------
const handleDetail = (row) => {
  isReadOnly.value = true
  dialogTitle.value = '活动详情'
  fillFormFromRow(row)
  dialogVisible.value = true
}

const openAddDialog = () => {
  loadOwners()
  isReadOnly.value = false
  dialogTitle.value = '添加市场活动'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  loadOwners()
  isReadOnly.value = false
  dialogTitle.value = '编辑市场活动'
  fillFormFromRow(row)
  dialogVisible.value = true
}

const fillFormFromRow = (row) => {
  activityForm.id = row.id
  activityForm.name = row.name || ''
  activityForm.ownerId = row.ownerId || ''
  activityForm.startTime = row.startTime || ''
  activityForm.endTime = row.endTime || ''
  activityForm.cost = row.cost != null ? String(row.cost) : ''
  activityForm.description = row.description || ''
  nextTick(() => {
    if (activityForm.id) loadRemarks()
    dialogFormRef.value?.clearValidate()
  })
}

const resetForm = () => {
  Object.assign(activityForm, initActivityForm())
  activityRemarks.value = []
  newRemarkContent.value = ''
  nextTick(() => dialogFormRef.value?.clearValidate())
}

// ---------- 提交表单（新增/编辑） ----------
const submitForm = async () => {
  if (!dialogFormRef.value || submitting.value) return
  await dialogFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    const params = {
      name: activityForm.name.trim(),
      ownerId: activityForm.ownerId,
      startTime: activityForm.startTime,
      endTime: activityForm.endTime,
      budget: activityForm.cost,
      description: activityForm.description.trim()
    }

    try {
      let res
      if (activityForm.id) {
        res = await doPut(`api/activities/${activityForm.id}`, params)
      } else {
        res = await doPost('api/activities', params)
      }

      if (res.data.code === 200) {
        ElMessage.success(activityForm.id ? '编辑成功' : '新增成功')
        dialogVisible.value = false
        getData(currentPage.value)
      } else {
        // 后端业务校验（时间倒挂/名称为空等）文案直接透出
        ElMessage.error(res.data.msg || '操作失败')
      }
    } catch (e) {
      console.error('提交失败', e)
      ElMessage.error('提交失败，请稍后再试')
    } finally {
      submitting.value = false
    }
  })
}

// ---------- 删除 ----------
const refreshAfterMutate = (removedCount) => {
  const remaining = total.value - removedCount
  const maxPage = Math.max(1, Math.ceil(remaining / pageSize.value))
  getData(Math.min(currentPage.value, maxPage))
}

const handleDelete = async (id) => {
  messageFrame('活动及其备注将一并删除且不可恢复，确定吗？').then(async () => {
    const res = await doDelete(`api/activities/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      refreshAfterMutate(1)
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  }).catch(() => {})
}

const deleteArr = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的活动')
    return
  }
  messageFrame(`确定要删除选中的 ${selectedIds.value.length} 个活动吗？备注将一并清除且不可恢复。`)
      .then(async () => {
        const count = selectedIds.value.length
        // 后端批量删除端点为 POST + JSON body（本轮补齐，旧版前端调用的是不存在的路径）
        const res = await doPostJson('api/activities/batch-delete', { ids: selectedIds.value })
        if (res.data.code === 200) {
          ElMessage.success(res.data.msg || `成功删除 ${count} 个活动`)
          selectedIds.value = []
          refreshAfterMutate(count)
        } else {
          ElMessage.error(res.data.msg || '批量删除失败')
        }
      }).catch(() => {})
}
</script>

<style scoped>
@import "@/assets/module-theme.css";

.activity-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

/* 时间区间筛选占两格，给 datetimerange 组件足够宽度 */
.filter-item--wide {
  grid-column: span 2;
}

/* 金额单元格：等宽衬线 + 右对齐 */
.money-cell {
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-weight: 600;
  color: #98704a;
}

/* 档期展示 */
.date-range {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #3d564a;
}
.date-sep { color: var(--champagne); font-weight: 700; }

/* 状态圆点 */
.phase-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.phase-dot.dot-green  { background: #69a675; box-shadow: 0 0 0 3px rgba(105,166,117,.15); }
.phase-dot.dot-blue   { background: #6f9db8; }
.phase-dot.dot-neutral { background: #a9b7ae; }

/* ---------- 备注区 ---------- */
.remark-section { margin-top: 8px; }

.remark-divider {
  font-size: 13px;
  color: var(--forest-text);
  font-weight: 600;
  letter-spacing: 0.5px;
}

.remark-input-area {
  margin-bottom: 14px;
}

.remark-add-btn { margin-top: 8px; }

.remark-list {
  max-height: 280px;
  overflow-y: auto;
  margin-top: 10px;
}

.remark-item {
  position: relative;
  background: #f7faf8;
  border: 1px solid var(--forest-border);
  border-radius: 12px;
  padding: 11px 13px;
  margin-bottom: 10px;
}

.remark-content {
  font-size: 13px;
  line-height: 1.55;
  color: #2c3e4f;
  white-space: pre-wrap;
  word-break: break-word;
  padding-right: 96px;
}

.remark-meta {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 11.5px;
  color: #8c9aa8;
  margin-top: 6px;
}

.remark-author { color: var(--champagne); font-weight: 600; }

.remark-edited {
  padding: 0 6px;
  border-radius: 99px;
  background: #eef2f0;
  color: #6d8272;
}

.remark-actions {
  position: absolute;
  top: 9px;
  right: 11px;
  display: flex;
}

.remark-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 22px 0;
  color: #93a89b;
  font-size: 12.5px;
}

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

@media (max-width: 992px) {
  .filter-item--wide { grid-column: span 1; }
}
</style>
