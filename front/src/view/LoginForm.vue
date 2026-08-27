<template>
  <!-- 整个浏览器界面为火车车厢内壁，锁定上下滑动 -->
  <div class="cabin-viewport" @wheel.prevent="handleWheel">
    
    <!-- 【前景覆盖层】：精美座名牌 (悬浮在钢板前) -->
    <div class="seat-badge seat-badge-left">09<br/><span>A</span></div>
    <div class="seat-badge seat-badge-right">16<br/><span>B</span></div>

    <!-- 铬电镀拉丝车窗 3D 拟真金属物理外框 -->
    <div class="train-window-frame">
        
        <!-- 车窗内部高保真裁剪蒙版 (在内部实现所有视差滚动，绝对保证不溢出车窗外，对齐完美) -->
        <div class="train-window-inner">
          
          <!-- ==================== 【唯一、横贯全局的超长全景风光长卷】 ==================== -->
          <!-- 采用 140% 宽度，起始 left 设为 0。滑轮滚到底时，最大仅往左平行视差平移自身 25% 的距离！也就是物理位移 35% 窗宽。大图在最右端还额外保留了 5% 的安全余量空间，彻底根治一切背景露黑漏洞！ -->
          <div class="window-landscape-wide-panorama" :style="{ transform: 'translateX(' + (-scrollProgress * 25) + '%) scale(1.05)' }"></div>
          
          <div class="scrolling-shadows"></div>
          <div class="window-atmosphere-overlay"></div>

          <!-- ====== 【第一画区：春之森林飞驰意境】 (绝对定位，随着滑动在 X 轴向左退场并淡出) ====== -->
          <div class="track-block block-train-view" :style="{ opacity: 1 - scrollProgress * 1.5, transform: 'translateX(' + (-scrollProgress * 100) + '%)' }">
            
            <!-- 治愈文案 -->
            <div class="window-caption">
              <h2 class="caption-main">世界很大</h2>
              <h1 class="caption-sub">我们一起去看看</h1>
              <p class="caption-desc">JOURNEY · CRM SYSTEM</p>
            </div>

            <!-- 滑动滚轮指示器 -->
            <div class="scroll-tip-indicator" @click="autoGlideToLogin" style="cursor: pointer; pointer-events: auto;">
              <span class="tip-text">点击或滑动滚轮，开启你的旅程</span>
              <div class="mouse-icon">
                <div class="mouse-wheel"></div>
              </div>
              <el-icon class="arrow-down-bounce"><ArrowDown /></el-icon>
            </div>
          </div>

          <!-- ====== 【第二画区：列车停靠终点·静谧居中登录】 (绝对定位，从右侧滑动入场，在完成时正好 0 偏移精确居中于车窗物理正中央) ====== -->
          <div class="track-block block-login-view" :style="{ transform: 'translateX(' + ((1 - scrollProgress) * 100) + '%)' }">
            
            <!-- 登录表单定位包装器 (随着滚动在 Y 轴伴随 45px 的落差淡入，确保与大轨差速契合，极其顺畅) -->
            <div class="journey-login-wrapper" :style="{ transform: 'translateY(' + (-45 * (1 - loginReveal)) + 'px)', opacity: loginReveal }">
              
              <!-- 墨绿磨砂拉窗式登录表单卡 (调高遮光率至 96.5%，降低过高透明度带来的背景干扰，卡片超矮化紧凑设计保证所有分辨率一屏100%全显露) -->
              <div class="journey-login-card">
                
                <!-- 精致微顶 Ribbon 小条替代高占用 Avatar 和大标题 (直接缩减 110px 物理高度，确保注册一屏可见) -->
                <div class="journey-header-thin">
                  <div class="ribbon-brand">
                    <span class="ribbon-logo">🍃</span>
                    <span class="ribbon-text">专员登录舱 · 欢迎回到 {{ systemConfig.name }} 智慧管理端</span>
                  </div>
                </div>

                <!-- 登录表单本体 -->
                <el-form
                  ref="loginFormRef"
                  :model="loginForm"
                  :rules="loginRules"
                  label-width="0"
                  status-icon
                  class="journey-form"
                >
                  <!-- 账号输入 -->
                  <el-form-item prop="account">
                    <el-input
                      v-model="loginForm.loginAct"
                      placeholder="用户名/邮箱"
                      clearable
                    >
                      <template #prefix>
                        <el-icon><User /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>

                  <!-- 密码输入 -->
                  <el-form-item prop="password">
                    <el-input
                      v-model="loginForm.loginPwd"
                      type="password"
                      placeholder="密码"
                      clearable
                      show-password
                      @keyup.enter="handleLogin"
                    >
                      <template #prefix>
                        <el-icon><Lock /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>

                  <!-- 辅助逻辑 -->
                  <div class="journey-extra">
                    <el-checkbox v-model="rememberMe">记住我</el-checkbox>
                    <el-link class="forgot-link" :underline="false" @click="openResetDialog">忘记密码？</el-link>
                  </div>

                  <!-- 登录提交 -->
                  <el-form-item>
                    <el-button
                      type="primary"
                      :loading="loading"
                      class="journey-button"
                      @click="handleLogin"
                    >
                      登 录
                    </el-button>
                  </el-form-item>

                  <!-- 注册引导 & 微信合并底栏 (高度极其干炼省位，100% 漏出在用户的视平线上！) -->
                  <div class="journey-register-tip">
                    还没有账号？ <span class="register-clickable" @click="openRegisterDialog">立即注册</span>
                    <span class="wechat-quick-link" @click="openWechatTip"> | 微信快捷</span>
                  </div>
                </el-form>

              </div>
            </div> <!-- journey-login-wrapper -->
          </div>

        </div> <!-- train-window-inner -->
      </div> <!-- train-window-frame -->

    <!-- [全局注册弹窗] -->
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

    <!-- [全局密码重置弹窗] -->
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
import {ref, reactive, onMounted, computed} from 'vue'
import {ElMessage} from 'element-plus'
import {User, Lock, UserFilled, Message, Iphone, DataAnalysis, TrendCharts, Promotion, ArrowDown} from '@element-plus/icons-vue'
import {doGet, doPost, doPut, doPostJson} from "@/http/httpRequest.js";
import {getTokenName, removeToken} from "@/util/util.js";

