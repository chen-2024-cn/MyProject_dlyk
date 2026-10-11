<template>
  <div class="module-shell dic-type-module">

    <!-- ============ 页面 Hero ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">SYSTEM · DICTIONARY CATALOG</span>
        <h2>字典类型目录</h2>
        <p>全系统枚举数据源的总览：点击任一类型进入字典数据页，维护它的具体取值</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ typeQuery.typeCode || typeQuery.typeName ? '已启用目录筛选' : '字典类型目录' }}
      </div>
    </section>

    <!-- ============ 概览指标 ============ -->
    <div class="module-metrics">
      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><Collection /></el-icon></div>
        <div>
          <div class="metric-tile__label">字典类型总数</div>
          <div class="metric-tile__value">{{ filteredTypes.length }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><Files /></el-icon></div>
        <div>
          <div class="metric-tile__label">字典值总量</div>
          <div class="metric-tile__value">{{ totalValues }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><WarningFilled /></el-icon></div>
        <div>
          <div class="metric-tile__label">待维护空类型</div>
          <div class="metric-tile__value">{{ emptyTypeCount }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
        <div class="metric-tile__icon"><el-icon :size="18"><DataAnalysis /></el-icon></div>
        <div>
          <div class="metric-tile__label">最大类型值数</div>
          <div class="metric-tile__value">{{ maxTypeValueCount }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 目录工具条 ============ -->
    <div class="panel-card">
      <div class="panel-card__body">
        <div class="catalog-toolbar">
          <el-input v-model="typeQuery.typeCode" placeholder="按类型代码筛选，如 stage" clearable
                    :prefix-icon="Search" style="max-width: 240px" />
          <el-input v-model="typeQuery.typeName" placeholder="按类型名称筛选，如 阶段" clearable
                    :prefix-icon="PriceTag" style="max-width: 240px" />
          <div class="catalog-toolbar__spacer"></div>
          <!-- 「仅看待维护」快速过滤：数据质量巡检常用入口 -->
          <el-checkbox v-model="onlyEmpty" border size="default">仅看待维护空类型</el-checkbox>
          <el-button type="primary" :icon="Plus" @click="openTypeAdd" v-hasPermission="'dictype:add'">
            新增类型
          </el-button>
        </div>
      </div>
    </div>

    <!-- ============ 类型卡片网格 ============ -->
    <div v-loading="typeLoading" class="type-grid-wrap">
      <div v-if="filteredTypes.length" class="type-grid">
        <div v-for="item in filteredTypes" :key="item.id"
             class="type-card" :class="{ 'type-card--empty': !item.valueCount }"
             @click="goMaintain(item)">
          <div class="type-card__top">
            <span class="type-card__name">{{ item.typeName }}</span>
            <span class="type-card__count" :class="item.valueCount ? 'is-filled' : 'is-empty'">
              {{ item.valueCount || 0 }} 值
            </span>
          </div>
          <code class="type-card__code">{{ item.typeCode }}</code>
          <p class="type-card__remark">{{ item.remark || '暂无说明' }}</p>

          <!-- 数据质量提示：空类型说明该枚举尚未配置，业务下拉将为空 -->
          <div v-if="!item.valueCount" class="type-card__warn">
            <el-icon :size="12"><WarningFilled /></el-icon>
            尚未维护字典值，业务下拉将为空
          </div>

          <div class="type-card__foot" @click.stop>
            <el-button link type="primary" size="small" :icon="Setting"
                       @click="goMaintain(item)">维护字典值</el-button>
            <el-button link type="success" size="small" :icon="EditPen"
                       @click="openTypeEdit(item)" v-hasPermission="'dictype:edit'">编辑</el-button>
            <el-button link type="danger" size="small" :icon="Delete"
                       @click="confirmDeleteType(item)" v-hasPermission="'dictype:delete'">删除</el-button>
          </div>
        </div>
      </div>

      <div v-else-if="!typeLoading" class="panel-card">
        <div class="empty-state">
          <el-icon :size="46"><FolderOpened /></el-icon>
          <p v-if="onlyEmpty">没有待维护的空类型，字典配置完整 👍</p>
          <p v-else>没有符合条件的字典类型</p>
          <el-button size="small" @click="clearFilters">清除筛选</el-button>
        </div>
      </div>
    </div>

    <!-- ============ 类型新增/编辑 弹窗 ============ -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="460px"
               :close-on-click-modal="false" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="类型代码" prop="typeCode">
          <el-input v-model="typeForm.typeCode" placeholder="如 stage / source（字母开头，创建后不建议改）" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="typeForm.typeName" placeholder="如 交易阶段 / 线索来源" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" placeholder="用途说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false" :disabled="submitting">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitTypeForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { doGet, doPost, doPut, doDelete } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'
import { messageFrame } from '@/util/util.js'
import {
  Search, Plus, Delete, EditPen, Collection, Files, DataAnalysis, Setting,
  WarningFilled, PriceTag, FolderOpened
} from '@element-plus/icons-vue'

const router = useRouter()
const typeLoading = ref(false)
const submitting = ref(false)
const allTypes = ref([])
const typeQuery = reactive({ typeCode: '', typeName: '' })
const onlyEmpty = ref(false)

// 目录筛选：代码/名称模糊 + 「仅看待维护」开关（前端过滤，类型总量有限无需请求）
const filteredTypes = computed(() => {
  const code = typeQuery.typeCode.trim().toLowerCase()
  const name = typeQuery.typeName.trim()
  return allTypes.value.filter(t => {
    if (onlyEmpty.value && t.valueCount) return false
    if (code && !t.typeCode.toLowerCase().includes(code)) return false
    if (name && !(t.typeName || '').includes(name)) return false
    return true
  })
})

const totalValues = computed(() => allTypes.value.reduce((s, t) => s + (t.valueCount || 0), 0))
const emptyTypeCount = computed(() => allTypes.value.filter(t => !t.valueCount).length)
const maxTypeValueCount = computed(() => allTypes.value.reduce((m, t) => Math.max(m, t.valueCount || 0), 0))

const loadTypes = async () => {
  typeLoading.value = true
  try {
    // /api/dictypes/all：不分页全量类型，带每类字典值数量（本轮新增）
    const res = await doGet('api/dictypes/all', {})
    if (res.data.code === 200) {
      allTypes.value = res.data.data || []
    } else {
      ElMessage.error(res.data.msg || '加载字典类型失败')
    }
  } catch (e) {
    console.error('加载字典类型失败', e)
    ElMessage.error('加载字典类型失败')
  } finally {
    typeLoading.value = false
  }
}

const clearFilters = () => {
  typeQuery.typeCode = ''
  typeQuery.typeName = ''
  onlyEmpty.value = false
}

// 跳转「字典数据」维护页，深链携带 typeCode 直接定位到该类型
const goMaintain = (item) => {
  router.push({ path: '/dashboard/dicvalue', query: { typeCode: item.typeCode } })
}

// ---- 类型 CRUD ----
const typeDialogVisible = ref(false)
const typeDialogTitle = ref('')
const typeFormRef = ref(null)
const typeForm = reactive({ id: null, typeCode: '', typeName: '', remark: '' })
const typeRules = {
  typeCode: [
    { required: true, message: '请输入类型代码', trigger: 'blur' },
    { pattern: /^[A-Za-z][A-Za-z0-9_]*$/, message: '以字母开头，仅含字母/数字/下划线', trigger: 'blur' }
  ],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

const openTypeAdd = () => {
  typeDialogTitle.value = '新增字典类型'
  typeForm.id = null; typeForm.typeCode = ''; typeForm.typeName = ''; typeForm.remark = ''
  typeDialogVisible.value = true
}

const openTypeEdit = (item) => {
  typeDialogTitle.value = '编辑字典类型'
  typeForm.id = item.id; typeForm.typeCode = item.typeCode
  typeForm.typeName = item.typeName; typeForm.remark = item.remark || ''
  typeDialogVisible.value = true
}

const submitTypeForm = async () => {
  if (!typeFormRef.value || submitting.value) return
  await typeFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    const params = { typeCode: typeForm.typeCode.trim(), typeName: typeForm.typeName, remark: typeForm.remark }
    try {
      let res
      if (typeForm.id) {
        res = await doPut(`api/dictypes/${typeForm.id}`, params)
      } else {
        res = await doPost('api/dictypes', params)
      }
      if (res.data.code === 200) {
        ElMessage.success(typeForm.id ? '编辑成功' : '新增成功')
        typeDialogVisible.value = false
        loadTypes()
      } else {
        // 后端把「编码重复」转为精准业务文案，直接展示
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

const confirmDeleteType = (item) => {
  // 有值的类型后端会拦截（外键 RESTRICT + 前置校验），此处预置提示减少无效点击
  const warn = item.valueCount
      ? `\n该类型下有 ${item.valueCount} 个字典值，需先到「字典数据」页清空。`
      : ''
  messageFrame(`删除字典类型「${item.typeName}」后不可恢复，确定继续吗？${warn}`).then(async () => {
    const res = await doDelete(`api/dictypes/${item.id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      loadTypes()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadTypes()
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.dic-type-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

/* 目录工具条 */
.catalog-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.catalog-toolbar__spacer { flex: 1; }

/* 类型卡片网格 */
.type-grid-wrap {
  position: relative;
  z-index: 1;
  min-height: 200px;
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(255px, 1fr));
  gap: 16px;
}

.type-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 17px 12px;
  border: 1px solid var(--card-border);
  border-radius: 16px;
  background: var(--card-bg);
  box-shadow: var(--card-shadow);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  overflow: hidden;
}

.type-card::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #5f936b, #a8cdb1);
  opacity: 0.75;
}

.type-card:hover {
  transform: translateY(-4px);
  border-color: #bcd6c2;
  box-shadow: var(--card-shadow-hover);
}

/* 空类型：左边条改香槟金，提示待维护 */
.type-card--empty::before {
  background: linear-gradient(180deg, #ae8255, #d6b189);
}

.type-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.type-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--forest-text);
  letter-spacing: 0.4px;
}

.type-card__count {
  flex-shrink: 0;
  font-size: 11.5px;
  padding: 2px 9px;
  border-radius: 99px;
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-weight: 700;
}
.type-card__count.is-filled {
  background: var(--forest-soft);
  color: #4f865b;
  border: 1px solid var(--forest-border);
}
.type-card__count.is-empty {
  background: #faf5ec;
  color: #a47a4f;
  border: 1px solid #eee0cb;
}

.type-card__code {
  font-size: 12.5px;
  color: var(--champagne);
  font-family: Georgia, 'Microsoft YaHei', serif;
  background: rgba(174, 130, 85, 0.07);
  padding: 1px 8px;
  border-radius: 6px;
  align-self: flex-start;
}

.type-card__remark {
  margin: 0;
  font-size: 12.5px;
  color: var(--forest-muted);
  line-height: 1.5;
  min-height: 36px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.type-card__warn {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: #a47a4f;
  background: #faf5ec;
  border: 1px solid #eee0cb;
  border-radius: 8px;
  padding: 5px 9px;
}

.type-card__foot {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  margin-top: 2px;
  padding-top: 8px;
  border-top: 1px dashed var(--forest-border);
  cursor: default;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 70px 0;
  color: #93a89b;
  text-align: center;
}
.empty-state p { margin: 0; font-size: 13px; letter-spacing: 0.4px; max-width: 420px; }

@media (max-width: 768px) {
  .type-grid { grid-template-columns: 1fr; }
  .catalog-toolbar__spacer { flex-basis: 100%; height: 0; }
}
</style>
