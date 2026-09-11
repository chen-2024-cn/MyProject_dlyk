<template>
  <div class="module-shell clue-module">

    <!-- ============ 页面 Hero：明确告知“这是什么模块、能做什么” ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">JOURNEY · LEAD POOL</span>
        <h2>线索管理</h2>
        <p>销售旅程的起点：录入、跟进、筛选潜在客户，并把成熟线索转化为客户</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ isFiltering ? '已启用条件筛选' : '线索池实时同步中' }}
      </div>
    </section>

    <!-- ============ 概览指标：让“数字说话”，筛选后即时反馈匹配规模 ============ -->
    <div class="module-metrics">
      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><Files /></el-icon></div>
        <div>
          <div class="metric-tile__label">{{ isFiltering ? '匹配线索' : '线索总数' }}</div>
          <div class="metric-tile__value">{{ total }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><CircleCheck /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页已转化</div>
          <div class="metric-tile__value">{{ convertedCount }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><Clock /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页待跟进</div>
          <div class="metric-tile__value">{{ pendingCount }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
        <div class="metric-tile__icon"><el-icon :size="18"><UserFilled /></el-icon></div>
        <div>
          <div class="metric-tile__label">已选中</div>
          <div class="metric-tile__value">{{ clueIdArray.length }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 筛选面板：补齐线索模块长期缺失的检索能力 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Filter /></el-icon>
          <span>线索筛选</span>
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

          <div class="filter-item">
            <label class="filter-item__label">转化状态</label>
            <el-select v-model="filters.converted" placeholder="全部" clearable
                       style="width: 100%" @change="doSearch">
              <!-- 对应后端 ClueQuery.converted：true=已转客户(state=-1)，false=仍在跟进 -->
              <el-option label="仍在跟进" :value="false" />
              <el-option label="已转客户" :value="true" />
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
          <el-icon :size="16"><Grid /></el-icon>
          <span>线索清单</span>
        </div>

        <div class="toolbar">
          <span v-if="clueIdArray.length > 0" class="toolbar__selected">
            已选中 {{ clueIdArray.length }} 条
          </span>
          <el-button type="primary" :icon="Plus" @click="addClue" v-hasPermission="'clue:add'">
            录入线索
          </el-button>
          <!-- 【权限对齐】后端 /api/importExcel 要求 clue:import，旧版按钮误用 clue:add，
               会导致仅有 clue:add 的用户看到按钮、点击后却收到 403，已修正为同一权限码 -->
          <el-button type="success" :icon="Upload" @click="importExcel" v-hasPermission="'clue:import'">
            Excel 导入
          </el-button>
          <el-button type="danger" :icon="Delete" @click="batchDelClue" v-hasPermission="'clue:delete'">
            批量删除
          </el-button>
        </div>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table
            v-loading="loading"
            :data="clueList"
            style="width: 100%"
            @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column type="index" label="#" width="54" :index="rowIndex" />
          <el-table-column label="姓名" min-width="96" fixed>
            <template #default="scope">
              <a href="javascript:" class="forest-link" @click="view(scope.row.id)">
                {{ scope.row.fullName || '—' }}
              </a>
            </template>
          </el-table-column>
          <el-table-column property="appellationDO.typeValue" label="称呼" width="70" />
          <el-table-column property="phone" label="手机" width="116" />
          <el-table-column property="ownerDO.name" label="负责人" width="90" />
          <el-table-column property="activityDO.name" label="所属活动" min-width="128" show-overflow-tooltip />
          <el-table-column label="意向状态" width="102">
            <template #default="scope">
              <span v-if="scope.row.intentionStateDO?.typeValue"
                    class="state-tag" :class="intentionTagType(scope.row.intentionState)">
                {{ scope.row.intentionStateDO.typeValue }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column property="intentionProductDO.name" label="意向产品" min-width="108" show-overflow-tooltip />
          <el-table-column label="贷款需求" width="92">
            <template #default="scope">
              <span v-if="scope.row.needLoanDO?.typeValue"
                    class="state-tag" :class="scope.row.needLoan === 1 ? 'state-tag--gold' : 'state-tag--neutral'">
                {{ scope.row.needLoanDO.typeValue }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="线索状态" width="106">
            <template #default="scope">
              <!-- state = -1 是“已转客户”的业务魔法值（非字典项），需单独渲染为语义标签 -->
              <span v-if="scope.row.state === -1" class="state-tag state-tag--green">
                <el-icon :size="11"><Check /></el-icon>已转客户
              </span>
              <span v-else-if="scope.row.stateDO?.typeValue" class="state-tag state-tag--blue">
                {{ scope.row.stateDO.typeValue }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column property="sourceDO.typeValue" label="来源" width="90" />
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
          <el-table-column label="操作" width="178" fixed="right">
            <template #default="scope">
              <el-button type="primary" link size="small" :icon="View"
                         @click="view(scope.row.id)" v-hasPermission="'clue:view'">详情</el-button>
              <el-button type="success" link size="small" :icon="EditPen"
                         @click="edit(scope.row.id)" v-hasPermission="'clue:edit'">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete"
                         @click="del(scope.row.id)" v-hasPermission="'clue:delete'">删除</el-button>
            </template>
          </el-table-column>

          <!-- 空状态：区分“没有数据”与“筛选无结果”，并给出一键出口 -->
          <template #empty>
            <div class="empty-state">
              <el-icon :size="44"><FolderOpened /></el-icon>
              <p v-if="isFiltering">没有符合当前条件的线索</p>
              <p v-else>线索池还是空的</p>
              <el-button v-if="isFiltering" size="small" @click="resetFilters">清除筛选条件</el-button>
              <el-button v-else size="small" type="primary" @click="addClue" v-hasPermission="'clue:add'">
                录入第一条线索
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

    <!-- 导入线索 Excel 弹窗：放卡片外部并 append-to-body，避免卡片悬浮位移导致弹窗抖动 -->
    <el-dialog v-model="importExcelDialogVisible" title="导入线索 Excel" width="620px" center
               :append-to-body="true" :destroy-on-close="true">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 14px;"
                title="第一行将被视为字段名；文件大小不超过 5MB" />
      <el-upload
          ref="uploadRef"
          method="post"
          drag
          accept=".xls,.xlsx"
          :limit="1"
          :http-request="uploadFile"
          :auto-upload="false">
        <template #trigger>
          <el-icon :size="42" class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">将 Excel 拖到此处，或 <em>点击选择文件</em></div>
        </template>
        <template #tip>
          <div class="upload-tips">
            <div><b>格式要求：</b>仅支持 .xls / .xlsx</div>
            <div><b>日期字段：</b>yyyy-MM-dd；日期时间字段：yyyy-MM-dd HH:mm:ss</div>
            <div><b>负责人：</b>Excel 中的 owner_id 必须是系统内已存在的用户 ID</div>
          </div>
        </template>
      </el-upload>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importExcelDialogVisible = false">关 闭</el-button>
          <el-button class="ml-3" type="success" :loading="uploading" @click="submitUpload">开始导入</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {doDelete, doGet, doUploadFile} from '../http/httpRequest'
import { messageFrame } from "@/util/util.js";
import {
  Search, Refresh, Plus, Upload, Delete, EditPen, View, Check,
  Files, CircleCheck, Clock, UserFilled, Filter, Grid, FolderOpened, UploadFilled, Iphone
} from '@element-plus/icons-vue'

// 路由
const router = useRouter()
// 说明：本页不再注入 reload()——它会整页重挂 router-view，导致用户的筛选条件
// 与所在页码全部丢失。改为 getData() 就地刷新，保留完整操作上下文。

// 数据
const clueList = ref([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const loading = ref(false)
const uploading = ref(false)
const importExcelDialogVisible = ref(false)
const clueIdArray = ref([])

// 上传组件 ref
const uploadRef = ref(null)

// 筛选条件（全部可选；不传即等价于原来的全量分页行为）
const filters = reactive({
  fullName: '',
  phone: '',
  ownerId: '',
  source: '',
  intentionState: '',
  converted: ''
})

// 筛选下拉的数据源（负责人复用 /api/owner 带 Redis 缓存；字典项走服务端 cacheMap）
const ownerOptions = ref([])
const sourceOptions = ref([])
const intentionOptions = ref([])

// 是否处于筛选态：决定指标语义与空状态文案
const isFiltering = computed(() => !!(filters.fullName || filters.phone || filters.ownerId
    || filters.source || filters.intentionState || filters.converted !== ''))
const activeFilterCount = computed(() => [filters.fullName, filters.phone, filters.ownerId,
      filters.source, filters.intentionState].filter(v => v !== '' && v != null).length
    + (filters.converted === '' ? 0 : 1))

// 本页已转化 / 待跟进（标签明确写“本页”，避免被误读为全量统计）
const convertedCount = computed(() => clueList.value.filter(c => c.state === -1).length)
const pendingCount = computed(() => clueList.value
  .filter(c => c.state !== -1 && c.nextContactTime).length)

// 组装查询参数：仅携带用户实际填写的条件，避免空串导致后端拼出无意义的 like '%%'
const buildParams = (current) => {
  const params = { current }
  if (filters.fullName) params.fullName = filters.fullName.trim()
  if (filters.phone) params.phone = filters.phone.trim()
  if (filters.ownerId) params.ownerId = filters.ownerId
  if (filters.source) params.source = filters.source
  if (filters.intentionState) params.intentionState = filters.intentionState
  if (filters.converted !== '') params.converted = filters.converted
  return params
}

/**
 * 获取线索分页列表数据。
 *
 * 【移除页级缓存的理由】旧实现把每页结果永久缓存在 Map 里，仅靠增删改时手动 clear。
 * 但线索数据是多销售共享的：别人新增了线索、或后端字典/负责人变了，
 * 本页看到的仍是陈旧快照，且用户无法主动刷新。数据一致性比省一次请求更重要，
 * 改为每次真实拉取 + loading 反馈（列表页每页仅 10–50 条，开销可忽略）。
 */
const getData = (current) => {
  currentPage.value = current
  loading.value = true
  doGet('/api/clues', buildParams(current)).then(resp => {
    if (resp.data.code === 200) {
      clueList.value = resp.data.data.list
      pageSize.value = resp.data.data.pageSize || pageSize.value
      total.value = resp.data.data.total
      currentPage.value = resp.data.data.pageNum || current
    }
  }).finally(() => {
    loading.value = false
  })
}

// 加载筛选下拉的数据源
const loadFilterOptions = () => {
  doGet('/api/owner').then(resp => {
    if (resp.data.code === 200) ownerOptions.value = resp.data.data || []
  })
  doGet('/api/dicvalue/source').then(resp => {
    if (resp.data.code === 200) sourceOptions.value = resp.data.data || []
  })
  doGet('/api/dicvalue/intentionState').then(resp => {
    if (resp.data.code === 200) intentionOptions.value = resp.data.data || []
  })
}

// 查询（回到第一页，因为筛选条件已变）
const doSearch = () => {
  getData(1)
}

// 重置筛选
const resetFilters = () => {
  filters.fullName = ''
  filters.phone = ''
  filters.ownerId = ''
  filters.source = ''
  filters.intentionState = ''
  filters.converted = ''
  getData(1)
}

// 分页函数
const page = (number) => {
  getData(number)
}

// 连续序号：跳页保持递增（第 2 页从 11 开始而非 1）
const rowIndex = (index) => (currentPage.value - 1) * pageSize.value + index + 1

// 日期格式化：去掉秒级噪音，列表只保留到分
const formatDate = (val) => {
  if (!val) return ''
  return String(val).slice(0, 16)
}

// 是否已逾期：下次联系时间早于当前时刻
const isOverdue = (val) => {
  if (!val) return false
  const t = new Date(String(val).replace(' ', 'T'))
  return !isNaN(t.getTime()) && t.getTime() < Date.now()
}

// 意向状态标签配色：按字典文本语义分档，让“意向强弱”一眼可辨
const intentionTagType = (intentionStateId) => {
  const name = intentionOptions.value.find(d => d.id === intentionStateId)?.typeValue || ''
  if (name.includes('高')) return 'state-tag--green'
  if (name.includes('中')) return 'state-tag--gold'
  if (name.includes('低')) return 'state-tag--neutral'
  return 'state-tag--blue'
}

/**
 * 增删改后的列表刷新策略。
 * 关键细节：若删除后当前页已空且不是第 1 页，自动回退一页——
 * 否则用户会停在一个空白页上，误以为“数据丢光了”。
 */
const refreshAfterMutate = (removedCount) => {
  const remaining = total.value - removedCount
  const maxPage = Math.max(1, Math.ceil(remaining / pageSize.value))
  getData(Math.min(currentPage.value, maxPage))
}

// 录入线索
const addClue = () => {
  router.push('/dashboard/clue/add')
}

// 编辑线索
const edit = (id) => {
  router.push(`/dashboard/clue/edit/${id}`)
}

// 导入线索Excel（打开弹窗）
const importExcel = () => {
  importExcelDialogVisible.value = true
}

// 自定义文件上传逻辑
const uploadFile = (param) => {
  const fileObj = param.file
  const formData = new FormData()
  formData.append('file', fileObj)
  uploading.value = true
  doUploadFile('/api/importExcel', formData).then(resp => {
    if (resp.data.code === 200) {
      messageFrame('导入成功', 'success')
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
      importExcelDialogVisible.value = false
      // 导入后回到第一页：新导入的记录按 create_time desc 排在最前，
      // 用户能立即看到刚导入的数据（旧版整页 reload 会同时丢掉筛选条件）
      getData(1)
    } else {
      messageFrame('导入失败：' + resp.data.msg, 'error')
    }
  }).finally(() => {
    uploading.value = false
  })
}

// 提交上传
const submitUpload = () => {
  if (uploadRef.value) {
    uploadRef.value.submit()
  }
}

// 单个删除
const del = (id) => {
  messageFrame('删除后不可恢复，您确定要删除这条线索吗？').then(() => {
    doDelete(`/api/clue/${id}`, {}).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('删除成功', 'success')
        refreshAfterMutate(1)
      } else {
        messageFrame('删除失败，原因：' + resp.data.msg, 'error')
      }
    })
  }).catch(() => {})
}

// 批量删除
const batchDelClue = () => {
  if (clueIdArray.value.length <= 0) {
    messageFrame('请先勾选要删除的线索', 'warning')
    return
  }
  // 二次确认文案带上条数：批量删除是高危不可逆操作，必须让用户看清影响范围
  messageFrame(`删除后不可恢复，确定删除选中的 ${clueIdArray.value.length} 条线索吗？`).then(() => {
    const count = clueIdArray.value.length
    const ids = clueIdArray.value.join(',')
    doDelete('/api/clue/batch', { ids }).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('批量删除成功', 'success')
        refreshAfterMutate(count)
      } else {
        messageFrame('批量删除失败，原因：' + resp.data.msg, 'error')
      }
    })
  }).catch(() => {})
}

// 表格勾选变化
const handleSelectionChange = (dataObjectArray) => {
  clueIdArray.value = dataObjectArray.map(item => item.id)
}

// 查看详情
const view = (id) => {
  router.push(`/dashboard/clue/${id}`)
}

// 初始化加载数据
onMounted(() => {
  getData(1)
  loadFilterOptions()
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.clue-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

/* 逾期时间高亮：提醒销售“这条线索该联系了” */
.overdue-text { color: #c2564e; font-weight: 600; }

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 44px 0;
  color: #93a89b;
}
.empty-state p { margin: 0; font-size: 13px; letter-spacing: 0.4px; }

/* 上传区 */
.upload-icon { color: #7ba082; }
.upload-text { margin-top: 10px; color: #6d8272; font-size: 13px; }
.upload-text em { color: #5f936b; font-style: normal; font-weight: 600; }
.upload-tips {
  margin-top: 4px;
  font-size: 12px;
  color: #88a094;
  line-height: 1.9;
}
.upload-tips b { color: #6d8272; font-weight: 600; }
</style>