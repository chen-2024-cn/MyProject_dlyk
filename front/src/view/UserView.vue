<template>
  <el-button type="success" round @click="openAddUserDialog" v-hasPermission="'user:add'">添加用户</el-button>
  <!--  新增用户对话对话框 -->
  <el-dialog
      v-model="dialogVisible"
      class="custom-transition-dialog"
      :title="userForm.id > 0 ? '编辑用户' : '新增用户'"
      center
      width="35%"
      destroy-on-close
      :transition="transitionConfig"
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
        <!-- 账号输入框 -->
        <el-form-item label="账号" prop="loginAct">
          <el-input
              v-model="userForm.loginAct"
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
        <el-form-item label="密码" prop="loginPwd">
          <el-input
              v-model="userForm.loginPwd"
              type="password"
              :placeholder="userForm.id > 0 ? '不修改请留空' : '请输入密码'"
              clearable
              show-password
              size="large"
          >
            <template #prefix
            >
              <el-icon>
                <Lock/>
              </el-icon>
            </template>
          </el-input>
        </el-form-item>
        <div v-if="userForm.id > 0" style="font-size: 12px; color: #909399; margin-top: -15px; margin-bottom: 15px;">
          <el-icon style="vertical-align: middle;">
            <InfoFilled/>
          </el-icon>
          <span style="vertical-align: middle;">编辑模式下，不填写密码将保持原密码不变</span>
        </div>


        <!-- 姓名 -->
        <el-form-item label="姓名" prop="name">
          <el-input
              v-model="userForm.name"
              placeholder="请输入姓名"
              clearable
              size="large"
          />
        </el-form-item>

        <!-- 手机号 -->
        <el-form-item label="手机号" prop="phone">
          <el-input
              v-model="userForm.phone"
              placeholder="请输入手机号"
              clearable
              size="large"
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item label="邮箱" prop="email">
          <el-input
              v-model="userForm.email"
              placeholder="请输入邮箱"
              clearable
              size="large"
          />
        </el-form-item>

        <!-- 账号状态配置区域 -->
        <el-divider content-position="center">账号状态配置</el-divider>

        <!-- 账号未过期 -->
        <el-form-item label="账号未过期" prop="accountNoExpired">
          <el-switch
              v-model="userForm.accountNoExpired"
              active-text="未过期"
              inactive-text="已过期"
              :active-value="1"
              :inactive-value="0"
          />
        </el-form-item>

        <!-- 凭证未过期 -->
        <el-form-item label="密码未过期" prop="credentialsNoExpired">
          <el-switch
              v-model="userForm.credentialsNoExpired"
              active-text="未过期"
              inactive-text="已过期"
              :active-value="1"
              :inactive-value="0"
          />
        </el-form-item>

        <!-- 账号未锁定 -->
        <el-form-item label="账号未锁定" prop="accountNoLocked">
          <el-switch
              v-model="userForm.accountNoLocked"
              active-text="未锁定"
              inactive-text="已锁定"
              :active-value="1"
              :inactive-value="0"
          />
        </el-form-item>

        <!-- 账号启用 -->
        <el-form-item label="账号启用" prop="accountEnabled">
          <el-switch
              v-model="userForm.accountEnabled"
              active-text="启用"
              inactive-text="禁用"
              :active-value="1"
              :inactive-value="0"
          />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="cancelAdd">取消</el-button>
      <el-button type="primary" @click="submitAddUser">确认</el-button>
    </template>
  </el-dialog>

  <el-button type="danger" round @click="deleteArr" v-hasPermission="'user:delete'">批量删除</el-button>

  <div v-if="search" style="margin-top: 10px; color: #909399; font-size: 14px;">
    <el-icon style="vertical-align: middle;"><Search /></el-icon>
    搜索结果：共找到 {{ filteredUserList.length }} 条记录
  </div>


  <el-table
      :data="filteredUserList"
      style="width: 100%"
      @selection-change="handleSelectionChange"
  >

    <el-table-column type="selection" width="55"/>
    <el-table-column type="index" label="序号" min-width="55"/>
    <el-table-column label="账号" prop="loginAct"/>
    <el-table-column label="姓名" prop="name" show-overflow-tooltip/>
    <el-table-column label="手机" prop="phone" show-overflow-tooltip/>
    <el-table-column label="邮箱" prop="email" show-overflow-tooltip/>
    <el-table-column label="创建时间" prop="createTime" show-overflow-tooltip/>
    <el-table-column align="right" min-width="100">
      <template #header>
        <div style="display: flex; align-items: center; gap: 5px;">
          <el-input
              v-model="search"
              size="small"
              placeholder="搜索账号/姓名/手机/邮箱"
              clearable            style="width: 200px;"
          />
        </div>
      </template>
      <template #default="scope">
        <el-button size="small" type="success" @click="handleDetail(scope.row.id)">详情</el-button>
        <el-button size="small" @click="handleEdit(scope.row.id)">Edit</el-button>
        <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">Delete</el-button>
      </template>
    </el-table-column>
  </el-table>

  <el-pagination
      background
      layout="prev, pager, next"
      :total="total"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      @current-change="toPage"
  />
