<template>
  <div class="activity-page">
    <!-- 搜索区域卡片（淡绿色主题） -->
    <div class="search-card">
      <el-form
          ref="searchFormRef"
          :model="activityQuery"
          :rules="activityRules"
          label-width="90px"
          class="compact-form"
      >
        <el-row>
          <el-col :span="24">
            <div class="form-grid">
              <el-form-item label="负责人">
                <el-select
                    v-model="activityQuery.ownerId"
                    placeholder="请选择负责人"
                    clearable
                    @visible-change="loadOwner"
                    style="width: 100%"
                >
                  <el-option
                      v-for="item in ownerOption"
                      :key="item.id"
                      :label="item.name"
                      :value="item.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="活动名称">
                <el-input
                    v-model="activityQuery.activityName"
                    placeholder="请输入活动名称"
                    clearable
                />
              </el-form-item>

              <el-form-item label="活动时间">
                <el-date-picker
                    v-model="activityQuery.dateRange"
                    type="datetimerange"
                    range-separator="至"
                    start-placeholder="开始时间"
                    end-placeholder="结束时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    style="width: 100%"
                />
              </el-form-item>

              <el-form-item label="活动预算" prop="budget">
                <el-input
                    v-model="activityQuery.budget"
                    placeholder="请输入预算金额"
                    clearable
                />
              </el-form-item>

              <el-form-item label="创建时间">
                <el-date-picker
                    v-model="activityQuery.createTime"
                    type="datetime"
                    placeholder="请选择创建时间"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    style="width: 100%"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="success" @click="onSubmit" icon="Search" v-hasPermission="'activity:view'">查询</el-button>
                <el-button @click="onReset" icon="RefreshLeft" class="reset-btn">重置</el-button>
              </el-form-item>
            </div>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 操作栏卡片 -->
    <div class="toolbar-card">
      <div class="toolbar-left">
        <el-button type="success" @click="openAddUserDialog" icon="Plus" v-hasPermission="'activity:add'">
          添加市场活动
        </el-button>
        <el-popconfirm
            title="确定要批量删除选中的活动吗？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="deleteArr"
        >
          <template #reference>
            <el-button
                type="danger"
                icon="Delete"
                :disabled="!selectedIds.length"
                v-hasPermission="'activity:delete'"
            >
              批量删除 {{ selectedIds.length ? '(' + selectedIds.length + ')' : '' }}
            </el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="table-card">
      <el-table
          v-loading="tableLoading"
          :data="activityList"
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
          :header-cell-style="{ background: '#f5f7fa', color: '#1f2f3d', fontWeight: 600 }"
          row-class-name="table-row"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="负责人" min-width="120">
          <template #default="scope">
            <span v-if="scope.row.ownerDo">{{ scope.row.ownerDo.name }}</span>
            <el-tag v-else type="info" size="small">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活动名称" prop="name" show-overflow-tooltip min-width="150" />
        <el-table-column label="开始时间" prop="startTime" min-width="160" />
        <el-table-column label="结束时间" prop="endTime" min-width="160" />
        <el-table-column label="活动预算" prop="cost" min-width="120">
          <template #default="scope">
            ¥{{ scope.row.cost }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button size="small" type="success" link @click="handleDetail(scope.row.id)" v-hasPermission="'activity:view'">详情</el-button>
            <el-button size="small" type="primary" link @click="handleEdit(scope.row.id)" v-hasPermission="'activity:edit'">编辑</el-button>
            <el-popconfirm
                title="确定删除该活动吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete(scope.row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger" link v-hasPermission="'activity:delete'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!activityList.length && !tableLoading" description="暂无数据" />

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next, jumper"
            background
            @current-change="toPage"
        />
      </div>
    </div>

    <!-- 新增/编辑/详情 对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="660px"
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
          <el-select
              v-model="activityForm.ownerId"
              placeholder="请选择负责人"
              style="width: 100%"
          >
            <el-option
                v-for="item in ownerOption"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
              v-model="activityForm.startTime"
              type="datetime"
              placeholder="请选择开始时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
              v-model="activityForm.endTime"
              type="datetime"
              placeholder="请选择结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动预算" prop="cost">
          <el-input v-model="activityForm.cost" placeholder="请输入预算金额" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input
              v-model="activityForm.description"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 6 }"
              placeholder="请输入活动描述"
          />
        </el-form-item>
      </el-form>

      <!-- 备注模块（仅当已有活动ID时显示） -->
      <div v-if="activityForm.id" class="remark-section">
        <el-divider content-position="left">活动备注</el-divider>

        <!-- 添加备注（只读模式隐藏操作） -->
        <div v-if="!isReadOnly" class="remark-input-area">
          <el-input
              v-model="newRemarkContent"
              type="textarea"
              :rows="2"
              placeholder="输入备注内容..."
              resize="none"
          />
          <el-button type="primary" size="small" @click="addRemark" style="margin-top: 8px">
            添加备注
          </el-button>
        </div>

        <!-- 备注列表 -->
        <div class="remark-list">
          <div v-for="remark in activityRemarks" :key="remark.id" class="remark-item">
            <div class="remark-content">{{ remark.noteContent }}</div>
            <div class="remark-meta">
              <span>{{ remark.createByName || ('创建人ID:' + remark.createBy) }}</span>
              <span>{{ remark.createTime }}</span>
            </div>
            <div v-if="!isReadOnly" class="remark-actions">
              <el-button link type="primary" size="small" @click="editRemark(remark)" >编辑</el-button>
              <el-button link type="danger" size="small" @click="deleteRemark(remark.id)">删除</el-button>
            </div>
          </div>
          <el-empty v-if="!activityRemarks.length" description="暂无备注" :image-size="60" />
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button v-if="!isReadOnly" type="primary" @click="submitForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, nextTick, watch } from 'vue'
import { doGet, doPost, doPut, doDelete } from '@/http/httpRequest.js'
import { ElMessage, ElMessageBox } from 'element-plus'

