<template>
  <el-container class="main-container" :class="'theme-' + activeTheme">

    <!-- 左侧导航栏 -->
    <el-aside
        :width="isCollapse ? '64px' : '200px'"
        class="sidebar"
        :style="{ background: sidebarBg }"
    >
      <div class="logo-area" @click="goToStatistic">
        <h3 v-show="!isCollapse" class="logo-text">🍃 旅途专员舱</h3>
        <div v-show="isCollapse" class="logo-short">🍃</div>
      </div>

      <el-divider/>

      <!-- 展开/收起切换按钮 -->
      <div class="toggle-btn">
        <el-button
            @click="toggleCollapse"
            :icon="isCollapse ? 'Expand' : 'Fold'"
            size="small"
            circle
        />
      </div>

      <el-menu
          class="side-menu"
          :collapse="isCollapse"
          :background-color="menuBgColor"
          :text-color="menuTextColor"
          :active-text-color="menuActiveColor"
          :unique-opened="true"
          :default-active="currentPath"
          :collapse-transition="false"
          :router="true"
          @open="handleOpen"
          @close="handleClose"
      >

        <!--循环遍历各个板块-->
        <el-sub-menu :index="index.toString()" v-for="(menuPermission, index) in userInfo.menuPermissionList" :key="menuPermission.id" :popper-class="'theme-' + activeTheme">
          <template #title>
           <el-icon> <component :is="menuPermission.icon"></component> </el-icon>
            <span>{{ menuPermission.name }}</span>
          </template>

          <el-menu-item :index="subPermission.url" v-for="subPermission in menuPermission.subPermissionList" :key="subPermission.id">
            {{ subPermission.name }}
          </el-menu-item>

        </el-sub-menu>




      </el-menu>

    </el-aside>

    <el-container class="content-container">
      <!-- 右侧头部 -->
      <el-header class="header">
        <div class="header-content">
          <div class="brand-greeting">
            <span class="greeting-emoji">🌤️</span>
            <div class="greeting-text">
              <h3>{{ systemConfig.name }} · 专员看板</h3>
              <p class="greeting-desc">用心服务每一位客户，开启美好的工作旅程</p>
            </div>
          </div>
          
          <div class="header-right-zone">
            <div class="header-time-badge">
              <span class="badge-dot"></span>
              <span class="badge-time">{{ currentTime }}</span>
            </div>
            
            <div class="user-profile-wrapper">
              <el-avatar :size="32" class="user-badge-avatar">
                {{ userInfo?.name ? userInfo.name.charAt(0).toUpperCase() : 'U' }}
              </el-avatar>
              <el-dropdown class="user-dropdown-nav">
                <span class="el-dropdown-link">
                  {{ userInfo?.name || '专员' }}<el-icon><arrow-down/></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="$router.push('/dashboard/profile')">个人中心</el-dropdown-item>
                    <el-dropdown-item @click="logout" divided class="logout-item">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </el-header>

      <!-- 右侧主体内容 -->
      <el-main class="main-content">
         <router-view v-if="isVisible"/>
      </el-main>

      <!-- 右侧底部 -->
      <el-footer class="footer">
        <div class="footer-content">
          <span>© 2026 {{ systemConfig.name }} {{ systemConfig.version }} | {{ systemConfig.description }}</span>
        </div>
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup>
import {ref, onMounted, onUnmounted, provide, nextTick} from 'vue'
import {useRoute, useRouter} from "vue-router";
import {
  Document,
  Setting,
  ArrowDown, ShoppingBag, User, OfficeBuilding, Compass, Management, Money
} from '@element-plus/icons-vue'
import {doGet} from "@/http/httpRequest.js";
import {getTokenName, messageFrame, removeToken} from "@/util/util.js";
import {ElMessage} from "element-plus";

