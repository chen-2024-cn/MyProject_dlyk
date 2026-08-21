<template>
  <el-container class="main-container">

    <!-- 左侧导航栏 -->
    <el-aside
        :width="isCollapse ? '64px' : '200px'"
        class="sidebar"
    >
      <div class="logo-area">
        <h3 v-show="!isCollapse" class="logo-text">管理系统</h3>
        <div v-show="isCollapse" class="logo-short">M</div>
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
        <el-sub-menu :index="index" v-for="(menuPermission, index) in userInfo.menuPermissionList" :key="menuPermission.id">
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
          <h2>控制面板</h2>
          <p> {{ currentTime }}</p>
          <div class="user-info">
            <el-dropdown>
              <span class="el-dropdown-link">
                {{userInfo?.name}}<el-icon><arrow-down/></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/dashboard/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <!-- 右侧主体内容 -->
      <el-main class="main-content">
        <el-card class="content-card">
         <router-view v-if="isVisible"/>
        </el-card>
      </el-main>

      <!-- 右侧底部 -->
      <el-footer class="footer">
        <div class="footer-content">
          <span>© 2026 管理系统 v0.0.1 | 设计精美，操作便捷</span>
        </div>
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup>
import {ref, onMounted, provide, nextTick} from 'vue'
import {useRoute} from "vue-router";
import {
  Document,
  Setting,
  ArrowDown, ShoppingBag, User, OfficeBuilding, Compass, Management, Money
} from '@element-plus/icons-vue'
import {doGet} from "@/http/httpRequest.js";
import {messageFrame, removeToken} from "@/util/util.js";
import {ElMessage} from "element-plus";

/*------------左侧----------*/
// 控制侧边栏展开/收起状态
const isCollapse = ref(true)
const route = useRoute()
// 菜单样式配置 - 浅绿色主题
const menuBgColor = ref('#e8f5e9')     // 菜单背景色 - 浅绿色
const menuTextColor = ref('#2e7d32')   // 菜单文字颜色 - 深绿色
const menuActiveColor = ref('#FF99A4') // 激活项文字颜色 - 粉红
const userInfo = ref({menuPermissionList: []}) //用户信息
const isVisible = ref(true)//右侧内容是否展示
const currentPath = ref("")//当前所处的路径

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
  const res = await doGet("api/logout", {});
  if (res.data.code === 200) {
    ElMessage.success("退出成功！");
    //清除token
    removeToken();
    //跳转登录页
    window.location.href="/"
  }else {
    messageFrame("退出异常，是否强制退出？")
        .then(() => {//确认后
      //清除token
      removeToken();
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
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
}

/* 侧边栏样式 */
.sidebar {
  background: #f1f8e9;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
}

.logo-area {
  padding: 20px 10px;
  text-align: center;
}

.logo-text {
  color: #2e7d32;
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.logo-short {
  width: 40px;
  height: 40px;
  background: #4caf50;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  font-weight: bold;
}

.toggle-btn {
  text-align: center;
  padding: 10px 0;
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

/* 头部样式 */
.header {
  background: #a5d6a7;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.header h2 {
  color: #1b5e20;
  margin: 0;
}

.user-info .el-dropdown-link {
  cursor: pointer;
  color: #1b5e20;
  font-weight: 500;
}

/* 主体内容样式 */
.main-content {
  background: #f5f5f5;
  padding: 20px;
}

.content-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  color: #2e7d32;
}

/* 底部样式 */
.footer {
  background: #a5d6a7;
  padding: 0;
  color: #1b5e20;
  display: flex;
  align-items: center;
  justify-content: center;
}

.footer-content {
  width: 100%;
  text-align: center;
}

/* Element Plus 组件样式覆盖 */
:deep(.el-sub-menu__title:hover),
:deep(.el-menu-item:hover) {
  background-color: #c8e6c9 !important;
}

:deep(.el-menu-item.is-active) {
  background-color: #a5d6a7 !important;
  border-left: 4px solid #4caf50;
}
</style>