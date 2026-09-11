<template>
  <div class="module-shell tran-detail">

    <!-- ============ 详情 Hero：金额作为交易的首要信息被突出展示 ============ -->
    <div class="panel-card">
      <div class="panel-card__body">
        <div class="detail-hero">
          <div class="detail-hero__avatar detail-hero__avatar--money">
            ¥
          </div>

          <div class="detail-hero__main">
            <h3 class="detail-hero__name">
              {{ tran.customerName || '未关联客户' }}
              <span class="hero-tranno">{{ tran.tranNo || '—' }}</span>
            </h3>

            <!-- 金额作为交易的核心指标，用大号字体单独呈现（旧版与流水号同字号，轻重不分） -->
            <div class="hero-amount">{{ formatMoney(tran.money) }}</div>

            <div class="detail-hero__meta">
              <span>
                <el-icon :size="12"><User /></el-icon>
                创建人：{{ tran.createByDO?.name || '—' }}
              </span>
              <span>
                <el-icon :size="12"><Calendar /></el-icon>
                创建于：{{ tran.createTime || '—' }}
              </span>
              <span>
                <el-icon :size="12"><AlarmClock /></el-icon>
                下次联系：
                <span :class="{ 'overdue-text': isOverdue(tran.nextContactTime) }">
                  {{ tran.nextContactTime || '未设置' }}
                </span>
              </span>
            </div>
          </div>

          <div class="detail-hero__actions">
            <el-button type="primary" :icon="EditPen" @click="goEdit" v-hasPermission="'tran:edit'">
              编辑交易
            </el-button>
            <el-button type="danger" :icon="Delete" @click="delTran" v-hasPermission="'tran:delete'">
              删除
            </el-button>
            <el-button :icon="Back" @click="goBack">返回列表</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 阶段管道时间轴 ============
         【核心体验升级】旧版只用一行文本“当前：xx”描述阶段，销售看不出：
         ① 这笔交易在整个流程中走到了哪一步；② 还剩几步才成交。
         现用完整管道时间轴呈现（已完成/当前/未到达三态），并把阶段推进按钮放在同一张卡片，
         形成“看到位置 → 立即推进”的闭环。 -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Guide /></el-icon>
          <span>交易阶段管道</span>
          <el-tag v-if="currentStageIndex >= 0" size="small" type="success" effect="plain" round>
            {{ currentStageIndex + 1 }} / {{ stageList.length }}
          </el-tag>
        </div>
        <div class="stage-btns">
          <!-- 终态徽标：成交/丢失关闭后流程已定局，明确告知而非只把按钮藏掉 -->
          <span v-if="isTerminalState" class="state-tag" :class="terminalTagClass">
            <!-- 丢失关闭（order 最大）显 ✕ 红色；付款成交显 ✓ 绿色 -->
            <el-icon :size="11"><CircleClose v-if="isLostClosed" /><CircleCheck v-else /></el-icon>
            流程已定局：{{ stageMap[tran.stage]?.typeValue }}
          </span>
          <el-button type="primary" :icon="Right" v-if="nextStage" :disabled="stageChanging"
                     @click="showStageDialog(nextStage.id)" v-hasPermission="'tran:edit'">
            推进至：{{ nextStage.typeValue }}
          </el-button>
          <el-button type="warning" :icon="Back" v-if="prevStage" :disabled="stageChanging"
                     @click="showStageDialog(prevStage.id)" v-hasPermission="'tran:edit'">
            回退至：{{ prevStage.typeValue }}
          </el-button>
        </div>
      </div>

      <div class="panel-card__body">
        <!-- 时间轴：按字典 order 排序后逐段渲染，is-done / is-current / 未到达三态 -->
        <div class="stage-track" v-if="stageList.length > 0">
          <div v-for="(s, idx) in stageList" :key="s.id" class="stage-node"
               :class="{ 'is-done': idx < currentStageIndex, 'is-current': idx === currentStageIndex }">
            <div class="stage-node__dot">
              <el-icon v-if="idx < currentStageIndex" :size="13"><Select /></el-icon>
              <span v-else>{{ idx + 1 }}</span>
            </div>
            <div class="stage-node__label">{{ s.typeValue }}</div>
          </div>
        </div>

        <!-- 无阶段字典时的降级提示：避免用户面对空白区域不知发生什么 -->
        <el-empty v-else description="阶段字典未加载，无法展示管道" :image-size="70" />

        <!-- 阶段推进规则提示：后端限制“只能变更为相邻阶段”+“终态禁止推进”，前端显式告知避免反复碰壁 -->
        <div class="stage-note" v-if="stageList.length > 0">
          <el-icon :size="13"><InfoFilled /></el-icon>
          <span v-if="isTerminalState">
            该交易已处于终态，不可再向前推进；如属误操作可回退至前一阶段纠错。
          </span>
          <span v-else>阶段只能逐级推进或逐级回退（相邻变更），不可跳跃；「付款成交」「丢失关闭」为终态，到达后不可再推进。</span>
        </div>
      </div>
    </div>

    <!-- ============ 交易信息 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Tickets /></el-icon>
          <span>交易信息</span>
        </div>
        <span class="panel-card__hint">创建、预计成交与关联客户信息</span>
      </div>

      <div class="panel-card__body">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-item__label">交易流水号</span>
            <span class="detail-item__value">
              <a v-if="tran.tranNo" href="javascript:" class="forest-link">{{ tran.tranNo }}</a>
              <span v-else>—</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">关联客户</span>
            <span class="detail-item__value">{{ tran.customerName || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">交易金额</span>
            <span class="detail-item__value money-text">{{ formatMoney(tran.money) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">所处阶段</span>
            <span class="detail-item__value">{{ tran.stageDO?.typeValue || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">预计成交</span>
            <span class="detail-item__value">{{ tran.expectedDate ? String(tran.expectedDate).slice(0,10) : '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">下次联系</span>
            <span class="detail-item__value" :class="{ 'overdue-text': isOverdue(tran.nextContactTime) }">
              {{ tran.nextContactTime || '未设置' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">创建时间</span>
            <span class="detail-item__value">{{ tran.createTime || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-item__label">创建人</span>
            <span class="detail-item__value">{{ tran.createByDO?.name || '—' }}</span>
          </div>
          <div class="detail-item detail-item--full">
            <span class="detail-item__label">交易描述</span>
            <span class="detail-item__value">{{ tran.description || '—' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 阶段历史 / 跟踪记录 ============ -->
    <div class="panel-card forest-table">
      <div class="panel-card__body panel-card__body--flush">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane name="history">
            <template #label>
              <span class="tab-label">
                <el-icon :size="14"><List /></el-icon>
                阶段历史
                <el-tag v-if="historyList.length" size="small" effect="plain" round>{{ historyList.length }}</el-tag>
              </span>
            </template>
            <el-table v-loading="trLoading" :data="historyList" style="width: 100%;">
              <el-table-column type="index" label="#" width="54"/>
              <el-table-column label="阶段" min-width="130">
                <template #default="scope">
                  <span class="state-tag state-tag--green">{{ getStageName(scope.row.stage) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="128" align="right">
                <template #default="scope">
                  <span v-if="scope.row.money != null" class="money-cell">{{ formatMoney(scope.row.money) }}</span>
                  <span v-else class="empty-dash">—</span>
                </template>
              </el-table-column>
              <el-table-column label="预计成交" width="120">
                <template #default="scope">
                  {{ scope.row.expectedDate ? String(scope.row.expectedDate).slice(0,10) : '—' }}
                </template>
              </el-table-column>
              <el-table-column label="变更时间" min-width="158">
                <template #default="scope">{{ scope.row.createTime || '—' }}</template>
              </el-table-column>
              <el-table-column label="操作人" width="110">
                <template #default="scope">{{ scope.row.createByName || scope.row.createBy || '—' }}</template>
              </el-table-column>

              <template #empty>
                <div class="empty-state">
                  <el-icon :size="40"><List /></el-icon>
                  <p>暂无阶段变更记录</p>
                </div>
              </template>
            </el-table>
          </el-tab-pane>

          <el-tab-pane name="remark">
            <template #label>
              <span class="tab-label">
                <el-icon :size="14"><ChatLineSquare /></el-icon>
                跟踪记录
                <el-tag v-if="remarkTotal" size="small" effect="plain" round>{{ remarkTotal }}</el-tag>
              </span>
            </template>

            <!-- 录入区：与线索详情页一致的“先录入 → 下方即见历史”动线 -->
            <div class="remark-composer">
              <el-form ref="remarkRefForm" :model="remarkForm" :rules="remarkRules" label-width="0">
                <el-form-item prop="noteContent" class="composer-textarea">
                  <el-input v-model="remarkForm.noteContent" :rows="3" type="textarea"
                            maxlength="500" show-word-limit
                            placeholder="记录本次沟通的关键信息，如客户异议、报价反馈与下一步约定…"/>
                </el-form-item>
                <div class="composer-actions">
                  <el-form-item prop="noteWay" class="composer-way">
                    <el-select v-model="remarkForm.noteWay" placeholder="选择跟踪方式" style="width: 100%"
                               @click="loadNoteWayDic" clearable>
                      <el-option v-for="item in noteWayOptions" :key="item.id"
                                 :label="item.typeValue" :value="item.id"/>
                    </el-select>
                  </el-form-item>
                  <el-button type="primary" :icon="Plus" @click="submitRemark">提交记录</el-button>
                </div>
              </el-form>
            </div>

            <el-table v-loading="remarkLoading" :data="remarkList" style="width: 100%;">
              <el-table-column type="index" label="#" width="54"/>
              <el-table-column label="跟踪方式" width="116">
                <template #default="scope">
                  <span v-if="scope.row.noteWayName" class="state-tag state-tag--neutral">
                    {{ scope.row.noteWayName }}
                  </span>
                  <span v-else class="empty-dash">—</span>
                </template>
              </el-table-column>
              <el-table-column prop="noteContent" label="跟踪内容" min-width="240" show-overflow-tooltip/>
              <el-table-column label="跟踪人" width="96">
                <template #default="scope">{{ scope.row.createByName || '—' }}</template>
              </el-table-column>
              <el-table-column label="跟踪时间" width="158">
                <template #default="scope">{{ scope.row.createTime || '—' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="scope">
                  <el-button type="primary" link size="small" :icon="EditPen"
                             @click="editRemark(scope.row.id)" v-hasPermission="'tran:edit'">编辑</el-button>
                  <el-button type="danger" link size="small" :icon="Delete"
                             @click="delRemark(scope.row.id)" v-hasPermission="'tran:delete'">删除</el-button>
                </template>
              </el-table-column>

              <template #empty>
                <div class="empty-state">
                  <el-icon :size="40"><ChatDotSquare /></el-icon>
                  <p>还没有跟踪记录，在上方录入第一条吧</p>
                </div>
              </template>
            </el-table>

            <div class="pager-bar">
              <el-pagination
                  background
                  layout="total, prev, pager, next"
                  :page-size="remarkPageSize"
                  :total="remarkTotal"
                  @prev-click="toRemarkPage"
                  @next-click="toRemarkPage"
                  @current-change="toRemarkPage"/>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 【约定对齐】Dialog 放卡片外部并加 append-to-body + destroy-on-close，
         避免详情页卡片悬浮位移造成的层级遮挡与残影 -->
    <el-dialog v-model="stageDialogVisible" title="阶段变更确认" width="460px" center
               :append-to-body="true" :destroy-on-close="true">
      <el-form :model="stageForm" label-width="140px">
        <el-form-item label="变更后金额">
          <el-input v-model="stageForm.money" placeholder="不修改则留空"/>
        </el-form-item>
        <el-form-item label="变更后预计成交">
          <el-date-picker
              v-model="stageForm.expectedDate"
              type="date"
              style="width: 100%;"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="不修改则留空"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="stageChanging" @click="confirmChangeStage">确 认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editRemarkVisible" title="编辑跟踪记录" width="460px" center
               :append-to-body="true" :destroy-on-close="true">
      <el-form ref="editRemarkRefForm" :model="editRemarkForm" :rules="editRemarkRules" label-width="90px">
        <el-form-item label="跟踪方式" prop="noteWay">
          <el-select v-model="editRemarkForm.noteWay" style="width: 100%;" @click="loadNoteWayDic" clearable>
            <el-option v-for="item in noteWayOptions" :key="item.id" :label="item.typeValue" :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="跟踪内容" prop="noteContent">
          <el-input v-model="editRemarkForm.noteContent" type="textarea" :rows="4" maxlength="500" show-word-limit/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editRemarkVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmEditRemark">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'
import { messageFrame } from "../util/util.js"
import { ElMessageBox } from 'element-plus'
import {
  User, Calendar, AlarmClock, Guide, Tickets, List, ChatLineSquare,
  ChatDotSquare, Plus, Delete, EditPen, Select, InfoFilled, Right, Back,
  CircleCheck, CircleClose
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
// 说明：本页不再使用 inject('reload')——它会整页重挂 router-view，
// 丢失当前 tab、滚动位置与阶段管道状态；各写操作改为精准就地刷新。

const tran = ref({ stageDO: {}, createByDO: {} })
const activeTab = ref('history')

const historyList = ref([])
const prevStage = ref(null)
const nextStage = ref(null)
const stageMap = ref({})

// 阶段时间轴数据源（按字典 order 升序）与当前阶段下标
const stageList = ref([])
const currentStageIndex = computed(() =>
  stageList.value.findIndex(s => s.id === tran.value.stage))

/**
 * 终态判定（与后端 Constants.TRAN_TERMINAL_STAGE_DEPTH=2 同口径）：
 * stage 字典中 order 最大的前 2 个值视为终态（当前：06=05付款成交、07=06丢失关闭）。
 * 终态交易：① 不产生 nextStage（推进按钮隐藏）；② 后端 changeStage 同步拦截（双层防御）；
 * ③ 回退保留（纠错/重开丢失单）。
 * 不写死 order 值，字典新增阶段时终态语义自动漂移（与后端同一设计）。
 */
const TERMINAL_DEPTH = 2
const terminalOrders = computed(() => {
  const orders = stageList.value.map(s => s.order).filter(o => o != null)
  return orders.slice().sort((a, b) => b - a).slice(0, TERMINAL_DEPTH)
})
const isTerminalState = computed(() => {
  const cur = stageMap.value[tran.value.stage]
  return !!(cur && terminalOrders.value.includes(cur.order))
})
// 终局语义区分：order 最大的终态是「丢失关闭」（✕ 红），另一终态是「付款成交」（✓ 绿）。
// 【命名自纠】初版误名为 isDealClosed 并把图标用反（丢失显绿勾），已改为 isLostClosed 名实相符。
const isLostClosed = computed(() => {
  const cur = stageMap.value[tran.value.stage]
  if (!cur || !terminalOrders.value.length) return false
  return cur.order === terminalOrders.value[0]
})
const terminalTagClass = computed(() => isLostClosed.value ? 'state-tag--red' : 'state-tag--green')

const stageDialogVisible = ref(false)
const selectedStage = ref(null)
const stageForm = reactive({ money: '', expectedDate: '' })

// 各区域加载态与阶段变更提交中状态（防重复点击造成状态机双推进）
const trLoading = ref(false)
const remarkLoading = ref(false)
const stageChanging = ref(false)

const remarkList = ref([])
const remarkPageSize = ref(0)
const remarkTotal = ref(0)
const noteWayOptions = ref([])
const remarkRefForm = ref(null)
const editRemarkRefForm = ref(null)

const remarkForm = reactive({ noteContent: '', noteWay: '' })
const remarkRules = {
  noteContent: [{ required: true, message: '请填写跟踪内容', trigger: 'blur' }],
  noteWay: [{ required: true, message: '请选择跟踪方式', trigger: 'change' }]
}

const editRemarkVisible = ref(false)
const editRemarkForm = reactive({ id: '', noteWay: '', noteContent: '' })
const editRemarkRules = {
  noteContent: [{ required: true, message: '请填写跟踪内容', trigger: 'blur' }],
  noteWay: [{ required: true, message: '请选择跟踪方式', trigger: 'change' }]
}

const loadTran = () => {
  doGet(`/api/tran/${route.params.id}`).then(resp => {
    if (resp.data.code === 200) {
      tran.value = resp.data.data
      loadStages()
      loadHistory()
    }
  })
}

/**
 * 加载阶段字典并推导前/后一阶段。
 *
 * 【为何用 order 而非数组下标推导相邻阶段】后端 TranServiceImpl.changeStage 的校验是
 * {@code Math.abs(currentStage.getOrder() - targetStage.getOrder()) != 1}，
 * 即严格按字典的 order 值判定相邻。若前端改用数组下标相邻，一旦 order 存在断号
 * （如历史数据被删过），前端显示的“下一阶段”会与后端校验结果不一致，
 * 用户点了却报错“只能变更为相邻阶段”。故这里与后端保持同一套推导口径。
 *
 * stageList 仅服务于时间轴可视化（按位置渲染已完成/当前/未到达三态），
 * 与相邻阶段判定解耦，不参与业务校验。
 */
const loadStages = () => {
  doGet('/api/dicvalue/stage').then(resp => {
    if (resp.data.code === 200) {
      // slice() 避免就地 sort 修改响应原数组（该数组来自模块级 cacheMap 缓存）
      const stages = (resp.data.data || []).slice().sort((a, b) => (a.order || 0) - (b.order || 0))
      stageList.value = stages
      stageMap.value = {}
      stages.forEach(s => { stageMap.value[s.id] = s })
      const currentStage = stageMap.value[tran.value.stage]
      prevStage.value = null
      nextStage.value = null
      if (currentStage) {
        for (const s of stages) {
          if (s.order === currentStage.order - 1) prevStage.value = s
          if (s.order === currentStage.order + 1) nextStage.value = s
        }
        // 【终态规则】已成交/已丢失的交易不再提供推进入口：
        // 成交后再标丢失属业务错误（丢失关闭是另一终局，非后继步骤），
        // 后端 changeStage 同步拦截，前端隐藏按钮避免“可点却报错”的割裂体验。
        // 回退（prevStage）保留，承担纠错与重开丢失单的职责。
        const orders = stages.map(s => s.order).filter(o => o != null)
            .slice().sort((a, b) => b - a).slice(0, TERMINAL_DEPTH)
        if (orders.includes(currentStage.order)) {
          nextStage.value = null
        }
      }
    }
  })
}

const getStageName = (stageId) => {
  return stageMap.value[stageId]?.typeValue || stageId
}

const loadHistory = () => {
  trLoading.value = true
  doGet(`/api/tran/${route.params.id}/history`).then(resp => {
    if (resp.data.code === 200) {
      historyList.value = resp.data.data
    }
  }).finally(() => {
    trLoading.value = false
  })
}

const loadRemark = (current) => {
  remarkLoading.value = true
  doGet('/api/tran/remark', { current, tranId: route.params.id }).then(resp => {
    if (resp.data.code === 200) {
      remarkList.value = resp.data.data.list
      remarkPageSize.value = resp.data.data.pageSize
      remarkTotal.value = resp.data.data.total
    }
  }).finally(() => {
    remarkLoading.value = false
  })
}

const toRemarkPage = (current) => { loadRemark(current) }

const loadNoteWayDic = () => {
  doGet('/api/dicvalue/noteWay').then(resp => {
    if (resp.data.code === 200) {
      noteWayOptions.value = resp.data.data
    }
  })
}

const submitRemark = async () => {
  if (!remarkRefForm.value) return
  await remarkRefForm.value.validate(async (valid) => {
    if (valid) {
      const resp = await doPost('/api/tran/remark', {
        tranId: tran.value.id,
        noteContent: remarkForm.noteContent,
        noteWay: remarkForm.noteWay
      })
      if (resp.data.code === 200) {
        messageFrame('提交成功', 'success')
        // 清空录入框 + 就地刷新列表，新记录立即可见，且不打断用户所处 tab
        remarkForm.noteContent = ''
        remarkForm.noteWay = ''
        remarkRefForm.value.clearValidate()
        loadRemark(1)
      } else {
        messageFrame('提交失败，原因：' + resp.data.msg, 'error')
      }
    }
  })
}

const editRemark = (id) => {
  const record = remarkList.value.find(item => item.id === id)
  if (record) {
    editRemarkForm.id = record.id
    editRemarkForm.noteWay = record.noteWay
    editRemarkForm.noteContent = record.noteContent
    editRemarkVisible.value = true
  }
}

const confirmEditRemark = async () => {
  if (!editRemarkRefForm.value) return
  await editRemarkRefForm.value.validate(async (valid) => {
    if (valid) {
      const resp = await doPut('/api/tran/remark', {
        id: editRemarkForm.id,
        noteWay: editRemarkForm.noteWay,
        noteContent: editRemarkForm.noteContent
      })
      if (resp.data.code === 200) {
        messageFrame('更新成功', 'success')
        editRemarkVisible.value = false
        loadRemark(1)
      } else {
        messageFrame('更新失败', 'error')
      }
    }
  })
}

const delRemark = (id) => {
  ElMessageBox.confirm('删除后不可恢复，确定删除吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const resp = await doDelete(`/api/tran/remark/${id}`, {})
    if (resp.data.code === 200) {
      messageFrame('删除成功', 'success')
      loadRemark(1)
    } else {
      messageFrame('删除失败', 'error')
    }
  }).catch(() => {})
}

const showStageDialog = (stageId) => {
  selectedStage.value = stageId
  stageForm.money = ''
  stageForm.expectedDate = ''
  stageDialogVisible.value = true
}

const confirmChangeStage = async () => {
  // stageChanging 兼做防重锁：后端 changeStage 是“改交易 + 写历史 + 推阶段”的事务，
  // 连击两次可能把同一笔交易推进两个阶段，必须在前端阻断重复提交。
  if (stageChanging.value) return
  stageChanging.value = true
  try {
    const resp = await doPost('/api/tran/stage', {
      tranId: tran.value.id,
      stage: selectedStage.value,
      money: stageForm.money || undefined,
      expectedDate: stageForm.expectedDate || undefined
    })
    if (resp.data.code === 200) {
      messageFrame('阶段变更成功', 'success')
      stageDialogVisible.value = false
      // 就地刷新：loadTran 内部会连带 loadStages（重算管道与时间轴）与 loadHistory
      loadTran()
      loadRemark(1)
    } else {
      // 后端业务校验失败（如非相邻阶段）会返回具体原因，直接展示不再笼统说“失败”
      messageFrame('阶段变更失败：' + (resp.data.msg || '未知原因'), 'error')
    }
  } catch (error) {
    console.error('阶段变更异常', error)
    messageFrame('操作异常，请稍后重试', 'error')
  } finally {
    stageChanging.value = false
  }
}

const delTran = () => {
  ElMessageBox.confirm('删除后不可恢复，确定删除该交易吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const resp = await doDelete(`/api/tran/${route.params.id}`, {})
    if (resp.data.code === 200) {
      messageFrame('删除成功', 'success')
      window.history.back()
    } else {
      messageFrame('删除失败', 'error')
    }
  }).catch(() => {})
}

const goBack = () => { window.history.back() }

// ============ 详情页派生展示逻辑 ============

/** 交易金额格式化：千分位 + 2 位小数，与看板/交易列表口径一致 */
const formatMoney = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (isNaN(n)) return String(val)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 是否已逾期：下次联系时间早于当前时刻 */
const isOverdue = (val) => {
  if (!val) return false
  const t = new Date(String(val).replace(' ', 'T'))
  return !isNaN(t.getTime()) && t.getTime() < Date.now()
}

/** 跳转到交易编辑页（Hero 新增快捷入口） */
const goEdit = () => {
  router.push(`/dashboard/tran/edit/${route.params.id}`)
}

onMounted(() => {
  loadTran()
  loadRemark(1)
  loadNoteWayDic()
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.tran-detail { min-height: 100%; box-sizing: border-box; }

/* Hero 金额展示：大号字凸显交易价值 */
.detail-hero__avatar--money {
  font-size: 26px;
  background: linear-gradient(135deg, #b08d5e 0%, #d5b381 100%);
  box-shadow: 0 8px 20px rgba(176, 141, 94, 0.3);
}
.hero-tranno {
  display: inline-block;
  margin-left: 10px;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 500;
  color: #6d8272;
  background: #eef2f0;
  border: 1px solid #dfe7e2;
  font-family: monospace;
}
.hero-amount {
  margin: 8px 0 4px;
  font-size: 30px;
  font-weight: 700;
  color: #98704a;
  font-family: Georgia, 'Microsoft YaHei', serif;
  line-height: 1.1;
}

/* 阶段推进按钮组 */
.stage-btns { display: flex; gap: 10px; flex-wrap: wrap; }

/* 阶段推进规则提示 */
.stage-note {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 4px;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--forest-soft);
  border: 1px solid var(--forest-border);
  color: #6d8272;
  font-size: 12.5px;
}
.stage-note .el-icon { color: var(--forest-accent); }

/* tabs */
.detail-tabs :deep(.el-tabs__item) { font-size: 14px; }
.tab-label { display: inline-flex; align-items: center; gap: 6px; }

/* 信息分组竖线标记 */
.info-group__label em {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #5f936b, #a8cdb1);
}

/* 金额单元格 */
.money-cell, .money-text {
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-weight: 600;
  color: #98704a;
}

/* 跟进记录录入区 */
.remark-composer {
  padding: 18px 20px;
  border-bottom: 1px solid var(--forest-soft);
  background: rgba(245, 250, 246, 0.5);
}
.remark-composer :deep(.el-form-item) { margin-bottom: 0; }
.composer-textarea { margin-bottom: 14px !important; }
.composer-actions { display: flex; align-items: center; gap: 12px; }
.composer-way { flex: 0 0 220px; }

/* 逾期高亮 */
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