/*------------单设备登录互斥：心跳轮询----------*/
// 旧设备被顶下线后往往不会再发业务请求，靠心跳每 5 秒探测一次登录态：
// /api/login/free 会经过后端 TokenVerifyFilter 校验 token，
// 若账号已在其他设备重新登录，后端返回 905，由 axios 响应拦截器统一弹窗并强制回登录页
const heartbeatTimer = ref(null)
const startLoginHeartbeat = () => {
  stopLoginHeartbeat()
  heartbeatTimer.value = setInterval(async () => {
    // 心跳前先检查本地 token 是否还在：
    // 收到 905 后拦截器会立刻清除 token，若此时定时器仍在运行，
    // 就会每 5 秒发出一次无 token 的请求，被后端返回 901「Token参数为空」并叠加弹框。
    // 因此一旦 token 已不存在，直接停掉心跳，静默等待拦截器完成跳转。
    const token = window.sessionStorage.getItem(getTokenName())
        || window.localStorage.getItem(getTokenName());
    if (!token) {
      stopLoginHeartbeat();
      return;
    }
    try {
      await doGet("/api/login/free", {});
    } catch (e) {
      // 收到 905（被顶下线）等 900+ 拦截码时拦截器已弹出提示并会跳转，此处静默吞掉即可
    }
  }, 5000)
}
const stopLoginHeartbeat = () => {
  if (heartbeatTimer.value) {
    clearInterval(heartbeatTimer.value)
    heartbeatTimer.value = null
  }
}
// 组件卸载（比如跳转登录页前）时清理定时器，避免泄漏
onUnmounted(() => {
  stopLoginHeartbeat()
})

/*------------左侧----------*/
// 控制侧边栏展开/收起状态
const isCollapse = ref(true)
const route = useRoute()
const router = useRouter()
// 菜单样式配置 - 高端意境森林墨绿主题，其余由深度选择器承载
const menuBgColor = ref('transparent')  // 菜单背景色透明以承载aside渐变
const menuTextColor = ref('#b2d8c3')   // 雅银绿菜单文字
const menuActiveColor = ref('#ffffff') // 激活项文字颜色
const sidebarBg = ref('linear-gradient(180deg, #112d1b 0%, #08150d 100%)') // 侧边栏动态背景色
const activeTheme = ref('forest')

const applyTheme = (themeKey) => {
  activeTheme.value = themeKey || 'forest'
  const themes = {
    forest: {
      gradient: 'linear-gradient(180deg, #112d1b 0%, #08150d 100%)',
      text: '#b2d8c3',
      active: '#ffffff'
    },
    midnight: {
      gradient: 'linear-gradient(180deg, #0f3c5f 0%, #061e31 100%)',
      text: '#9cbada',
      active: '#ffffff'
    },
    burgundy: {
      gradient: 'linear-gradient(180deg, #510f1b 0%, #2f060f 100%)',
      text: '#e69da5',
      active: '#ffffff'
    },
    cyberpunk: {
      gradient: 'linear-gradient(180deg, #1a1a24 0%, #0b0b0f 100%)',
      text: '#9499a6',
      active: '#52b788'
    }
  }
  
  const currentTheme = themes[themeKey] || themes.forest
  sidebarBg.value = currentTheme.gradient
  menuTextColor.value = currentTheme.text
  menuActiveColor.value = currentTheme.active
}

const userInfo = ref({menuPermissionList: []}) //用户信息
const isVisible = ref(true)//右侧内容是否展示
const currentPath = ref("")//当前所处的路径

// 系统元信息配置
const systemConfig = ref({
  name: '管理系统',
  version: 'v0.1.2',
  description: '设计精美，操作便捷'
})

// 加载公开系统配置
const loadSystemConfig = async () => {
  try {
    const res = await doGet("/api/system/info/public", {});
    if (res.data.code === 200 && res.data.data) {
      systemConfig.value = res.data.data;
      if (res.data.data.title) {
        document.title = res.data.data.title;
      }
      
      // 核心融合变动：系统登录后立即在数据库里面查询并覆盖默认主题色，并强制应用设置此主题色，完全屏蔽本地脏缓存对换肤的干扰
      const dbTheme = res.data.data.systemCode || 'forest';
      applyTheme(dbTheme);
    }
  } catch (error) {
    console.error("加载公开系统配置异常，已使用保底兜底信息:", error);
  }
}

const loadPath = () => {
  let path = route.path;
  let arr = path.split("/");
  if (arr.length > 3) {
    currentPath.value = "/" + arr[1] + "/" + arr[2];
  } else {
    currentPath.value = path;
  }
}

