<template>
  <div class="module-shell user-detail-module" v-loading="loading" element-loading-text="正在加载账号详情…">

    <template v-if="detail">
      <!-- ============ 详情 Hero ============ -->
      <div class="panel-card">
        <div class="panel-card__body">
          <div class="detail-hero">
            <div class="detail-hero__avatar" :style="{ background: avatarGradient }">
              {{ userInitial }}
            </div>

            <div class="detail-hero__main">
              <h3 class="detail-hero__name">
                {{ detail.name || '未设置姓名' }}
                <span v-if="detail.id === currentUserId" class="state-tag state-tag--gold self-badge">本人账号</span>
              </h3>

              <div class="detail-hero__meta">
                <span><el-icon :size="12"><User /></el-icon> 账号：{{ detail.loginAct || '—' }}</span>
                <span><el-icon :size="12"><Iphone /></el-icon> {{ detail.phone || '—' }}</span>
                <span><el-icon :size="12"><Message /></el-icon> {{ detail.email || '—' }}</span>
                <span v-if="onlineText"><el-icon :size="12"><Clock /></el-icon> {{ onlineText }}</span>
              </div>

              <!-- 账号状态标签组：启用/锁定/账期/密码四态一眼可辨 -->
              <div class="hero-tags">
                <span class="state-tag" :class="detail.accountEnabled === 1 ? 'state-tag--green' : 'state-tag--red'">
                  {{ detail.accountEnabled === 1 ? '已启用' : '已禁用' }}
                </span>
                <span class="state-tag" :class="detail.accountNoLocked === 1 ? 'state-tag--neutral' : 'state-tag--gold'">
                  {{ detail.accountNoLocked === 1 ? '未锁定' : '已锁定' }}
                </span>
                <span class="state-tag" :class="detail.accountNoExpired === 1 ? 'state-tag--neutral' : 'state-tag--red'">
                  {{ detail.accountNoExpired === 1 ? '账期正常' : '账期已过' }}
                </span>
                <span class="state-tag" :class="detail.credentialsNoExpired === 1 ? 'state-tag--neutral' : 'state-tag--gold'">
                  {{ detail.credentialsNoExpired === 1 ? '密码正常' : '密码已过期' }}
                </span>
                <span v-for="r in roleList" :key="r" class="state-tag state-tag--blue">{{ r }}</span>
                <span v-if="!roleList.length" class="state-tag state-tag--neutral">未分配角色</span>
              </div>
            </div>

            <div class="detail-hero__actions">
              <el-button :icon="Back" @click="goBack">返回列表</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- ============ 账号档案 ============ -->
      <div class="panel-card">
        <div class="panel-card__header">
          <div class="panel-card__title">
            <el-icon :size="16"><Document /></el-icon>
            <span>账号档案</span>
          </div>
          <span class="panel-card__hint">登录身份、联系方式与授权范围</span>
        </div>
        <div class="panel-card__body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">用户 ID</span>
              <span class="detail-item__value">{{ detail.id }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">登录账号</span>
              <span class="detail-item__value">{{ detail.loginAct || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">姓名</span>
              <span class="detail-item__value">{{ detail.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">手机号</span>
              <span class="detail-item__value">{{ detail.phone || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">邮箱</span>
              <span class="detail-item__value">{{ detail.email || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">授权角色</span>
              <span class="detail-item__value">
                <template v-if="roleList.length">
                  <el-tag v-for="r in roleList" :key="r" size="small" effect="plain" class="role-chip">{{ r }}</el-tag>
                </template>
                <span v-else class="empty-dash">未分配</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- ============ 生命周期 ============ -->
      <div class="panel-card">
        <div class="panel-card__header">
          <div class="panel-card__title">
            <el-icon :size="16"><Timer /></el-icon>
            <span>账号生命周期</span>
          </div>
          <span class="panel-card__hint">创建、编辑与登录轨迹</span>
        </div>
        <div class="panel-card__body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">创建时间</span>
              <span class="detail-item__value">
                {{ formatDate(detail.createTime) }}
                <span v-if="durationText" class="duration-badge">{{ durationText }}</span>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">创建人</span>
              <span class="detail-item__value">{{ detail.createByUser?.name || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">最近编辑</span>
              <span class="detail-item__value">{{ detail.editTime ? formatDate(detail.editTime) : '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">编辑人</span>
              <span class="detail-item__value">{{ detail.editByUser?.name || '—' }}</span>
            </div>
            <div class="detail-item detail-item--full">
              <span class="detail-item__label">最近登录</span>
              <span class="detail-item__value">{{ detail.lastLoginTime ? formatDate(detail.lastLoginTime) : '从未登录' }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 无数据状态 -->
    <div v-if="!loading && !detail" class="panel-card">
      <div class="empty-state">
        <el-icon :size="44"><FolderOpened /></el-icon>
        <p>未找到该账号信息，可能已被删除</p>
        <el-button size="small" type="primary" :icon="Back" @click="goBack">返回列表</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from "vue";
import { useRoute } from "vue-router";
import { doGet } from "@/http/httpRequest.js";
import { goBack, getPermissionCache } from "@/util/util.js";
import { ElMessage } from "element-plus";
import {
  User, Iphone, Message, Clock, Timer, Document, Back, FolderOpened
} from "@element-plus/icons-vue";

const route = useRoute();
const detail = ref(null);
const loading = ref(true);

// 当前登录人（用于「本人账号」标记）
const currentUserId = computed(() => getPermissionCache()?.userId ?? null);

// 格式化日期时间（后端 Jackson 已输出 yyyy-MM-dd HH:mm:ss，截取到分即可）
const formatDate = (dateTime) => {
  if (!dateTime) return '—';
  return String(dateTime).slice(0, 16);
};

// 头像首字
const userInitial = computed(() => {
  const name = detail.value?.name;
  const act = detail.value?.loginAct;
  if (name && name.trim()) return name.trim().charAt(0).toUpperCase();
  if (act && act.trim()) return act.trim().charAt(0).toUpperCase();
  return "U";
});

/**
 * 头像底色：由 ID/账号哈希出森林-香槟色带内的色相，保持与全站设计语言一致。
 * 旧版用 hsl(hue,70%,60%) 随机全色相（蓝紫红都可能出现），与墨绿主题割裂。
 */
const avatarGradient = computed(() => {
  const str = String(detail.value?.id || detail.value?.loginAct || "default");
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  // 色相限制在绿色(95°)~香槟色(38°)区间内往复，保证任何账号头像都不出主题色系
  const hue = 38 + (Math.abs(hash) % 60);
  return `linear-gradient(135deg, hsl(${hue}, 32%, 38%), hsl(${hue + 20}, 38%, 52%))`;
});

// 在线状态描述
const onlineText = computed(() => {
  const last = detail.value?.lastLoginTime;
  if (!last) return '';
  const t = new Date(String(last).replace(' ', 'T'));
  if (isNaN(t.getTime())) return '';
  if (t.toDateString() === new Date().toDateString()) return '今日有登录';
  const days = Math.floor((Date.now() - t.getTime()) / 86400000);
  if (days <= 7) return `${days} 天前登录`;
  return `上次登录 ${formatDate(last).slice(0, 10)}`;
});

// 注册时长徽章
const durationText = computed(() => {
  const createTime = detail.value?.createTime;
  if (!createTime) return '';
  const create = new Date(String(createTime).replace(' ', 'T'));
  if (isNaN(create.getTime())) return '';
  const diffDays = Math.floor((Date.now() - create.getTime()) / 86400000);
  if (diffDays < 0) return '';
  if (diffDays === 0) return '今日添加';
  if (diffDays < 30) return `已存在 ${diffDays} 天`;
  if (diffDays < 365) return `已存在 ${Math.floor(diffDays / 30)} 个月`;
  return `已存在 ${Math.floor(diffDays / 365)} 年`;
});

// 角色列表（getUserById 实时查库返回 roleList 字符串数组）
const roleList = computed(() => {
  const roles = detail.value?.roleList;
  if (!roles) return [];
  if (Array.isArray(roles)) return roles;
  if (typeof roles === 'string') return roles.split(',').map(r => r.trim()).filter(Boolean);
  return [];
});

const loadUserDetail = async () => {
  const id = route.params.id;
  if (!id) {
    ElMessage.error('缺少用户 ID 参数');
    loading.value = false;
    return;
  }
  loading.value = true;
  try {
    const response = await doGet("/api/user/" + id, {});
    // 【安全修复】旧版 console.log("UserDetailView:", response) 打印完整用户对象
    // （改造前含 BCrypt 密码哈希与全量权限清单），已移除
    if (response.data.code === 200) {
      detail.value = response.data.data;
    } else {
      ElMessage.error(response.data.msg || '获取账号详情失败');
      detail.value = null;
    }
  } catch (error) {
    console.error('请求账号详情异常:', error);
    ElMessage.error('获取账号详情失败');
    detail.value = null;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadUserDetail();
});
</script>

<style scoped>
@import "@/assets/module-theme.css";

.user-detail-module {
  min-height: 100%;
  box-sizing: border-box;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.self-badge {
  vertical-align: 3px;
  margin-left: 8px;
  transform: scale(0.86);
}

.role-chip { margin: 1px 4px 1px 0; }

.duration-badge {
  display: inline-block;
  margin-left: 10px;
  background: var(--forest-soft);
  color: #4f865b;
  border: 1px solid var(--forest-border);
  padding: 1px 10px;
  border-radius: 40px;
  font-size: 11.5px;
  font-weight: 600;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 70px 0;
  color: #93a89b;
  text-align: center;
}
.empty-state p { margin: 0; font-size: 13px; letter-spacing: 0.4px; }
</style>
