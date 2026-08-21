<template>
  <div class="dic-page">
    <el-row :gutter="16">
      <!-- 左侧：字典类型 -->
      <el-col :span="8">
        <div class="panel-card">
          <div class="panel-header">字典类型</div>
          <div class="panel-search">
            <el-input v-model="typeQuery.typeCode" placeholder="类型代码" clearable size="small" style="margin-bottom:8px" />
            <el-input v-model="typeQuery.typeName" placeholder="类型名称" clearable size="small" style="margin-bottom:8px" />
            <el-button type="success" size="small" @click="loadTypeData(1)" icon="Search">查询</el-button>
            <el-button size="small" @click="openTypeAdd" icon="Plus" v-hasPermission="'dictype:add'">新增</el-button>
          </div>
          <div class="panel-list" v-loading="typeLoading">
            <div
              v-for="item in typeList"
              :key="item.id"
              class="list-item"
              :class="{ active: selectedType && selectedType.id === item.id }"
              @click="selectType(item)"
            >
              <div class="item-info">
                <span class="item-name">{{ item.typeName }}</span>
                <span class="item-code">{{ item.typeCode }}</span>
              </div>
              <div class="item-actions">
                <el-button link type="primary" size="small" @click.stop="openTypeEdit(item)" v-hasPermission="'dictype:edit'">编辑</el-button>
                <el-popconfirm title="确定删除该类型？" @confirm="deleteType(item.id)" @click.stop>
                  <template #reference>
                    <el-button link type="danger" size="small" v-hasPermission="'dictype:delete'">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
            <el-empty v-if="!typeList.length && !typeLoading" description="暂无数据" :image-size="60" />
          </div>
          <div class="panel-pagination" v-if="typeTotal > pageSize">
            <el-pagination
              small
              v-model:current-page="typeCurrent"
              :page-size="pageSize"
              :total="typeTotal"
              layout="prev, pager, next"
              @current-change="loadTypeData"
            />
          </div>
        </div>
      </el-col>

      <!-- 右侧：字典值 -->
      <el-col :span="16">
        <div class="panel-card">
          <div class="panel-header">
            字典值
            <span v-if="selectedType" class="type-badge">{{ selectedType.typeName }} ({{ selectedType.typeCode }})</span>
          </div>
          <div class="panel-toolbar" v-if="selectedType">
            <el-button type="success" size="small" @click="openValueAdd" icon="Plus" v-hasPermission="'dicvalue:add'">新增字典值</el-button>
          </div>
          <el-table
            v-if="selectedType"
            v-loading="valueLoading"
            :data="valueList"
            stripe
            style="width: 100%"
            :header-cell-style="{ background: '#f5f7fa', color: '#1f2f3d', fontWeight: 600 }"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column label="字典值" prop="typeValue" min-width="120" />
            <el-table-column label="排序" prop="order" width="80" align="center" />
            <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <el-button size="small" type="primary" link @click="openValueEdit(scope.row)" v-hasPermission="'dicvalue:edit'">编辑</el-button>
                <el-popconfirm title="确定删除该字典值？" @confirm="deleteValue(scope.row.id)">
                  <template #reference>
                    <el-button size="small" type="danger" link v-hasPermission="'dicvalue:delete'">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!selectedType" description="请选择左侧字典类型" :image-size="80" />
          <div class="panel-pagination" v-if="valueTotal > pageSize">
            <el-pagination
              small
              v-model:current-page="valueCurrent"
              :page-size="pageSize"
              :total="valueTotal"
              layout="prev, pager, next"
              @current-change="loadValueData"
            />
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 类型弹窗 -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="450px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="类型代码" prop="typeCode">
          <el-input v-model="typeForm.typeCode" placeholder="请输入类型代码" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="typeForm.typeName" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitTypeForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 值弹窗 -->
    <el-dialog v-model="valueDialogVisible" :title="valueDialogTitle" width="450px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="valueFormRef" :model="valueForm" :rules="valueRules" label-width="80px">
        <el-form-item label="字典值" prop="typeValue">
          <el-input v-model="valueForm.typeValue" placeholder="请输入字典值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="valueForm.order" :min="0" :max="9999" placeholder="排序号" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="valueForm.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="valueDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitValueForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { doGet, doPost, doPut, doDelete } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'

const pageSize = 10

// ---- 类型列表 ----
const typeQuery = reactive({ typeCode: '', typeName: '' })
const typeList = ref([])
const typeCurrent = ref(1)
const typeTotal = ref(0)
const typeLoading = ref(false)
const selectedType = ref(null)

const loadTypeData = async (current) => {
  typeLoading.value = true
  typeCurrent.value = current
  try {
    const res = await doGet('api/dictypes', { current, typeCode: typeQuery.typeCode, typeName: typeQuery.typeName })
    if (res.data.code === 200) {
      typeList.value = res.data.data.list
      typeTotal.value = res.data.data.total
    }
  } catch (e) {
    console.error('加载类型失败', e)
  } finally {
    typeLoading.value = false
  }
}

const selectType = (item) => {
  selectedType.value = item
  valueCurrent.value = 1
  loadValueData(1)
}