/*------------右上----------*/
//组件挂载完成后执行钩子函数（生命周期的其中之一）
onMounted(() =>{
  loadLoginUser();
  loadSystemConfig(); // 此处异步拉取并 100% 同步执行 applyTheme 控制

  // 启动单设备登录心跳检测（被顶下线时最迟 5 秒内感知）
  startLoginHeartbeat()

  // 建立浏览器广播监听，在当前网页修改配置后即时动态重绘
  window.addEventListener('theme-change', (e) => {
    applyTheme(e.detail)
  })

  updateTime()
  // 设置定时器每秒更新时间
  setInterval(updateTime, 1000)

  loadPath();
})

//加载用户信息
const loadLoginUser = async () => {
  const request = await doGet("/api/login/info", {});
  console.log('DashboardView.vue:  用户信息：' , request)
  userInfo.value = request.data.data;
}

//退出登录
const logout =async () => {
  stopLoginHeartbeat() // 主动退出时先停掉心跳轮询
  const res = await doGet("api/logout", {});
  if (res.data.code === 200) {
    ElMessage.success("退出成功！");
    //清除token
    removeToken();
    // 清空用户临时的设备级主题偏好，重新登入时强制优先读取数据库管理员修改的默认权威主题色
    localStorage.removeItem('app-theme');
    //跳转登录页
    window.location.href="/"
  }else {
    messageFrame("退出异常，是否强制退出？")
        .then(() => {//确认后
      //清除token
      removeToken();
      localStorage.removeItem('app-theme');
      //跳转登录页
      window.location.href="/"
    })
        .catch(() => {//取消后
          ElMessage({
            type: 'info',
            message: '已取消退出 ',
          })
        })
  }
}

// 当前时间显示
const currentTime = ref('')

// 初始化时间
const updateTime = () => {
  currentTime.value = new Date().toLocaleString('zh-CN')
}


// 跳转到统计大屏（StatisticView）
const goToStatistic = () => {
  // 如果当前已经在主看板，触发刷新组件
  if (route.path === '/dashboard' || route.path === '/dashboard/') {
    reload()
  } else {
    router.push('/dashboard')
  }
}

// 切换侧边栏展开/收起状态
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 菜单打开事件处理
const handleOpen = (key, keyPath) => {
}

// 菜单关闭事件处理
const handleClose = (key, keyPath) => {
}



// 组件名称（使用 defineOptions）
defineOptions({
  name: 'LayoutPage'
})

const reload = () => {
  isVisible.value = false;//右侧内容隐藏
  nextTick(() => {//当数据更新后，在dom渲染后，自动执行该函数
    isVisible.value = true;
  })
}
provide('reload', reload);//生产者

</script>

<style scoped>
/* 主容器样式 */
.main-container {
  height: 100vh;
  background: #e9f0ec;
}

/* 侧边栏样式 - 极致典雅林空沙龙墨绿 */
.sidebar {
  background: linear-gradient(180deg, #112d1b 0%, #08150d 100%);
  box-shadow: 4px 0 15px rgba(0, 0, 0, 0.15);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo-area {
  padding: 24px 12px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;
}

.logo-area:hover {
  background: rgba(255, 255, 255, 0.03);
}

.logo-text {
  color: #e8f5e9;
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.8px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.logo-short {
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.08);
  color: #e8f5e9;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  font-size: 16px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

:deep(.el-divider--horizontal) {
  margin: 12px 0;
  border-color: rgba(255, 255, 255, 0.05);
}

.toggle-btn {
  text-align: center;
  padding: 8px 0;
}

.toggle-btn :deep(.el-button) {
  background: rgba(255, 255, 255, 0.06) !important;
  border: none !important;
  color: #b2d8c3 !important;
  transition: all 0.2s ease;
}

.toggle-btn :deep(.el-button:hover) {
  background: #52b788 !important;
  color: #ffffff !important;
  transform: scale(1.05);
}

.side-menu {
  border-right: none;
  flex: 1;
}

/* 内容区域容器 */
.content-container {
  display: flex;
  flex-direction: column;
}

/* 头部样式 - 雅致香草玻璃白 */
.header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid #e1ebe6;
  padding: 0;
  box-shadow: 0 2px 10px rgba(17, 45, 27, 0.03);
  height: 64px !important;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 24px;
}

/* 品牌问候样式 */
.brand-greeting {
  display: flex;
  align-items: center;
  gap: 12px;
}

.greeting-emoji {
  font-size: 22px;
}

.greeting-text h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #112d1b;
}

.greeting-desc {
  margin: 2px 0 0 0;
  font-size: 11px;
  color: #6d8272;
}

/* 右侧控制区 */
.header-right-zone {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-time-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f0f5f2;
  padding: 6px 14px;
  border-radius: 30px;
  border: 1px solid #e1ebe6;
}

.badge-dot {
  width: 6px;
  height: 6px;
  background-color: #52b788;
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(82, 183, 136, 0.2);
}

.badge-time {
  font-size: 12px;
  color: #112d1b;
  font-weight: 600;
  font-family: monospace;
}

.user-profile-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px;
}

