<template>
  <div class="login-container">
    <el-card class="login-card" shadow="never" :body-style="{ padding: '32px' }">
      <!-- 可选的 Logo 或图标 -->
      <div class="login-header">
        <el-avatar :size="60" :icon="UserFilled" style="background: var(--el-color-primary); margin-bottom: 16px;"/>
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
          <el-link type="primary" :underline="false">忘记密码？</el-link>
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
            登录
          </el-button>
        </el-form-item>

        <!-- 注册引导 -->
        <div class="register-tip">
          还没有账号？
          <el-link type="primary" :underline="false">立即注册</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {ElMessage} from 'element-plus'
import {User, Lock, UserFilled} from '@element-plus/icons-vue'
import {doGet, doPost} from "@/http/httpRequest.js";
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

//函数钩子，渲染dom元素后触发
onMounted(() => {
  freeLogin();
})

const freeLogin = async () => {
  const token = window.localStorage.getItem(getTokenName());
  if (token) {
    //token不为空
    const response = await doGet("api/login/free", {});
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
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(145deg, #f5f7fa 0%, #e9ecf3 100%);
  /* 更柔和的背景色 */
}

.login-card {
  width: 100%;
  max-width: 440px;
  margin: 0 20px;
  border-radius: 20px;
  transition: transform 0.3s, box-shadow 0.3s;
  border: none;
}

.login-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 30px -10px rgba(0, 0, 0, 0.15) !important;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.login-title {
  margin: 0 0 4px 0;
  font-weight: 600;
  font-size: 24px;
  color: #1a1a1a;
}

.login-subtitle {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 4px 12px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset, 0 2px 12px rgba(64, 158, 255, 0.2);
}

.form-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0 24px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 12px;
  letter-spacing: 0.5px;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.3);
  transition: all 0.3s;
}

.login-button:hover {
  transform: scale(1.02);
  box-shadow: 0 10px 20px rgba(64, 158, 255, 0.4);
}

.register-tip {
  text-align: center;
  margin-top: 16px;
  color: #666;
  font-size: 14px;
}

/* 移动端优化 */
@media (max-width: 480px) {
  .login-card {
    margin: 0 12px;
  }

  .login-card :deep(.el-card__body) {
    padding: 24px !important;
  }

  .login-title {
    font-size: 22px;
  }
}
</style>