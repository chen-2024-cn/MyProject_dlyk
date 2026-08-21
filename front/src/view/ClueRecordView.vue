<template>
  <el-form
      ref="clueRefForm"
      :model="clueQuery"
      :rules="clueRules"
      label-width="100px"
      style="max-width: 95%;">

    <el-form-item label="负责人">
      <el-select
          v-model="clueQuery.ownerId"
          placeholder="请选择负责人"
          style="width: 100%"
          clearable
          disabled>
        <el-option
            v-for="item in ownerOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="所属活动">
      <el-select
          v-model="clueQuery.activityId"
          placeholder="请选择所属活动"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in activityOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="姓名" prop="fullName">
      <el-input v-model="clueQuery.fullName"/>
    </el-form-item>

    <el-form-item label="称呼">
      <el-select
          v-model="clueQuery.appellation"
          placeholder="请选择称呼"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in appellationOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="手机" v-if="clueQuery.id > 0"><!--此时是编辑-->
      <el-input v-model="clueQuery.phone" disabled/>
    </el-form-item>

    <el-form-item label="手机" prop="phone" v-else><!--此时是录入-->
      <el-input v-model="clueQuery.phone"/>
    </el-form-item>

    <el-form-item label="微信">
      <el-input v-model="clueQuery.weixin"/>
    </el-form-item>

    <el-form-item label="QQ" prop="qq">
      <el-input v-model="clueQuery.qq"/>
    </el-form-item>

    <el-form-item label="邮箱" prop="email">
      <el-input v-model="clueQuery.email"/>
    </el-form-item>

    <el-form-item label="年龄" prop="age">
      <el-input v-model="clueQuery.age"/>
    </el-form-item>

    <el-form-item label="职业">
      <el-input v-model="clueQuery.job"/>
    </el-form-item>

    <el-form-item label="年收入" prop="yearIncome">
      <el-input v-model="clueQuery.yearIncome"/>
    </el-form-item>

    <el-form-item label="住址">
      <el-input v-model="clueQuery.address"/>
    </el-form-item>

    <el-form-item label="贷款">
      <el-select
          v-model="clueQuery.needLoan"
          placeholder="请选择是否需要贷款"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in needLoanOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="意向状态">
      <el-select
          v-model="clueQuery.intentionState"
          placeholder="请选择意向状态"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in intentionStateOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="意向产品">
      <el-select
          v-model="clueQuery.intentionProduct"
          placeholder="请选择意向产品"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in productOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="线索状态">
      <el-select
          v-model="clueQuery.state"
          placeholder="请选择线索状态"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in clueStateOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="线索来源">
      <el-select
          v-model="clueQuery.source"
          placeholder="请选择线索来源"
          style="width: 100%"
          clearable>
        <el-option
            v-for="item in sourceOptions"
            :key="item.id"
            :label="item.typeValue"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="线索描述" prop="description">
      <el-input
          v-model="clueQuery.description"
          :rows="5"
          type="textarea"
          placeholder="请输入线索描述"/>
    </el-form-item>

    <el-form-item label="下次联系时间">
      <el-date-picker
          v-model="clueQuery.nextContactTime"
          type="datetime"
          style="width: 100%;"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择下次联系时间"/>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="addClueSubmit">提 交</el-button>
      <el-button type="success" plain @click="goBack">返 回</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { doGet, doPost, doPut } from "../http/httpRequest.js";
import { goBack, messageFrame } from "../util/util.js";

const router = useRouter();
const route = useRoute();

const clueRefForm = ref(null);

const clueQuery = reactive({
  id: 0,
  ownerId: '',
  activityId: '',
  fullName: '',
  appellation: '',
  phone: '',
  weixin: '',
  qq: '',
  email: '',
  age: '',
  yearIncome: '',
  description: '',
  nextContactTime: '',
  state: '',
  source: '',
  intentionState: '',
  intentionProduct: ''
});