// ---------- 查询相关 ----------
const searchFormRef = ref(null)
const activityQuery = reactive({
  ownerId: '',
  activityName: '',
  dateRange: [],
  budget: '',
  createTime: '',
  description: ''
})

const activityRules = {
  budget: [
    {
      validator: (rule, value, callback) => {
        if (!value) return callback()
        if (!/^[0-9]+(\.[0-9]{1,2})?$/.test(value)) {
          callback(new Error('预算必须为整数或最多两位小数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const activityList = ref([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const ownerOption = ref([])
const selectedIds = ref([])
const tableLoading = ref(false)

// ---------- 对话框相关 ----------
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isReadOnly = ref(false)
const dialogFormRef = ref(null)

// 备注相关数据
const activityRemarks = ref([])
const newRemarkContent = ref('')

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
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  description: [{ required: true, message: '请输入活动描述', trigger: 'change' }],
  cost: [
    { required: true, message: '请输入预算金额', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!value) return callback(new Error('请输入预算金额'))
        if (!/^[0-9]+(\.[0-9]{1,2})?$/.test(value)) {
          callback(new Error('预算必须为整数或最多两位小数'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 监听对话框关闭，重置备注相关数据
watch(dialogVisible, (val) => {
  if (!val) {
    resetForm()
  }
})

// ---------- 数据获取 ----------
onMounted(() => {
  getData(1)
})

const getData = async (current) => {
  tableLoading.value = true
  currentPage.value = current

  let startTime = ''
  let endTime = ''
  if (activityQuery.dateRange?.length === 2) {
    startTime = activityQuery.dateRange[0]
    endTime = activityQuery.dateRange[1]
  }

  try {
    const response = await doGet('api/activities', {
      current,
      ownerId: activityQuery.ownerId,
      name: activityQuery.activityName,
      startTime,
      endTime,
      budget: activityQuery.budget,
      createTime: activityQuery.createTime,
      description: activityQuery.description
    })
    if (response.data.code === 200) {
      activityList.value = response.data.data.list
      pageSize.value = response.data.data.pageSize
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

const toPage = (current) => {
  getData(current)
}

// ---------- 负责人下拉加载 ----------
let ownerLoaded = false
const loadOwner = async (visible) => {
  if (visible && !ownerLoaded) {
    try {
      const res = await doGet('/api/owner', {})
      if (res.data.code === 200) {
        ownerOption.value = res.data.data
        ownerLoaded = true
      }
    } catch (e) {
      console.error('加载负责人失败', e)
    }
  }
}

// ---------- 搜索与重置 ----------
const onSubmit = async () => {
  if (!searchFormRef.value) return
  await searchFormRef.value.validate((valid) => {
    if (valid) {
      getData(1)
    } else {
      ElMessage.warning('请检查搜索条件格式')
    }
  })
}

const onReset = () => {
  activityQuery.ownerId = ''
  activityQuery.activityName = ''
  activityQuery.dateRange = []
  activityQuery.budget = ''
  activityQuery.createTime = ''
  searchFormRef.value?.clearValidate()
  getData(1)
}

// ---------- 表格选择 ----------
const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((row) => row.id)
}

// ---------- 加载备注 ----------
const loadRemarks = async () => {
  if (!activityForm.id) return
  try {
    const res = await doGet(`api/activities/${activityForm.id}/remarks`)
    if (res.data.code === 200) {
      activityRemarks.value = res.data.data || []

    }
  } catch (e) {
    console.error('加载备注失败', e)
  }
}

// ---------- 添加备注 ----------
const addRemark = async () => {
  if (!newRemarkContent.value.trim()) {
    ElMessage.warning('请输入备注内容')
    return
  }
  try {
    const res = await doPost(`api/activities/${activityForm.id}/remarks`, {
      noteContent: newRemarkContent.value
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
  }
}

// ---------- 编辑备注 ----------
const editRemark = async (remark) => {
  const { value: newContent } = await ElMessageBox.prompt('编辑备注内容', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: remark.noteContent,
    inputType: 'textarea',
    inputValidator: (val) => (val && val.trim() ? true : '内容不能为空')
  })
  if (newContent === undefined) return
  try {
    const res = await doPut(`api/activities/remarks/${remark.id}`, {
      noteContent: newContent.trim()
    })
    if (res.data.code === 200) {
      ElMessage.success('修改成功')
      await loadRemarks()
    } else {
      ElMessage.error(res.data.msg || '修改失败')
    }
  } catch (e) {
    console.error('修改备注失败', e)
    ElMessage.error('修改备注失败')
  }
}

// ---------- 删除备注 ----------
const deleteRemark = async (remarkId) => {
  try {
    await ElMessageBox.confirm('确定删除该备注吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await doDelete(`api/activities/remarks/${remarkId}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      await loadRemarks()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

// ---------- 详情 ----------
const handleDetail = (id) => {
  const row = activityList.value.find((item) => item.id === id)
  if (!row) {
    ElMessage.error('未找到该活动数据')
    return
  }
  isReadOnly.value = true
  dialogTitle.value = '活动详情'
  fillFormFromRow(row)
  dialogVisible.value = true
  nextTick(() => dialogFormRef.value?.clearValidate())
}

// ---------- 新增 ----------
const openAddUserDialog = async () => {
  if (!ownerLoaded) {
    try {
      const res = await doGet('/api/owner', {})
      if (res.data.code === 200) {
        ownerOption.value = res.data.data
        ownerLoaded = true
      }
    } catch (e) {
      console.error('加载负责人失败', e)
    }
  }
  isReadOnly.value = false
  dialogTitle.value = '添加市场活动'
  resetForm()
  dialogVisible.value = true
}

// ---------- 编辑 ----------
const handleEdit = async (id) => {
  const row = activityList.value.find((item) => item.id === id)
  if (!row) {
    ElMessage.error('未找到该活动数据')
    return
  }
  if (!ownerLoaded) {
    try {
      const res = await doGet('/api/owner', {})
      if (res.data.code === 200) {
        ownerOption.value = res.data.data
        ownerLoaded = true
      }
    } catch (e) {
      console.error('加载负责人失败', e)
    }
  }
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
  // 加载备注
  nextTick(() => {
    if (activityForm.id) loadRemarks()
  })
  nextTick(() => dialogFormRef.value?.clearValidate())
}

// 重置表单（清空备注）
const resetForm = () => {
  Object.assign(activityForm, initActivityForm())
  activityRemarks.value = []
  newRemarkContent.value = ''
  nextTick(() => dialogFormRef.value?.clearValidate())
}

// ---------- 提交表单（新增/编辑） ----------
const submitForm = async () => {
  if (!dialogFormRef.value) return
  await dialogFormRef.value.validate(async (valid) => {
    if (!valid) return

    const params = {
      name: activityForm.name,
      ownerId: activityForm.ownerId,
      startTime: activityForm.startTime,
      endTime: activityForm.endTime,
      budget: activityForm.cost,
      description: activityForm.description
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
        ElMessage.error(res.data.msg || '操作失败')
      }
    } catch (e) {
      console.error('提交失败', e)
      ElMessage.error('提交失败，请稍后再试')
    }
  })
}

// ---------- 删除单个活动 ----------
const handleDelete = async (id) => {
  try {
    const res = await doDelete(`api/activities/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      if (activityList.value.length === 1 && currentPage.value > 1) {
        getData(currentPage.value - 1)
      } else {
        getData(currentPage.value)
      }
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    console.error('删除失败', e)
    ElMessage.error('删除失败')
  }
}

// ---------- 批量删除 ----------
const deleteArr = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的活动')
    return
  }

  try {
    const res = await doPost('api/activities/batch-delete', { ids: selectedIds.value })
    if (res.data.code === 200) {
      ElMessage.success(`成功删除 ${selectedIds.value.length} 个活动`)
      selectedIds.value = []
      getData(1)
    } else {
      ElMessage.error(res.data.msg || '批量删除失败')
    }
  } catch (e) {
    console.error('批量删除失败', e)
    ElMessage.error('批量删除失败')
  }
}
</script>

<style scoped>
.activity-page {
  padding: 0;
  background-color: transparent;
}

.search-card {
  background: #f0f9f0;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(82, 196, 26, 0.08);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  align-items: center;
}

.compact-form :deep(.el-form-item) {
  margin-bottom: 0;
  width: 100%;
}

.compact-form :deep(.el-form-item__label) {
  font-weight: 600;
  color: #1f2f3d;
  padding-right: 12px;
}

.search-card :deep(.el-input__wrapper),
.search-card :deep(.el-select .el-input__wrapper),
.search-card :deep(.el-date-editor .el-input__wrapper) {
  box-shadow: 0 0 0 1px #b7eb8f inset !important;
  background-color: #ffffff;
  border-radius: 6px;
  transition: box-shadow 0.3s;
}

.search-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #95de64 inset !important;
}

.search-card :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #73d13d inset !important;
}

.toolbar-card, .table-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.toolbar-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.table-card .el-table {
  border-radius: 8px;
  overflow: hidden;
}

.table-row:hover {
  background-color: #f0f5ff !important;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 备注模块样式 */
.remark-section {
  margin-top: 20px;
}

.remark-list {
  max-height: 300px;
  overflow-y: auto;
  margin-top: 12px;
}

.remark-item {
  background: #f9f9fb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  position: relative;
}

.remark-content {
  font-size: 14px;
  line-height: 1.5;
  color: #2c3e4f;
  white-space: pre-wrap;
  word-break: break-word;
  padding-right: 80px;
}

.remark-meta {
  font-size: 12px;
  color: #8c9aa8;
  margin-top: 6px;
  display: flex;
  gap: 12px;
}

.remark-actions {
  position: absolute;
  top: 12px;
  right: 12px;
}

.remark-input-area {
  margin-bottom: 16px;
}
</style>