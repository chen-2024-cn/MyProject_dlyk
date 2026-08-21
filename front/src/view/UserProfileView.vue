<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 左侧：个人信息卡片 -->
      <el-col :span="10">
        <div class="profile-card">
          <div class="avatar-section">
            <el-avatar :size="80" class="user-avatar">{{ userInfo.name?.charAt(0) }}</el-avatar>
            <h3 class="user-name">{{ userInfo.name }}</h3>
            <p class="user-role">
              <el-tag v-for="role in userInfo.roleList" :key="role" size="small" type="success">{{ role }}</el-tag>
            </p>
          </div>
          <el-divider />
          <div class="info-list">
            <div class="info-item">
              <span class="info-label">登录账号</span>
              <span class="info-value">{{ userInfo.loginAct }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">手机号</span>
              <span class="info-value">{{ userInfo.phone || '未填写' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">邮箱</span>
              <span class="info-value">{{ userInfo.email || '未填写' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">账号状态</span>
              <span class="info-value">
                <template v-if="userInfo.accountEnabled === 1 && userInfo.accountNoLocked === 1">
                  <el-tag type="success" size="small">正常</el-tag>
                </template>
                <template v-else>
                  <el-tag v-if="userInfo.accountEnabled !== 1" type="danger" size="small">已禁用</el-tag>
                  <el-tag v-if="userInfo.accountNoLocked !== 1" type="warning" size="small">已锁定</el-tag>
                </template>
              </span>
            </div>
          </div>
          <div class="profile-actions">
            <el-button type="primary" icon="Edit" @click="openEditDialog">编辑资料</el-button>
            <el-button type="warning" icon="Lock" @click="openPwdDialog">修改密码</el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧：登录安全信息 -->
      <el-col :span="14">
        <div class="detail-card">
          <div class="card-title">账户详情</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户ID">{{ userInfo.id }}</el-descriptions-item>
            <el-descriptions-item label="最近登录时间">{{ formatTime(userInfo.lastLoginTime) }}</el-descriptions-item>
            <el-descriptions-item label="账号创建时间">{{ formatTime(userInfo.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后修改时间">{{ formatTime(userInfo.editTime) }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ userInfo.createByUser?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="修改人">{{ userInfo.editByUser?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账户未过期">
              <el-tag :type="userInfo.accountNoExpired === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo.accountNoExpired === 1 ? '正常' : '已过期' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="密码未过期">
              <el-tag :type="userInfo.credentialsNoExpired === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo.credentialsNoExpired === 1 ? '正常' : '已过期' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-card" style="margin-top:16px">
          <div class="card-title">权限列表</div>
          <div class="permission-tags">
            <el-tag
              v-for="perm in userInfo.permissionList"
              :key="perm"
              size="small"
              class="perm-tag"
            >{{ perm }}</el-tag>
            <el-empty v-if="!userInfo.permissionList?.length" description="暂无权限" :image-size="40" />
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="450px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitEdit">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="450px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitPwd">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { doGet, doPut } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'

const userInfo = ref({})

// ---- 编辑资料 ----
const editDialogVisible = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ name: '', phone: '', email: '' })
const editRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const openEditDialog = () => {
  editForm.name = userInfo.value.name || ''
  editForm.phone = userInfo.value.phone || ''
  editForm.email = userInfo.value.email || ''
  editDialogVisible.value = true
}

const submitEdit = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const res = await doPut('api/profile', { name: editForm.name, phone: editForm.phone, email: editForm.email })
      if (res.data.code === 200) {
        ElMessage.success('资料修改成功')
        editDialogVisible.value = false
        loadUserInfo()
      } else {
        ElMessage.error(res.data.msg || '修改失败')
      }
    } catch (e) {
      console.error('修改失败', e)
    }
  })
}

// ---- 修改密码 ----
const pwdDialogVisible = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

const openPwdDialog = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

const submitPwd = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const res = await doPut('api/profile/password', {
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      if (res.data.code === 200) {
        ElMessage.success('密码修改成功，请重新登录')
        pwdDialogVisible.value = false
        setTimeout(() => {
          window.location.href = '/'
        }, 1500)
      } else {
        ElMessage.error(res.data.msg || '修改失败')
      }
    } catch (e) {
      console.error('修改密码失败', e)
    }
  })
}

// ---- 工具 ----
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

const loadUserInfo = async () => {
  try {
    const res = await doGet('/api/login/info', {})
    if (res.data.code === 200) {
      userInfo.value = res.data.data
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.profile-card, .detail-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.avatar-section {
  text-align: center;
}

.user-avatar {
  background: linear-gradient(135deg, #4caf50, #2e7d32);
  font-size: 32px;
  margin-bottom: 12px;
}

.user-name {
  margin: 8px 0 4px;
  color: #1f2f3d;
}

.user-role {
  display: flex;
  gap: 6px;
  justify-content: center;
  flex-wrap: wrap;
}

.info-list {
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.info-label {
  color: #8c9aa8;
  font-size: 14px;
}

.info-value {
  color: #1f2f3d;
  font-size: 14px;
  font-weight: 500;
}

.profile-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2f3d;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e8f5e9;
}

.permission-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-tag {
  font-family: monospace;
  font-size: 12px;
}
</style>