const ownerOptions = ref([]);
const activityOptions = ref([]);
const productOptions = ref([]);
const appellationOptions = ref([]);
const needLoanOptions = ref([]);
const intentionStateOptions = ref([]);
const clueStateOptions = ref([]);
const sourceOptions = ref([]);

// 手机号重复校验
const checkPhone = async (_rule, value) => {
  if (!value) return;
  const resp = await doGet(`/api/clue/${value}`);
  if (resp.data.code === 500) {
    throw new Error('该手机号录入过了，不能再录入.');
  }
};

const clueRules = {
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号码格式有误', trigger: 'blur' },
    { validator: checkPhone, trigger: 'blur' }
  ],
  fullName: [
    { min: 2, message: '姓名至少2个汉字', trigger: 'blur' },
    { pattern: /^[一-龥]{0,}$/, message: '姓名必须为中文汉字', trigger: 'blur' }
  ],
  qq: [
    { min: 5, message: 'QQ号至少为5位', trigger: 'blur' },
    { pattern: /^\d+$/, message: 'QQ号码必须为数字', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '邮箱格式有误', trigger: 'blur' }
  ],
  age: [
    { pattern: /^\d+$/, message: '年龄必须为数字', trigger: 'blur' }
  ],
  yearIncome: [
    { pattern: /^[0-9]+(\.[0-9]{2})?$/, message: '年收入必须是整数或者两位小数', trigger: 'blur' }
  ],
  description: [
    { min: 5, max: 255, message: '线索描述长度为5-255个字符', trigger: 'blur' }
  ]
};

// 加载字典值
const loadDicValue = async (typeCode) => {
  const resp = await doGet(`/api/dicvalue/${typeCode}`);
  if (resp.data.code === 200) {
    const map = {
      appellation: appellationOptions,
      needLoan: needLoanOptions,
      intentionState: intentionStateOptions,
      clueState: clueStateOptions,
      source: sourceOptions,
      activity: activityOptions,
      product: productOptions
    };
    if (map[typeCode]) {
      map[typeCode].value = resp.data.data;
    }
  }
};

// 加载负责人选项
const loadOwner = async () => {
  const resp = await doGet('/api/owner');
  if (resp.data.code === 200) {
    ownerOptions.value = resp.data.data;
  }
};

// 加载当前登录用户并设置为负责人
const loadLoginUser = async () => {
  const resp = await doGet('/api/login/info');
  clueQuery.ownerId = resp.data.data.id;
};

// 加载要编辑的线索
const loadClue = async () => {
  const id = route.params.id;
  if (id) {
    const resp = await doGet(`/api/clue/detail/${id}`);
    if (resp.data.code === 200) {
      Object.assign(clueQuery, resp.data.data);
    }
  }
};

// 提交表单
const addClueSubmit = async () => {
  if (!clueRefForm.value) return;
  try {
    const valid = await clueRefForm.value.validate();
    if (valid) {
      const payload = { ...clueQuery };

      // Remove any Vue-specific reactivity if needed
      const cleanPayload = JSON.parse(JSON.stringify(payload));

      console.log('Sending payload:', cleanPayload);

      const api = clueQuery.id > 0 ? doPut : doPost;
      const resp = await api('/api/clue', cleanPayload);
      if (resp.data.code === 200) {
        messageFrame(clueQuery.id > 0 ? '编辑成功' : '录入成功', 'success');
        router.push('/dashboard/clue');
      } else {
        messageFrame(clueQuery.id > 0 ? '编辑失败' : '录入失败', 'error');
      }
    }
  } catch (error) {
    console.error(error);
    messageFrame('操作失败', 'error');
  }
};

onMounted(() => {
  loadDicValue('appellation');
  loadDicValue('needLoan');
  loadDicValue('intentionState');
  loadDicValue('clueState');
  loadDicValue('source');
  loadDicValue('activity');
  loadDicValue('product');
  loadOwner();
  loadLoginUser();
  loadClue();
});
</script>



<style scoped>

</style>