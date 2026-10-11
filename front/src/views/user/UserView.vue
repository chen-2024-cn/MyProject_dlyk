<template>
  <div class="module-shell user-module">

    <!-- ============ 页面 Hero ============ -->
    <section class="module-hero">
      <div class="module-hero__content">
        <span class="module-hero__eyebrow">SYSTEM · MEMBER DIRECTORY</span>
        <h2>用户管理</h2>
        <p>维护系统账号与角色授权：新增、编辑、停用与删除专员账号，掌控团队准入</p>
      </div>
      <div class="module-hero__status">
        <span class="module-hero__status-dot"></span>
        {{ isFiltering ? '已启用条件筛选' : '账号名册实时同步中' }}
      </div>
    </section>

    <!-- ============ 概览指标 ============ -->
    <div class="module-metrics">
      <div class="metric-tile">
        <div class="metric-tile__icon"><el-icon :size="18"><UserFilled /></el-icon></div>
        <div>
          <div class="metric-tile__label">{{ isFiltering ? '匹配账号' : '账号总数' }}</div>
          <div class="metric-tile__value">{{ total }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--blue">
        <div class="metric-tile__icon"><el-icon :size="18"><CircleCheck /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页启用</div>
          <div class="metric-tile__value">{{ enabledOnPage }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--gold">
        <div class="metric-tile__icon"><el-icon :size="18"><Lock /></el-icon></div>
        <div>
          <div class="metric-tile__label">本页停用/锁定</div>
          <div class="metric-tile__value">{{ disabledOnPage }}</div>
        </div>
      </div>

      <div class="metric-tile metric-tile--bronze">
        <div class="metric-tile__icon"><el-icon :size="18"><Select /></el-icon></div>
        <div>
          <div class="metric-tile__label">已选中</div>
          <div class="metric-tile__value">{{ userId.length }}</div>
        </div>
      </div>
    </div>

    <!-- ============ 筛选面板 ============ -->
    <div class="panel-card">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Filter /></el-icon>
          <span>账号筛选</span>
        </div>
        <span class="panel-card__hint">服务端全量检索，跨页有效</span>
      </div>

      <div class="panel-card__body">
        <!--
          【修复的既有缺陷】旧版搜索是纯前端 computed 过滤，只能过滤当前页 10 条，
          跨页失效且"搜索结果 N 条"实际是"本页命中 N 条"，严重误导。
          现改为把条件提交后端 /api/users（本轮已补全 XML 动态条件），全量检索。
        -->
        <div class="filter-grid">
          <div class="filter-item">
            <label class="filter-item__label">登录账号</label>
            <el-input v-model="filters.loginAct" placeholder="账号模糊匹配" clearable
                      :prefix-icon="Search" @keyup.enter="doSearch" />
          </div>
          <div class="filter-item">
            <label class="filter-item__label">姓名</label>
            <el-input v-model="filters.name" placeholder="姓名模糊匹配" clearable
                      :prefix-icon="User" @keyup.enter="doSearch" />
          </div>
          <div class="filter-item">
            <label class="filter-item__label">手机号</label>
            <el-input v-model="filters.phone" placeholder="手机号模糊匹配" clearable
                      :prefix-icon="Iphone" @keyup.enter="doSearch" />
          </div>
          <div class="filter-item">
            <label class="filter-item__label">邮箱</label>
            <el-input v-model="filters.email" placeholder="邮箱模糊匹配" clearable
                      :prefix-icon="Message" @keyup.enter="doSearch" />
          </div>
          <div class="filter-item">
            <label class="filter-item__label">账号状态</label>
            <el-select v-model="filters.accountEnabled" placeholder="全部状态" clearable
                       style="width: 100%" @change="doSearch">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </div>
          <div class="filter-item">
            <label class="filter-item__label">所属角色</label>
            <el-select v-model="filters.roleId" placeholder="全部角色" clearable
                       style="width: 100%" @change="doSearch">
              <el-option v-for="role in roleOptions" :key="role.id"
                         :label="role.roleName" :value="role.id" />
            </el-select>
          </div>
        </div>

        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
          <span v-if="isFiltering" class="toolbar__selected">
            已生效 {{ activeFilterCount }} 项条件
          </span>
        </div>
      </div>
    </div>

    <!-- ============ 账号清单 ============ -->
    <div class="panel-card forest-table">
      <div class="panel-card__header">
        <div class="panel-card__title">
          <el-icon :size="16"><Grid /></el-icon>
          <span>账号清单</span>
        </div>

        <div class="toolbar">
          <span v-if="userId.length > 0" class="toolbar__selected">已选中 {{ userId.length }} 人</span>
          <el-button type="primary" :icon="Plus" @click="openAddUserDialog" v-hasPermission="'user:add'">
            添加账号
          </el-button>
          <el-button type="danger" :icon="Delete" :disabled="!userId.length"
                     @click="deleteArr" v-hasPermission="'user:delete'">
            批量删除
          </el-button>
        </div>
      </div>

      <div class="panel-card__body panel-card__body--flush">
        <el-table v-loading="loading" :data="userList" style="width: 100%"
                  @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="48" :selectable="rowSelectable" />
          <el-table-column type="index" label="#" width="54" :index="rowIndex" />
          <el-table-column label="登录账号" min-width="120" fixed>
            <template #default="scope">
              <a href="javascript:" class="forest-link" @click="handleDetail(scope.row.id)"
                 v-hasPermission="'user:view'">
                {{ scope.row.loginAct }}
              </a>
            </template>
          </el-table-column>
          <el-table-column label="姓名" prop="name" min-width="100" show-overflow-tooltip>
            <template #default="scope">
              {{ scope.row.name || '—' }}
              <el-tag v-if="scope.row.id === currentUserId" size="small" effect="plain"
                      type="success" class="self-tag">本人</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="150">
            <template #default="scope">
              <!-- 角色由后端列表接口批量回填（一次查询，避免逐行 N+1） -->
              <template v-if="scope.row.roleList && scope.row.roleList.length">
                <el-tag v-for="r in scope.row.roleList" :key="r" size="small"
                        effect="plain" class="role-chip">{{ r }}</el-tag>
              </template>
              <span v-else class="empty-dash">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="125">
            <template #default="scope">{{ scope.row.phone || '—' }}</template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="170" show-overflow-tooltip>
            <template #default="scope">{{ scope.row.email || '—' }}</template>
          </el-table-column>
          <el-table-column label="账号状态" width="118">
            <template #default="scope">
              <span class="state-tag" :class="statusTag(scope.row).cls">
                {{ statusTag(scope.row).label }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="最近登录" width="152">
            <template #default="scope">
              <span v-if="scope.row.lastLoginTime">{{ formatDate(scope.row.lastLoginTime) }}</span>
              <span v-else class="empty-dash">从未登录</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="152">
            <template #default="scope">
              <span v-if="scope.row.createTime">{{ formatDate(scope.row.createTime) }}</span>
              <span v-else class="empty-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="scope">
              <!-- 【修复】旧版按钮为英文 Edit/Delete 且无任何权限指令，与全站中文风格割裂 -->
              <el-button type="primary" link size="small" :icon="View"
                         @click="handleDetail(scope.row.id)" v-hasPermission="'user:view'">详情</el-button>
              <el-button type="success" link size="small" :icon="EditPen"
                         @click="handleEdit(scope.row.id)" v-hasPermission="'user:edit'">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete"
                         :disabled="scope.row.id === currentUserId"
                         @click="handleDelete(scope.row.id)" v-hasPermission="'user:delete'">删除</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <el-icon :size="44"><FolderOpened /></el-icon>
              <p v-if="isFiltering">没有符合当前条件的账号</p>
              <p v-else>暂无用户账号</p>
              <el-button v-if="isFiltering" size="small" @click="resetFilters">清除筛选条件</el-button>
            </div>
          </template>
        </el-table>
      </div>

      <div class="pager-bar">
        <!-- 后端 Constants.PAGE_SIZE 固定 10 条，不提供页容量选择器（避免欺骗性交互） -->
        <el-pagination background layout="total, prev, pager, next, jumper"
                       :page-size="pageSize" :total="total" :current-page="currentPage"
                       @prev-click="toPage" @next-click="toPage" @current-change="toPage" />
      </div>
    </div>

    <!-- ============ 新增/编辑 对话框 ============ -->
    <el-dialog
        v-model="dialogVisible"
        class="custom-transition-dialog"
        :title="userForm.id > 0 ? '编辑账号' : '新增账号'"
        width="560px"
        :close-on-click-modal="false"
        destroy-on-close
    >
      <div>
        <el-form
            ref="userFormRef"
            :model="userForm"
            :rules="userFormRules"
            label-width="100px"
            status-icon
            class="add-user-form"
        >
          <el-form-item label="账号" prop="loginAct">
            <el-input v-model="userForm.loginAct" placeholder="请输入登录账号" clearable
                      :disabled="userForm.id > 0">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="loginPwd">
            <el-input v-model="userForm.loginPwd" type="password"
                      :placeholder="userForm.id > 0 ? '不修改请留空' : '请输入初始密码'"
                      clearable show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <div v-if="userForm.id > 0" class="pwd-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>编辑模式下，不填写密码将保持原密码不变</span>
          </div>

          <el-form-item label="姓名" prop="name">
            <el-input v-model="userForm.name" placeholder="请输入姓名" clearable />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" placeholder="请输入手机号" clearable />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email" placeholder="请输入邮箱" clearable />
          </el-form-item>

          <el-divider content-position="center">账号状态配置</el-divider>

          <div class="switch-grid">
            <el-form-item label="账号未过期" prop="accountNoExpired">
              <el-switch v-model="userForm.accountNoExpired" active-text="未过期" inactive-text="已过期"
                         :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="密码未过期" prop="credentialsNoExpired">
              <el-switch v-model="userForm.credentialsNoExpired" active-text="未过期" inactive-text="已过期"
                         :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="账号未锁定" prop="accountNoLocked">
              <el-switch v-model="userForm.accountNoLocked" active-text="未锁定" inactive-text="已锁定"
                         :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="账号启用" prop="accountEnabled">
              <el-switch v-model="userForm.accountEnabled" active-text="启用" inactive-text="禁用"
                         :active-value="1" :inactive-value="0" />
            </el-form-item>
          </div>

          <el-form-item label="角色" prop="roleIds">
            <el-select v-model="userForm.roleIds" multiple clearable
                       placeholder="请为该账号分配角色" style="width: 100%">
              <el-option v-for="role in roleOptions" :key="role.id"
                         :label="role.roleName" :value="role.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="cancelAdd" :disabled="submitting">取 消</el-button>
        <!-- 【修复】旧版无提交锁，双击会产生重复账号/重复请求 -->
        <el-button type="primary" :loading="submitting" @click="submitAddUser">确 认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { doDelete, doGet, doPost, doPut } from "@/http/httpRequest.js";
import router from "@/router/router.js";
import {
  Search, Refresh, Plus, Delete, EditPen, View, Filter, Grid,
  UserFilled, CircleCheck, Lock, Select, FolderOpened, User, Message, Iphone, InfoFilled
} from '@element-plus/icons-vue';
import { ElMessage } from "element-plus";
import { nextTick } from "vue";
import { getPermissionCache, messageFrame } from "@/util/util.js";

// 说明：不再注入 reload()——它会整页重挂 router-view，丢失筛选条件与所在页码；
// 改为 getData() 就地刷新，保留完整操作上下文（与交易/客户模块同范式）。

const userList = ref([]);
const pageSize = ref(10);
const total = ref(0);
const currentPage = ref(1);
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);

// 当前登录人 ID，用于「本人」标记与删除自我保护。
// 【不用 computed(getPermissionCache)】该读取无响应式依赖，computed 会首值即永久缓存，
// 若权限缓存尚未就绪则恒为 null。改为 onMounted 显式加载的 ref：缓存命中直接取，
// 未命中则回源 /api/login/info 兜底，确保拿到真实用户 ID。
const currentUserId = ref(null);
const loadCurrentUser = async () => {
  const cached = getPermissionCache();
  if (cached && cached.userId != null) {
    currentUserId.value = cached.userId;
    return;
  }
  try {
    const res = await doGet("/api/login/info", {});
    if (res.data.code === 200) currentUserId.value = res.data.data?.id ?? null;
  } catch (e) {
    // 拉取失败不阻断列表渲染，自我保护以后端为准
  }
};

const userForm = ref({
  id: 0,                     // 0 表示新增模式
  loginAct: '',
  loginPwd: '',
  name: '',
  phone: '',
  email: '',
  accountNoExpired: 1,
  credentialsNoExpired: 1,
  accountNoLocked: 1,
  accountEnabled: 1,
  roleIds: []
});
const roleOptions = ref([]);
const userFormRef = ref(null);
const userId = ref([]);      // 批量选中（旧版是普通数组，不参与响应式，工具条计数不刷新）

// 服务端筛选条件（对应后端 UserQuery 本轮补齐的检索能力）
const filters = ref({
  loginAct: '',
  name: '',
  phone: '',
  email: '',
  accountEnabled: '',
  roleId: ''
});

const isFiltering = computed(() => !!(filters.value.loginAct || filters.value.name
    || filters.value.phone || filters.value.email
    || filters.value.accountEnabled !== '' || filters.value.roleId !== ''))

const activeFilterCount = computed(() =>
    [filters.value.loginAct, filters.value.name, filters.value.phone, filters.value.email,
      filters.value.accountEnabled, filters.value.roleId]
        .filter(v => v !== '' && v != null).length)

// 本页启用/停用统计（账号健康度速览）
const enabledOnPage = computed(() => userList.value.filter(u => u.accountEnabled === 1).length)
const disabledOnPage = computed(() => userList.value.length - enabledOnPage.value)

// 基础校验规则（loginPwd 不设置 required，由 computed 动态决定）
const baseRules = {
  loginAct: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 2, max: 20, message: '账号长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 1, max: 20, message: '姓名长度在 1 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
};

// 动态表单规则（新增时密码必填，编辑时可选）
const userFormRules = computed(() => {
  if (userForm.value.id > 0) {
    return {
      ...baseRules,
      loginPwd: [{ min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }]
    };
  }
  return {
    ...baseRules,
    loginPwd: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
    ]
  };
});

onMounted(() => {
  getData(1);
  loadRoleOptions();
  loadCurrentUser();
});

// 角色下拉（新增/编辑分配 + 筛选共用；无 user:add/edit 权限者接口会拒绝，静默降级即可）
const loadRoleOptions = async () => {
  try {
    const res = await doGet("api/roles", {});
    if (res.data.code === 200) {
      roleOptions.value = res.data.data || [];
    }
  } catch (e) {
    console.error("加载角色列表失败", e);
  }
};

// 组装查询参数：仅携带实际填写的条件
const buildParams = (current) => {
  const params = { current };
  if (filters.value.loginAct) params.loginAct = filters.value.loginAct.trim();
  if (filters.value.name) params.name = filters.value.name.trim();
  if (filters.value.phone) params.phone = filters.value.phone.trim();
  if (filters.value.email) params.email = filters.value.email.trim();
  if (filters.value.accountEnabled !== '') params.accountEnabled = filters.value.accountEnabled;
  if (filters.value.roleId !== '') params.roleId = filters.value.roleId;
  return params;
};

const getData = async (current) => {
  currentPage.value = current;
  loading.value = true;
  try {
    const response = await doGet("api/users", buildParams(current));
    if (response.data.code === 200) {
      userList.value = response.data.data.list;
      pageSize.value = response.data.data.pageSize || pageSize.value;
      total.value = response.data.data.total;
    } else {
      ElMessage.error(response.data.msg || '获取账号列表失败');
    }
  } catch (e) {
    ElMessage.error('获取账号列表失败');
  } finally {
    loading.value = false;
  }
};

const doSearch = () => getData(1);

const resetFilters = () => {
  filters.value = { loginAct: '', name: '', phone: '', email: '', accountEnabled: '', roleId: '' };
  getData(1);
};

const toPage = (current) => getData(current);

// 连续序号：跳页保持递增
const rowIndex = (index) => (currentPage.value - 1) * pageSize.value + index + 1;

const formatDate = (val) => (val ? String(val).slice(0, 16) : '');

/**
 * 账号状态标签：按严重度取最突出的一个状态展示。
 * 禁用 > 锁定 > 账号过期 > 密码过期 > 正常
 */
const statusTag = (row) => {
  if (row.accountEnabled !== 1) return { label: '已禁用', cls: 'state-tag--red' };
  if (row.accountNoLocked !== 1) return { label: '已锁定', cls: 'state-tag--gold' };
  if (row.accountNoExpired !== 1) return { label: '账期已过', cls: 'state-tag--neutral' };
  if (row.credentialsNoExpired !== 1) return { label: '密码已过期', cls: 'state-tag--neutral' };
  return { label: '正常', cls: 'state-tag--green' };
};

// 当前登录人不可被勾选（配合后端「禁止删除自己」形成前后端一致的自我保护）
const rowSelectable = (row) => row.id !== currentUserId.value;

const handleDetail = (id) => {
  router.push("/dashboard/user/" + id);
};

// 打开新增对话框
const openAddUserDialog = () => {
  dialogVisible.value = true;
  nextTick(() => {
    if (userFormRef.value) {
      userFormRef.value.resetFields();
      userForm.value = {
        id: 0, loginAct: '', loginPwd: '', name: '', phone: '', email: '',
        accountNoExpired: 1, credentialsNoExpired: 1, accountNoLocked: 1,
        accountEnabled: 1, roleIds: []
      };
    }
  });
};

const cancelAdd = () => {
  dialogVisible.value = false;
  if (userFormRef.value) {
    userFormRef.value.resetFields();
    userForm.value.id = 0;
  }
};

// 加载用户数据（编辑回显；后端详情接口已脱敏，loginPwd 恒为 null）
const loadUser = async (id) => {
  const res = await doGet('api/user/' + id, {});
  if (res.data.code === 200) {
    userForm.value = { ...res.data.data };
    if (!userForm.value.roleIds) userForm.value.roleIds = [];
    userForm.value.loginPwd = '';   // 清空密码字段，留空表示不修改
    nextTick(() => {
      userFormRef.value?.clearValidate();
    });
  } else {
    ElMessage.error(res.data.msg || '加载账号信息失败');
  }
};

// 提交（新增或编辑）
const submitAddUser = async () => {
  if (!userFormRef.value || submitting.value) return;

  // 手动密码校验（表单规则之外的补充拦截）
  const isEdit = userForm.value.id > 0;
  const password = userForm.value.loginPwd;
  if (!isEdit && !password) {
    ElMessage.warning("新增账号时密码不能为空");
    return;
  }
  if (isEdit && password && (password.length < 6 || password.length > 20)) {
    ElMessage.warning("密码长度应在 6 到 20 个字符之间");
    return;
  }

  await userFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning("请正确填写表单信息");
      return;
    }
    submitting.value = true;
    try {
      let response;
      const submitData = { ...userForm.value };
      // 【安全修复】旧版此处 console.log('提交的数据', submitData) 把明文密码打印进控制台，已移除
      if (isEdit) {
        if (!submitData.loginPwd) {
          delete submitData.loginPwd;   // 编辑模式密码留空 → 不修改
        }
        response = await doPut("api/user", submitData);
      } else {
        response = await doPost("api/user", submitData);
      }

      if (response.data.code === 200) {
        ElMessage.success(isEdit ? "编辑账号成功" : "添加账号成功");
        dialogVisible.value = false;
        userForm.value.id = 0;
        userFormRef.value?.resetFields();
        // 就地刷新列表：保留当前筛选与页码上下文
        getData(currentPage.value);
      } else {
        // 后端已把账号/手机/邮箱重复转为精准文案，直接透出
        ElMessage.error(response.data.msg || (isEdit ? "编辑失败" : "添加失败"));
      }
    } catch (error) {
      console.error("提交用户出错", error);
      ElMessage.error("操作失败，请稍后重试");
    } finally {
      submitting.value = false;
    }
  });
};

