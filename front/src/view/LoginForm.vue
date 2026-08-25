<template>
  <div class="login-container">
    <!-- 装饰性浮动元素 -->
    <div class="deco-circle deco-circle-1"></div>
    <div class="deco-circle deco-circle-2"></div>
    <div class="deco-circle deco-circle-3"></div>

    <div class="login-wrapper">
      <!-- 左侧品牌展示区 -->
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-logo">M</div>
          <h1 class="brand-name">管理系统</h1>
          <p class="brand-slogan">高效协作 · 数据驱动 · 智慧管理</p>
          <div class="brand-features">
            <div class="feature-item">
              <el-icon><DataAnalysis/></el-icon>
              <span>全链路客户线索管理</span>
            </div>
            <div class="feature-item">
              <el-icon><TrendCharts/></el-icon>
              <span>可视化商机统计分析</span>
            </div>
            <div class="feature-item">
              <el-icon><Lock/></el-icon>
              <span>企业级权限安全体系</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <el-card class="login-card" shadow="never" :body-style="{ padding: '36px' }">
      <!-- Logo 与标题 -->
      <div class="login-header">
        <el-avatar :size="56" :icon="UserFilled" class="login-avatar"/>
        <h2 class="login-title">欢迎回来</h2>
        <p class="login-subtitle">请登录您的账号继续</p>
      </div>

      <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="0"
          status-icon
          class="login-form"
      >
        <!-- 账号输入框：图标在左侧 -->
        <el-form-item prop="account">
          <el-input
              v-model="loginForm.loginAct"
              placeholder="请输入账号"
              clearable
              size="large"
          >
            <template #prefix>
              <el-icon>
                <User/>
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <el-input
              v-model="loginForm.loginPwd"
              type="password"
              placeholder="请输入密码"
              clearable
              show-password
              size="large"
              @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon>
                <Lock/>
              </el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 辅助功能行：记住密码 + 忘记密码 -->
        <div class="form-extra">
          <el-checkbox v-model="rememberMe">记住密码</el-checkbox>
          <el-link type="primary" :underline="false" @click="openResetDialog">忘记密码？</el-link>
        </div>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button
              type="primary"
              :loading="loading"
              size="large"
              class="login-button"
              @click="handleLogin"
          >
            <el-icon v-if="!loading" style="margin-right: 6px"><Promotion/></el-icon>
            登 录
          </el-button>
        </el-form-item>

        <!-- 注册引导 -->
        <div class="register-tip">
          还没有账号？
          <el-link type="primary" :underline="false" @click="openRegisterDialog">立即注册</el-link>
        </div>
      </el-form>
    </el-card> <!-- 结束登录卡片，将弹窗节点彻底剥离出卡片范围，防止受到 hover:translate 引起的重绘抖动 -->
    </div> <!-- 结束 login-wrapper 双栏布局 -->

    <!-- [新加的弹层 1]：新用户注册弹窗Dialog (追加 :append-to-body="true" 挂载到 body 下) -->
    <el-dialog v-model="registerVisible" title="新专员注册" width="480px" :close-on-click-modal="false" :destroy-on-close="true" :append-to-body="true" align-center>
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="90px">
        <el-form-item label="登录账号" prop="loginAct">
          <el-input v-model="registerForm.loginAct" :prefix-icon="User" placeholder="请设置以字母开头的4-16位账号" clearable />
        </el-form-item>
        <el-form-item label="真实姓名" prop="name">
          <el-input v-model="registerForm.name" :prefix-icon="User" placeholder="请输入您的姓名" clearable />
        </el-form-item>
        <el-form-item label="设置密码" prop="loginPwd">
          <el-input type="password" show-password :prefix-icon="Lock" v-model="registerForm.loginPwd" placeholder="请设置登录强度密码" clearable />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPwd">
          <el-input type="password" show-password :prefix-icon="Lock" v-model="registerForm.confirmPwd" placeholder="请确认登录密码" clearable />
        </el-form-item>
        <el-form-item label="工作邮箱" prop="email">
          <el-input v-model="registerForm.email" :prefix-icon="Message" placeholder="重置密码等身份校验的重要媒介" clearable />
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="registerForm.phone" :prefix-icon="Iphone" placeholder="支持11位手机号格式" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="registerVisible = false">取消</el-button>
          <el-button type="primary" :loading="registerBtnLoading" @click="submitRegister">立即注册</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- [新加的弹层 2]：忘记密码重置弹窗Dialog (同理，挂载到 body 下) -->
    <el-dialog v-model="resetVisible" title="找回账户密码" width="480px" :close-on-click-modal="false" :destroy-on-close="true" :append-to-body="true" align-center>
      <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef" label-width="90px">
        <el-form-item label="用户名" prop="loginAct">
          <el-input v-model="resetForm.loginAct" :prefix-icon="User" placeholder="请输入需要重置的登录账号" clearable />
        </el-form-item>
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="resetForm.email" :prefix-icon="Message" placeholder="填入该账户验证绑定的安全邮箱" clearable />
        </el-form-item>
        <el-form-item label="校验码" prop="code">
          <div style="display: flex; gap: 10px; width: 100%;">
            <el-input v-model="resetForm.code" placeholder="6位邮箱校验码" style="flex:1" clearable />
            <el-button type="primary" :disabled="countdown > 0" @click="sendVerificationCode">
              {{ countdown > 0 ? `${countdown}s 后重新获取` : '获取核验码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="重置密码" prop="newPassword">
          <el-input type="password" show-password :prefix-icon="Lock" v-model="resetForm.newPassword" placeholder="设置物理级高强登录密码" clearable />
        </el-form-item>
        <el-form-item label="密码确认" prop="confirmPassword">
          <el-input type="password" show-password :prefix-icon="Lock" v-model="resetForm.confirmPassword" placeholder="请再次确认输入的新密码" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetVisible = false">取消</el-button>
          <el-button type="primary" :loading="resetBtnLoading" @click="submitResetPassword">确认重写密码</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {User, Lock, UserFilled, Message, Iphone, DataAnalysis, TrendCharts, Promotion} from '@element-plus/icons-vue'
import {doGet, doPost, doPut, doPostJson} from "@/http/httpRequest.js";
import {getTokenName, removeToken} from "@/util/util.js";

const loginForm = reactive({
  loginAct: '',
  loginPwd: ''
})
const rememberMe = ref(false)
const loading = ref(false)
const loginFormRef = ref(null)


const loginRules = {
  loginAct: [
    {required: true, message: '请输入账号', trigger: 'blur'},
    {min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur'}
  ],
  loginPwd: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur'}
  ]
}

//用户注册模块
const registerVisible = ref(false)
const registerBtnLoading = ref(false)
const registerFormRef = ref(null)
const registerForm = reactive({
  loginAct: '',
  name: '',
  loginPwd: '',
  confirmPwd: '',
  email: '',
  phone: ''
})

const checkConfirmPwd = (rule, value, callback) => {
  if (value !== registerForm.loginPwd) {
    callback(new Error('两次输入的新密码不匹配一致，请检查'))
  } else {
    callback()
  }
}

const registerRules = {
  loginAct: [
    { required: true, message: '请填写登录主用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,15}$/, message: '由字母开头、长度4-16位且仅包含字母/数字/下划线', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请真实填写个人姓名，用于CRM系统审计', trigger: 'blur' },
    { min: 2, max: 10, message: '姓名长度应在2到10位字符之间', trigger: 'blur' }
  ],
  loginPwd: [
    { required: true, message: '请输入注册登录密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请确认您的登录密码', trigger: 'blur' },
    { validator: checkConfirmPwd, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '绑定邮箱是日后取回身份与校验的唯一钥匙哦', trigger: 'blur' },
    { type: 'email', message: '请填入准确的标准邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请提供您的手机号码，方便系统留档', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入合法的11位中国手机号格式', trigger: 'blur' }
  ]
}

const openRegisterDialog = () => {
  registerVisible.value = true
  if (registerFormRef.value) registerFormRef.value.resetFields()
}

const submitRegister = async () => {
  if (!registerFormRef.value) return
  
  try {
    await registerFormRef.value.validate()
    registerBtnLoading.value = true
    // 引入注册交互 API 调用，改用 doPostJson 发送原生 JSON，适配后端的 @RequestBody 接收
    const r = await doPostJson("/api/register", { ...registerForm })
    if (r.data && r.data.code === 200) {
      ElMessage.success("恭喜您，注册成功且已生成初始专员档，去右侧完成登录吧")
      registerVisible.value = false
    } else {
      ElMessage.error((r.data && r.data.msg) || "被管理员规则拦截了")
    }
  } catch (error) {
    console.error("表单校验未通过或注册请求出错:", error)
  } finally {
    registerBtnLoading.value = false
  }
}

//忘记密码并重设密码模块
const resetVisible = ref(false)
const resetBtnLoading = ref(false)
const resetFormRef = ref(null)
const countdown = ref(0)
let timer = null

const resetForm = reactive({
  loginAct: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const checkResetConfirmPwd = (rule, value, callback) => {
  if (value !== resetForm.newPassword) {
    callback(new Error('两次输入的新密码不一致，请检查'))
  } else {
    callback()
  }
}

const resetRules = {
  loginAct: [{ required: true, message: '请输入要重设密码的登录账号名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入账户关联的安全邮箱', trigger: 'blur' },
    { type: 'email', message: '不是合法的电子邮箱模式', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '验证码不能为空', trigger: 'blur' },
    { len: 6, message: '它是一个固定的6位数字校验码哦', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请定义重塑密码方案', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认重置的密码', trigger: 'blur' },
    { validator: checkResetConfirmPwd, trigger: 'blur' }
  ]
}

const openResetDialog = () => {
  resetVisible.value = true
  if (resetFormRef.value) resetFormRef.value.resetFields()
}

// 邮件重设短信核验触发器与60秒倒数：
const sendVerificationCode = async () => {
  if (!resetForm.loginAct || !resetForm.email) {
    ElMessage.warning("请务必填写预先对应的登录账号与邮箱信息才可以提取验证码哦")
    return
  }
  
  try {
    // 修复 Bug：添加前置斜杠，且通过 r.data 获取返回数据包装体
    const r = await doGet("/api/password/reset/code", {
      loginAct: resetForm.loginAct,
      email: resetForm.email
    })
    
    if (r.data && r.data.code === 200) {
      ElMessage.success("验证验证码正插空发到您的邮件，请在5分钟内完成核验")
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    } else {
      ElMessage.error((r.data && r.data.msg) || "发送动作由于防刷或其他拦截失败")
    }
  } catch (error) {
    ElMessage.error("网络不通，请检查是否出现服务器断离。")
  }
}

const submitResetPassword = async () => {
  if (!resetFormRef.value) return
  
  try {
    await resetFormRef.value.validate()
    resetBtnLoading.value = true
    // 修复 Bug：添加前置斜杠，通过 r.data 解析状态且支持 Promise 式 validate 校验
    const r = await doPut("/api/password/reset", {
      loginAct: resetForm.loginAct,
      code: resetForm.code,
      newPassword: resetForm.newPassword
    })
    if (r.data && r.data.code === 200) {
      ElMessage.success("恭喜您已经成功解开历史限制，重塑密码完成，请登录！")
      resetVisible.value = false
    } else {
      ElMessage.error((r.data && r.data.msg) || "操作不合规，拒绝了修改")
    }
  } catch (error) {
    console.error("表单校验未通过或请求重置密码出错:", error)
  } finally {
    resetBtnLoading.value = false
  }
}

//函数钩子，渲染dom元素后触发
onMounted(() => {
  freeLogin();
})

const freeLogin = async () => {
  const token = window.localStorage.getItem(getTokenName());
  if (token) {
    //token不为空
    const response = await doGet("/api/login/free", {});
    console.log('response响应值为：' , response);

    if (response.data.code === 200 ) {
      //token验证成功
      window.location.href = "/dashboard";
    }
  }
}


//处理登录请求
const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate()
  loading.value = true

  //构建请求数据
  const requestData = {
    loginAct: loginForm.loginAct,
    loginPwd: loginForm.loginPwd,
    rememberMe: rememberMe.value
  }
  console.log('发送的登录参数:', requestData)

  // ✅ 正确：直接 await，不要混用 then
  const response = await doPost('/api/login', requestData)

  console.log('后台用户信息', response)


  // 处理业务状态码
  if (response.data.code === 200) {
    //登录成功
    ElMessage.success('登录成功！')
    ElMessage.success("你好 " + response.data.data.loginAct)

    //删除之前存储在localStorage和sessionStorage里面的token
    removeToken();
    // 保存 token 或跳转
    //前端存储jwt
    if (rememberMe.value === true) {
      window.localStorage.setItem(getTokenName(), response.data.data);
    } else {
      window.sessionStorage.setItem(getTokenName(), response.data.data);
    }
    // 跳转系统总页面
    window.location.href = "/dashboard";


  } else {
    //登录失败
    ElMessage.error(response.data.msg || '登录失败')

  }

  loading.value = false
}

</script>

<style scoped>
/* ============ 页面容器：与 Dashboard 一致的清新绿色品牌渐变 ============ */
.login-container {
  position: relative;
  height: 100vh;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 60%, #a5d6a7 100%);
  overflow: hidden;
  box-sizing: border-box;
  padding: 0 20px; /* 容器内边距替代子元素 margin，避免 flex 子项宽度计算溢出 */
}

/* ============ 浮动装饰圆，增强高级感 ============ */
.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
  pointer-events: none;
}
.deco-circle-1 {
  width: 320px; height: 320px;
  background: radial-gradient(circle, rgba(76, 175, 80, 0.25), transparent 70%);
  top: -80px; left: -80px;
  animation: float 9s ease-in-out infinite;
}
.deco-circle-2 {
  width: 240px; height: 240px;
  background: radial-gradient(circle, rgba(46, 125, 50, 0.18), transparent 70%);
  bottom: -60px; right: 8%;
  animation: float 11s ease-in-out infinite reverse;
}
.deco-circle-3 {
  width: 150px; height: 150px;
  background: radial-gradient(circle, rgba(165, 214, 167, 0.6), transparent 70%);
  top: 18%; right: 22%;
  animation: float 7s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-24px) scale(1.05); }
}

/* ============ 双栏布局包装器 ============ */
.login-wrapper {
  display: flex;
  align-items: stretch;
  width: 100%;
  max-width: 880px; /* 修复：配合容器 padding 收缩，不再溢出视口右边界 */
  box-sizing: border-box;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 24px 48px -12px rgba(27, 94, 32, 0.25);
  animation: fadeUp 0.6s ease-out;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============ 左侧品牌展示区 ============ */
.brand-panel {
  flex: 1 1 0;   /* 弹性填充剩余空间，与右侧卡片形成均衡分栏 */
  min-width: 0;
  display: flex;
  align-items: center;
  background: linear-gradient(160deg, #2e7d32 0%, #4caf50 55%, #66bb6a 100%);
  color: #fff;
  padding: 48px 36px;
  box-sizing: border-box;
  position: relative;
}
.brand-panel::after {
  content: '';
  position: absolute;
  width: 220px; height: 220px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  right: -70px; bottom: -70px;
}
.brand-content {
  position: relative;
  z-index: 1;
}
.brand-logo {
  width: 52px; height: 52px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  backdrop-filter: blur(4px);
}
.brand-name {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 1px;
}
.brand-slogan {
  font-size: 13px;
  opacity: 0.85;
  margin: 0 0 36px;
  letter-spacing: 2px;
}
.brand-features .feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  padding: 12px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.2);
}
.brand-features .feature-item:last-child {
  border-bottom: none;
}
.brand-features .el-icon {
  font-size: 18px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  padding: 6px;
}

/* ============ 右侧登录卡片 ============ */
.login-card {
  flex: 0 1 460px;   /* 基准 460px，允许收缩；左侧品牌区弹性填充剩余空间 */
  min-width: 0;      /* 允许 flex 子项收缩到内容宽度以下 */
  border-radius: 0;
  border: none;
  background: #ffffff;
}
.login-card :deep(.el-card__body) {
  width: 100%;
  box-sizing: border-box; /* 核心修复：padding 计入宽度，内容不再溢出右边界 */
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 26px;
}
.login-avatar {
  background: linear-gradient(145deg, #4caf50, #66bb6a);
  margin-bottom: 14px;
  box-shadow: 0 6px 14px rgba(76, 175, 80, 0.4);
}
.login-title {
  margin: 0 0 4px 0;
  font-weight: 700;
  font-size: 24px;
  color: #1b5e20;
}
.login-subtitle {
  margin: 0;
  color: #8a9a8e;
  font-size: 13px;
}

/* ============ 表单美化：绿色主题 ============ */
.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}
.login-form :deep(.el-input__wrapper) {
  padding: 4px 12px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(46, 125, 50, 0.06);
  transition: box-shadow 0.25s;
}
.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 14px rgba(76, 175, 80, 0.22);
}
.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #4caf50 inset, 0 4px 14px rgba(76, 175, 80, 0.22);
}
.login-form :deep(.el-input__prefix .el-icon) {
  color: #66bb6a;
}
.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0 24px;
}
.form-extra :deep(.el-link--primary) {
  color: #2e7d32;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  letter-spacing: 2px;
  border: none;
  background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%);
  box-shadow: 0 8px 20px rgba(76, 175, 80, 0.35);
  transition: all 0.3s;
}
.login-button:hover, .login-button:focus {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(76, 175, 80, 0.45);
  background: linear-gradient(135deg, #43a047 0%, #5cb860 100%);
}
.login-button:active {
  transform: translateY(0);
}

.register-tip {
  text-align: center;
  margin-top: 18px;
  color: #8a9a8e;
  font-size: 14px;
}
.register-tip :deep(.el-link--primary) {
  color: #2e7d32;
  font-weight: 600;
}

/* ============ 响应式适配 ============ */
@media (max-width: 768px) {
  .brand-panel {
    display: none; /* 移动端只保留表单区，避免拥挤 */
  }
  .login-wrapper {
    max-width: 460px;
    border-radius: 20px;
  }
  .login-card {
    width: 100%;
    border-radius: 20px;
  }
}
@media (max-width: 480px) {
  .login-container {
    justify-content: center;
  }
  .login-wrapper {
    margin: 0 12px;
  }
  .login-card :deep(.el-card__body) {
    padding: 26px !important;
  }
  .login-title {
    font-size: 22px;
  }
}
</style>