<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <!-- 左侧：个人信息卡片 + 备忘随想手账 -->
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

        <!-- 扩展功能 1：专员快捷随身草稿便签 -->
        <div class="profile-card" style="margin-top: 20px;">
          <div class="card-title">✍️ 旅途草稿 · 销售随身记</div>
          <p class="notepad-subtitle">速记跟进想法、灵感便签，自动在本地保存</p>
          
          <div class="notepad-input-zone">
            <el-input
              v-model="notepadText"
              placeholder="记录一条待办：下午三点回复客户..."
              size="small"
              @keyup.enter="addNote"
            >
              <template #append>
                <el-button icon="Plus" @click="addNote" />
              </template>
            </el-input>
          </div>

          <div class="notepad-list">
            <div v-for="note in todoNotes" :key="note.id" class="notepad-item" :class="{ 'note-done': note.done }">
              <el-checkbox :model-value="note.done" @change="toggleNote(note)">
                <span class="note-text">{{ note.text }}</span>
              </el-checkbox>
              <el-button type="danger" icon="Delete" size="small" link class="delete-note-btn" @click="deleteNote(note.id)" />
            </div>
            <el-empty v-if="!todoNotes.length" description="便签空空如也，写个灵感吧 🍃" :image-size="40" />
          </div>

          <div class="notepad-actions" v-if="todoNotes.some(n => n.done)">
            <el-button type="warning" size="small" plain icon="Finished" @click="clearCompleted">
              清空已完成备忘
            </el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧：账户信息描述 + 个性换肤 + 优化分类权限 -->
      <el-col :span="14">
        <div class="detail-card">
          <div class="card-title">账户基本详情</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户ID">{{ userInfo.id }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ formatTime(userInfo.lastLoginTime) }}</el-descriptions-item>
            <el-descriptions-item label="账户创建">{{ formatTime(userInfo.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="最后修改">{{ formatTime(userInfo.editTime) }}</el-descriptions-item>
            <el-descriptions-item label="创建专员">{{ userInfo.createByUser?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="更改专员">{{ userInfo.editByUser?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账户时效">
              <el-tag :type="userInfo.accountNoExpired === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo.accountNoExpired === 1 ? '未过期' : '已过期' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="证书时效">
              <el-tag :type="userInfo.credentialsNoExpired === 1 ? 'success' : 'danger'" size="small">
                {{ userInfo.credentialsNoExpired === 1 ? '未过期' : '已过期' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 扩展功能 2：系统外壳个性化主题色定制 -->
        <div class="detail-card" style="margin-top: 16px;">
          <div class="card-title">🎨 列车美学 · 专属主题皮肤</div>
          <p class="theme-subtitle">点击切换方案，无感跨图层换肤即时调整左侧导航背景色色系</p>
          <div class="theme-palette-grid">
            <div
              v-for="theme in themeList"
              :key="theme.key"
              class="theme-color-block"
              :class="{ 'theme-active': activeTheme === theme.key }"
              @click="selectTheme(theme.key)"
            >
              <div class="theme-color-preview" :style="{ background: theme.gradient }">
                <el-icon v-if="activeTheme === theme.key" class="check-icon"><Select /></el-icon>
              </div>
              <span class="theme-color-name">{{ theme.name }}</span>
            </div>
          </div>
        </div>

        <!-- 优化功能 3：分类别中文释义权限列表 -->
        <div class="detail-card" style="margin-top:16px">
          <div class="card-title">🔐 安全授权 · 我的具体权限</div>
          <p class="perm-subtitle">已将底层繁冗的英文权限标识映射为模块级中文业务行为：</p>
          
          <el-collapse class="perm-collapse" accordion>
            <el-collapse-item v-for="(group, key) in groupedPermissions" :key="key" :name="key">
              <template #title>
                <div class="perm-group-header">
                  <span class="group-title">{{ group.title }}</span>
                  <el-tag size="small" round type="info" class="group-badge">{{ group.perms.length }} 项操作授权</el-tag>
                </div>
              </template>
              
              <div class="perm-sub-grid">
                <div v-for="perm in group.perms" :key="perm.code" class="perm-sub-item">
                  <div class="perm-name-cn">{{ perm.name }}</div>
                  <div class="perm-code-en">{{ perm.code }}</div>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>

          <el-empty v-if="!userInfo.permissionList?.length" description="暂无权限，请联系管理员分配" :image-size="60" />
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
import { onMounted, reactive, ref, computed } from 'vue'
import { doGet, doPut } from '@/http/httpRequest.js'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Finished, Select, Edit, Lock } from '@element-plus/icons-vue'

const userInfo = ref({})

// ==================== 优化功能 1：权限标识映射表 & 分组计算属性 ====================
const permissionMap = {
  'user:view': '查看用户列表',
  'user:add': '新增系统用户',
  'user:edit': '修改系统用户',
  'user:delete': '禁用/删除用户',
  'user:detail': '查看用户详情',
  
  'activity:view': '查看市场活动',
  'activity:add': '新增市场活动',
  'activity:edit': '修改市场活动',
  'activity:delete': '删除市场活动',
  'activity:detail': '查看活动详情',
  'activity:remark': '管理活动备注',
  
  'clue:view': '查看销售线索',
  'clue:add': '录入销售线索',
  'clue:edit': '修改销售线索',
  'clue:delete': '删除销售线索',
  'clue:detail': '查看线索详情',
  'clue:remark': '跟进线索备注',
  'clue:import': '批量导入线索',
  'clue:export': '批量导出线索',
  
  'customer:view': '查看客户名录',
  'customer:add': '新增潜在客户',
  'customer:edit': '完善客户信息',
  'customer:delete': '标记/删除客户',
  'customer:detail': '查看客户画像',
  'customer:import': '导入客户资料',
  'customer:export': '导出客户列表',
  'customer:assign': '同步指派客户',
  'customer:remark': '追加客户备注',
  
  'tran:view': '查看交易流水',
  'tran:add': '创建全新交易',
  'tran:edit': '变更交易阶段',
  'tran:delete': '标记作废交易',
  'tran:detail': '多角度交易画像',
  'tran:history': '查看交易演变史',
  'tran:remark': '管理交易备注',
  
  'product:view': '查看产品库',
  'product:add': '上架企业产品',
  'product:edit': '编辑产品参数',
  'product:delete': '下架商品列表',
  
  'dic:view': '查看数据字典',
  'dic:add': '新增通用字典',
  'dic:edit': '完善字典信息',
  'dic:delete': '删除/清库字典',
  
  'system:view': '查看系统配置',
  'system:edit': '修改系统全局配置'
}

const groupedPermissions = computed(() => {
  const groups = {
    user: { title: '👥 系统与账户管理', perms: [] },
    activity: { title: '📣 市场营销推广', perms: [] },
    clue: { title: '🎯 销售线索跟进', perms: [] },
    customer: { title: '🤝 客户精细运营', perms: [] },
    tran: { title: '💰 商业交易流水', perms: [] },
    product: { title: '📦 企业产品库', perms: [] },
    system: { title: '⚙️ 基础系统配置', perms: [] },
    dic: { title: '📖 字典数据对照', perms: [] },
    other: { title: '🔒 其他高级授权', perms: [] }
  }
  
  if (userInfo.value.permissionList) {
    userInfo.value.permissionList.forEach(perm => {
      const prefix = perm.split(':')[0]
      if (groups[prefix]) {
        groups[prefix].perms.push({ code: perm, name: permissionMap[perm] || '未知底层权限' })
      } else {
        groups['other'].perms.push({ code: perm, name: permissionMap[perm] || '自定义操作开发' })
      }
    })
  }
  
  // 滤除没有授权项的卡片分组
  return Object.keys(groups).reduce((acc, key) => {
    if (groups[key].perms.length > 0) {
      acc[key] = groups[key]
    }
    return acc
  }, {})
})


// ==================== 扩展功能 2：专员快捷便签备忘录机制 ====================
const notepadText = ref('')
const todoNotes = ref([])
// 当前登录用户ID（便签存储按用户隔离的依据）
const notesUserId = ref(null)

// 便签存储 Key 按「用户ID」隔离：修复原固定 Key 导致同一浏览器下所有账号共享同一份便签的缺陷。
// 命名规范：业务前缀:用户ID，例如 crm-todo-notes:3
const NOTES_KEY_PREFIX = 'crm-todo-notes:'
const LEGACY_SHARED_NOTES_KEY = 'crm-todo-notes' // 历史版本的全局共享 Key（升级兼容用）

const getNotesStorageKey = () => NOTES_KEY_PREFIX + notesUserId.value

const loadNotes = () => {
  // 登录人信息尚未就绪时不加载（此时无法确定归属用户）
  if (!notesUserId.value) return

  const storageKey = getNotesStorageKey()
  let local = localStorage.getItem(storageKey)

  // 历史版本兼容：旧数据存在固定全局 Key（所有账号共享）。升级后首次打开的账号接管旧数据，
  // 其余账号各自从默认示例开始，确保便签不再串账号；同时清除遗留的全局 Key。
  if (!local && localStorage.getItem(LEGACY_SHARED_NOTES_KEY)) {
    local = localStorage.getItem(LEGACY_SHARED_NOTES_KEY)
    localStorage.setItem(storageKey, local)
    localStorage.removeItem(LEGACY_SHARED_NOTES_KEY)
  }

  if (local) {
    try {
      todoNotes.value = JSON.parse(local)
    } catch (e) {
      console.error('便签数据解析失败，已重置为空列表', e)
      todoNotes.value = []
    }
  } else {
    todoNotes.value = [
      { id: Date.now(), text: '尝试在下方定制一套自己喜欢的高颜值主题皮肤！', done: false },
      { id: Date.now() + 1, text: '回访下午在销售线索中新指派的客户方案', done: false }
    ]
    saveNotes()
  }
}

const saveNotes = () => {
  if (!notesUserId.value) return
  localStorage.setItem(getNotesStorageKey(), JSON.stringify(todoNotes.value))
}

const addNote = () => {
  if (!notepadText.value.trim()) return
  todoNotes.value.unshift({
    id: Date.now(),
    text: notepadText.value.trim(),
    done: false
  })
  notepadText.value = ''
  saveNotes()
}

const toggleNote = (note) => {
  note.done = !note.done
  saveNotes()
}

const deleteNote = (noteId) => {
  todoNotes.value = todoNotes.value.filter(n => n.id !== noteId)
  saveNotes()
}

const clearCompleted = () => {
  todoNotes.value = todoNotes.value.filter(n => !n.done)
  saveNotes()
}


// ==================== 扩展功能 3：专属美学调色换肤服务 ====================
const themeList = [
  { key: 'forest', name: '森林墨绿', primary: '#112d1b', gradient: 'linear-gradient(135deg, #112d1b 0%, #2d6a4f 100%)' },
  { key: 'midnight', name: '静谧深海', primary: '#0f3c5f', gradient: 'linear-gradient(135deg, #0f3c5f 0%, #1d70b8 100%)' },
  { key: 'burgundy', name: '奢华酒红', primary: '#510f1b', gradient: 'linear-gradient(135deg, #510f1b 0%, #a9203b 100%)' },
  { key: 'cyberpunk', name: '极客护眼', primary: '#1a1a24', gradient: 'linear-gradient(135deg, #1a1a24 0%, #3e3e52 100%)' }
]

const activeTheme = ref(localStorage.getItem('app-theme') || 'forest')

const loadSystemTheme = async () => {
  try {
    // 补齐查询公开系统级主题配置接口
    const res = await doGet("/api/system/info/public", {});
    if (res.data.code === 200 && res.data.data) {
      const dbTheme = res.data.data.systemCode || 'forest';
      // 如果员工个人本地没有选择过个性的覆盖色，就让皮肤选项的高亮勾选框自适应和数据库中的统一默认色无缝重叠对齐
      if (!localStorage.getItem('app-theme')) {
        activeTheme.value = dbTheme;
      }
    }
  } catch (error) {
    console.error("个人中心获取系统主配置主题失败, 回退默认Forest:", error);
  }
}

const selectTheme = (themeKey) => {
  activeTheme.value = themeKey
  localStorage.setItem('app-theme', themeKey)
  ElMessage.success(`主题色已应用为 [${themeList.find(t => t.key === themeKey).name}]，已同步至左侧导航！`)
  
  // 核心！向全局派发一个自定义重绘事件
  window.dispatchEvent(new CustomEvent('theme-change', { detail: themeKey }))
}


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
      const res = await doPut('/api/profile', { name: editForm.name, phone: editForm.phone, email: editForm.email })
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
      const res = await doPut('/api/profile/password', {
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
      // 记录当前登录人ID，作为便签存储隔离的依据，随后再按用户加载便签
      notesUserId.value = res.data.data.id
      loadNotes()
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

onMounted(() => {
  // 先加载登录人信息，拿到用户ID后再加载该用户专属的便签数据（修复多账号共享便签问题）
  loadUserInfo()
  loadSystemTheme()
})
</script>

<style scoped>
.profile-page {
  padding: 24px;
  background-color: #e9f0ec; /* 改为柔和茶绿灰 */
  min-height: calc(100vh - 120px);
}

.profile-card, .detail-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 4px 15px rgba(17, 45, 27, 0.04);
  border: 1px solid rgba(225, 235, 230, 0.8);
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.profile-card:hover, .detail-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(17, 45, 27, 0.08);
}

.avatar-section {
  text-align: center;
  padding: 10px 0;
}

.user-avatar {
  background: linear-gradient(135deg, #2d6a4f, #112d1b) !important;
  font-size: 36px;
  margin-bottom: 14px;
  box-shadow: 0 4px 10px rgba(17, 45, 27, 0.15);
}

.user-name {
  margin: 8px 0 4px;
  color: #112d1b;
  font-weight: 700;
  font-size: 18px;
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
  border-bottom: 1px solid #e1ebe6;
}

.info-label {
  color: #6d8272;
  font-size: 14px;
}

.info-value {
  color: #112d1b;
  font-size: 14px;
  font-weight: 600;
}

.profile-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #112d1b;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #52b788;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ==================== 扩展：销售随身记草稿 ==================== */
.notepad-subtitle, .theme-subtitle, .perm-subtitle {
  font-size: 12px;
  color: #6d8272;
  margin: -8px 0 16px 0;
  line-height: 1.4;
}

.notepad-input-zone {
  margin-bottom: 14px;
}

.notepad-input-zone :deep(.el-input-group__append) {
  background-color: #52b788 !important;
  color: #ffffff !important;
  border: none !important;
  cursor: pointer;
  transition: all 0.2s;
}

.notepad-input-zone :deep(.el-input-group__append:hover) {
  opacity: 0.9;
}

.notepad-list {
  max-height: 200px;
  overflow-y: auto;
  padding-right: 4px;
}

.notepad-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f4f8f5;
  border: 1px solid #e1ebe6;
  margin-bottom: 8px;
  transition: all 0.2s;
}

.notepad-item:hover {
  background: #e9f2eb;
  border-color: #b2d8c3;
}

.notepad-item :deep(.el-checkbox) {
  flex: 1;
  display: flex;
  align-items: center;
  height: auto;
  white-space: normal;
}

.notepad-item :deep(.el-checkbox__label) {
  padding-left: 10px;
  line-height: 1.3;
}

.note-text {
  color: #112d1b;
  font-size: 13px;
  transition: all 0.2s;
}

.note-done .note-text {
  text-decoration: line-through;
  color: #c0ccc3 !important;
}

.delete-note-btn {
  color: #cb4345 !important;
  opacity: 0;
  transition: opacity 0.2s;
}

.notepad-item:hover .delete-note-btn {
  opacity: 1;
}

.notepad-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* ==================== 扩展：个性化换肤色盘 ==================== */
.theme-palette-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 12px;
}

.theme-color-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1);
  padding: 8px;
  border-radius: 12px;
  border: 2px solid transparent;
}

.theme-color-block:hover {
  background: #f4f8f5;
  transform: translateY(-2px);
}

.theme-color-block.theme-active {
  background: #e9f2eb;
  border-color: #52b788;
}

.theme-color-preview {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.12);
  margin-bottom: 8px;
  position: relative;
  overflow: hidden;
}

.theme-color-preview .check-icon {
  color: #ffffff;
  font-size: 20px;
  font-weight: bold;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.25));
}

.theme-color-name {
  font-size: 13px;
  color: #112d1b;
  font-weight: 600;
}

/* ==================== 优化：具体操作权限面板 ==================== */
.perm-collapse {
  border: none !important;
}

.perm-collapse :deep(.el-collapse-item__header) {
  border: 1px solid #e1ebe6 !important;
  border-radius: 10px;
  margin-bottom: 10px;
  padding: 0 16px;
  height: 48px;
  line-height: 48px;
  background: #f4f8f5 !important;
  transition: all 0.2s;
}

.perm-collapse :deep(.el-collapse-item__header:hover) {
  background: #e9f2eb !important;
  border-color: #b2d8c3 !important;
}

.perm-collapse :deep(.el-collapse-item__wrap) {
  border: none !important;
  background: transparent !important;
}

.perm-collapse :deep(.el-collapse-item__content) {
  padding: 10px 16px 20px 16px;
}

.perm-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 15px;
}

.group-title {
  font-weight: 600;
  color: #112d1b;
  font-size: 14px;
}

.group-badge {
  background-color: #e1ebe6 !important;
  color: #112d1b !important;
  border: 1px solid #c0ccc3 !important;
}

.perm-sub-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.perm-sub-item {
  background: #ffffff;
  border: 1px solid #e1ebe6;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: all 0.2s;
}

.perm-sub-item:hover {
  border-color: #52b788;
  box-shadow: 0 4px 10px rgba(82, 183, 136, 0.08);
}

.perm-name-cn {
  font-size: 13px;
  font-weight: 600;
  color: #112d1b;
}

.perm-code-en {
  font-size: 11px;
  font-family: monospace;
  color: #7b9487;
  letter-spacing: 0.2px;
}
</style>