.user-badge-avatar {
  background-color: #112d1b;
  color: #ffffff;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(17, 45, 27, 0.2);
}

.user-dropdown-nav .el-dropdown-link {
  cursor: pointer;
  color: #112d1b;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  outline: none !important;
}

.logout-item {
  color: #cb4345 !important;
}

/* 主体内容样式 - 清凉茶绿灰底 */
.main-content {
  background: #e9f0ec;
  padding: 24px;
  min-height: calc(100vh - 116px);
}

/* 底部样式 */
.footer {
  background: #e9f0ec;
  padding: 12px 0;
  color: #7b9487;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #e1ebe6;
  height: auto !important;
}

.footer-content {
  width: 100%;
  text-align: center;
  font-size: 12px;
  letter-spacing: 0.3px;
}

/* Element Plus 组件菜单样式覆盖 - 尊贵舱室圆角悬浮 */
:deep(.el-sub-menu__title) {
  color: #b2d8c3 !important;
  border-radius: 8px;
  margin: 4px 8px;
  transition: all 0.25s ease;
}

:deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: #ffffff !important;
}

:deep(.el-menu-item) {
  color: #9cbda2 !important;
  border-radius: 8px;
  margin: 4px 12px;
  height: 40px !important;
  line-height: 40px !important;
  transition: all 0.25s ease;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: #ffffff !important;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #2d6a4f 0%, #112d1b 100%) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  font-weight: 600;
}

:deep(.el-menu) {
  border-right: none;
  background: transparent !important;
}

/* 当折叠（或非折叠）时，只要子菜单包含激活项，顶级子菜单标题（包括图标）高亮明晰化 */
:deep(.el-sub-menu.is-active .el-sub-menu__title) {
  color: #ffffff !important;
  font-weight: 600;
}

:deep(.el-sub-menu.is-active .el-sub-menu__title .el-icon) {
  transform: scale(1.1);
  transition: transform 0.2s ease;
}

/* ==================== 多波主题全站式换肤 深度样式覆盖 ==================== */
/* 1. 森林墨绿 (Forest) */
.theme-forest :deep(.el-sub-menu.is-active .el-sub-menu__title .el-icon) {
  color: #52b788 !important; /* 极光翠绿 */
}
.theme-forest :deep(.badge-dot) {
  background-color: #52b788 !important;
  box-shadow: 0 0 0 2px rgba(82, 183, 136, 0.2);
}
.theme-forest :deep(.user-badge-avatar) {
  background-color: #112d1b !important;
}
.theme-forest .header {
  background: rgba(244, 248, 245, 0.9) !important;
}
.theme-forest .footer {
  background: #e9f0ec !important;
}

/* 2. 静谧深海 (Midnight) */
.theme-midnight :deep(.el-sub-menu.is-active .el-sub-menu__title .el-icon) {
  color: #1d70b8 !important; /* 深海靓蓝 */
}
.theme-midnight :deep(.badge-dot) {
  background-color: #1d70b8 !important;
  box-shadow: 0 0 0 2px rgba(29, 112, 184, 0.2);
}
.theme-midnight :deep(.user-badge-avatar) {
  background-color: #0f3c5f !important;
}
.theme-midnight :deep(.header-time-badge) {
  border-color: #d1e2f4 !important;
  background: #f0f6fc !important;
}
.theme-midnight .header {
  background: rgba(240, 244, 248, 0.93) !important;
  border-bottom: 1px solid #d1e2f4 !important;
}
.theme-midnight .footer {
  background: #dce6f0 !important;
  border-top: 1px solid #d1e2f4 !important;
  color: #5b7487 !important;
}