const handleEdit = (id) => {
  dialogVisible.value = true;
  loadUser(id);
};

/**
 * 增删后的刷新策略：当前页删空且非第 1 页时自动回退一页，避免停在空白页。
 */
const refreshAfterMutate = (removedCount) => {
  const remaining = total.value - removedCount;
  const maxPage = Math.max(1, Math.ceil(remaining / pageSize.value));
  getData(Math.min(currentPage.value, maxPage));
};

// 删除单个账号
const handleDelete = async (id) => {
  if (id === currentUserId.value) {
    ElMessage.warning("不能删除当前登录的账号，请使用其他管理员账号操作");
    return;
  }
  messageFrame('删除后不可恢复，确认要删除该账号吗？').then(async () => {
    const response = await doDelete(`api/user/${id}`, {});
    if (response.data.code === 200) {
      ElMessage.success("删除成功");
      refreshAfterMutate(1);
    } else {
      // 后端对「被业务数据引用的账号」返回友好业务提示（如名下仍有关联活动）
      ElMessage.warning("删除失败：" + response.data.msg);
    }
  }).catch(() => {
    ElMessage({ type: 'info', message: '已取消删除' });
  });
};

// 批量删除
const deleteArr = () => {
  if (userId.value.length <= 0) {
    ElMessage.warning("请选择要删除的账号");
    return;
  }
  const ids = userId.value.join(",");

  messageFrame(`删除后不可恢复，确认要删除选中的 ${userId.value.length} 个账号吗？`).then(async () => {
    const count = userId.value.length;
    const deleteRes = await doDelete("api/user", { ids: ids });
    if (deleteRes.data.code === 200) {
      ElMessage.success("批量删除成功");
      userId.value = [];
      refreshAfterMutate(count);
    } else {
      ElMessage.warning("批量删除失败：" + deleteRes.data.msg);
    }
  }).catch(() => {
    ElMessage({ type: 'info', message: '已取消批量删除' });
  });
};

const handleSelectionChange = (selectionArr) => {
  userId.value = selectionArr.map(d => d.id);
};
</script>

<style scoped>
@import "@/assets/module-theme.css";

.user-module {
  min-height: 100%;
  box-sizing: border-box;
}

.module-hero__content { min-width: 0; }

/* 「本人」标记：紧跟姓名，弱化存在感 */
.self-tag {
  margin-left: 6px;
  transform: scale(0.84);
}

/* 角色芯片：多角色横向排列 */
.role-chip {
  margin: 1px 4px 1px 0;
}

/* 编辑模式密码提示 */
.pwd-hint {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #909399;
  margin: -12px 0 14px 100px;
}

/* 状态开关两列排布：避免对话框过长 */
.switch-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 12px;
}

.add-user-form {
  max-height: 56vh;
  overflow-y: auto;
  padding-right: 10px;
}

.add-user-form::-webkit-scrollbar { width: 6px; }
.add-user-form::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 44px 0;
  color: #93a89b;
  text-align: center;
}
.empty-state p { margin: 0; font-size: 13px; letter-spacing: 0.4px; max-width: 420px; }
</style>
