<template>
  <div class="cyber-container">
    <el-row :gutter="20" class="full-height-row">

      <!-- 左侧：身份卡片 + 能力商店 + 快捷指令 -->
      <el-col :xs="24" :sm="8" :md="6" class="privilege-col">
        <div class="cyber-deck-panel box-gradient-border">
          <!-- 身份卡 -->
          <div class="deck-header">
            <div class="deck-avatar-pulse">
              <el-icon v-if="isAdmin" :size="24" color="#dff0e3"><Tools /></el-icon>
              <el-icon v-else :size="24" color="#8faa94"><User /></el-icon>
            </div>
            <div class="deck-title-wrapper">
              <h4 class="deck-main-title">AI 业务领航员</h4>
              <p class="deck-sub-title">
                {{ profile ? (isAdmin ? '管理模式 · ' : '服务模式 · ') + profile.userName : '身份校验中…' }}
              </p>
            </div>
          </div>

          <el-divider class="cyber-divider" />

          <!-- 能力商店：按角色由服务端下发，免费能力直接可用，付费能力需开通 -->
          <div class="deck-section" v-loading="!profile">
            <h5 class="section-title"><el-icon><Cpu /></el-icon> 智能能力舱</h5>
            <p class="section-desc">以下能力由系统按您的角色职责自动配备；付费能力开通后立即可用。</p>

            <div class="permission-nodes">
              <div v-for="ab in abilities" :key="ab.key" class="permission-card"
                   :class="{ 'card-active': ab.purchased, 'card-premium': ab.premium }">
                <div class="card-left">
                  <div class="node-meta">
                    <span class="node-title">
                      {{ ab.premium ? '💎 ' : '' }}{{ ab.name }}
                      <el-tag v-if="ab.premium" size="small" effect="dark" class="premium-tag">
                        {{ ab.purchased ? '已开通' : '¥' + ab.price }}
                      </el-tag>
                    </span>
                    <span class="node-key-badge">{{ ab.key }}</span>
                  </div>
                </div>
                <div class="node-desc">{{ ab.description }}</div>
                <div class="node-action" v-if="ab.premium">
                  <el-button v-if="!ab.purchased" type="warning" size="small" plain @click="buyAbility(ab)">
                    立即开通
                  </el-button>
                  <span v-else class="purchased-badge">✓ 已解锁，直接向领航员下达指令即可</span>
                </div>
              </div>
            </div>
          </div>

          <el-divider class="cyber-divider" />

          <!-- 角色差异化快捷指令 -->
          <div class="deck-section">
            <h5 class="section-title"><el-icon><Compass /></el-icon> {{ isAdmin ? '管理员指令台' : '常用服务指令' }}</h5>
            <div class="quick-commands">
              <el-button v-for="cmd in quickCommands" :key="cmd.text" :type="cmd.type" plain
                         class="cyber-btn-mini" @click="sendQuickCommand(cmd.text)">
                {{ cmd.label }}
              </el-button>
            </div>
          </div>

          <div class="deck-footer">
            <el-button class="cyber-btn-mini" type="info" plain style="width:100%" @click="showOrders = true">
              🧾 我的充值订单
            </el-button>
            <el-button class="cyber-reset-btn" @click="resetMemory">
              <el-icon><Refresh /></el-icon> 清空对话记忆
            </el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧：对话区 -->
      <el-col :xs="24" :sm="16" :md="18" class="chat-col">
        <div class="cyber-chat-panel box-gradient-border">
          <div class="chat-header">
            <div class="header-left">
              <span class="status-dot pulsing"></span>
              <span class="terminal-title">业务领航频道 · {{ isAdmin ? '管理驾驶舱' : '旅客服务窗' }}</span>
            </div>
            <div class="header-right">
              <span class="network-badge">服务在线</span>
            </div>
          </div>

          <div class="chat-body" ref="chatBodyRef">
            <!-- 欢迎语：按角色差异化 -->
            <div class="welcome-intro" v-if="messages.length === 0 && profile">
              <div class="welcome-logo">
                <el-icon v-if="isAdmin" :size="48" color="#52b788"><Tools /></el-icon>
                <el-icon v-else :size="48" color="#a9d6b4"><User /></el-icon>
              </div>
              <h3 class="welcome-title">{{ isAdmin ? '领航员已切换为管理模式' : '欢迎回来，' + profile.userName }}</h3>
              <p class="welcome-desc" v-if="isAdmin">
                已识别您的管理员身份。我可以为您导出/导入用户 Excel、全局查询订单、
                管理用户角色与账号状态，以及提供增值的经营洞察与趋势预测。
              </p>
              <p class="welcome-desc" v-else>
                我可以帮您查询自己名下的订单、提醒待跟进的交易、解答业务流程疑难、
                咨询产品行情，以及提供增值的个人经营洞察。直接说出您的需求即可。
              </p>
              <div class="guide-box" v-if="!isAdmin">
                <h5>💡 试试这样问：</h5>
                <ol>
                  <li>“查询我最近的订单”</li>
                  <li>“未来3天我有哪些交易需要跟进？”</li>
                  <li>“交易阶段是怎么流转的？”</li>
                </ol>
              </div>
            </div>

            <!-- 消息列表 -->
            <div v-for="(msg, idx) in messages" :key="idx" class="message-wrapper"
                 :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
              <div class="message-avatar">
                <span v-if="msg.role === 'user'">🧑‍💼</span>
                <span v-else>🤖</span>
              </div>
              <div class="message-bubble-wrapper">
                <div class="sender-info">
                  <span class="sender-name">{{ msg.role === 'user' ? '我' : 'AI 领航员' }}</span>
                  <span class="sender-time">{{ msg.time }}</span>
                </div>
                <div class="message-bubble" :class="{ 'stream-pulsing': msg.streaming }">
                  <div class="message-text" v-html="formatMessage(msg.content)"></div>
                  <span v-if="msg.streaming" class="cyber-typing-cursor">_</span>
                  <!-- 结构化事件卡片（服务端工具推送，不依赖大模型转述） -->
                  <div v-for="(ev, ei) in (msg.events || [])" :key="ei" class="event-card"
                       :class="'event-' + ev.type">
                    <template v-if="ev.type === 'PREMIUM_REQUIRED'">
                      <div class="event-title">💎 增值能力待开通：{{ ev.abilityName }}</div>
                      <div class="event-desc">该指令基于特定条件触发，开通后可立即重新发送指令使用。</div>
                      <el-button type="warning" size="small" @click="buyAbilityByKey(ev.abilityKey)"
                                 :disabled="ev.done">
                        {{ ev.done ? '✓ 已开通' : '¥' + ev.price + ' 立即开通' }}
                      </el-button>
                    </template>
                    <template v-else-if="ev.type === 'FILE_READY'">
                      <div class="event-title">📦 文件已生成：{{ ev.fileName }}</div>
                      <el-button type="success" size="small" @click="downloadFile(ev.fileName)">下载文件</el-button>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="chat-footer">
            <div class="input-container">
              <el-input v-model="userInput" type="textarea" :rows="3" resize="none"
                        :placeholder="isAdmin ? '向领航员下达管理指令…（Ctrl + Enter 发送）' : '向领航员说出您的需求…（Ctrl + Enter 发送）'"
                        class="cyber-input" @keydown.enter.ctrl.prevent="sendInstruction" :disabled="loading" />
              <div class="input-controls">
                <span class="shortcuts-tip">
                  Ctrl + Enter 发送
                </span>
                <!-- 管理员专属：附件上传入口（导入用；已选文件显示附件条并可移除） -->
                <span class="attach-zone" v-if="isAdmin">
                  <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileChosen" />
                  <span v-if="attachment" class="attachment-chip">
                    📎 {{ attachment.fileName }}
                    <el-button type="danger" size="small" text class="chip-remove" @click="clearAttachment">移除</el-button>
                  </span>
                  <el-button v-else type="primary" size="small" plain class="attach-upload-btn"
                             @click="fileInputRef?.click()">
                    <el-icon><Upload /></el-icon> 上传 Excel
                  </el-button>
                </span>
                <el-button type="success" class="cyber-send-btn" @click="sendInstruction" :loading="loading">
                  <el-icon v-if="!loading"><Compass /></el-icon> 发送
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 充值订单弹窗 -->
    <el-dialog v-model="showOrders" title="我的充值订单" width="560px" :append-to-body="true" destroy-on-close>
      <el-table :data="orders" size="small" v-loading="loadingOrders" empty-text="暂无订单记录" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="abilityName" label="能力" width="140" />
        <el-table-column prop="price" label="金额" width="80">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">已支付</el-tag>
            <el-tag v-else-if="row.status === 2" type="info" size="small">已取消</el-tag>
            <el-tag v-else type="warning" size="small">待支付</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="primary" size="small" text @click="payOrderRow(row)">支付</el-button>
              <el-button type="danger" size="small" text @click="cancelOrderRow(row)">取消</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Cpu, Compass, Refresh, Upload, Tools, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { doGet, doPostJson, doUploadFile } from "@/http/httpRequest.js"
