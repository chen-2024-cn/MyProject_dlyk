<template>
  <div class="module-shell clue-detail">

    <!-- ============ 详情 Hero：一屏看清“这是谁、什么状态、能做什么” ============ -->
    <div class="panel-card">
      <div class="panel-card__body">
        <div class="detail-hero">
          <div class="detail-hero__avatar">
            {{ avatarText }}
          </div>

          <div class="detail-hero__main">
            <h3 class="detail-hero__name">
              {{ clueDetail.fullName || '未命名线索' }}
              <span v-if="clueDetail.appellationDO?.typeValue" class="hero-appellation">
                {{ clueDetail.appellationDO.typeValue }}
              </span>
            </h3>

            <div class="detail-hero__meta">
              <span>
                <el-icon :size="12"><User /></el-icon>
                负责人：{{ clueDetail.ownerDO?.name || '—' }}
              </span>
              <span>
                <el-icon :size="12"><ShoppingBag /></el-icon>
                活动：{{ clueDetail.activityDO?.name || '—' }}
              </span>
              <span>
                <el-icon :size="12"><Iphone /></el-icon>
                {{ clueDetail.phone || '—' }}
              </span>
            </div>

            <!-- 状态标签组：把字典文本升级为语义化彩色标签 -->
            <div class="hero-tags">
              <!-- state = -1 是“已转客户”的业务魔法值（非字典项），单独渲染 -->
              <span v-if="clueDetail.state === -1" class="state-tag state-tag--green">
                <el-icon :size="11"><Check /></el-icon>已转客户
              </span>
              <span v-else-if="clueDetail.stateDO?.typeValue" class="state-tag state-tag--blue">
                {{ clueDetail.stateDO.typeValue }}
              </span>
              <span v-if="clueDetail.intentionStateDO?.typeValue"
                    class="state-tag" :class="intentionTagByText(clueDetail.intentionStateDO.typeValue)">
                意向：{{ clueDetail.intentionStateDO.typeValue }}
              </span>
              <span v-if="clueDetail.sourceDO?.typeValue" class="state-tag state-tag--neutral">
                来源：{{ clueDetail.sourceDO.typeValue }}
              </span>
              <span v-if="clueDetail.nextContactTime"
                    class="state-tag" :class="isOverdue(clueDetail.nextContactTime) ? 'state-tag--red' : 'state-tag--gold'">
                <el-icon :size="11"><Clock /></el-icon>
                {{ isOverdue(clueDetail.nextContactTime) ? '已逾期：' : '待联系：' }}
                {{ clueDetail.nextContactTime }}
              </span>
            </div>
          </div>

          <!-- 主动作区：转化客户是本模块最核心的业务动作，放在首屏最高优先级位置 -->
          <div class="detail-hero__actions">
            <el-button type="success" :icon="Promotion" @click="convertCustomer"
                       v-if="clueDetail.state !== -1" v-hasPermission="'clue:edit'">
              转化为客户
            </el-button>
            <el-button type="primary" :icon="EditPen" @click="goEdit" v-hasPermission="'clue:edit'">
              编辑线索
            </el-button>
            <el-button :icon="Back" @click="goBack">返回列表</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 信息分组：把 19 个平铺字段按认知分组，降低阅读负荷 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Document /></el-icon>
          <span>画像档案</span>
        </div>
        <span class="panel-card__hint">基础资料、联系方式与意向画像</span>
      </div>

      <div class="panel-card__body">
        <!-- 分组一：基础资料 -->
        <div class="info-group">
          <div class="info-group__label"><em></em>基础资料</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">姓名</span>
              <span class="detail-item__value">{{ clueDetail.fullName || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">称呼</span>
              <span class="detail-item__value">{{ clueDetail.appellationDO?.typeValue || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">年龄</span>
              <span class="detail-item__value">{{ clueDetail.age || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">职业</span>
              <span class="detail-item__value">{{ clueDetail.job || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">年收入</span>
              <span class="detail-item__value">{{ formatIncome(clueDetail.yearIncome) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">住址</span>
              <span class="detail-item__value">{{ clueDetail.address || '—' }}</span>
            </div>
          </div>
        </div>

        <!-- 分组二：联系方式 -->
        <div class="info-group">
          <div class="info-group__label"><em></em>联系方式</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">手机</span>
              <span class="detail-item__value">{{ clueDetail.phone || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">微信</span>
              <span class="detail-item__value">{{ clueDetail.weixin || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">QQ</span>
              <span class="detail-item__value">{{ clueDetail.qq || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">邮箱</span>
              <span class="detail-item__value">{{ clueDetail.email || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">下次联系</span>
              <span class="detail-item__value"
                    :class="{ 'overdue-text': isOverdue(clueDetail.nextContactTime) }">
                {{ clueDetail.nextContactTime || '未设置' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 分组三：意向画像 -->
        <div class="info-group">
          <div class="info-group__label"><em></em>意向画像</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">负责人</span>
              <span class="detail-item__value">{{ clueDetail.ownerDO?.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">所属活动</span>
              <span class="detail-item__value">{{ clueDetail.activityDO?.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">贷款需求</span>
              <span class="detail-item__value">
                <span v-if="clueDetail.needLoanDO?.typeValue" class="state-tag"
                      :class="needLoanTagByText(clueDetail.needLoanDO.typeValue)">
                  {{ clueDetail.needLoanDO.typeValue }}
                </span>
                <span v-else class="empty-dash">—</span>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">意向产品</span>
              <span class="detail-item__value">{{ clueDetail.intentionProductDO?.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">线索来源</span>
              <span class="detail-item__value">{{ clueDetail.sourceDO?.typeValue || '—' }}</span>
            </div>
            <div class="detail-item detail-item--full">
              <span class="detail-item__label">线索描述</span>
              <span class="detail-item__value">{{ clueDetail.description || '—' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 跟进记录：独立卡片 ============
         【交互修正】旧版把“填写跟踪记录”的表单嵌在信息展示表单内部，
         看上去像是线索本身的一个字段，而非一个独立操作——功能定位模糊。
         现拆为独立卡片：先填记录列表（历史轨迹），再提供录入区（当前动作）。 -->
    <div class="panel-card forest-table">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><ChatLineSquare /></el-icon>
          <span>跟进记录</span>
          <el-tag v-if="total > 0" size="small" type="success" effect="plain" round>
            {{ total }} 条
          </el-tag>
        </div>
        <span class="panel-card__hint">每一次跟进都是客户信任的累积，请及时记录</span>
      </div>

      <!-- 录入区：置于卡片内部顶部，形成“先录入 → 下方即见历史”的自然动线 -->
      <div class="panel-card__body remark-composer">
        <el-form ref="clueRemarkRefForm" :model="clueRemark" :rules="clueRemarkRules" label-width="0">
          <el-form-item prop="noteContent" class="composer-textarea">
            <el-input v-model="clueRemark.noteContent" :rows="4" type="textarea"
                      maxlength="500" show-word-limit
                      placeholder="记录本次沟通的关键信息，如客户需求、异议点与下一步约定…" />
          </el-form-item>
          <div class="composer-actions">
            <el-form-item prop="noteWay" class="composer-way">
              <el-select v-model="clueRemark.noteWay" placeholder="选择跟踪方式" style="width: 100%"
                         @click="loadDicValue('noteWay')" clearable>
                <el-option v-for="item in noteWayOptions" :key="item.id"
                           :label="item.typeValue" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :icon="Plus" @click="clueRemarkSubmit">提交记录</el-button>
          </div>
        </el-form>
      </div>

      <!-- 历史轨迹 -->
      <div class="panel-card__body panel-card__body--flush">
        <el-table v-loading="remarkLoading" :data="clueRemarkList" style="width: 100%">
          <el-table-column type="index" label="#" width="54" />
          <el-table-column label="跟踪方式" width="116">
            <template #default="scope">
              <span v-if="scope.row.noteWayName" class="state-tag state-tag--neutral">
                {{ scope.row.noteWayName }}
              </span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="noteContent" label="跟踪内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="跟踪人" width="96">
            <template #default="scope">{{ scope.row.createByName || '—' }}</template>
          </el-table-column>
          <el-table-column label="跟踪时间" width="158">
            <template #default="scope">
              <span>{{ scope.row.createTime || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="最后编辑" width="180">
            <template #default="scope">
              <!-- 未编辑过时显示占位，而非旧版那样留一个空白单元格让人困惑“是丢了数据还是没改过” -->
              <span v-if="scope.row.editTime">
                {{ scope.row.editTime }}
                <span class="edit-by">{{ scope.row.editByName ? '(' + scope.row.editByName + ')' : '' }}</span>
              </span>
              <span v-else class="empty-dash">未编辑</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="scope">
              <el-button type="primary" link size="small" :icon="EditPen"
                         @click="edit(scope.row.id)" v-hasPermission="'clue:edit'">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete"
                         @click="del(scope.row.id)" v-hasPermission="'clue:delete'">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <el-icon :size="40"><ChatDotSquare /></el-icon>
              <p>还没有跟进记录，在上方录入第一条吧</p>
            </div>
          </template>
        </el-table>
      </div>

      <div class="pager-bar">
        <el-pagination background layout="total, prev, pager, next"
                       :page-size="pageSize" :total="total"
                       @prev-click="toPage" @next-click="toPage" @current-change="toPage" />
      </div>
    </div>

  <!-- 线索转换为客户的弹窗 -->
  <!-- 【约定对齐】Dialog 必须放卡片外部并加 append-to-body + destroy-on-close：
       详情页卡片带悬浮位移与层叠上下文，不外挂会出现弹窗层级遮挡/残影；
       destroy-on-close 同时防止表单状态在多次打开之间残留 -->
  <el-dialog v-model="convertCustomerDialogVisible" title="线索转化为客户" width="560px" center
             :append-to-body="true" :destroy-on-close="true">
    <el-form ref="convertCustomerRefForm" :model="customerQuery" label-width="110px" :rules="convertCustomerRules">
      <el-form-item label="意向产品" prop="product">
        <el-select v-model="customerQuery.product" placeholder="请选择" style="width: 100%;" @click="loadDicValue('product')">
          <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="客户描述" prop="description">
        <el-input
            v-model="customerQuery.description"
            :rows="8"
            type="textarea"
            placeholder="请输入客户描述"/>
      </el-form-item>
      <el-form-item label="下次跟踪时间" prop="nextContactTime">
        <el-date-picker
            v-model="customerQuery.nextContactTime"
            type="datetime"
            style="width: 100%;"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择下次跟踪时间"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="convertCustomerDialogVisible = false">关 闭</el-button>
        <el-button type="primary" @click="convertCustomerSubmit">转 换</el-button>
      </span>
    </template>
  </el-dialog>

  <!-- 编辑跟踪记录的弹窗 -->
  <el-dialog v-model="editDialogVisible" title="编辑跟踪记录" width="560px" center
             :append-to-body="true" :destroy-on-close="true">
    <el-form ref="editRemarkRefForm" :model="editRemarkData" label-width="110px" :rules="editRemarkRules">
      <el-form-item label="跟踪方式" prop="noteWay">
        <el-select v-model="editRemarkData.noteWay" placeholder="请选择跟踪方式" style="width: 100%;" @click="loadDicValue('noteWay')" clearable>
          <el-option
              v-for="item in noteWayOptions"
              :key="item.id"
              :label="item.typeValue"
              :value="item.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="跟踪内容" prop="noteContent">
        <el-input v-model="editRemarkData.noteContent" type="textarea" :rows="6" placeholder="请输入跟踪内容"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="updateRemark">确 定</el-button>
      </span>
    </template>
  </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {doDelete, doGet, doPost, doPut} from "../http/httpRequest.js"
import { goBack, messageFrame } from "../util/util.js"
import {
  User, ShoppingBag, Iphone, Check, Clock, Document, ChatLineSquare,
  ChatDotSquare, Plus, Delete, EditPen, Back, Promotion
} from '@element-plus/icons-vue'

// 路由和刷新注入
const route = useRoute()
const router = useRouter()
const reload = inject('reload')

// 跟进记录表格加载态
const remarkLoading = ref(false)

// -------------- 响应式数据 --------------
// 线索详情
const clueDetail = ref({
  ownerDO: {},
  activityDO: {},
  appellationDO: {},
  needLoanDO: {},
  intentionStateDO: {},
  intentionProductDO: {},
  stateDO: {},
  sourceDO: {}
})

// 跟踪记录表单
const clueRemark = reactive({
  noteContent: '',
  noteWay: ''
})
// 跟踪记录列表
const clueRemarkList = ref([])
// 分页相关
const pageSize = ref(0)
const total = ref(0)

// 下拉选项
const noteWayOptions = ref([])
const productOptions = ref([])

// 转换客户弹窗相关
const convertCustomerDialogVisible = ref(false)
const customerQuery = reactive({
  product: '',
  description: '',
  nextContactTime: ''
})
const convertCustomerRules = {
  product: [{ required: true, message: '请选择意向产品', trigger: 'change' }],
  description: [
    { required: true, message: '客户描述不能为空', trigger: 'blur' },
    { min: 5, max: 255, message: '客户描述长度为5-255个字符', trigger: 'blur' }
  ],
  nextContactTime: [{ required: true, message: '请选择下次联系时间', trigger: 'change' }]
}

// 编辑跟踪记录相关
const editDialogVisible = ref(false)
const editRemarkData = reactive({
  id: '',
  noteWay: '',
  noteContent: ''
})
const editRemarkRules = {
  noteWay: [{ required: true, message: '请选择跟踪方式', trigger: 'change' }],
  noteContent: [
    { required: true, message: '跟踪内容不能为空', trigger: 'blur' },
    { min: 1, max: 500, message: '跟踪内容长度在1-500个字符', trigger: 'blur' }
  ]
}

// 表单 ref
const clueRemarkRefForm = ref(null)
const convertCustomerRefForm = ref(null)
const editRemarkRefForm = ref(null)

// ============ 详情页派生展示逻辑 ============

/** Hero 头像占位：取姓名首字（空值时用“客”兜底，避免圆形头像完全空白） */
const avatarText = computed(() => {
  const name = clueDetail.value.fullName
  return name ? String(name).charAt(0).toUpperCase() : '客'
})

/**
 * 意向状态标签配色。
 * 基于字典文本而非 ID 推导：详情页未加载意向字典列表，
 * 而后端直接下发 stateDO.typeValue 文本，用文本判定即可且不依赖额外请求。
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

/** 年收入格式化：千分位 + 万元单位提示（空值给占位符而非空白） */
const formatIncome = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (isNaN(n)) return String(val)
  return '¥' + n.toLocaleString('zh-CN')
}

/** 跳转到线索编辑页（详情页新增的快捷入口，避免“想看完整字段还得回列表”） */
const goEdit = () => {
  router.push(`/dashboard/clue/edit/${route.params.id}`)
}

// 跟踪记录表单验证规则（补充）
const clueRemarkRules = {
  noteContent: [
    { required: true, message: '请填写跟踪记录', trigger: 'blur' },
    { min: 1, max: 500, message: '跟踪记录长度在1-500个字符', trigger: 'blur' }
  ],
  noteWay: [{ required: true, message: '请选择跟踪方式', trigger: 'change' }]
}

// -------------- 方法 --------------
// 加载线索详情
const loadClueDetail = async () => {
  const id = route.params.id
  const resp = await doGet(`/api/clue/detail/${id}`, {})
  if (resp.data.code === 200) {
    clueDetail.value = resp.data.data
  }
}

// 加载字典数据（跟踪方式 / 意向产品）
const loadDicValue = async (typeCode) => {
  const resp = await doGet(`/api/dicvalue/${typeCode}`, {})
  if (resp.data.code === 200) {
    if (typeCode === 'noteWay') {
      noteWayOptions.value = resp.data.data
    } else if (typeCode === 'product') {
      productOptions.value = resp.data.data
    }
  }
}

// 加载跟踪记录列表
const loadClueRemarkList = async (current) => {
  remarkLoading.value = true
  try {
    const resp = await doGet("/api/clue/remark", {
      current: current,
      clueId: route.params.id
    })
    if (resp.data.code === 200) {
      clueRemarkList.value = resp.data.data.list
      pageSize.value = resp.data.data.pageSize
      total.value = resp.data.data.total
    }
  } finally {
    remarkLoading.value = false
  }
}

// 分页跳转
const toPage = (current) => {
  loadClueRemarkList(current)
}

// 提交跟踪记录
const clueRemarkSubmit = async () => {
  if (!clueRemarkRefForm.value) return
  await clueRemarkRefForm.value.validate(async (valid) => {
    if (valid) {
      const resp = await doPost("/api/clue/remark", {
        clueId: clueDetail.value.id,
        noteContent: clueRemark.noteContent,
        noteWay: clueRemark.noteWay
      })
      if (resp.data.code === 200) {
        messageFrame("提交成功", "success")
        // 【交互修正】旧版调 reload() 整页重挂，会清空用户刚填的内容并丢失滚动位置。
        // 改为清空表单字段 + 就地刷新列表，新记录立即可见且体验连贯。
        clueRemark.noteContent = ''
        clueRemark.noteWay = ''
        clueRemarkRefForm.value.clearValidate()
        loadClueRemarkList(1)
      } else {
        messageFrame("提交失败，原因：" + resp.data.msg, "error")
      }
    }
  })
}

// 编辑跟踪记录（打开弹窗）
const edit = async (id) => {
  // 从列表中查找当前记录数据
  const record = clueRemarkList.value.find(item => item.id === id)
  if (record) {
    editRemarkData.id = record.id
    editRemarkData.noteWay = record.noteWay
    editRemarkData.noteContent = record.noteContent
    editDialogVisible.value = true
  } else {
    messageFrame("未找到记录", "warning")
  }
}

// 更新跟踪记录
const updateRemark = async () => {
  if (!editRemarkRefForm.value) return
  await editRemarkRefForm.value.validate(async (valid) => {
    if (valid) {
      const resp = await doPut("/api/clue/remark/update", {
        id: editRemarkData.id,
        noteWay: editRemarkData.noteWay,
        noteContent: editRemarkData.noteContent
      })
      if (resp.data.code === 200) {
        messageFrame("更新成功", "success")
        editDialogVisible.value = false
        loadClueRemarkList(1) // 刷新列表，回到第一页
      } else {
        messageFrame("更新失败", "error")
      }
    }
  })
}

// 删除跟踪记录
const del = (id) => {
  ElMessageBox.confirm('删除后不可恢复，确定删除该条跟踪记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // 使用 doPost 模拟 DELETE 请求（后端需支持 _method=DELETE 或适配）
    const resp = await doDelete(`/api/clue/remark/delete/${id}`, {})
    if (resp.data.code === 200) {
      messageFrame("删除成功", "success")
      loadClueRemarkList(1)
    } else {
      messageFrame("删除失败", "error")
    }
  }).catch(() => {})
}

// 转换客户 - 打开弹窗
const convertCustomer = () => {
  convertCustomerDialogVisible.value = true
}

// 转换客户 - 提交
const convertCustomerSubmit = async () => {
  if (!convertCustomerRefForm.value) return
  await convertCustomerRefForm.value.validate(async (valid) => {
    if (valid) {
      const resp = await doPost("/api/clue/customer", {
        clueId: clueDetail.value.id,
        product: customerQuery.product,
        description: customerQuery.description,
        nextContactTime: customerQuery.nextContactTime
      })
      if (resp.data.code === 200) {
        messageFrame("转化成功，该线索已转为客户", "success")
        convertCustomerDialogVisible.value = false
        // 转化后刷新详情（state 会变 -1）与列表，保持两处一致
        loadClueDetail()
        loadClueRemarkList(1)
        reload()
      } else {
        messageFrame("转化失败，原因：" + resp.data.msg, "error")
      }
    }
  })
}

// 生命周期
onMounted(() => {
  loadClueDetail()
  loadClueRemarkList(1)
})
</script>

<style scoped>
@import "@/assets/module-theme.css";

.clue-detail { min-height: 100%; box-sizing: border-box; }

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

/* Hero 状态标签组 */
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

/* 跟进记录录入区 */
.remark-composer {
  border-bottom: 1px solid #edf5ef;
  background: rgba(245, 250, 246, 0.5);
}
.remark-composer :deep(.el-form-item) { margin-bottom: 0; }
.composer-textarea { margin-bottom: 14px !important; }

.composer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.composer-way { flex: 0 0 220px; }

/* 编辑人小字 */
.edit-by { color: #a3b3a8; font-size: 12px; }

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