</template>

<script setup>
import {computed, inject, onMounted, ref} from "vue";
import {doDelete, doGet, doPost, doPut} from "@/http/httpRequest.js";
import router from "@/router/router.js";
import {Lock, User} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {nextTick} from "vue";
import {messageFrame} from "@/util/util.js";

let userList = ref([]);
const pageSize = ref(0);
const total = ref(0);
const currentPage = ref(1);
const dialogVisible = ref(false);
const userForm = ref({
  id: 0,                     // 增加 id 字段，0 表示新增模式
  loginAct: '',
  loginPwd: '',
  name: '',
  phone: '',
  email: '',
  accountNoExpired: 1,
  credentialsNoExpired: 1,
  accountNoLocked: 1,
  accountEnabled: 1
});
const search = ref('');
const userFormRef = ref(null);
const reload = inject("reload");
let userId = []

// 基础校验规则（loginPwd 不设置 required，手动控制）
const baseRules = {
  loginAct: [
    {required: true, message: '请输入账号', trigger: 'blur'},
    {min: 2, max: 20, message: '账号长度在 2 到 20 个字符', trigger: 'blur'}
  ],
  name: [
    {required: true, message: '请输入姓名', trigger: 'blur'},
    {min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur'}
  ],
  phone: [
    {required: true, message: '请输入手机号', trigger: 'blur'},
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur'}
  ],
  email: [
    {required: true, message: '请输入邮箱', trigger: 'blur'},
    {type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}
  ]
};

// 动态表单规则（新增时密码必填，编辑时可选）
const userFormRules = computed(() => {
  if (userForm.value.id > 0) {
    // 编辑模式：密码不是必填，但若有值需校验长度
    return {
      ...baseRules,
      loginPwd: [
        {min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur'}
      ]
    };
  } else {
    // 新增模式：密码必填
    return {
      ...baseRules,
      loginPwd: [
        {required: true, message: '请输入密码', trigger: 'blur'},
        {min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur'}
      ]
    };
  }
});

onMounted(() => {
  getData(1);
});

// 过滤后的用户列表（根据搜索关键词）
const filteredUserList = computed(() => {
  if (!search.value.trim()) {
    return userList.value;
  }

  const keyword = search.value.toLowerCase().trim();
  return userList.value.filter(user => {
    return (
        (user.loginAct && user.loginAct.toLowerCase().includes(keyword)) ||
        (user.name && user.name.toLowerCase().includes(keyword)) ||
        (user.phone && user.phone.toLowerCase().includes(keyword)) ||
        (user.email && user.email.toLowerCase().includes(keyword))
    );
  });
});



//批量删除
const deleteArr = () => {
  console.log(userId.length);
  if (userId.length <= 0){
    ElMessage.warning("请选择要删除的用户")
    return
  }

  let ids = userId.join(",");//将用户Id数组转为字符串

  messageFrame('确认要批量删除这些信息吗？').then(async () => {
    const deleteRes = await doDelete("api/user", {ids:ids});
        if (deleteRes.data.code === 200) {
          reload();
          ElMessage.success("批量删除成功");
        } else {
          ElMessage.warning("批量删除失败," + deleteRes.data.msg);
        }
      }
  ).catch( () => {
        ElMessage({
          type: 'info',
          message: '已取消批量删除 ',
        })
      }
  )
}
const handleSelectionChange = (selectionArr) => {
  userId = []
  selectionArr.forEach(data => {
    userId.push(data.id)
  })
};

const getData = async (current) => {
  currentPage.value = current;
  const response = await doGet("api/users", {current});
  if (response.data.code === 200) {
    userList.value = response.data.data.list;
    pageSize.value = response.data.data.pageSize;
    total.value = response.data.data.total;
  }
};

const toPage = (current) => {
  getData(current);
};

const handleDetail = (id) => {
  router.push("/dashboard/user/" + id);
};

// 打开新增用户对话框
const openAddUserDialog = () => {
  dialogVisible.value = true;
  nextTick(() => {
    if (userFormRef.value) {
      userFormRef.value.resetFields();
      // 重置为初始状态（id = 0 表示新增模式）
      userForm.value = {
        id: 0,
        loginAct: '',
        loginPwd: '',
        name: '',
        phone: '',
        email: '',
        accountNoExpired: 1,
        credentialsNoExpired: 1,
        accountNoLocked: 1,
        accountEnabled: 1
      };
    }
  });
};

// 取消关闭对话框
const cancelAdd = () => {
  dialogVisible.value = false;
  if (userFormRef.value) {
    userFormRef.value.resetFields();
    // 确保重置 id
    userForm.value.id = 0;
  }
};

// 加载用户数据（编辑用）
const loadUser = async (id) => {
  const res = await doGet('api/user/' + id, {});
  if (res.data.code === 200) {
    userForm.value = res.data.data;
    userForm.value.loginPwd = '';   // 清空密码字段，避免显示占位符
    // 清除表单校验状态
    nextTick(() => {
      userFormRef.value?.clearValidate();
    });
  }
};

// 提交（新增或编辑）
const submitAddUser = async () => {
  if (!userFormRef.value) return;

  // 额外的手动密码校验
  const isEdit = userForm.value.id > 0;
  const password = userForm.value.loginPwd;

  if (!isEdit && !password) {
    ElMessage.warning("新增用户时密码不能为空");
    return;
  }
  if (isEdit && password && (password.length < 6 || password.length > 20)) {
    ElMessage.warning("密码长度应在 6 到 20 个字符之间");
    return;
  }

  // 触发表单其他字段校验
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let response;
        // 准备提交的数据（深拷贝避免影响原对象）
        const submitData = {...userForm.value};
        console.log('提交的数据',submitData)

        if (isEdit) {
          // 编辑模式：如果密码为空则删除该字段，后端不会修改密码
          if (!submitData.loginPwd) {
            delete submitData.loginPwd;
          }
          response = await doPut("api/user", submitData); // 假设更新接口为 PUT /api/user
        } else {
          // 新增模式
          response = await doPost("api/user", submitData);
        }

        if (response.data.code === 200) {
          ElMessage.success(isEdit ? "编辑用户成功" : "添加用户成功");
          dialogVisible.value = false;
          reload();                 // 刷新父组件数据
          // 重置表单（清空 id 等）
          userForm.value.id = 0;
          userFormRef.value?.resetFields();
        } else {
          ElMessage.error(response.data.msg || (isEdit ? "编辑失败" : "添加失败"));
        }
      } catch (error) {
        console.error("提交用户出错", error);
        ElMessage.error("操作失败，请稍后重试");
      }
    } else {
      ElMessage.warning("请正确填写表单信息");
    }
  });
};

// 编辑按钮逻辑
const handleEdit = (id) => {
  dialogVisible.value = true;
  loadUser(id);   // 加载数据，此时 userForm.id 会被赋值为 id > 0
};

// 删除
const handleDelete = async (id) => {
    messageFrame('确认要删除此人信息吗？').then(async () => {
          const response = await doDelete(`api/user/${id}`, {});
          if (response.data.code === 200) {
            reload();
            ElMessage.success("删除成功");
          } else {
            ElMessage.warning("删除失败," + response.data.msg);
          }
        }
    ).catch( () => {
      ElMessage({
        type: 'info',
        message: '已取消删除 ',
      })
        }
    )

};
</script>

<style scoped>
.el-table {
  margin-top: 15px;
}

.el-pagination {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}

.el-button {
  margin: 15px;
}

.add-user-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 10px;
}

/* 自定义滚动条样式（可选） */
.add-user-form::-webkit-scrollbar {
  width: 6px;
}

.add-user-form::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}
</style>