import { createSseConnection, AI_STREAM_CHAT } from "@/http/sseClient.js"
import { getToken } from "@/util/util.js"

const userInput = ref('')
const loading = ref(false)
const messages = ref([])
const chatBodyRef = ref(null)
// 会话 ID 兜底：profile 加载成功后会替换为 'AI-BIZ-{userId}' 的确定性 ID，
// 保证同一账号切换模块再回来时延续同一上下文记忆与历史记录
const threadMemoryId = ref('AI-BIZ-' + Math.random().toString(36).substring(2, 11).toUpperCase())
const currentSse = ref(null)

// ------------------------- 角色与能力 -------------------------
const profile = ref(null)
const isAdmin = computed(() => profile.value?.role === 'ADMIN')
const abilities = computed(() => profile.value?.abilities || [])

/** 加载角色画像：能力清单由服务端按登录人角色下发 */
const loadProfile = async () => {
  try {
    const r = await doGet('/api/ai/profile', {})
    if (r.data?.code === 200) {
      profile.value = r.data.data
      // 确定性会话 ID：同一账号多次进入 AI 模块复用同一上下文（记忆/附件暂存延续）
      if (profile.value.userId) {
        threadMemoryId.value = 'AI-BIZ-' + profile.value.userId
      }
      await loadHistory()
    }
  } catch (e) {
    console.error('加载 AI 角色画像失败', e)
  }
}

