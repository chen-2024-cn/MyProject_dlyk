<template>
  <!-- Basic Info -->
  <el-form label-width="120px" style="max-width: 95%;">
    <el-form-item label="交易流水号">
      <div class="desc">{{ tran.tranNo || '' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="客户">
      <div class="desc">{{ tran.customerName || '-' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="交易金额">
      <div class="desc">{{ tran.money || '' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="所处阶段">
      <div class="desc">{{ tran.stageDO?.typeValue || '-' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="预计成交日期">
      <div class="desc">{{ tran.expectedDate || '' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="下次联系时间">
      <div class="desc">{{ tran.nextContactTime || '' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="创建时间">
      <div class="desc">{{ tran.createTime || '' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="创建人">
      <div class="desc">{{ tran.createByDO?.name || '-' }}&nbsp;</div>
    </el-form-item>

    <el-form-item label="交易描述">
      <div class="desc">{{ tran.description || '' }}&nbsp;</div>
    </el-form-item>
  </el-form>

  <!-- Stage Change Area -->
  <div class="stage-change-area" v-if="prevStage || nextStage">
    <div class="stage-info">
      <span v-if="prevStage">上一阶段：{{ prevStage.typeValue }}</span>
      <span class="current">当前：{{ tran.stageDO?.typeValue }}</span>
      <span v-if="nextStage">下一阶段：{{ nextStage.typeValue }}</span>
    </div>
    <div class="stage-btns">
      <el-button type="primary" v-if="nextStage" @click="showStageDialog(nextStage.id)">变更到下一阶段</el-button>
      <el-button type="warning" v-if="prevStage" @click="showStageDialog(prevStage.id)">回退到上一阶段</el-button>
    </div>
  </div>

  <!-- Tabs: History / Remarks -->
  <el-tabs v-model="activeTab" style="margin-top: 16px;">
    <el-tab-pane label="阶段历史" name="history">
      <el-table :data="historyList" style="width: 100%;">
        <el-table-column type="index" label="序号" width="60"/>
        <el-table-column label="阶段">
          <template #default="scope">
            {{ getStageName(scope.row.stage) }}
          </template>
        </el-table-column>
        <el-table-column property="money" label="金额"/>
        <el-table-column property="expectedDate" label="预计成交日期"/>
        <el-table-column property="createTime" label="变更时间"/>
        <el-table-column label="创建人">
          <template #default="scope">
            {{ scope.row.createByName || scope.row.createBy }}
          </template>
        </el-table-column>
      </el-table>
    </el-tab-pane>

    <el-tab-pane label="跟踪记录" name="remark">
      <el-form
          ref="remarkRefForm"
          :model="remarkForm"
          :rules="remarkRules"
          label-width="110px">
        <el-form-item label="跟踪内容" prop="noteContent">
          <el-input v-model="remarkForm.noteContent" :rows="4" type="textarea"/>
        </el-form-item>
        <el-form-item label="跟踪方式" prop="noteWay">
          <el-select
              v-model="remarkForm.noteWay"
              placeholder="请选择跟踪方式"
              style="width: 100%"
              @click="loadNoteWayDic"
              clearable>
            <el-option
                v-for="item in noteWayOptions"
                :key="item.id"
                :label="item.typeValue"
                :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitRemark">提 交</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="remarkList" style="width: 100%;">
        <el-table-column type="index" label="序号" width="60"/>
        <el-table-column prop="noteWayName" label="跟踪方式"/>
        <el-table-column prop="noteContent" label="跟踪内容"/>
        <el-table-column prop="createTime" label="跟踪时间"/>
        <el-table-column prop="createByName" label="跟踪人"/>
        <el-table-column label="操作">
          <template #default="scope">
            <a href="javascript:" @click="editRemark(scope.row.id)">编辑</a>
            &nbsp;
            <a href="javascript:" @click="delRemark(scope.row.id)">删除</a>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          background
          layout="prev, pager, next"
          :page-size="remarkPageSize"
          :total="remarkTotal"
          @prev-click="toRemarkPage"
          @next-click="toRemarkPage"
          @current-change="toRemarkPage"/>
    </el-tab-pane>
  </el-tabs>

  <!-- Action Buttons -->
  <div style="margin-top: 16px;">
    <el-button type="danger" @click="delTran">删 除</el-button>
    <el-button type="success" plain @click="goBack">返 回</el-button>
  </div>

  <!-- Stage Change Dialog -->
  <el-dialog v-model="stageDialogVisible" title="阶段变更确认" width="40%">
    <el-form :model="stageForm" label-width="140px">
      <el-form-item label="变更后金额">
        <el-input v-model="stageForm.money" placeholder="不修改则留空"/>
      </el-form-item>
      <el-form-item label="变更后预计成交日期">
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
      <el-button type="primary" @click="confirmChangeStage">确 认</el-button>
    </template>
  </el-dialog>

  <!-- Edit Remark Dialog -->
  <el-dialog v-model="editRemarkVisible" title="编辑跟踪记录" width="40%">
    <el-form ref="editRemarkRefForm" :model="editRemarkForm" :rules="editRemarkRules" label-width="110px">
      <el-form-item label="跟踪方式" prop="noteWay">
        <el-select v-model="editRemarkForm.noteWay" style="width: 100%;" @click="loadNoteWayDic" clearable>
          <el-option v-for="item in noteWayOptions" :key="item.id" :label="item.typeValue" :value="item.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="跟踪内容" prop="noteContent">
        <el-input v-model="editRemarkForm.noteContent" type="textarea" :rows="4"/>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editRemarkVisible = false">取 消</el-button>
      <el-button type="primary" @click="confirmEditRemark">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import { doGet, doPost, doPut, doDelete } from '../http/httpRequest'
import { messageFrame } from "../util/util.js"
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const reload = inject('reload')

const tran = ref({ stageDO: {}, createByDO: {} })
const activeTab = ref('history')

const historyList = ref([])
const prevStage = ref(null)
const nextStage = ref(null)
const stageMap = ref({})

const stageDialogVisible = ref(false)
const selectedStage = ref(null)
const stageForm = reactive({ money: '', expectedDate: '' })

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

const loadStages = () => {
  doGet('/api/dicvalue/stage').then(resp => {
    if (resp.data.code === 200) {
      const stages = resp.data.data.sort((a, b) => a.order - b.order)
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
      }
    }
  })
}

const getStageName = (stageId) => {
  return stageMap.value[stageId]?.typeValue || stageId
}

const loadHistory = () => {
  doGet(`/api/tran/${route.params.id}/history`).then(resp => {
    if (resp.data.code === 200) {
      historyList.value = resp.data.data
    }
  })
}

const loadRemark = (current) => {
  doGet('/api/tran/remark', { current, tranId: route.params.id }).then(resp => {
    if (resp.data.code === 200) {
      remarkList.value = resp.data.data.list
      remarkPageSize.value = resp.data.data.pageSize
      remarkTotal.value = resp.data.data.total
    }
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
        reload()
      } else {
        messageFrame('提交失败', 'error')
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
      reload()
    } else {
      messageFrame('阶段变更失败', 'error')
    }
  } catch (error) {
    messageFrame('操作失败', 'error')
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

onMounted(() => {
  loadTran()
  loadRemark(1)
  loadNoteWayDic()
})
</script>

<style scoped>
.desc {
  background-color: #F0FFFF;
  width: 100%;
  padding-left: 15px;
}
.stage-change-area {
  background: #f0f9eb;
  padding: 12px 16px;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.stage-info { display: flex; gap: 16px; font-size: 14px; }
.stage-info .current { font-weight: bold; color: #409eff; }
.stage-btns { display: flex; gap: 8px; }
</style>