// 全局公共系统配置
const systemConfig = ref({
  name: '智路 CRM',
  title: '智路 CRM 管理系统'
})

const loadSystemConfig = async () => {
  try {
    const res = await doGet("/api/system/info/public", {});
    if (res.data.code === 200 && res.data.data) {
      systemConfig.value = res.data.data;
      if (res.data.data.title) {
        document.title = res.data.data.title;
      }
    }
  } catch (error) {
    console.error("加载公开系统配置异常，已使用保底兜底信息:", error);
  }
}

// ====== 开启火车旅行·横向流动视差阻尼滑轨核心控制 ======
const scrollProgress = ref(0) // 核心缓释进度比例 (0 ~ 1)
let targetProgress = 0        // 滑轮滑动指向的目标进度
let animId = null             // requestAnimationFrame 动画循环 ID

// 人性化登录浮现系数 (全流程平滑渐入，极富渐进性，保障登录绝对一目了然)
const loginReveal = computed(() => {
  return scrollProgress.value
})

const openWechatTip = () => {
  ElMessage.info("微信扫码快捷登录通道正在整备中，请先使用专员账号登录哦。🍃")
}

// 鼠标滑轮滚动推进监听 (灵敏度加权至 0.0022，保障任意高低 DPI 滚轮或触摸板触控行进更顺脚)
const handleWheel = (e) => {
  targetProgress += e.deltaY * 0.0022
  targetProgress = Math.max(0, Math.min(1, targetProgress))
  
  if (!animId) {
    updateAnimation()
  }
}