/**
 * 恢复聊天记录（服务端 Redis 持久化）：
 * 登录态内切换模块/刷新页面/关浏览器再打开均完整恢复；退出登录后永久清除。
 * 注意：付费开通卡片/文件下载按钮等瞬时交互事件不恢复（产品已确认取舍）。
 */
const loadHistory = async () => {
  try {
    const r = await doGet('/api/ai/history', {})
    if (r.data?.code === 200 && Array.isArray(r.data.data)) {
      messages.value = r.data.data.map(item => ({
        role: item.role === 'user' ? 'user' : 'ai',
        content: item.content || '',
        time: item.time || '',
        streaming: false,
        events: []
      }))
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('恢复 AI 聊天记录失败', e)
  }
}

/** 按角色渲染快捷指令（与后端工具包能力一一对应） */
const quickCommands = computed(() => {
  if (!isAdmin.value) {
    return [
      { label: '查询我的订单', text: '查询我最近的订单', type: 'primary' },
      { label: '跟进提醒', text: '未来3天我有哪些交易需要跟进联系？', type: 'success' },
      { label: '业务疑难', text: '交易阶段是怎么流转的？线索如何转化为客户？', type: 'warning' },
      { label: '产品行情', text: '查询当前在售的产品和报价', type: 'primary' },
      { label: '经营洞察（付费）', text: '生成我的经营深度洞察报告', type: 'danger' },
      { label: '趋势预测（付费）', text: '预测我的交易趋势', type: 'danger' }
    ]
  }
  return [
    { label: '导出用户Excel', text: '导出全部用户数据为Excel', type: 'primary' },
    { label: '下载导入模板', text: '生成用户批量导入的标准模板', type: 'success' },
    { label: '导入用户', text: '把我刚才上传的Excel附件批量导入用户', type: 'success' },
    { label: '全局订单', text: '全局查询最近的交易订单', type: 'primary' },
    { label: '用户角色盘点', text: '查询所有用户的账号状态与角色', type: 'primary' },
    { label: '角色清单', text: '查询系统中全部可用角色及ID', type: 'primary' },
    { label: '全局洞察（付费）', text: '生成全局经营深度洞察报告', type: 'danger' },
    { label: '趋势预测（付费）', text: '做全局交易趋势预测', type: 'danger' }
  ]
})

// ------------------------- 附件（管理员导入） -------------------------
const fileInputRef = ref(null)
const attachment = ref(null) // { fileId, fileName }

const onFileChosen = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  try {
    const r = await doUploadFile('/api/ai/file/upload', formData)
    if (r.data?.code === 200) {
      attachment.value = { fileId: r.data.data.fileId, fileName: r.data.data.fileName }
      ElMessage.success('附件已暂存，发送消息时输入导入指令即可。')
    } else {
      ElMessage.error(r.data?.msg || '上传失败')
    }
  } catch (err) {
    ElMessage.error('上传失败：' + (err.response?.data?.msg || err.message))
  } finally {
    e.target.value = ''
  }
}