/* 3. 奢华酒红 (Burgundy) */
.theme-burgundy :deep(.el-sub-menu.is-active .el-sub-menu__title .el-icon) {
  color: #a9203b !important; /* 尊贵奢红 */
}
.theme-burgundy :deep(.badge-dot) {
  background-color: #a9203b !important;
  box-shadow: 0 0 0 2px rgba(169, 32, 59, 0.2);
}
.theme-burgundy :deep(.user-badge-avatar) {
  background-color: #510f1b !important;
}
.theme-burgundy :deep(.header-time-badge) {
  border-color: #fbd6da !important;
  background: #fef2f3 !important;
}
.theme-burgundy .header {
  background: rgba(254, 247, 248, 0.93) !important;
  border-bottom: 1px solid #fbd6da !important;
}
.theme-burgundy .footer {
  background: #fae2e4 !important;
  border-top: 1px solid #fbd6da !important;
  color: #8c5b62 !important;
}

/* 4. 极客夜色 (Cyberpunk) */
.theme-cyberpunk :deep(.el-sub-menu.is-active .el-sub-menu__title .el-icon) {
  color: #10b981 !important; /* 极光翡翠 */
}
.theme-cyberpunk :deep(.badge-dot) {
  background-color: #10b981 !important;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}
.theme-cyberpunk :deep(.user-badge-avatar) {
  background-color: #1a1a24 !important;
}
.theme-cyberpunk :deep(.header-time-badge) {
  border-color: #3b3b4f !important;
  background: #242433 !important;
}
.theme-cyberpunk :deep(.badge-time) {
  color: #9499a6 !important;
}
.theme-cyberpunk .header {
  background: rgba(30, 30, 41, 0.95) !important;
  border-bottom: 1px solid #3b3b4f !important;
}
.theme-cyberpunk .footer {
  background: #151522 !important;
  border-top: 1px solid #3b3b4f !important;
  color: #646a78 !important;
}
.theme-cyberpunk .greeting-text h3, .theme-cyberpunk .user-dropdown-nav .el-dropdown-link {
  color: #e5e7eb !important;
}
.theme-cyberpunk .greeting-desc {
  color: #9ca3af !important;
}
</style>

<style>
/* ==================== 彻底破除 scoped 限制，彻底攻克 Element Plus 收起侧边栏时的悬浮气泡 hover 色调问题 ==================== */
/* 1. 全局定制气泡弹出窗背景色 */
body .el-menu--popup.theme-forest {
  background: #112d1b !important;
}
body .el-menu--popup.theme-midnight {
  background: #0f3c5f !important;
}
body .el-menu--popup.theme-burgundy {
  background: #510f1b !important;
}
body .el-menu--popup.theme-cyberpunk {
  background: #1a1a24 !important;
  border: 1px solid #3b3b4f !important;
}

/* 2. 全局定制气泡内各个子选项未 hover 时的基础字体色与内圆角 */
body .el-menu--popup .el-menu-item {
  border-radius: 6px !important;
  margin: 2px 4px !important;
  height: 38px !important;
  line-height: 38px !important;
  transition: all 0.2s ease !important;
}

body .el-menu--popup.theme-forest .el-menu-item {
  color: #b2d8c3 !important;
}
body .el-menu--popup.theme-forest .el-menu-item:hover,
body .el-menu--popup.theme-forest .el-menu-item.is-active {
  background: linear-gradient(90deg, #2d6a4f 0%, #112d1b 100%) !important;
  color: #ffffff !important;
}

body .el-menu--popup.theme-midnight .el-menu-item {
  color: #9cbada !important;
}
body .el-menu--popup.theme-midnight .el-menu-item:hover,
body .el-menu--popup.theme-midnight .el-menu-item.is-active {
  background: linear-gradient(90deg, #1d70b8 0%, #0f3c5f 100%) !important;
  color: #ffffff !important;
}

body .el-menu--popup.theme-burgundy .el-menu-item {
  color: #e69da5 !important;
}
body .el-menu--popup.theme-burgundy .el-menu-item:hover,
body .el-menu--popup.theme-burgundy .el-menu-item.is-active {
  background: linear-gradient(90deg, #a9203b 0%, #510f1b 100%) !important;
  color: #ffffff !important;
}

body .el-menu--popup.theme-cyberpunk .el-menu-item {
  color: #9499a6 !important;
}
body .el-menu--popup.theme-cyberpunk .el-menu-item:hover,
body .el-menu--popup.theme-cyberpunk .el-menu-item.is-active {
  background: linear-gradient(90deg, #3e3e52 0%, #22222e 100%) !important;
  color: #52b788 !important;
}
</style>