// ---- 类型 CRUD ----
const typeDialogVisible = ref(false)
const typeDialogTitle = ref('')
const typeFormRef = ref(null)
const typeForm = reactive({ id: null, typeCode: '', typeName: '', remark: '' })
const typeRules = {
  typeCode: [{ required: true, message: '请输入类型代码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

const openTypeAdd = () => {
  typeDialogTitle.value = '新增字典类型'
  typeForm.id = null; typeForm.typeCode = ''; typeForm.typeName = ''; typeForm.remark = ''
  typeDialogVisible.value = true
}

const openTypeEdit = (item) => {
  typeDialogTitle.value = '编辑字典类型'
  typeForm.id = item.id; typeForm.typeCode = item.typeCode; typeForm.typeName = item.typeName; typeForm.remark = item.remark || ''
  typeDialogVisible.value = true
}

const submitTypeForm = async () => {
  if (!typeFormRef.value) return
  await typeFormRef.value.validate(async (valid) => {
    if (!valid) return
    const params = { typeCode: typeForm.typeCode, typeName: typeForm.typeName, remark: typeForm.remark }
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
        loadTypeData(typeCurrent.value)
      } else {
        ElMessage.error(res.data.msg || '操作失败')
      }
    } catch (e) {
      console.error('提交失败', e)
    }
  })
}

const deleteType = async (id) => {
  try {
    const res = await doDelete(`api/dictypes/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      if (selectedType.value && selectedType.value.id === id) {
        selectedType.value = null
        valueList.value = []
      }
      loadTypeData(typeCurrent.value)
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    console.error('删除失败', e)
  }
}

// ---- 值列表 ----
const valueList = ref([])
const valueCurrent = ref(1)
const valueTotal = ref(0)
const valueLoading = ref(false)

const loadValueData = async (current) => {
  if (!selectedType.value) return
  valueLoading.value = true
  valueCurrent.value = current
  try {
    const res = await doGet('api/dicvalues', { current, typeCode: selectedType.value.typeCode })
    if (res.data.code === 200) {
      valueList.value = res.data.data.list
      valueTotal.value = res.data.data.total
    }
  } catch (e) {
    console.error('加载字典值失败', e)
  } finally {
    valueLoading.value = false
  }
}

// ---- 值 CRUD ----
const valueDialogVisible = ref(false)
const valueDialogTitle = ref('')
const valueFormRef = ref(null)
const valueForm = reactive({ id: null, typeValue: '', order: 0, remark: '' })
const valueRules = {
  typeValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }]
}

const openValueAdd = () => {
  valueDialogTitle.value = '新增字典值'
  valueForm.id = null; valueForm.typeValue = ''; valueForm.order = 0; valueForm.remark = ''
  valueDialogVisible.value = true
}

const openValueEdit = (item) => {
  valueDialogTitle.value = '编辑字典值'
  valueForm.id = item.id; valueForm.typeValue = item.typeValue; valueForm.order = item.order || 0; valueForm.remark = item.remark || ''
  valueDialogVisible.value = true
}

const submitValueForm = async () => {
  if (!valueFormRef.value) return
  await valueFormRef.value.validate(async (valid) => {
    if (!valid) return
    const params = { typeCode: selectedType.value.typeCode, typeValue: valueForm.typeValue, order: valueForm.order, remark: valueForm.remark }
    try {
      let res
      if (valueForm.id) {
        res = await doPut(`api/dicvalues/${valueForm.id}`, params)
      } else {
        res = await doPost('api/dicvalues', params)
      }
      if (res.data.code === 200) {
        ElMessage.success(valueForm.id ? '编辑成功' : '新增成功')
        valueDialogVisible.value = false
        loadValueData(valueCurrent.value)
      } else {
        ElMessage.error(res.data.msg || '操作失败')
      }
    } catch (e) {
      console.error('提交失败', e)
    }
  })
}

const deleteValue = async (id) => {
  try {
    const res = await doDelete(`api/dicvalues/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      loadValueData(valueCurrent.value)
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    console.error('删除失败', e)
  }
}

onMounted(() => {
  loadTypeData(1)
})
</script>

<style scoped>
.dic-page {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.panel-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  min-height: 500px;
}

.panel-header {
  font-size: 16px;
  font-weight: 700;
  color: #1f2f3d;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e8f5e9;
}

.type-badge {
  font-size: 13px;
  font-weight: 400;
  color: #2e7d32;
  background: #e8f5e9;
  padding: 2px 10px;
  border-radius: 12px;
  margin-left: 8px;
}

.panel-search {
  margin-bottom: 12px;
}

.panel-toolbar {
  margin-bottom: 12px;
}

.panel-list {
  max-height: 380px;
  overflow-y: auto;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.list-item:hover {
  background: #f5f7fa;
}

.list-item.active {
  background: #e8f5e9;
  border-left: 3px solid #4caf50;
}

.item-info {
  display: flex;
  flex-direction: column;
}

.item-name {
  font-weight: 600;
  font-size: 14px;
  color: #1f2f3d;
}

.item-code {
  font-size: 12px;
  color: #8c9aa8;
  margin-top: 2px;
}

.item-actions {
  display: flex;
  gap: 4px;
}

.panel-pagination {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}
</style>