const clearAttachment = () => { attachment.value = null }

// ------------------------- 付费开通与订单 -------------------------
const showOrders = ref(false)
const orders = ref([])
const loadingOrders = ref(false)

const abilityByKey = (key) => abilities.value.find(a => a.key === key)

/** 发起开通购买：下单 → 确认 → 模拟支付 → 刷新能力状态 */
const buyAbility = async (ab) => {
  try {
    const r = await doPostJson('/api/ai/payment/order', { abilityKey: ab.key })
    if (r.data?.code !== 200) return ElMessage.error(r.data?.msg || '下单失败')
    const order = r.data.data

    await ElMessageBox.confirm(
      `确认支付 ¥${order.price} 开通「${order.abilityName}」？（演示环境模拟支付网关，确认后即生效）`,
      '支付确认', { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }
    )

    const pay = await doPostJson('/api/ai/payment/pay', { orderNo: order.orderNo })
    if (pay.data?.code === 200) {
      ElMessage.success(`「${order.abilityName}」开通成功！`)
      await loadProfile()
    } else {
      ElMessage.error(pay.data?.msg || '支付失败')
    }
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败：' + (err.response?.data?.msg || '请稍后重试'))
    }
  }
}

/** 事件卡片中的开通入口（按 key 查能力） */
const buyAbilityByKey = async (key) => {
  const ab = abilityByKey(key)
  if (ab) await buyAbility(ab)
  else {
    // 兜底：能力列表尚未加载时直接下单
    const r = await doPostJson('/api/ai/payment/order', { abilityKey: key })
    if (r.data?.code === 200) {
      try {
        await ElMessageBox.confirm(`确认支付 ¥${r.data.data.price} 开通该能力？`, '支付确认',
          { confirmButtonText: '确认支付', cancelButtonText: '取消' })
        await doPostJson('/api/ai/payment/pay', { orderNo: r.data.data.orderNo })
        ElMessage.success('开通成功！')
        await loadProfile()
      } catch (e) { /* 用户取消 */ }
    }
  }
}

const loadOrders = async () => {
  if (!showOrders.value) return
  loadingOrders.value = true
  try {
    const r = await doGet('/api/ai/payment/orders', {})
    if (r.data?.code === 200) orders.value = r.data.data || []
  } finally {
    loadingOrders.value = false
  }
}

const payOrderRow = async (row) => {
  const r = await doPostJson('/api/ai/payment/pay', { orderNo: row.orderNo })
  if (r.data?.code === 200) {
    ElMessage.success('支付成功，能力已开通！')
    await Promise.all([loadOrders(), loadProfile()])
  } else {
    ElMessage.error(r.data?.msg || '支付失败')
  }
}

const cancelOrderRow = async (row) => {
  const r = await doPostJson('/api/ai/payment/cancel', { orderNo: row.orderNo })
  if (r.data?.code === 200) {
    ElMessage.success('订单已取消')
    await loadOrders()
  } else {
    ElMessage.error(r.data?.msg || '取消失败')
  }
}

// 弹窗打开时懒加载订单
watch(showOrders, (v) => { if (v) loadOrders() })

