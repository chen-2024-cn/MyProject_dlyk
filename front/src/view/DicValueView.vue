<template>
  <div class="module-shell dic-value-module">

    <!-- ============ 页面 Hero ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">SYSTEM · DICTIONARY WORKBENCH</span>
        <h2>字典数据维护</h2>
        <p>管理某个字典类型下的具体取值与展示顺序：线索状态、交易阶段等下拉选项在此落库</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ selectedType ? `正在维护「${selectedType.typeName}」` : '请选择要维护的字典类型' }}
      </div>
    </section>

    <!-- ============ 概览指标 ============ -->
    <div class="module-metrics">
      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><Files /></el-icon></div>
        <div>
          <div class="metric-tile__label">{{ selectedType ? '当前类型字典值' : '字典值（未选类型）' }}</div>
          <div class="metric-tile__value">{{ selectedType ? valueTotal : '—' }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><Sort /></el-icon></div>
        <div>
          <div class="metric-tile__label">排序区间</div>
          <div class="metric-tile__value">{{ orderRangeText }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
        <div class="metric-tile__icon"><el-icon :size="18"><MagicStick /></el-icon></div>
        <div>
          <div class="metric-tile__label">新增建议排序号</div>
          <div class="metric-tile__value">{{ nextOrderHint }}</div>
        </div>
      </div>

      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><Collection /></el-icon></div>
        <div>
          <div class="metric-tile__label">字典类型总数</div>
          <div class="metric-tile__value">{{ typeOptions.length }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 类型选择器（工作台入口，替代旧版左侧列表） ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Aim /></el-icon>
          <span>选择字典类型</span>
        </div>
        <span class="panel-card__hint">
          支持深链定位（?typeCode=xxx）；也可从「字典类型」目录页点击卡片跳转
        </span>
      </div>
      <div class="panel-card__body">
        <div class="type-picker">
          <el-select v-model="selectedTypeCode" placeholder="请选择要维护的字典类型" filterable
                     size="large" style="max-width: 380px" @change="onTypeChange">
            <el-option v-for="t in typeOptions" :key="t.id"
                       :label="`${t.typeName}（${t.typeCode}）`" :value="t.typeCode">
              <span class="opt-name">{{ t.typeName }}</span>
              <code class="opt-code">{{ t.typeCode }}</code>
              <span class="opt-count" :class="t.valueCount ? 'is-filled' : 'is-empty'">
                {{ t.valueCount || 0 }} 值
              </span>
            </el-option>
          </el-select>

          <el-button :icon="Collection" @click="goCatalog">浏览类型目录</el-button>

          <!-- 当前类型的元信息速览 -->
          <div v-if="selectedType" class="picker-meta">
            <span class="picker-meta__name">{{ selectedType.typeName }}</span>
            <code>{{ selectedType.typeCode }}</code>
            <span v-if="selectedType.remark" class="picker-meta__remark">{{ selectedType.remark }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 字典值工作台 ============ -->
    <div class="panel-card forest-table">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Files /></el-icon>
          <span>字典值清单</span>
          <span v-if="selectedType" class="count-badge">{{ valueTotal }} 条</span>
        </div>
        <div class="toolbar" v-if="selectedType">
          <el-input v-model="valueKeyword" placeholder="按字典值内容检索" clearable
                    :prefix-icon="Search" style="width: 210px"
                    @keyup.enter="searchValues" @clear="searchValues" />
          <el-button type="primary" :icon="Plus" @click="openValueAdd" v-hasPermission="'dicvalue:add'">
            新增字典值
          </el-button>
        </div>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table
            v-if="selectedType"
            v-loading="valueLoading"
            :data="valueList"
            style="width: 100%">
          <el-table-column type="index" label="#" width="54" align="center" :index="valueRowIndex" />
          <el-table-column label="字典值（业务展示文案）" min-width="180">
            <template #default="scope">
              <span class="value-name">{{ scope.row.typeValue }}</span>
            </template>
          </el-table-column>
          <el-table-column label="排序号" width="100" align="center">
            <template #default="scope">
              <span class="order-chip">{{ scope.row.order ?? '—' }}</span>
            </template>
          </el-table-column>
          <!-- 【新增】排序连续性提示：order 缺号/重号会导致阶段类字典展示乱序，就地预警 -->
          <el-table-column label="顺序检查" width="110" align="center">
            <template #default="scope">
              <el-tooltip v-if="orderWarning(scope.row.order).warn"
                          :content="orderWarning(scope.row.order).text" placement="top">
                <span class="state-tag state-tag--gold">
                  <el-icon :size="11"><WarningFilled /></el-icon>
                  {{ orderWarning(scope.row.order).label }}
                </span>
              </el-tooltip>
              <span v-else class="state-tag state-tag--green">
                <el-icon :size="11"><CircleCheck /></el-icon>正常
              </span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="170" show-overflow-tooltip>
            <template #default="scope">
              <span v-if="scope.row.remark">{{ scope.row.remark }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="scope">
              <el-button type="success" link size="small" :icon="EditPen"
                         @click="openValueEdit(scope.row)" v-hasPermission="'dicvalue:edit'">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete"
                         @click="confirmDeleteValue(scope.row)" v-hasPermission="'dicvalue:delete'">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <el-icon :size="42"><FolderOpened /></el-icon>
              <p v-if="valueKeyword">没有名为「{{ valueKeyword }}」的字典值</p>
              <p v-else>该类型下暂无字典值（「待维护空类型」），点击「新增字典值」创建第一个</p>
              <el-button v-if="!valueKeyword" size="small" type="primary" :icon="Plus"
                         @click="openValueAdd" v-hasPermission="'dicvalue:add'">
                新增第一个字典值
              </el-button>
            </div>
          </template>
        </el-table>

        <div v-else class="empty-state empty-state--tall">
          <el-icon :size="48"><Aim /></el-icon>
          <p>请先在上方选择一个字典类型，开始维护它的字典值</p>
          <el-button size="small" :icon="Collection" @click="goCatalog">浏览类型目录</el-button>
        </div>
      </div>

      <div class="pager-bar" v-if="selectedType && valueTotal > pageSize">
        <el-pagination background layout="total, prev, pager, next"
                       v-model:current-page="valueCurrent"
                       :page-size="pageSize" :total="valueTotal"
                       @current-change="loadValueData" />
      </div>
    </div>

    <!-- ============ 值弹窗 ============ -->
    <el-dialog v-model="valueDialogVisible" :title="valueDialogTitle" width="470px"
               :close-on-click-modal="false" destroy-on-close>
      <el-form ref="valueFormRef" :model="valueForm" :rules="valueRules" label-width="90px">
        <el-form-item label="所属类型">
          <!-- 类型由选择器决定，弹窗内只读展示，避免误提交到错误类型 -->
          <div class="readonly-type">{{ selectedType?.typeName }} · <code>{{ selectedType?.typeCode }}</code></div>
        </el-form-item>
        <el-form-item label="字典值" prop="typeValue">
          <el-input v-model="valueForm.typeValue" placeholder="业务展示文案，如 01·获得线索" />
        </el-form-item>
        <el-form-item label="排序号" prop="order">
          <el-input-number v-model="valueForm.order" :min="0" :max="9999" style="width: 100%" />
          <div class="form-tip">
            排序号决定下拉与时间轴的展示顺序。当前类型已用排序号：{{ usedOrderText }}；
            建议新增值使用 <b>{{ nextOrderHint }}</b>，保持连续递增。
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="valueForm.remark" type="textarea" :rows="2" placeholder="可选说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="valueDialogVisible = false" :disabled="submitting">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitValueForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { doGet, doPost, doPut, doDelete } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'
import { messageFrame } from '@/util/util.js'
import {
  Search, Plus, Delete, EditPen, Files, Sort, Collection, MagicStick,
  Aim, FolderOpened, CircleCheck, WarningFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const pageSize = 10
const submitting = ref(false)

// ---- 类型选择器 ----
const typeOptions = ref([])
const selectedTypeCode = ref('')

const selectedType = computed(() =>
    typeOptions.value.find(t => t.typeCode === selectedTypeCode.value) || null)

const loadTypes = async () => {
  try {
    const res = await doGet('api/dictypes/all', {})
    if (res.data.code === 200) {
      typeOptions.value = res.data.data || []
      return true
    }
    ElMessage.error(res.data.msg || '加载字典类型失败')
  } catch (e) {
    console.error('加载字典类型失败', e)
    ElMessage.error('加载字典类型失败')
  }
  return false
}

/**
 * 类型切换的统一入口：
 * 深链 ?typeCode=xxx（目录页跳转/收藏链接）与手动下拉选择都收敛到这里，
 * 并把 typeCode 同步进路由 query（地址栏可复制分享、刷新不丢上下文）。
 */
const onTypeChange = (code) => {
  router.replace({ path: '/dashboard/dicvalue', query: code ? { typeCode: code } : {} }).catch(() => {})
  valueKeyword.value = ''
  if (code) {
    loadValueData(1)
  } else {
    valueList.value = []
    valueTotal.value = 0
  }
}

// 路由 query 变化（浏览器前进后退、目录页二次跳转）时同步选择器
watch(() => route.query.typeCode, (code) => {
  if (code && code !== selectedTypeCode.value) {
    selectedTypeCode.value = code
    loadValueData(1)
  }
})

const goCatalog = () => router.push('/dashboard/dictype')

// ---- 值列表 ----
const valueList = ref([])
const valueCurrent = ref(1)
const valueTotal = ref(0)
const valueLoading = ref(false)
const valueKeyword = ref('')

const loadValueData = async (current) => {
  if (!selectedTypeCode.value) return
  valueLoading.value = true
  valueCurrent.value = current
  try {
    const res = await doGet('api/dicvalues', {
      current,
      typeCode: selectedTypeCode.value,
      typeValue: valueKeyword.value.trim() || undefined
    })
    if (res.data.code === 200) {
      valueList.value = res.data.data.list
      valueTotal.value = res.data.data.total
    } else {
      ElMessage.error(res.data.msg || '加载字典值失败')
    }
  } catch (e) {
    console.error('加载字典值失败', e)
    ElMessage.error('加载字典值失败')
  } finally {
    valueLoading.value = false
  }
}

const searchValues = () => loadValueData(1)

const valueRowIndex = (index) => (valueCurrent.value - 1) * pageSize + index + 1

// ---- 排序号智能提示（解决 order 缺号/重号导致的展示乱序隐患） ----
const usedOrders = computed(() =>
    valueList.value.map(v => v.order).filter(o => o != null).sort((a, b) => a - b))

const usedOrderText = computed(() => usedOrders.value.length ? usedOrders.value.join('、') : '无')

const nextOrderHint = computed(() =>
    usedOrders.value.length ? Math.max(...usedOrders.value) + 1 : 1)

const orderRangeText = computed(() => {
  if (!selectedType.value || !usedOrders.value.length) return '—'
  const min = usedOrders.value[0]
  const max = usedOrders.value[usedOrders.value.length - 1]
  return min === max ? `${min}` : `${min} ~ ${max}`
})

/**
 * 逐行排序检查：重号或与相邻行存在断档时给出就地预警。
 * 「断档」只提示不判定为错误（order=1,3,4 是既有数据的常态，如 stage 缺 2），
 * 但重号一定标红——同 order 会导致排序不稳定、阶段推进判定失效。
 */
const orderWarning = (order) => {
  if (order == null) return { warn: true, label: '未设置', text: '该值未设置排序号，展示顺序将不稳定' }
  const dupCount = usedOrders.value.filter(o => o === order).length
  if (dupCount > 1) {
    return { warn: true, label: '重号', text: `排序号 ${order} 被 ${dupCount} 个值共用，将导致顺序不稳定，建议改为唯一值` }
  }
  return { warn: false }
}

// ---- 值 CRUD ----
const valueDialogVisible = ref(false)
const valueDialogTitle = ref('')
const valueFormRef = ref(null)
const valueForm = ref({ id: null, typeValue: '', order: 1, remark: '' })
const valueRules = {
  typeValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
  order: [{ required: true, message: '请填写排序号', trigger: 'blur' }]
}

const openValueAdd = () => {
  valueDialogTitle.value = `新增字典值 · ${selectedType.value?.typeName || ''}`
  // 缺省排序号 = 已用最大值 + 1（减少手动试错，用户仍可修改）
  valueForm.value = { id: null, typeValue: '', order: nextOrderHint.value, remark: '' }
  valueDialogVisible.value = true
}

const openValueEdit = (item) => {
  valueDialogTitle.value = '编辑字典值'
  valueForm.value = { id: item.id, typeValue: item.typeValue, order: item.order ?? 0, remark: item.remark || '' }
  valueDialogVisible.value = true
}

const submitValueForm = async () => {
  if (!valueFormRef.value || submitting.value) return
  await valueFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    const params = {
      typeCode: selectedTypeCode.value,
      typeValue: valueForm.value.typeValue.trim(),
      order: valueForm.value.order,
      remark: valueForm.value.remark
    }
    try {
      let res
      if (valueForm.value.id) {
        res = await doPut(`api/dicvalues/${valueForm.value.id}`, params)
      } else {
        res = await doPost('api/dicvalues', params)
      }
      if (res.data.code === 200) {
        ElMessage.success(valueForm.value.id ? '编辑成功' : '新增成功')
        valueDialogVisible.value = false
        loadValueData(valueCurrent.value)
        // 值数量变了 → 刷新选择器里的计数徽章
        refreshTypeCount()
      } else {
        ElMessage.error(res.data.msg || '操作失败')
      }
    } catch (e) {
      console.error('提交失败', e)
      ElMessage.error('操作失败，请稍后重试')
    } finally {
      submitting.value = false
    }
  })
}

// 局部刷新当前类型的值计数（避免整页重载选择器丢失选中态）
const refreshTypeCount = async () => {
  try {
    const res = await doGet('api/dictypes/all', {})
    if (res.data.code === 200) {
      typeOptions.value = res.data.data || []
    }
  } catch (e) { /* 计数刷新失败不阻断主流程 */ }
}

const confirmDeleteValue = (row) => {
  messageFrame(`删除字典值「${row.typeValue}」后不可恢复；若已被业务数据使用将无法删除。确定继续吗？`)
      .then(async () => {
        const res = await doDelete(`api/dicvalues/${row.id}`)
        if (res.data.code === 200) {
          ElMessage.success('删除成功')
          const remaining = valueTotal.value - 1
          const maxPage = Math.max(1, Math.ceil(remaining / pageSize))
          loadValueData(Math.min(valueCurrent.value, maxPage))
          refreshTypeCount()
        } else {
          // 后端已把外键 RESTRICT 转为「已被业务数据引用」友好提示
          ElMessage.error(res.data.msg || '删除失败')
        }
      }).catch(() => {})
}

onMounted(async () => {
  await loadTypes()
  // 深链优先：?typeCode=xxx 直接定位；未携带时默认选中第一个类型（工作台开箱可用）
  const code = route.query.typeCode
  if (code && typeOptions.value.some(t => t.typeCode === code)) {
    selectedTypeCode.value = code
    loadValueData(1)
  } else if (typeOptions.value.length) {
    selectedTypeCode.value = typeOptions.value[0].typeCode
    router.replace({ path: '/dashboard/dicvalue', query: { typeCode: selectedTypeCode.value } }).catch(() => {})
    loadValueData(1)
  }
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.dic-value-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

/* 类型选择器 */
.type-picker {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.picker-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 7px 14px;
  border-radius: 99px;
  background: var(--forest-soft);
  border: 1px solid var(--forest-border);
  font-size: 12.5px;
  color: var(--forest-muted);
}

.picker-meta__name {
  font-weight: 600;
  color: var(--forest-text);
}

.picker-meta code {
  color: var(--champagne);
  font-family: Georgia, 'Microsoft YaHei', serif;
}

.picker-meta__remark {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 下拉选项内部布局 */
.opt-name { font-weight: 600; color: var(--forest-text); }
.opt-code {
  margin-left: 8px;
  font-size: 12px;
  color: var(--champagne);
  font-family: Georgia, 'Microsoft YaHei', serif;
}
.opt-count {
  float: right;
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 99px;
}
.opt-count.is-filled { background: var(--forest-soft); color: #4f865b; }
.opt-count.is-empty { background: #faf5ec; color: #a47a4f; }

.count-badge {
  font-size: 12px;
  font-weight: 400;
  color: #4f865b;
  background: var(--forest-soft);
  border: 1px solid var(--forest-border);
  padding: 2px 10px;
  border-radius: 99px;
  margin-left: 6px;
}

.value-name {
  font-weight: 600;
  color: var(--forest-text);
  font-size: 13px;
}

.order-chip {
  display: inline-block;
  min-width: 30px;
  padding: 1px 8px;
  border-radius: 8px;
  background: #faf5ec;
  border: 1px solid #eee0cb;
  color: #a47a4f;
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-weight: 700;
  font-size: 12.5px;
}

.readonly-type {
  font-size: 13px;
  color: var(--forest-text);
  font-weight: 500;
}
.readonly-type code { color: var(--champagne); }

.form-tip {
  font-size: 11.5px;
  color: var(--forest-muted);
  line-height: 1.6;
  margin-top: 4px;
}
.form-tip b { color: #a47a4f; }

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 0;
  color: #93a89b;
  text-align: center;
}
.empty-state p { margin: 0; font-size: 13px; letter-spacing: 0.4px; max-width: 440px; }
.empty-state--tall { padding: 100px 0; }
</style>
