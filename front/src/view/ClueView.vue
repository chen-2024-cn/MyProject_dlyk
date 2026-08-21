<template>
  <el-button type="primary" class="btn" @click="addClue" v-hasPermission="'clue:add'">录入线索</el-button>
  <el-button type="success" class="btn" @click="importExcel" v-hasPermission="'clue:add'">导入线索(Excel)</el-button>
  <el-button type="danger" class="btn" @click="batchDelClue" v-hasPermission="'clue:delete'">批量删除</el-button>

  <el-table
      :data="clueList"
      style="width: 100%"
      @selection-change="handleSelectionChange">
    <el-table-column type="selection" width="50"/>
    <el-table-column type="index" label="序号" width="65"/>
    <el-table-column property="ownerDO.name" label="负责人" width="120" />
    <el-table-column property="activityDO.name" label="所属活动"/>
    <el-table-column label="姓名">
      <template #default="scope">
        <a href="javascript:" @click="view(scope.row.id)">{{ scope.row.fullName }}</a>
      </template>
    </el-table-column>
    <el-table-column property="appellationDO.typeValue" label="称呼"/>
    <el-table-column property="phone" label="手机" width="120"/>
    <el-table-column property="weixin" label="微信" width="120"/>
    <el-table-column property="needLoanDO.typeValue" label="是否贷款"/>
    <el-table-column property="intentionStateDO.typeValue" label="意向状态"/>
    <el-table-column property="intentionProductDO.name" label="意向产品"/>
    <el-table-column property="stateDO.typeValue" label="线索状态"/>
    <el-table-column property="sourceDO.typeValue" label="线索来源"/>
    <el-table-column property="nextContactTime" label="下次联系时间" width="165"/>
    <el-table-column label="操作" width="230">
      <template #default="scope">
        <el-button type="primary" @click="view(scope.row.id)" v-hasPermission="'clue:view'">详情</el-button>
        <el-button type="success" @click="edit(scope.row.id)" v-hasPermission="'clue:edit'">编辑</el-button>
        <el-button type="danger" @click="del(scope.row.id)" v-hasPermission="'clue:delete'">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
  <p>
    <el-pagination
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="total"
        @prev-click="page"
        @next-click="page"
        @current-change="page"/>
  </p>

  <!--导入线索Excel的弹窗（对话框）-->
  <el-dialog v-model="importExcelDialogVisible" title="导入线索Excel" width="55%" center v-hasPermission="'clue:import'">
    <el-upload
        ref="uploadRef"
        method="post"
        drag
        :http-request="uploadFile"
        :auto-upload="false">
      <template #trigger>
        <el-button type="primary">选择Excel文件</el-button>
        <br/>
        &nbsp;仅支持后缀名为.xls或.xlsx的文件
      </template>

      <br/>
      <br/>
      <div>重要提示：</div>
      <ul>
        <li>上传仅支持后缀名为.xls或.xlsx的文件；</li>
        <li>给定Excel文件的第一行将视为字段名；</li>
        <li>请确认您的文件大小不超过50MB；</li>
        <li>日期值以文本形式保存，必须符合yyyy-MM-dd格式；</li>
        <li>日期时间以文本形式保存，必须符合yyyy-MM-dd HH:mm:ss的格式；</li>
      </ul>
    </el-upload>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="importExcelDialogVisible = false">关 闭</el-button>
        <el-button class="ml-3" type="success" @click="submitUpload" >上 传</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import {doDelete, doGet, doPost, doUploadFile} from '../http/httpRequest'
import { messageFrame } from "@/util/util.js";

// 路由
const router = useRouter()
// 页面刷新方法（由父组件注入）
const reload = inject('reload')

// 数据
const clueList = ref([])
const pageSize = ref(0)
const total = ref(0)
const importExcelDialogVisible = ref(false)
const clueIdArray = ref([])

// 上传组件 ref
const uploadRef = ref(null)

// 页面数据缓存，key: 页码, value: 该页数据列表
const pageCache = ref(new Map())

// 获取线索分页列表数据
const getData = (current) => {
  // 如果缓存中已有该页数据，直接使用缓存
  if (pageCache.value.has(current)) {
    clueList.value = pageCache.value.get(current)
    return
  }
  doGet('/api/clues', {
    current: current
  }).then(resp => {
    if (resp.data.code === 200) {
      const list = resp.data.data.list
      clueList.value = list
      pageSize.value = resp.data.data.pageSize
      total.value = resp.data.data.total
      // 将该页数据存入缓存
      pageCache.value.set(current, list)
    }
  })
}

// 清空页面缓存（数据变更时调用）
const clearPageCache = () => {
  pageCache.value.clear()
}

// 分页函数
const page = (number) => {
  getData(number)
}

// 录入线索
const addClue = () => {
  router.push('/dashboard/clue/add')
}

// 编辑线索
const edit = (id) => {
  router.push(`/dashboard/clue/edit/${id}`)
}

// 导入线索Excel（打开弹窗）
const importExcel = () => {
  importExcelDialogVisible.value = true
}

// 自定义文件上传逻辑
const uploadFile = (param) => {
  const fileObj = param.file
  const formData = new FormData()
  formData.append('file', fileObj)
  doUploadFile('/api/importExcel', formData).then(resp => {
    if (resp.data.code === 200) {
      messageFrame('导入成功', 'success')
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
      importExcelDialogVisible.value = false
      clearPageCache()
      reload()
    } else {
      messageFrame('导入失败：' + resp.data.msg, 'error')
    }
  })
}

// 提交上传
const submitUpload = () => {
  if (uploadRef.value) {
    uploadRef.value.submit()
  }
}

// 单个删除
const del = (id) => {
  messageFrame('您确定要删除此数据吗？').then(() => {
    doDelete(`/api/clue/${id}`, {}).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('删除成功', 'success')
        clearPageCache()
        reload()
      } else {
        messageFrame('删除失败，原因：' + resp.data.msg, 'error')
      }
    })
  }).catch(() => {
    messageFrame('取消删除', 'warning')
  })
}

// 批量删除
const batchDelClue = () => {
  if (clueIdArray.value.length <= 0) {
    messageFrame('请选择要删除的数据', 'warning')
    return
  }
  messageFrame('您确定要删除这些数据吗？').then(() => {
    const ids = clueIdArray.value.join(',')
    doDelete('/api/clue/batch', { ids }).then(resp => {
      if (resp.data.code === 200) {
        messageFrame('批量删除成功', 'success')
        clearPageCache()
        reload()
      } else {
        messageFrame('批量删除失败，原因：' + resp.data.msg, 'error')
      }
    })
  }).catch(() => {
    messageFrame('取消批量删除', 'warning')
  })
}

// 表格勾选变化
const handleSelectionChange = (dataObjectArray) => {
  clueIdArray.value = dataObjectArray.map(item => item.id)
}

// 查看详情
const view = (id) => {
  router.push(`/dashboard/clue/${id}`)
}

// 初始化加载数据
onMounted(() => {
  getData(1)
})
</script>

<style scoped>
.btn {
  margin-bottom: 10px;
}
</style>