// ------------------------- 文件下载（管理员） -------------------------
const downloadFile = (fileName) => {
  const token = getToken()
  const url = axios.defaults.baseURL + '/api/ai/file/download?fileName='
      + encodeURIComponent(fileName) + '&Authorization=' + encodeURIComponent(token)
  window.open(url, '_blank')
}

// ------------------------- 对话核心 -------------------------
onMounted(() => { loadProfile() })

const sendQuickCommand = (text) => {
  userInput.value = text
  sendInstruction()
}

const resetMemory = () => {
  ElMessageBox.confirm('确定清空本次对话记忆吗？', '确认', { type: 'warning' })
      .then(async () => {
        await doGet('/api/ai/reset', { memoryId: threadMemoryId.value }).catch(() => {})
        messages.value = []
        ElMessage.success('记忆已清空')
      }).catch(() => {})
}

/** 从流式文本中抽取结构化事件并转为事件卡片（事件标记可能被网络分片拆开，用缓冲合并） */
const EVENT_REGEX = /\[AI-EVENT:(PREMIUM_REQUIRED|FILE_READY):([^\]]*)\]/g

const extractEvents = (msg) => {
  // 1. 抽取已闭合的事件标记：从缓冲区移除并转成卡片（去重）
  EVENT_REGEX.lastIndex = 0
  let m
  while ((m = EVENT_REGEX.exec(msg.rawBuffer)) !== null) {
    const ev = { raw: m[0], type: m[1], key: m[2] }
    if (!msg.events.some(x => x.raw === ev.raw)) {
      if (ev.type === 'PREMIUM_REQUIRED') {
        const ab = abilityByKey(ev.key)
        msg.events.push({ raw: ev.raw, type: ev.type, abilityKey: ev.key,
          abilityName: ab?.name || ev.key, price: ab?.price ?? '-', done: ab?.purchased })
      } else if (ev.type === 'FILE_READY') {
        msg.events.push({ raw: ev.raw, type: ev.type, fileName: ev.key })
      }
    }
    msg.rawBuffer = msg.rawBuffer.replace(m[0], '')
  }

  // 2. 若末尾残留未闭合的 "[AI-EVENT" 片段：只显示其之前的正文，
  //    片段保留在 rawBuffer 中等待后续分片补齐（不能丢弃，否则文本缺失）
  const partial = msg.rawBuffer.lastIndexOf('[AI-EVENT')
  if (partial !== -1 && !msg.rawBuffer.slice(partial).includes(']')) {
    msg.content = msg.rawBuffer.slice(0, partial)
  } else {
    msg.content = msg.rawBuffer
  }
}

const sendInstruction = async () => {
  if (!userInput.value.trim() || loading.value) return
  const text = userInput.value.trim()
  userInput.value = ''
  loading.value = true

  messages.value.push({ role: 'user', content: text, time: new Date().toLocaleTimeString() })

  const aiMsg = {
    role: 'ai', content: '', rawBuffer: '', events: [],
    time: new Date().toLocaleTimeString(), streaming: true
  }
  messages.value.push(aiMsg)
  const idx = messages.value.length - 1

  // 组装参数：附件仅在有 fileId 时携带；权限不传（由服务端从登录态判定）
  const params = { message: text, memoryId: threadMemoryId.value }
  const sentAttachment = attachment.value
  if (sentAttachment) {
    params.attachmentFileId = sentAttachment.fileId
    attachment.value = null // 附件一次性消费
  }

  await nextTick()
  scrollToBottom()

  currentSse.value = createSseConnection(AI_STREAM_CHAT, {
    params,
    onMessage: (chunk) => {
      messages.value[idx].rawBuffer += chunk
      extractEvents(messages.value[idx])
      scrollToBottom()
    },
    onComplete: () => {
      currentSse.value = null
      // 收尾：残留未闭合的事件片段直接展示为正文（极端情况，流已终止不会再补齐）
      messages.value[idx].content = messages.value[idx].rawBuffer
      messages.value[idx].streaming = false
      loading.value = false
      scrollToBottom()
    },
    onError: () => {
      messages.value[idx].rawBuffer += '\n\n⚠️ 连接中断，请重试；若持续出现请检查后端大模型配置。'
      messages.value[idx].content = messages.value[idx].rawBuffer
      currentSse.value = null
      messages.value[idx].streaming = false
      loading.value = false
    }
  })
}

