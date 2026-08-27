<template>
  <!--渲染路由地址所对应的页面组件-->
  <router-view/>
</template>

<style>
/* 抹平浏览器默认样式，消除顶层白框和物理滚动条 */
html, body {
  margin: 0 !important;
  padding: 0 !important;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  box-sizing: border-box;
}

#app {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
}

/* ==================== Element Plus 折叠悬浮二级弹出菜单色彩与对比度重构 ==================== */
.el-menu--popup {
  background: #112d1b !important; /* 保持跟 Aside 一体化的尊贵暗绿 */
  border-radius: 12px !important;
  overflow: hidden !important;
  padding: 6px 0 !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25) !important;
  border: 1px solid rgba(255, 255, 255, 0.05) !important;
}

.el-menu--popup .el-menu-item {
  color: #9cbda2 !important; /* 未激活时清晰雅绿 */
  border-radius: 8px !important;
  margin: 4px 8px !important;
  height: 38px !important;
  line-height: 38px !important;
  transition: all 0.25s ease !important;
}

.el-menu--popup .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.08) !important;
  color: #ffffff !important;
}

.el-menu--popup .el-menu-item.is-active {
  background: linear-gradient(90deg, #2d6a4f 0%, #112d1b 100%) !important;
  color: #ffffff !important; /* 极佳的可读常亮纯白 */
  font-weight: 600 !important;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15) !important;
}
</style>