<template>
  <div class="product-page">
    <!-- 搜索区域 -->
    <div class="search-card">
      <el-form ref="searchFormRef" :model="productQuery" label-width="90px" class="compact-form">
        <el-row>
          <el-col :span="24">
            <div class="form-grid">
              <el-form-item label="产品名称">
                <el-input v-model="productQuery.name" placeholder="请输入产品名称" clearable />
              </el-form-item>
              <el-form-item label="产品状态">
                <el-select v-model="productQuery.state" placeholder="请选择状态" clearable style="width: 100%">
                  <el-option label="在售" :value="0" />
                  <el-option label="售罄" :value="1" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="success" @click="onSubmit" icon="Search" v-hasPermission="'product:list'">查询</el-button>
                <el-button @click="onReset" icon="RefreshLeft" class="reset-btn">重置</el-button>
              </el-form-item>
            </div>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar-card">
      <div class="toolbar-left">
        <el-button type="success" @click="openAddDialog" icon="Plus" v-hasPermission="'product:add'">
          添加产品
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table
        v-loading="tableLoading"
        :data="productList"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#1f2f3d', fontWeight: 600 }"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        <el-table-column label="产品名称" prop="name" show-overflow-tooltip min-width="150" />
        <el-table-column label="指导起始价" min-width="120">
          <template #default="scope">¥{{ scope.row.guidePriceS }}</template>
        </el-table-column>
        <el-table-column label="指导最高价" min-width="120">
          <template #default="scope">¥{{ scope.row.guidePriceE }}</template>
        </el-table-column>
        <el-table-column label="经销商报价" min-width="120">
          <template #default="scope">¥{{ scope.row.quotation }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.state === 0 ? 'success' : 'info'">
              {{ scope.row.state === 0 ? '在售' : '售罄' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" link @click="handleEdit(scope.row.id)" v-hasPermission="'product:edit'">编辑</el-button>
            <el-popconfirm
              title="确定删除该产品吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(scope.row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger" link v-hasPermission="'product:delete'">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!productList.length && !tableLoading" description="暂无数据" />
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="dialogFormRef" :model="productForm" :rules="dialogRules" label-width="110px">
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="官方指导起始价" prop="guidePriceS">
          <el-input v-model="productForm.guidePriceS" placeholder="请输入官方指导起始价" />
        </el-form-item>
        <el-form-item label="官方指导最高价" prop="guidePriceE">
          <el-input v-model="productForm.guidePriceE" placeholder="请输入官方指导最高价" />
        </el-form-item>
        <el-form-item label="经销商报价" prop="quotation">
          <el-input v-model="productForm.quotation" placeholder="请输入经销商报价" />
        </el-form-item>
        <el-form-item label="产品状态" prop="state">
          <el-select v-model="productForm.state" placeholder="请选择状态" style="width: 100%">
            <el-option label="在售" :value="0" />
            <el-option label="售罄" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { doGet, doPost, doPut, doDelete } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'

const searchFormRef = ref(null)
const productQuery = reactive({
  name: '',
  state: ''
})

const productList = ref([])
const pageSize = ref(10)
const total = ref(0)
const currentPage = ref(1)
const tableLoading = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('')
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

const priceValidator = (rule, value, callback) => {
  if (!value) return callback()
  if (!/^[0-9]+(\.[0-9]{1,2})?$/.test(value)) {
    callback(new Error('金额必须为整数或最多两位小数'))
  } else {
    callback()
  }
}

const dialogRules = {
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  guidePriceS: [
    { required: true, message: '请输入官方指导起始价', trigger: 'blur' },
    { validator: priceValidator, trigger: 'blur' }
  ],
  guidePriceE: [
    { required: true, message: '请输入官方指导最高价', trigger: 'blur' },
    { validator: priceValidator, trigger: 'blur' }
  ],
  quotation: [
    { required: true, message: '请输入经销商报价', trigger: 'blur' },
    { validator: priceValidator, trigger: 'blur' }
  ],
  state: [{ required: true, message: '请选择产品状态', trigger: 'change' }]
}

onMounted(() => {
  getData(1)
})

const getData = async (current) => {
  tableLoading.value = true
  currentPage.value = current
  try {
    const response = await doGet('api/products', {
      current,
      name: productQuery.name,
      state: productQuery.state || ''
    })
    if (response.data.code === 200) {
      productList.value = response.data.data.list
      pageSize.value = response.data.data.pageSize
      total.value = response.data.data.total
    } else {
      ElMessage.error(response.data.msg || '获取列表失败')
    }
  } catch (e) {
    console.error('获取产品列表失败', e)
    ElMessage.error('获取产品列表失败')
  } finally {
    tableLoading.value = false
  }
}

const toPage = (current) => {
  getData(current)
}

const onSubmit = () => {
  getData(1)
}

const onReset = () => {
  productQuery.name = ''
  productQuery.state = ''
  getData(1)
}

const openAddDialog = () => {
  dialogTitle.value = '添加产品'
  Object.assign(productForm, initProductForm())
  dialogVisible.value = true
}

const handleEdit = async (id) => {
  const row = productList.value.find((item) => item.id === id)
  if (!row) {
    ElMessage.error('未找到该产品数据')
    return
  }
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
  if (!dialogFormRef.value) return
  await dialogFormRef.value.validate(async (valid) => {
    if (!valid) return
    const params = {
      name: productForm.name,
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
        ElMessage.error(res.data.msg || '操作失败')
      }
    } catch (e) {
      console.error('提交失败', e)
      ElMessage.error('提交失败，请稍后再试')
    }
  })
}

const handleDelete = async (id) => {
  try {
    const res = await doDelete(`api/products/${id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      if (productList.value.length === 1 && currentPage.value > 1) {
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
</script>

<style scoped>
.product-page {
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
</style>