const scrollToBottom = () => {
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

const formatMessage = (text) => {
  if (!text) return '领航员正在处理…'
  let formatted = text
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) =>
          `<div class="cyber-code-block"><div class="code-header">${lang || 'CODE'}</div><pre><code>${code}</code></pre></div>`)
      .replace(/`([^`]+)`/g, '<code class="cyber-inline-code">$1</code>')
      .replace(/\n/g, '<br/>')
      .replace(/\*\*([\s\S]*?)\*\*/g, '<strong>$1</strong>')
  return formatted
}

onUnmounted(() => {
  if (currentSse.value) {
    currentSse.value.close()
    currentSse.value = null
  }
})
</script>

<style scoped>
.cyber-container {
  height: calc(100vh - 140px);
  padding: 10px;
  background:
    radial-gradient(circle at 20% 15%, rgba(33, 63, 40, 0.35) 0%, transparent 55%),
    linear-gradient(160deg, #0b1410 0%, #12241a 55%, #08100b 100%);
  color: #d7e5da;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

.full-height-row { height: 100%; }
.privilege-col, .chat-col { height: 100%; display: flex; flex-direction: column; }

.box-gradient-border {
  background: rgba(14, 25, 17, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 18px 45px rgba(1, 3, 2, 0.6), inset 0 1px 1px rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  transition: box-shadow 0.3s ease;
}
.box-gradient-border:hover {
  box-shadow: 0 18px 45px rgba(1, 3, 2, 0.6), inset 0 1px 1px rgba(255, 255, 255, 0.08),
    0 0 0 1px rgba(82, 183, 136, 0.25);
}

.cyber-deck-panel { padding: 20px; display: flex; flex-direction: column; height: 100%; overflow-y: auto; }
.deck-header { display: flex; align-items: center; gap: 15px; }
.deck-avatar-pulse {
  position: relative; width: 48px; height: 48px;
  background: radial-gradient(circle, #1c4028 0%, #0a1a10 100%);
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 24px; border: 2px solid rgba(82, 183, 136, 0.55);
}
.deck-main-title { font-size: 18px; font-weight: bold; color: #dff0e3; margin: 0; }
.deck-sub-title { font-size: 11px; color: #8faa94; margin: 2px 0 0 0; }

.cyber-divider { border-color: rgba(82, 183, 136, 0.18) !important; margin: 15px 0; }
.section-title { font-size: 14px; color: #9fd6ab; margin-top: 0; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.section-desc { font-size: 11px; color: #94a89a; line-height: 1.5; margin-bottom: 12px; }

.permission-nodes { display: flex; flex-direction: column; gap: 10px; }
.permission-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px; padding: 10px;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.permission-card:hover { background: rgba(255, 255, 255, 0.06); border-color: rgba(82, 183, 136, 0.5); }
.card-active { background: rgba(45, 106, 79, 0.14); border-color: rgba(82, 183, 136, 0.45); }
.card-premium { border-color: rgba(230, 180, 80, 0.4); }

.card-left { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.node-meta { display: flex; flex-direction: column; }
.node-title { font-size: 12px; font-weight: bold; color: #e3f0e6; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.node-key-badge {
  font-size: 9px; color: #9fd6ab; background: rgba(82, 183, 136, 0.12);
  padding: 1px 4px; border-radius: 3px; align-self: flex-start; margin-top: 2px;
}
.node-desc { font-size: 10px; color: #7f9484; line-height: 1.4; }
.node-action { margin-top: 6px; }
.premium-tag { margin-left: auto; }
.purchased-badge { font-size: 10px; color: #9fd6ab; }

.quick-commands { display: flex; flex-wrap: wrap; gap: 8px; }
.cyber-btn-mini {
  margin: 0 !important; background: rgba(255, 255, 255, 0.04) !important;
  border-color: rgba(82, 183, 136, 0.35) !important; color: #a9d6b4 !important;
  font-size: 11px !important; padding: 8px 10px !important; height: auto !important; text-align: left;
}
.cyber-btn-mini:hover { border-color: #52b788 !important; background: rgba(45, 106, 79, 0.2) !important; color: #fff !important; }

/* 输入框工具条：附件上传区（管理员专属，内联紧凑） */
.attach-zone { display: inline-flex; align-items: center; }

.attach-upload-btn {
  margin: 0 !important; font-size: 11px !important;
  border-color: rgba(82, 183, 136, 0.45) !important; color: #a9d6b4 !important;
  background: rgba(255, 255, 255, 0.04) !important;
}
.attach-upload-btn:hover {
  border-color: #52b788 !important; color: #fff !important;
  background: rgba(45, 106, 79, 0.25) !important;
}

.attach-zone .attachment-chip {
  max-width: 200px; padding: 4px 10px; border-radius: 7px;
  font-size: 11px; gap: 6px;
}
.attachment-chip {
  font-size: 12px; color: #d7e5da; background: rgba(45, 106, 79, 0.25);
  border: 1px dashed rgba(82, 183, 136, 0.5); border-radius: 8px; padding: 8px 10px;
  display: flex; justify-content: space-between; align-items: center;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.chip-remove { padding: 0 2px; }

.deck-footer { margin-top: auto; padding-top: 15px; display: flex; flex-direction: column; gap: 8px; }
.cyber-reset-btn {
  width: 100%; background: transparent !important;
  border: 1px dashed rgba(226, 157, 165, 0.5) !important; color: #e69da5 !important; font-weight: bold;
}
.cyber-reset-btn:hover { background: rgba(169, 32, 59, 0.1) !important; border-color: rgba(226, 157, 165, 0.8) !important; }

.cyber-chat-panel { flex: 1; display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.chat-header {
  padding: 12px 20px; border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 18, 12, 0.9); display: flex; align-items: center; justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.status-dot { width: 8px; height: 8px; background-color: #52b788; border-radius: 50%; box-shadow: 0 0 6px rgba(82, 183, 136, 0.8); }
.pulsing { animation: dot-pulse 1.5s infinite alternate; }
@keyframes dot-pulse { 0% { transform: scale(1); opacity: 0.5; } 100% { transform: scale(1.3); opacity: 1; } }
.terminal-title { font-size: 13px; font-weight: bold; letter-spacing: 1px; color: #dfeee2; }
.network-badge { font-size: 10px; color: #a9d6b4; background: rgba(45, 106, 79, 0.25); border: 1px solid rgba(82, 183, 136, 0.4); padding: 2px 8px; border-radius: 10px; }

.chat-body {
  flex: 1; padding: 20px; overflow-y: auto;
  background: radial-gradient(circle at 50% 20%, rgba(33, 63, 40, 0.25) 0%, transparent 60%), #0a120d;
  display: flex; flex-direction: column; gap: 20px;
}

.welcome-intro {
  margin: auto; max-width: 550px; text-align: center; padding: 30px;
  border: 1px dashed rgba(255, 255, 255, 0.12); background: rgba(14, 25, 17, 0.6); border-radius: 16px;
}
.welcome-logo { font-size: 44px; }
.welcome-title { color: #dff0e3; font-size: 20px; margin: 15px 0 10px 0; }
.welcome-desc { font-size: 12px; color: #9cb3a2; line-height: 1.8; }
.guide-box { margin-top: 20px; text-align: left; border-top: 1px solid rgba(82, 183, 136, 0.15); padding-top: 15px; }
.guide-box h5 { color: #a9d6b4; font-size: 12px; margin: 0 0 8px 0; }
.guide-box li { font-size: 11px; color: #94a89a; margin-bottom: 6px; line-height: 1.6; }

.message-wrapper { display: flex; gap: 15px; align-items: flex-start; animation: fade-in 0.3s ease forwards; }
.msg-user { flex-direction: row-reverse; }
.message-avatar {
  width: 36px; height: 36px; border-radius: 50%; background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12); display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0;
}
.message-bubble-wrapper { max-width: 80%; display: flex; flex-direction: column; }
.msg-user .message-bubble-wrapper { align-items: flex-end; }
.sender-info { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.sender-name { font-size: 11px; font-weight: bold; }
.msg-user .sender-name { color: #d9b98c; }
.msg-ai .sender-name { color: #a9d6b4; }
.sender-time { font-size: 9px; color: #6e8375; }
.message-bubble { padding: 12px 16px; border-radius: 12px; font-size: 13px; line-height: 1.8; }
.msg-user .message-bubble {
  background: linear-gradient(135deg, rgba(167, 131, 91, 0.16) 0%, rgba(114, 82, 46, 0.1) 100%);
  border: 1px solid rgba(167, 131, 91, 0.4); color: #f0e4d2; border-radius: 12px 2px 12px 12px;
}
.msg-ai .message-bubble {
  background: rgba(20, 34, 24, 0.9); border: 1px solid rgba(82, 183, 136, 0.35);
  color: #d7e5da; border-radius: 2px 12px 12px 12px;
}
.stream-pulsing { box-shadow: 0 0 8px rgba(82, 183, 136, 0.15); }
.cyber-typing-cursor { display: inline-block; font-weight: bold; color: #52b788; animation: cursor-blink 0.8s steps(2, start) infinite; }
@keyframes cursor-blink { to { visibility: hidden; } }

/* 结构化事件卡片 */
.event-card {
  margin-top: 10px; padding: 12px 14px; border-radius: 10px; font-size: 12px;
}
.event-PREMIUM_REQUIRED {
  background: linear-gradient(135deg, rgba(230, 180, 80, 0.12) 0%, rgba(120, 80, 20, 0.08) 100%);
  border: 1px solid rgba(230, 180, 80, 0.5);
}
.event-FILE_READY {
  background: rgba(45, 106, 79, 0.15);
  border: 1px solid rgba(82, 183, 136, 0.5);
}
.event-title { font-weight: bold; margin-bottom: 6px; color: #f0e6d0; }
.event-FILE_READY .event-title { color: #dff0e3; }
.event-desc { color: #b3a687; margin-bottom: 8px; }

.chat-footer { padding: 15px 20px; border-top: 1px solid rgba(255, 255, 255, 0.08); background: rgba(10, 18, 12, 0.95); }
.input-container {
  display: flex; flex-direction: column; background: rgba(14, 25, 17, 0.6);
  border: 1.5px solid rgba(255, 255, 255, 0.08); border-radius: 12px; padding: 8px; transition: all 0.3s ease;
}
.input-container:focus-within { border-color: #5c8b63; box-shadow: 0 4px 18px rgba(92, 139, 99, 0.2); }
.cyber-input :deep(.el-textarea__inner) {
  background: transparent !important; border: none !important; box-shadow: none !important;
  color: #e6f0e8 !important; font-family: inherit; font-size: 13px; padding: 5px;
}
.cyber-input :deep(.el-textarea__inner::placeholder) { color: #6e8675; }
.input-controls { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid rgba(255, 255, 255, 0.05); padding-top: 8px; margin-top: 5px; }
.shortcuts-tip { font-size: 10px; color: #5f7464; }
.cyber-send-btn {
  background: linear-gradient(135deg, #2d6a4f 0%, #1b4332 100%) !important;
  border-color: rgba(82, 183, 136, 0.5) !important; color: #ffffff !important; font-weight: bold;
}
.cyber-send-btn:hover { background: linear-gradient(135deg, #3a7d5e 0%, #2d6a4f 100%) !important; }

:deep(.cyber-code-block) { margin: 10px 0; background: #080f0a; border: 1px solid rgba(82, 183, 136, 0.25); border-radius: 8px; overflow: hidden; }
:deep(.code-header) { background: rgba(45, 106, 79, 0.22); padding: 5px 12px; font-size: 10px; color: #a9d6b4; border-bottom: 1px solid rgba(82, 183, 136, 0.15); text-transform: uppercase; }
:deep(.cyber-code-block pre) { margin: 0; padding: 10px 15px; overflow-x: auto; }
:deep(.cyber-code-block code) { font-family: 'Consolas', monospace; color: #b9ddb9; font-size: 12px; }
:deep(.cyber-inline-code) { background: rgba(45, 106, 79, 0.3); color: #aad5b2; padding: 2px 6px; border-radius: 4px; font-family: 'Consolas', monospace; }

@keyframes fade-in { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