// 缓和阻尼物理惯性推导
const updateAnimation = () => {
  const diff = targetProgress - scrollProgress.value
  if (Math.abs(diff) > 0.0004) {
    scrollProgress.value += diff * 0.095 // 0.095 为惯性缓释速度系数
    animId = requestAnimationFrame(updateAnimation)
  } else {
    scrollProgress.value = targetProgress
    cancelAnimationFrame(animId)
    animId = null
  }
}

// 一键平滑直连终点站 (点击首屏引箭或下方提示即能自动加速平靠终站，绝顶人性化)
const autoGlideToLogin = () => {
  targetProgress = 1.0
  if (!animId) {
    updateAnimation()
  }
}

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

const sendVerificationCode = async () => {
  if (!resetForm.loginAct || !resetForm.email) {
    ElMessage.warning("请务必填写预先对应的登录账号与邮箱信息才可以提取验证码哦")
    return
  }
  
  try {
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

// 渲染 DOM 后挂载
onMounted(() => {
  loadSystemConfig();
  freeLogin();
  
  // 查找并监听 3D 阻尼平移
  const scroller = document.querySelector('.cabin-viewport');
  if (scroller) {
    scroller.addEventListener('wheel', handleWheel, { passive: false });
  }
})

const freeLogin = async () => {
  const token = window.localStorage.getItem(getTokenName());
  if (token) {
    const response = await doGet("/api/login/free", {});
    console.log('response响应值为：' , response);

    if (response.data.code === 200 ) {
      window.location.href = "/dashboard";
    }
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate()
  loading.value = true

  const requestData = {
    loginAct: loginForm.loginAct,
    loginPwd: loginForm.loginPwd,
    rememberMe: rememberMe.value
  }
  console.log('发送的登录参数:', requestData)

  const response = await doPost('/api/login', requestData)
  console.log('后台用户信息', response)

  if (response.data.code === 200) {
    ElMessage.success('登录成功！')
    ElMessage.success("你好 " + response.data.data.loginAct)
    removeToken();
    if (rememberMe.value === true) {
      window.localStorage.setItem(getTokenName(), response.data.data);
    } else {
      window.sessionStorage.setItem(getTokenName(), response.data.data);
    }
    window.location.href = "/dashboard";
  } else {
    ElMessage.error(response.data.msg || '登录失败')
  }
  loading.value = false
}

</script>

<style scoped>
/* ==================== 锁定整个浏览器上下视窗防滑脱 & 列车座舱主布局基座 ==================== */
.cabin-viewport {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
  background: 
    linear-gradient(135deg, #090e09 0%, #152417 50%, #060906 100%);
}

/* 拟物 3D 电镀拉丝车窗金属物理外框 */
.train-window-frame {
  position: relative;
  width: 82vw;
  height: 64vh;
  max-width: 1100px;
  max-height: 520px;
  min-height: 430px; /* 保证卡片有足够且完美的支撑高度 */
  border-radius: 46px;
  background: #233124; /* 墨青钢片漆 */
  padding: 16px;
  box-sizing: border-box;
  box-shadow: 
    0 35px 85px rgba(0, 0, 0, 0.8), 
    inset 0 12px 30px rgba(0, 0, 0, 0.75),
    0 0 0 4px rgba(89, 107, 91, 0.65), /* 鎏金丝 */
    0 6px 1px 7px rgba(0,0,0,0.5);
  z-index: 10;
}

/* 车窗内边缘物理裁剪容器 (沙盒遮罩：所有横向履带仅在其内部滑动，多余元素绝不外溢) */
.train-window-inner {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 30px;
  overflow: hidden; /* Clipper 沙盒 */
  background-color: #050a06;
  box-shadow: inset 0 20px 40px rgba(0, 0, 0, 0.9);
}

/* ==================== 【唯一、横贯全局的超长全景风光长卷】 ==================== */
/* 宽度设为 140% 极画卷，从 left 0 起始，仅滑行 X轴 25% 距离，彻底打消黑边露白！ */
.window-landscape-wide-panorama {
  position: absolute;
  top: 0; left: 0;
  width: 140%;
  height: 100%;
  background-image: url('https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?auto=format&fit=crop&w=1600&q=80');
  background-position: center 46%;
  background-size: cover;
  transition: transform 0.1s cubic-bezier(0.1, 0.75, 0.25, 1);
  z-index: 1;
}

/* 前台悬浮座号牌 (固定附着在车壁钢体上，营造 3D 立体空间) */
.seat-badge {
  position: absolute;
  padding: 11px 9px;
  background: linear-gradient(135deg, #a7835b 0%, #72522e 100%);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  color: #f0dbbe;
  font-family: monospace;
  font-size: 15px;
  font-weight: bold;
  text-align: center;
  line-height: 12px;
  box-shadow: 
    0 6px 15px rgba(0,0,0,0.5),
    inset 0 1px 1px rgba(255,255,255,0.2);
  width: 25px;
  z-index: 28; /* 高于跑道 */
}
.seat-badge::after {
  content: '·';
  position: absolute;
  top: 2px; left: 50%; transform: translateX(-50%);
  font-size: 10px; color: rgba(255, 255, 255, 0.5);
}
.seat-badge span {
  font-size: 9px;
  font-weight: 300;
  opacity: 0.8;
}
.seat-badge-left {
  left: 3.5vw;
  top: 36vh;
}
.seat-badge-right {
  right: 3.5vw;
  top: 36vh;
}

/* ==================== 窗内：横向视差两幅分轴 (背景设为透明) ==================== */
.window-parallax-track {
  display: none; 
}

/* 单节跑道幅块 (绝对定位沙盒框架，通过 translateX 差值对流实现在滑动结束时 100% 投射在窗心正中央) */
.track-block {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0; left: 0;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10;
}

/* ===== 【第一幅画区：铁轨森林意境】 ===== */
.block-train-view {
  text-align: center;
  transition: transform 0.1s cubic-bezier(0.1, 0.75, 0.25, 1), opacity 0.1s ease-out;
}

.scrolling-shadows {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: radial-gradient(circle at 50% 50%, transparent 60%, rgba(0,0,0,0.45) 100%);
  z-index: 5;
  pointer-events: none;
}

.window-atmosphere-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-image: 
    linear-gradient(45deg, rgba(255, 255, 255, 0.015) 0%, transparent 80%),
    radial-gradient(circle at 50% 50%, rgba(92, 139, 99, 0.05), transparent 75%);
  z-index: 2;
  pointer-events: none;
}

.window-caption {
  position: relative;
  z-index: 12;
  font-family: 'PingFang SC', sans-serif;
  color: #ffffff;
  pointer-events: none;
}
.caption-main {
  font-size: 24px;
  font-weight: 300;
  color: #dfede1;
  letter-spacing: 12px;
  margin: 0 0 12px 0;
  text-shadow: 0 3px 12px rgba(0, 0, 0, 0.5);
  font-family: Georgia, serif;
}
.caption-sub {
  font-size: 38px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 15px;
  margin: 0 0 20px 0;
  text-shadow: 0 5px 20px rgba(0, 0, 0, 0.6);
}
.caption-desc {
  font-size: 11px;
  font-weight: 500;
  color: #92b196;
  letter-spacing: 4px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  display: inline-block;
  padding-top: 10px;
  width: 120px;
}

/* 鼠标滚轮引导 */
.scroll-tip-indicator {
  position: absolute;
  bottom: 4vh;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 15;
  color: rgba(255, 255, 255, 0.9);
  pointer-events: auto; /* 允许点击直达 */
  transition: opacity 0.12s ease-out;
}
.tip-text {
  font-size: 11.5px;
  letter-spacing: 2px;
  font-weight: 500;
  color: #addbb4;
  text-shadow: 0 2px 5px rgba(0,0,0,0.5);
}
.mouse-icon {
  width: 18px;
  height: 30px;
  border-radius: 12px;
  border: 1.5px solid rgba(173, 219, 180, 0.85);
  position: relative;
}
.mouse-wheel {
  width: 3px;
  height: 5px;
  background-color: #addbb4;
  position: absolute;
  top: 6px; left: 50%;
  transform: translateX(-50%);
  border-radius: 2px;
  animation: mouse-slide 1.8s infinite cubic-bezier(0.215, 0.61, 0.355, 1);
}
@keyframes mouse-slide {
  0% { transform: translate(-50%, 0); opacity: 1; }
  100% { transform: translate(-50%, 13px); opacity: 0; }
}
.arrow-down-bounce {
  font-size: 12px;
  color: #addbb4;
  animation: arrow-bounce 1.5s infinite;
}
@keyframes arrow-bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(4px); }
}

/* ===== 【第二幅画区：停靠终点登录表单，绝对对齐中央】 ===== */
.block-login-view {
  transition: transform 0.1s cubic-bezier(0.1, 0.75, 0.25, 1);
}

/* 登录框包装器：支持在车窗内部弹性居中 */
.journey-login-wrapper {
  position: relative;
  z-index: 10;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  transition: transform 0.12s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.12s ease-out;
}

/* 磨砂玻璃拉窗卡片 (调高遮挡度近 96.5%：遮蔽后方森林，卡片超矮化紧凑设计保证所有分辨率一屏100%全显露) */
.journey-login-card {
  position: relative;
  width: 90%;
  max-width: 410px;
  border-radius: 20px;
  background: rgba(14, 25, 16, 0.965); /* 几乎不透光的亚光墨玉，极大拉升可读性与人性化 */
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 
    0 30px 75px rgba(1, 3, 2, 0.85),
    inset 0 1px 1px rgba(255, 255, 255, 0.15);
  padding: 20px 24px; /* 紧扣矮身材定制，全露出 */
  box-sizing: border-box;
  overflow: hidden; /* 高度完全收拢，不需要卡盘垂直滚动 */
}

/* 精致微顶 Ribbon 小条 */
.journey-header-thin {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.1);
  padding-bottom: 12px;
}
.ribbon-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ribbon-logo {
  font-size: 16px;
  animation: leaf-sway 2.5s infinite ease-in-out;
}
@keyframes leaf-sway {
  0%, 100% { transform: rotate(0deg) scale(1); }
  50% { transform: rotate(15deg) scale(1.1); }
}
.ribbon-text {
  font-size: 13px;
  font-weight: 700;
  color: #dfede1;
  letter-spacing: 1.5px;
}

/* 表单定制 */
.journey-form :deep(.el-form-item) {
  margin-bottom: 12px; 
}
.journey-form :deep(.el-input__wrapper) {
  padding: 5px 14px;
  border-radius: 12px;
  background-color: rgba(14, 25, 17, 0.6) !important;
  border: 1.5px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2) !important;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.journey-form :deep(.el-input__wrapper:hover) {
  background-color: rgba(14, 25, 17, 0.8) !important;
  border-color: rgba(123, 160, 130, 0.35) !important;
}
.journey-form :deep(.el-input__wrapper.is-focus) {
  background-color: rgba(10, 18, 12, 0.92) !important;
  border-color: #5c8b63 !important;
  box-shadow: 
    inset 0 1px 1px rgba(0,0,0,0.1),
    0 4px 18px rgba(92, 139, 99, 0.25) !important;
}

.journey-form :deep(.el-input__inner) {
  color: #ffffff !important;
  font-weight: 500;
  font-size: 13.5px;
}
.journey-form :deep(.el-input__inner::placeholder) {
  color: #6e8675;
}

.journey-form :deep(.el-input__prefix .el-icon) {
  color: #5e7764;
  font-size: 15.5px;
  transition: all 0.3s ease;
}
.journey-form :deep(.is-focus .el-input__prefix .el-icon) {
  color: #aad5b2;
  transform: translateY(-1px) scale(1.08);
}

.journey-extra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 2px 14px;
}
.journey-extra :deep(.el-checkbox__label) {
  color: #9ab4a1;
  font-weight: 500;
  font-size: 13px;
}
.journey-extra :deep(.el-checkbox__inner) {
  border-radius: 4px;
  border-color: rgba(123, 160, 130, 0.3);
  background-color: rgba(14, 25, 17, 0.6);
}
.journey-extra :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #5c8b63;
  border-color: #5c8b63;
}

