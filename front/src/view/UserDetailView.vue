<template>
  <div class="user-detail-container">
    <!-- 加载中状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>正在加载用户详情...</p>
    </div>

    <!-- 成功加载状态 -->
    <div v-else-if="UserDetail" class="detail-content">
      <div class="user-detail-wrapper">
        <!-- 装饰性顶部条 -->
        <div class="card-accent"></div>

        <!-- 卡片头部：头像 + 欢迎信息 + 操作按钮 -->
        <div class="card-header">
          <div class="user-profile">
            <div class="avatar-wrapper">
              <el-avatar :size="80" :style="{ backgroundColor: avatarColor }" class="user-avatar">
                {{ userInitial }}
              </el-avatar>
              <span class="online-dot" :class="{ 'is-online': isOnline }"></span>
            </div>
            <div class="user-info">
              <h2 class="user-name">{{ UserDetail.name || '未设置姓名' }}</h2>
              <div class="user-meta">
                <el-tag size="small" type="info" effect="plain">ID: {{ UserDetail.id }}</el-tag>
                <el-tag size="small" type="success" effect="plain" v-if="isActive">已激活</el-tag>
                <el-tag size="small" type="danger" effect="plain" v-else>未激活</el-tag>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button @click="goBack" :icon="ArrowLeft" size="default" round>返回</el-button>
          </div>
        </div>

        <!-- 信息描述区域 -->
        <div class="info-section">
          <h3 class="section-title">
            <el-icon><User /></el-icon>
            详细信息
          </h3>
          <el-descriptions :column="2" border class="user-descriptions">
            <el-descriptions-item label="账号">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Monitor /></el-icon>
                  <span>账号</span>
                </div>
              </template>
              {{ UserDetail.loginAct || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="姓名">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><User /></el-icon>
                  <span>姓名</span>
                </div>
              </template>
              {{ UserDetail.name || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="手机">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Iphone /></el-icon>
                  <span>手机</span>
                </div>
              </template>
              {{ UserDetail.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Message /></el-icon>
                  <span>邮箱</span>
                </div>
              </template>
              {{ UserDetail.email || '-' }}
            </el-descriptions-item>

            <!-- 状态字段使用彩色标签 -->
            <el-descriptions-item label="账号状态">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Lock /></el-icon>
                  <span>账号状态</span>
                </div>
              </template>
              <el-tag :type="accountExpiredStatus.type" size="small" effect="light">
                {{ accountExpiredStatus.text }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="密码状态">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Key /></el-icon>
                  <span>密码状态</span>
                </div>
              </template>
              <el-tag :type="credentialsExpiredStatus.type" size="small" effect="light">
                {{ credentialsExpiredStatus.text }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="锁定状态">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Lock /></el-icon>
                  <span>锁定状态</span>
                </div>
              </template>
              <el-tag :type="lockedStatus.type" size="small" effect="light">
                {{ lockedStatus.text }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="启用状态">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><CircleCheck /></el-icon>
                  <span>启用状态</span>
                </div>
              </template>
              <el-tag :type="enabledStatus.type" size="small" effect="light">
                {{ enabledStatus.text }}
              </el-tag>
            </el-descriptions-item>

            <el-descriptions-item label="创始人">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><UserFilled /></el-icon>
                  <span>创始人</span>
                </div>
              </template>
              {{ UserDetail.createByUser?.name || '-' }}
            </el-descriptions-item>

            <el-descriptions-item label="编辑人">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Edit /></el-icon>
                  <span>编辑人</span>
                </div>
              </template>
              {{ UserDetail.editByUser?.name || '-' }}
            </el-descriptions-item>

            <el-descriptions-item label="编辑时间">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Clock /></el-icon>
                  <span>编辑时间</span>
                </div>
              </template>
              {{ UserDetail.editTime ? formatDate(UserDetail.editTime) : '-' }}
            </el-descriptions-item>

            <el-descriptions-item label="最近登录">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Timer /></el-icon>
                  <span>最近登录</span>
                </div>
              </template>
              {{ UserDetail.lastLoginTime ? formatDate(UserDetail.lastLoginTime) : '-' }}
            </el-descriptions-item>

            <el-descriptions-item label="角色" :span="2">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Avatar /></el-icon>
                  <span>角色</span>
                </div>
              </template>
              <div class="role-list">
                <el-tag v-for="(role, idx) in roleList" :key="idx" size="small" type="info" effect="plain">
                  {{ role }}
                </el-tag>
                <span v-if="!roleList.length">-</span>
              </div>
            </el-descriptions-item>

            <el-descriptions-item label="创建时间" :span="2">
              <template #label>
                <div class="label-with-icon">
                  <el-icon><Calendar /></el-icon>
                  <span>创建时间</span>
                </div>
              </template>
              <div class="time-info">
                {{ formatDate(UserDetail.createTime) }}
                <span class="duration-badge">{{ getDuration(UserDetail.createTime) }}</span>
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 底部装饰信息 -->
        <div class="card-footer">
          <span class="footer-text">
            <el-icon><InfoFilled /></el-icon>
            系统记录 · 信息真实有效
          </span>
        </div>
      </div>
    </div>

    <!-- 无数据状态 -->
    <div v-else class="empty-state">
      <el-empty description="未找到用户信息" :image-size="120">
        <el-button type="primary" @click="goBack" :icon="ArrowLeft">返回首页</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { doGet } from "@/http/httpRequest.js";
import {
  Loading,
  ArrowLeft,
  User,
  Monitor,
  Iphone,
  Message,
  Calendar,
  Lock,
  Key,
  CircleCheck,
  UserFilled,
  Edit,
  Clock,
  Timer,
  Avatar,
  InfoFilled,
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const UserDetail = ref(
    {
      createByUser : {},
      editByUser : {}
    }
);
const loading = ref(true);

// 返回上一页
const goBack = () => {
  router.go(-1);
};

// 格式化日期
const formatDate = (dateTime) => {
  if (!dateTime) return '-';
  try {
    const date = new Date(dateTime);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch (e) {
    return dateTime;
  }
};

// 计算注册时长
const getDuration = (createTime) => {
  if (!createTime) return '';
  const now = new Date();
  const create = new Date(createTime);
  const diffDays = Math.floor((now - create) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return '';
  if (diffDays === 0) return '今日注册';
  if (diffDays < 30) return `已注册 ${diffDays} 天`;
  if (diffDays < 365) return `已注册 ${Math.floor(diffDays / 30)} 个月`;
  return `已注册 ${Math.floor(diffDays / 365)} 年`;
};

// 头像首字母
const userInitial = computed(() => {
  if (UserDetail.value) {
    const name = UserDetail.value.name;
    const loginAct = UserDetail.value.loginAct;
    if (name && name.trim()) {
      return name.charAt(0).toUpperCase();
    }
    if (loginAct && loginAct.trim()) {
      return loginAct.charAt(0).toUpperCase();
    }
  }
  return "U";
});

// 头像背景色
const avatarColor = computed(() => {
  if (UserDetail.value) {
    const str = UserDetail.value.id || UserDetail.value.loginAct || "default";
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const hue = Math.abs(hash % 360);
    return `hsl(${hue}, 70%, 60%)`;
  }
  return "#909399";
});

// 在线状态：根据最近登录时间判断（今日登录为在线）
const isOnline = computed(() => {
  if (!UserDetail.value?.lastLoginTime) return false;
  const last = new Date(UserDetail.value.lastLoginTime);
  const today = new Date();
  return last.toDateString() === today.toDateString();
});

// 账号是否激活（简单用账号未过期且未锁定且启用综合判断）
const isActive = computed(() => {
  if (!UserDetail.value) return false;
  const notExpired = UserDetail.value.accountNoExpired === 1;
  const notLocked = UserDetail.value.accountNoLocked === 1;
  const enabled = UserDetail.value.accountNoLocked === 1; // 注意原代码中启用状态用了accountNoLocked，这里沿用
  return notExpired && notLocked && enabled;
});

// 状态映射
const accountExpiredStatus = computed(() => {
  const val = UserDetail.value?.accountNoExpired;
  if (val === 1) return { text: '正常', type: 'success' };
  if (val === 0) return { text: '已过期', type: 'danger' };
  return { text: '-', type: 'info' };
});

const credentialsExpiredStatus = computed(() => {
  const val = UserDetail.value?.credentialsNoExpired;
  if (val === 1) return { text: '正常', type: 'success' };
  if (val === 0) return { text: '已过期', type: 'danger' };
  return { text: '-', type: 'info' };
});

const lockedStatus = computed(() => {
  const val = UserDetail.value?.accountNoLocked;
  if (val === 1) return { text: '未锁定', type: 'success' };
  if (val === 0) return { text: '已锁定', type: 'danger' };
  return { text: '-', type: 'info' };
});

const enabledStatus = computed(() => {
  // 原代码中用accountNoLocked作为启用标志，保持逻辑一致
  const val = UserDetail.value?.accountNoLocked;
  if (val === 1) return { text: '已启用', type: 'success' };
  if (val === 0) return { text: '禁用', type: 'danger' };
  return { text: '-', type: 'info' };
});

// 角色列表（假设后端返回的是数组或逗号分隔字符串）
const roleList = computed(() => {
  const roles = UserDetail.value?.roleList;
  if (!roles) return [];
  if (Array.isArray(roles)) return roles;
  if (typeof roles === 'string') return roles.split(',').map(r => r.trim());
  return [];
});

// 加载用户详情
const loadUserDetail = async () => {
  let id = route.params.id;
  if (!id) {
    console.error('用户 ID 不能为空');
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const response = await doGet("/api/user/" + id, {});
    console.log("UserDetailView:", response);
    if (response.data.code === 200) {
      UserDetail.value = response.data.data;
    } else {
      console.error('获取用户详情失败:', response.data.message);
      UserDetail.value = null;
    }
  } catch (error) {
    console.error('请求异常:', error);
    UserDetail.value = null;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadUserDetail();
});
</script>

<style scoped>
/* 全局动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-detail-container {
  min-height: 50vh;
  padding: 0;
  background: transparent;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: transparent;
  color: #3b82f6;
  font-size: 16px;
  animation: fadeInUp 0.5s ease;
}
.loading-state .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.loading-state p {
  margin: 0;
  color: #4b5563;
}

/* 用户详情包装器 */
.user-detail-wrapper {
  max-width: 1000px;
  margin: 0 auto;
  overflow: hidden;
}

/* 装饰顶条 */
.card-accent {
  height: 4px;
  background: linear-gradient(90deg, #3b82f6, #10b981, #f59e0b);
  border-radius: 2px;
  margin-bottom: 20px;
}

/* 头部结构 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 20px 0;
  background: transparent;
  flex-wrap: wrap;
  gap: 20px;
  border-bottom: 1px dashed #e2e8f0;
}
.user-profile {
  display: flex;
  align-items: center;
  gap: 24px;
}
.avatar-wrapper {
  position: relative;
}
.user-avatar {
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.12);
  transition: transform 0.2s;
  font-weight: 600;
  font-size: 32px;
}
.user-avatar:hover {
  transform: scale(1.03);
}
.online-dot {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 16px;
  height: 16px;
  background-color: #9ca3af;
  border: 2px solid #ffffff;
  border-radius: 50%;
  transition: background-color 0.2s;
}
.online-dot.is-online {
  background-color: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.3);
}
.user-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.user-name {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #1f2937;
  letter-spacing: -0.3px;
}
.user-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.header-actions .el-button {
  border-radius: 40px;
  padding: 8px 24px;
  font-weight: 500;
  transition: all 0.2s;
  background: #f3f4f6;
  border-color: #e5e7eb;
  color: #374151;
}
.header-actions .el-button:hover {
  background: #e5e7eb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

/* 信息区域 */
.info-section {
  padding: 24px 0;
  background: transparent;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 8px 0 24px 0;
  padding-left: 12px;
  border-left: 5px solid #3b82f6;
}
.section-title .el-icon {
  font-size: 22px;
  color: #3b82f6;
}

/* 描述列表样式 */
.user-descriptions {
  margin-bottom: 16px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #edf2f7;
}
:deep(.el-descriptions__header) {
  display: none;
}
:deep(.el-descriptions__body) {
  background-color: #ffffff;
}
:deep(.el-descriptions__table) {
  border-collapse: separate;
  border-spacing: 0;
}
:deep(.el-descriptions__cell) {
  padding: 16px 20px;
}
:deep(.el-descriptions__label) {
  background-color: #fafcff;
  font-weight: 600;
  color: #334155;
  width: 140px;
  border-right: 1px solid #edf2f7;
}
:deep(.el-descriptions__content) {
  color: #1e293b;
  font-weight: 500;
}
.label-with-icon {
  display: flex;
  align-items: center;
  gap: 10px;
}
.label-with-icon .el-icon {
  font-size: 18px;
  color: #5b6e8c;
}
.role-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.time-info {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.duration-badge {
  background: #eef2ff;
  color: #3b82f6;
  padding: 4px 12px;
  border-radius: 40px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

/* 卡片底部 */
.card-footer {
  padding: 16px 0 0 0;
  border-top: 1px solid #f0f2f5;
  text-align: center;
  background: transparent;
}
.footer-text {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
}
.footer-text .el-icon {
  font-size: 14px;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: transparent;
  border-radius: 8px;
  animation: fadeInUp 0.5s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-detail-container {
    padding: 0;
  }
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 10px 0;
  }
  .user-profile {
    width: 100%;
  }
  .header-actions {
    width: 100%;
  }
  .header-actions .el-button {
    width: 100%;
    justify-content: center;
  }
  .info-section {
    padding: 16px 0;
  }
  .section-title {
    font-size: 18px;
    margin-bottom: 20px;
  }
  :deep(.el-descriptions__cell) {
    padding: 12px 16px;
  }
  :deep(.el-descriptions__label) {
    width: 100px;
  }
  .user-name {
    font-size: 22px;
  }
  .user-avatar {
    width: 64px;
    height: 64px;
    line-height: 64px;
    font-size: 26px;
  }
  .online-dot {
    width: 14px;
    height: 14px;
    bottom: 2px;
    right: 2px;
  }
  .duration-badge {
    font-size: 10px;
  }
  .time-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
}
</style>