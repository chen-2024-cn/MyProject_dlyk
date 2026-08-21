<template>
  <div class="system-page">
    <div class="page-card">
      <div class="page-header">系统信息配置</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" v-loading="loading" class="system-form">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入系统名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入系统标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="系统版本" prop="version">
              <el-input v-model="form.version" placeholder="请输入系统版本" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统编码">
              <el-input v-model="form.systemCode" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="系统描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入系统描述" />
        </el-form-item>

        <el-divider content-position="left">网站信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="网址" prop="site">
              <el-input v-model="form.site" placeholder="请输入网址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Logo地址">
              <el-input v-model="form.logo" placeholder="请输入Logo地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="图标地址">
              <el-input v-model="form.shortcuticon" placeholder="请输入图标地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关键词">
              <el-input v-model="form.keywords" placeholder="请输入关键词" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">联系方式</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="电话">
              <el-input v-model="form.tel" placeholder="请输入电话" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="微信">
              <el-input v-model="form.weixin" placeholder="请输入微信" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>

        <el-divider content-position="left">系统设置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开关状态">
              <el-radio-group v-model="form.isopen">
                <el-radio value="true">开启</el-radio>
                <el-radio value="false">关闭</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="关闭提示信息">
          <el-input v-model="form.closeMsg" type="textarea" :rows="2" placeholder="请输入关闭提示信息" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting" v-hasPermission="'system:edit'">
            保存配置
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { doGet, doPut } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  id: null,
  systemCode: '',
  name: '',
  site: '',
  logo: '',
  title: '',
  description: '',
  keywords: '',
  shortcuticon: '',
  tel: '',
  weixin: '',
  email: '',
  address: '',
  version: '',
  closeMsg: '',
  isopen: 'true'
})

const rules = {
  name: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
  title: [{ required: true, message: '请输入系统标题', trigger: 'blur' }],
  site: [{ required: true, message: '请输入网址', trigger: 'blur' }],
  description: [{ required: true, message: '请输入系统描述', trigger: 'blur' }],
  version: [{ required: true, message: '请输入系统版本', trigger: 'blur' }]
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await doGet('api/system/info', {})
    if (res.data.code === 200 && res.data.data) {
      const data = res.data.data
      form.id = data.id
      form.systemCode = data.systemCode || ''
      form.name = data.name || ''
      form.site = data.site || ''
      form.logo = data.logo || ''
      form.title = data.title || ''
      form.description = data.description || ''
      form.keywords = data.keywords || ''
      form.shortcuticon = data.shortcuticon || ''
      form.tel = data.tel || ''
      form.weixin = data.weixin || ''
      form.email = data.email || ''
      form.address = data.address || ''
      form.version = data.version || ''
      form.closeMsg = data.closeMsg || ''
      form.isopen = data.isopen || 'true'
    }
  } catch (e) {
    console.error('加载系统信息失败', e)
    ElMessage.error('加载系统信息失败')
  } finally {
    loading.value = false
  }
})

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await doPut('api/system/info', {
        id: form.id,
        systemCode: form.systemCode,
        name: form.name,
        site: form.site,
        logo: form.logo,
        title: form.title,
        description: form.description,
        keywords: form.keywords,
        shortcuticon: form.shortcuticon,
        tel: form.tel,
        weixin: form.weixin,
        email: form.email,
        address: form.address,
        version: form.version,
        closeMsg: form.closeMsg,
        isopen: form.isopen
      })
      if (res.data.code === 200) {
        ElMessage.success('保存成功')
      } else {
        ElMessage.error(res.data.msg || '保存失败')
      }
    } catch (e) {
      console.error('保存失败', e)
      ElMessage.error('保存失败')
    } finally {
      submitting.value = false
    }
  })
}
</script>

<style scoped>
.system-page {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.page-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.page-header {
  font-size: 18px;
  font-weight: 700;
  color: #1f2f3d;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e8f5e9;
}

.system-form {
  max-width: 900px;
}
</style>
