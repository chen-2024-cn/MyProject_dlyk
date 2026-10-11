<template>
  <div class="module-shell product-module">

    <!-- ============ 页面 Hero ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">CATALOG · PRODUCT LINEUP</span>
        <h2>产品管理</h2>
        <p>维护在售产品线与价格体系：指导价区间、经销商报价与上下架状态</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ isFiltering ? '已启用条件筛选' : '产品目录实时同步中' }}
      </div>
    </section>

    <!-- ============ 概览指标 ============ -->
    <div class="module-metrics">
      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><Goods /></el-icon></div>
        <div>
          <div class="metric-tile__label">{{ isFiltering ? '匹配产品' : '产品总数' }}</div>
          <div class="metric-tile__value">{{ total }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><CircleCheck /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页在售</div>
          <div class="metric-tile__value">{{ onSaleOnPage }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><PriceTag /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页平均报价</div>
          <div class="metric-tile__value">{{ avgQuotation }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
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
          <span>产品筛选</span>
        </div>
        <span class="panel-card__hint">名称模糊匹配，状态精确过滤（服务端生效）</span>
      </div>

      <div class="panel-card__body">
        <div class="filter-grid">
          <div class="filter-item">
            <label class="filter-item__label">产品名称</label>
            <el-input v-model="filters.name" placeholder="如 秦PLUS" clearable
                      :prefix-icon="Search" @keyup.enter="doSearch" />
          </div>

          <div class="filter-item">
            <label class="filter-item__label">上架状态</label>
            <el-select v-model="filters.state" placeholder="全部状态" clearable
                       style="width: 100%" @change="doSearch">
              <el-option label="在售" :value="0" />
              <el-option label="售罄" :value="1" />
            </el-select>
          </div>
        </div>

        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          <span v-if="isFiltering" class="toolbar__selected">已生效 {{ activeFilterCount }} 项条件</span>
        </div>
      </div>
    </div>

    <!-- ============ 产品清单 ============ -->
    <div class="panel-card forest-table">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Goods /></el-icon>
          <span>产品清单</span>
        </div>

        <div class="toolbar">
          <span v-if="selectedIds.length > 0" class="toolbar__selected">已选中 {{ selectedIds.length }} 个</span>
          <el-button type="primary" :icon="Plus" @click="openAddDialog" v-hasPermission="'product:add'">
            添加产品
          </el-button>
          <!-- 【功能补齐】批量删除：后端本轮新增 /api/products/batch-delete，
               与线索/交易/活动/用户模块能力对齐；含被引用产品时整体回滚并提示 -->
          <el-button type="danger" :icon="Delete" :disabled="!selectedIds.length"
                     @click="batchDelete" v-hasPermission="'product:delete'">
            批量删除
          </el-button>
        </div>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table v-loading="tableLoading" :data="productList" style="width: 100%"
                  @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" label="#" width="54" :index="rowIndex" />
          <el-table-column label="产品名称" min-width="150" show-overflow-tooltip>
            <template #default="scope">
              <span class="product-name">{{ scope.row.name }}</span>
            </template>
          </el-table-column>
          <!-- 【语义澄清】价格单位为「万元」（实测量纲：海鸥 10.18~10.58），
               原版直接拼 ¥ 前缀会让 10.18 万元被误读成 10 元 -->
          <el-table-column label="指导价区间（万元）" min-width="185" align="right">
            <template #default="scope">
              <span class="money-cell">{{ formatWan(scope.row.guidePriceS) }}</span>
              <span class="range-sep">~</span>
              <span class="money-cell">{{ formatWan(scope.row.guidePriceE) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="经销商报价" width="128" align="right">
            <template #default="scope">
              <span class="money-cell money-cell--quote">{{ formatWan(scope.row.quotation) }}</span>
            </template>
          </el-table-column>
          <!-- 【功能显性化】报价相对指导价下限的差额：全部产品报价低于指导价是行业常态，
               把"优惠空间"直接算给销售看，解决原版三列数字并排却看不出关系的问题 -->
          <el-table-column label="较指导价" width="112" align="center">
            <template #default="scope">
              <span v-if="discountText(scope.row)" class="state-tag"
                    :class="discountTag(scope.row)">{{ discountText(scope.row) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="96" align="center">
            <template #default="scope">
              <span class="state-tag" :class="scope.row.state === 0 ? 'state-tag--green' : 'state-tag--neutral'">
                <em class="phase-dot" :class="scope.row.state === 0 ? 'dot-green' : 'dot-neutral'"></em>
                {{ scope.row.state === 0 ? '在售' : '售罄' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="152">
            <template #default="scope">
              <span v-if="scope.row.createTime">{{ formatDate(scope.row.createTime) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="scope">
              <el-button type="success" link size="small" :icon="EditPen"
                         @click="handleEdit(scope.row)" v-hasPermission="'product:edit'">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete"
                         @click="handleDelete(scope.row)" v-hasPermission="'product:delete'">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <el-icon :size="44"><FolderOpened /></el-icon>
              <p v-if="isFiltering">没有符合当前条件的产品</p>
              <p v-else>还没有产品，点击「添加产品」上架第一款</p>
              <el-button v-if="isFiltering" size="small" @click="resetFilters">清除筛选条件</el-button>
              <el-button v-else size="small" type="primary" @click="openAddDialog" v-hasPermission="'product:add'">
                上架第一款产品
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

    <!-- ============ 新增/编辑对话框 ============ -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px"
               :close-on-click-modal="false" destroy-on-close>
      <el-form ref="dialogFormRef" :model="productForm" :rules="dialogRules" label-width="130px">
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="如 秦PLUS DM-i" />
        </el-form-item>
        <el-form-item label="官方指导起始价" prop="guidePriceS">
          <el-input v-model="productForm.guidePriceS" placeholder="单位：万元">
            <template #suffix>万元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="官方指导最高价" prop="guidePriceE">
          <el-input v-model="productForm.guidePriceE" placeholder="单位：万元，须 ≥ 起始价">
            <template #suffix>万元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="经销商报价" prop="quotation">
          <el-input v-model="productForm.quotation" placeholder="单位：万元">
            <template #suffix>万元</template>
          </el-input>
          <!-- 实时价格关系提示：让录入者在提交前就看到报价与指导价的关系 -->
          <div v-if="formPriceHint" class="form-tip">{{ formPriceHint }}</div>
        </el-form-item>
        <el-form-item label="上架状态" prop="state">
          <el-radio-group v-model="productForm.state">
            <el-radio :value="0">在售</el-radio>
            <el-radio :value="1">售罄（下架，不再出现在意向产品下拉）</el-radio>
          </el-radio-group>
          <div class="form-tip">售罄产品仍保留客户历史关联，可安全替代「删除已被引用的产品」</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false" :disabled="submitting">取 消</el-button>
          <!-- 【修复】原版无提交锁，双击产生重复产品 -->
          <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { doGet, doPost, doPut, doDelete, doPostJson } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'
import { messageFrame } from '@/util/util.js'
import {
  Search, Refresh, Plus, Delete, EditPen, Filter, Goods, PriceTag,
  CircleCheck, Select, FolderOpened
} from '@element-plus/icons-vue'

const filters = reactive({
  name: '',
  state: ''
})

const isFiltering = computed(() => filters.name !== '' || filters.state !== '')
const activeFilterCount = computed(() =>
    [filters.name, filters.state].filter(v => v !== '' && v != null).length)

const productList = ref([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const tableLoading = ref(false)
const selectedIds = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const dialogFormRef = ref(null)

const initProductForm = () => ({
  id: null,
  name: '',
  guidePriceS: '',
  guidePriceE: '',
  quotation: '',
  state: 0
})

const productForm = reactive(initProductForm())

// 金额格式校验（非负 + 最多两位小数），三个价格字段共用
const priceValidator = (rule, value, callback) => {
  if (!value) return callback()
  if (!/^[0-9]+(\.[0-9]{1,2})?$/.test(String(value))) {
    callback(new Error('金额必须为非负数，最多两位小数'))
  } else {
    callback()
  }
}

// 与后端 validateProduct 同口径的区间校验：起始价 ≤ 最高价
const rangeValidator = (rule, value, callback) => {
  const s = parseFloat(productForm.guidePriceS)
  const e = parseFloat(productForm.guidePriceE)
  if (!isNaN(s) && !isNaN(e) && s > e) {
    callback(new Error('指导起始价不能高于最高价'))
  } else {
    callback()
  }
}

const dialogRules = {
  name: [
    { required: true, message: '请输入产品名称', trigger: 'blur' },
    { max: 50, message: '名称不超过 50 个字符', trigger: 'blur' }
  ],
  guidePriceS: [
    { required: true, message: '请输入官方指导起始价', trigger: 'blur' },
    { validator: priceValidator, trigger: 'blur' },
    { validator: rangeValidator, trigger: 'blur' }
  ],
  guidePriceE: [
    { required: true, message: '请输入官方指导最高价', trigger: 'blur' },
    { validator: priceValidator, trigger: 'blur' },
    { validator: rangeValidator, trigger: 'blur' }
  ],
  quotation: [
    { required: true, message: '请输入经销商报价', trigger: 'blur' },
    { validator: priceValidator, trigger: 'blur' }
  ],
  state: [{ required: true, message: '请选择上架状态', trigger: 'change' }]
}

// ---------- 数据获取 ----------
const getData = async (current) => {
  tableLoading.value = true
  currentPage.value = current
  try {
    const params = { current }
    if (filters.name) params.name = filters.name.trim()
    if (filters.state !== '') params.state = filters.state
    const response = await doGet('api/products', params)
    if (response.data.code === 200) {
      productList.value = response.data.data.list
      pageSize.value = response.data.data.pageSize || pageSize.value
      total.value = response.data.data.total
    } else {
      ElMessage.error(response.data.msg || '获取产品列表失败')
    }
  } catch (e) {
    console.error('获取产品列表失败', e)
    ElMessage.error('获取产品列表失败')
  } finally {
    tableLoading.value = false
  }
}

const doSearch = () => getData(1)

const resetFilters = () => {
  filters.name = ''
  filters.state = ''
  getData(1)
}

const toPage = (current) => getData(current)

const rowIndex = (index) => (currentPage.value - 1) * pageSize.value + index + 1

const formatDate = (val) => (val ? String(val).slice(0, 16) : '')

// ---------- 价格展示辅助 ----------
const formatWan = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (isNaN(n)) return String(val)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/**
 * 报价相对指导价下限的差额文案（万元）。
 * 实测库内全部产品报价低于指导价下限（终端优惠是行业常态），
 * 故「低 X.XX」显示为绿色让利标签；若报价高于上限则金色提示溢价。
 */
const diffWan = (row) => {
  const q = Number(row.quotation)
  const s = Number(row.guidePriceS)
  const e = Number(row.guidePriceE)
  if (isNaN(q) || isNaN(s) || isNaN(e)) return null
  if (q < s) return q - s          // 负数：低于指导价
  if (q > e) return q - e          // 正数：高于指导价上限
  return 0                          // 落在区间内
}

const discountText = (row) => {
  const d = diffWan(row)
  if (d == null) return ''
  if (d === 0) return '区间内'
  return d < 0 ? `低 ${Math.abs(d).toFixed(2)}` : `高 ${d.toFixed(2)}`
}

const discountTag = (row) => {
  const d = diffWan(row)
  if (d == null || d === 0) return 'state-tag--neutral'
  return d < 0 ? 'state-tag--green' : 'state-tag--gold'
}

// 对话框内实时价格关系提示
const formPriceHint = computed(() => {
  const q = parseFloat(productForm.quotation)
  const s = parseFloat(productForm.guidePriceS)
  const e = parseFloat(productForm.guidePriceE)
  if (isNaN(q) || isNaN(s) || isNaN(e)) return ''
  if (q < s) return `当前报价比指导价下限低 ${(s - q).toFixed(2)} 万元（让利 ${(100 * (s - q) / s).toFixed(1)}%）`
  if (q > e) return `注意：当前报价高于指导价上限 ${(q - e).toFixed(2)} 万元`
  return '当前报价落在指导价区间内'
})

// ---------- 本页统计 ----------
const onSaleOnPage = computed(() => productList.value.filter(p => p.state === 0).length)

const avgQuotation = computed(() => {
  const quotes = productList.value.map(p => Number(p.quotation)).filter(n => !isNaN(n))
  if (!quotes.length) return '—'
  return (quotes.reduce((a, b) => a + b, 0) / quotes.length).toFixed(2)
})

// ---------- 对话框 ----------
const openAddDialog = () => {
  dialogTitle.value = '添加产品'
  Object.assign(productForm, initProductForm())
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑产品'
  productForm.id = row.id
  productForm.name = row.name || ''
  productForm.guidePriceS = row.guidePriceS != null ? String(row.guidePriceS) : ''
  productForm.guidePriceE = row.guidePriceE != null ? String(row.guidePriceE) : ''
  productForm.quotation = row.quotation != null ? String(row.quotation) : ''
  productForm.state = row.state != null ? row.state : 0
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!dialogFormRef.value || submitting.value) return
  await dialogFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    const params = {
      name: productForm.name.trim(),
      guidePriceS: productForm.guidePriceS,
      guidePriceE: productForm.guidePriceE,
      quotation: productForm.quotation,
      state: productForm.state
    }
    try {
      let res
      if (productForm.id) {
        res = await doPut(`api/products/${productForm.id}`, params)
      } else {
        res = await doPost('api/products', params)
      }
      if (res.data.code === 200) {
        ElMessage.success(productForm.id ? '编辑成功' : '新增成功')
        dialogVisible.value = false
        getData(currentPage.value)
      } else {
        // 后端价格校验（区间倒挂/负数）文案直接透出
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

const handleDelete = (row) => {
  messageFrame(`确定删除产品「${row.name}」吗？若已被客户意向引用将无法删除（可改为「售罄」下架）。`)
      .then(async () => {
        const res = await doDelete(`api/products/${row.id}`)
        if (res.data.code === 200) {
          ElMessage.success('删除成功')
          refreshAfterMutate(1)
        } else {
          // 后端已把外键 RESTRICT 转为「被引用请改售罄」的友好提示
          ElMessage.warning(res.data.msg || '删除失败')
        }
      }).catch(() => {})
}

const batchDelete = () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的产品')
    return
  }
  messageFrame(`确定删除选中的 ${selectedIds.value.length} 个产品吗？任一产品被客户意向引用时整批不会删除。`)
      .then(async () => {
        const count = selectedIds.value.length
        const res = await doPostJson('api/products/batch-delete', { ids: selectedIds.value })
        if (res.data.code === 200) {
          ElMessage.success(res.data.msg || `成功删除 ${count} 个产品`)
          selectedIds.value = []
          refreshAfterMutate(count)
        } else {
          ElMessage.warning(res.data.msg || '批量删除失败')
        }
      }).catch(() => {})
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

onMounted(() => {
  getData(1)
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.product-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

.product-name {
  font-weight: 600;
  color: var(--forest-text);
  font-size: 13px;
}

/* 金额单元格：等宽衬线便于纵向比对；报价用香槟金突出 */
.money-cell {
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-weight: 600;
  color: #4f865b;
}
.money-cell--quote { color: #98704a; }

.range-sep {
  margin: 0 6px;
  color: #b3c2b8;
}

/* 状态圆点（与活动模块同构） */
.phase-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.phase-dot.dot-green   { background: #69a675; box-shadow: 0 0 0 3px rgba(105,166,117,.15); }
.phase-dot.dot-neutral { background: #a9b7ae; }

.form-tip {
  font-size: 11.5px;
  color: var(--forest-muted);
  line-height: 1.6;
  margin-top: 4px;
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
</style>
