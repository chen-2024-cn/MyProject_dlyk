<template>
  <el-form
      ref="clueRemarkRefForm"
      :model="clueRemark"
      label-width="110px"
      :rules="clueRemarkRules"
      style="max-width: 95%;">

    <el-form-item label="负责人">
      <div class="desc">{{clueDetail.ownerDO?.name || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="所属活动">
      <div class="desc">{{clueDetail.activityDO?.name || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="姓名">
      <div class="desc">{{clueDetail.fullName || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="称呼">
      <div class="desc">{{clueDetail.appellationDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="手机">
      <div class="desc">{{clueDetail.phone || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="微信">
      <div class="desc">{{clueDetail.weixin || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="QQ">
      <div class="desc">{{clueDetail.qq || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="邮箱">
      <div class="desc">{{clueDetail.email || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="年龄">
      <div class="desc">{{clueDetail.age || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="职业">
      <div class="desc">{{clueDetail.job || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="年收入">
      <div class="desc">{{clueDetail.yearIncome || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="住址">
      <div class="desc">{{clueDetail.address || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="贷款">
      <div class="desc">{{clueDetail.needLoanDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="意向状态">
      <div class="desc">{{clueDetail.intentionStateDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="意向产品">
      <div class="desc">{{clueDetail.intentionProductDO?.name || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="线索状态">
      <div class="desc">{{clueDetail.stateDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="线索来源">
      <div class="desc">{{clueDetail.sourceDO?.typeValue || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="线索描述">
      <div class="desc">{{clueDetail.description || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="下次联系时间">
      <div class="desc">{{clueDetail.nextContactTime || ''}}&nbsp;</div>
    </el-form-item>

    <el-form-item label="填写跟踪记录" prop="noteContent">
      <el-input
          v-model="clueRemark.noteContent"
          :rows="8"
          type="textarea"/>
    </el-form-item>
    <el-form-item label="跟踪方式" prop="noteWay">
      <el-select
          v-model="clueRemark.noteWay"
          placeholder="请选择跟踪方式"
          style="width: 100%"
          @click="loadDicValue('noteWay')"
          clearable>
        <el-option
            v-for="item in noteWayOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="clueRemarkSubmit">提 交</el-button>
      <el-button type="success" @click="convertCustomer" v-if="clueDetail.state !== -1">转换客户</el-button>
      <el-button type="success" plain @click="goBack">返 回</el-button>
    </el-form-item>
  </el-form>

  <el-table
      :data="clueRemarkList"
      style="width: 100%">
    <el-table-column type="index" label="序号" width="60"/>
    <el-table-column prop="noteWayName" label="跟踪方式"/>
    <el-table-column prop="noteContent" label="跟踪内容"/>
    <el-table-column prop="createTime" label="跟踪时间"/>
    <el-table-column prop="createByName" label="跟踪人"/>
    <el-table-column prop="editTime" label="编辑时间"/>
    <el-table-column prop="editByName" label="编辑人"/>
    <el-table-column label="操作">
      <template #default="scope">
        <a href="javascript:" @click="edit(scope.row.id)">编辑</a>
        &nbsp;
        <a href="javascript:" @click="del(scope.row.id)">删除</a>
      </template>
    </el-table-column>
  </el-table>

  <el-pagination
      background
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="total"
      @prev-click="toPage"
      @next-click="toPage"
      @current-change="toPage"/>

  <!-- 线索转换为客户的弹窗 -->
  <el-dialog v-model="convertCustomerDialogVisible" title="线索转换客户" width="55%" center>
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
  <el-dialog v-model="editDialogVisible" title="编辑跟踪记录" width="50%" center>
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
</template>

<script setup>
import { ref, reactive, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {doDelete, doGet, doPost, doPut} from "../http/httpRequest.js"
import { goBack, messageFrame } from "../util/util.js"

// 路由和刷新注入
const route = useRoute()
const reload = inject('reload')

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
  const resp = await doGet("/api/clue/remark", {
    current: current,
    clueId: route.params.id
  })
  if (resp.data.code === 200) {
    clueRemarkList.value = resp.data.data.list
    pageSize.value = resp.data.data.pageSize
    total.value = resp.data.data.total
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
        reload()
      } else {
        messageFrame("提交失败", "error")
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
        messageFrame("转换成功", "success")
        convertCustomerDialogVisible.value = false
        reload()
      } else {
        messageFrame("转换失败", "error")
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
.desc {
  background-color: #F0FFFF;
  width: 100%;
  padding-left: 15px;
}
</style>