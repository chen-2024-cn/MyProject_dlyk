<template>
  <el-form
      ref="tranRefForm"
      :model="tranQuery"
      :rules="tranRules"
      label-width="120px"
      style="max-width: 95%;">

    <el-form-item label="客户" prop="customerId">
      <el-select
          v-model="tranQuery.customerId"
          placeholder="请选择客户"
          style="width: 100%"
          clearable
          filterable>
        <el-option
            v-for="item in customerOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"/>
      </el-select>
    </el-form-item>

    <el-form-item label="交易金额" prop="money">
      <el-input v-model="tranQuery.money" placeholder="请输入金额"/>
    </el-form-item>

    <el-form-item label="所处阶段" v-if="tranQuery.id === 0">
      <div class="desc">01创建交易（默认）</div>
    </el-form-item>

    <el-form-item label="预计成交日期">
      <el-date-picker
          v-model="tranQuery.expectedDate"
          type="date"
          style="width: 100%;"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择预计成交日期"/>
    </el-form-item>

    <el-form-item label="下次联系时间">
      <el-date-picker
          v-model="tranQuery.nextContactTime"
          type="datetime"
          style="width: 100%;"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择下次联系时间"/>
    </el-form-item>

    <el-form-item label="交易描述">
      <el-input
          v-model="tranQuery.description"
          :rows="5"
          type="textarea"
          placeholder="请输入交易描述"/>
    </el-form-item>

    <el-form-item>
      <el-button type="primary" @click="submitTran">提 交</el-button>
      <el-button type="success" plain @click="goBack">返 回</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { doGet, doPost, doPut } from '../http/httpRequest'
import { messageFrame } from "../util/util.js"

const router = useRouter()
const route = useRoute()

const tranRefForm = ref(null)
const customerOptions = ref([])

const tranQuery = reactive({
  id: 0,
  customerId: '',
  money: '',
  expectedDate: '',
  nextContactTime: '',
  description: ''
})

const tranRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  money: [
    { required: true, message: '请输入交易金额', trigger: 'blur' },
    { pattern: /^[0-9]+(\.[0-9]{1,2})?$/, message: '金额格式有误', trigger: 'blur' }
  ]
}

const loadCustomers = () => {
  doGet('/api/customer/options').then(resp => {
    if (resp.data.code === 200) {
      customerOptions.value = resp.data.data
    }
  })
}

const loadTran = () => {
  const id = route.params.id
  if (id) {
    doGet(`/api/tran/${id}`).then(resp => {
      if (resp.data.code === 200) {
        Object.assign(tranQuery, resp.data.data)
      }
    })
  }
}

const goBack = () => { window.history.back() }

const submitTran = async () => {
  if (!tranRefForm.value) return
  try {
    const valid = await tranRefForm.value.validate()
    if (valid) {
      const payload = JSON.parse(JSON.stringify(tranQuery))
      const api = tranQuery.id > 0 ? doPut : doPost
      const resp = await api('/api/trans', payload)
      if (resp.data.code === 200) {
        messageFrame(tranQuery.id > 0 ? '编辑成功' : '新建成功', 'success')
        router.push('/dashboard/tran')
      } else {
        messageFrame(tranQuery.id > 0 ? '编辑失败' : '新建失败', 'error')
      }
    }
  } catch (error) {
    console.error(error)
    messageFrame('操作失败', 'error')
  }
}

onMounted(() => {
  loadCustomers()
  loadTran()
})
</script>

<style scoped>
.desc {
  background-color: #F0FFFF;
  width: 100%;
  padding-left: 15px;
}
</style>