.forgot-link, .register-clickable, .wechat-quick-link {
  color: #addbb4 !important;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  padding-bottom: 2px;
  transition: color 0.3s;
}
.forgot-link::after, .journey-register-tip span::after {
  content: '';
  position: absolute;
  bottom: 0; left: 50%;
  width: 0; height: 1.5px;
  background: linear-gradient(90deg, #addbb4, #7ba082);
  transition: all 0.35s cubic-bezier(0.25, 0.8, 0.25, 1);
  transform: translateX(-50%);
}
.forgot-link:hover, .register-clickable:hover, .wechat-quick-link:hover {
  color: #ffffff !important;
}
.forgot-link:hover::after, .journey-register-tip .register-clickable:hover::after {
  width: 100%;
}

.journey-button {
  position: relative;
  width: 100%;
  height: 44px; /* 精简 */
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  letter-spacing: 5px;
  border: none;
  background: linear-gradient(135deg, #4b7551 0%, #5c8b63 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.journey-button:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #5c8b63 0%, #6e9f76 100%) !important;
  box-shadow: 
    0 10px 22px rgba(0, 0, 0, 0.35),
    0 0 15px rgba(92, 139, 99, 0.3);
}

.line-divider {
  display: none; 
}
.wechat-login-row {
  display: none; 
}

.journey-register-tip {
  text-align: center;
  color: #6e8675;
  font-size: 13px;
  font-weight: 500;
  margin-top: 14px;
}
.wechat-quick-link {
  color: #728c79 !important;
}

/* ==================== 弹窗 DiaLog 全局质感 ==================== */
:deep(.el-dialog) {
  border-radius: 20px !important;
  overflow: hidden;
  box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.15) !important;
  border: 1px solid rgba(255, 255, 255, 0.8);
}
:deep(.el-dialog__header) {
  margin-right: 0px;
  padding: 24px 24px 12px;
  border-bottom: 1px solid rgba(46, 125, 50, 0.05);
}
:deep(.el-dialog__title) {
  font-weight: 700;
  font-size: 18px;
  color: #1b5e20;
}
:deep(.el-dialog__body) {
  padding: 26px 24px !important;
}
:deep(.el-dialog__footer) {
  padding: 12px 24px 24px;
  border-top: 1px solid rgba(46, 125, 50, 0.05);
}

/* ==================== 高保真响应式适配 ==================== */
@media (max-width: 820px) {
  .train-window-frame {
    width: 90vw;
    height: 70vh;
    padding: 10px;
    border-radius: 36px;
  }
  .train-window-inner {
    border-radius: 24px;
  }
  .caption-main {
    font-size: 19px;
    letter-spacing: 8px;
  }
  .caption-sub {
    font-size: 30px;
    letter-spacing: 11px;
  }
  .seat-badge {
    display: none;
  }
}
@media (max-width: 480px) {
  .train-window-frame {
    height: 76vh;
  }
  .journey-login-card {
    padding: 20px 18px;
    width: 94%;
  }
  .journey-title {
    font-size: 20px;
  }
}
</style>