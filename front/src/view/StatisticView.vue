<template>
  <div class="statistic-dashboard">
    <section class="dashboard-hero">
      <div class="dashboard-hero__content">
        <span class="dashboard-hero__eyebrow">JOURNEY · SALES COMMAND CENTER</span>
        <h2>销售旅程总览</h2>
        <p>洞察客户转化轨迹，让每一次业务决策都有数据可循</p>
      </div>
      <div class="dashboard-hero__status">
        <span class="dashboard-hero__status-dot"></span>
        数据驾驶舱运行中
      </div>
    </section>

    <!-- 概览统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card--activity" v-for="(card, index) in statCards" :key="index">
        <div class="stat-card__icon">
          <el-icon :size="28"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__label">{{ card.label }}</div>
          <div class="stat-card__value">
            <span class="stat-card__main">{{ card.mainValue }}</span>
            <span v-if="card.suffix !== null && card.suffix !== undefined" class="stat-card__suffix">/ {{ card.suffix }}</span>
          </div>
        </div>
        <div class="stat-card__glow"></div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-area">
      <div class="chart-card">
        <div class="chart-card__header">
          <el-icon :size="18"><TrendCharts /></el-icon>
          <span>销售漏斗图</span>
        </div>
        <div class="chart-card__body" ref="funnelChartRef"></div>
      </div>

      <div class="chart-card">
        <div class="chart-card__header">
          <el-icon :size="18"><PieChart /></el-icon>
          <span>线索来源统计</span>
        </div>
        <div class="chart-card__body" ref="pieChartRef"></div>
      </div>
    </div>

    <!-- 月度趋势曲线图 -->
    <div class="chart-card chart-card--trend">
      <div class="chart-card__header">
        <el-icon :size="18"><TrendCharts /></el-icon>
        <span>月度趋势</span>
      </div>
      <div class="chart-card__body" ref="trendChartRef"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { doGet } from "../http/httpRequest.js"
import * as echarts from 'echarts'
import { TrendCharts, PieChart, ShoppingBag, Phone, User, Money } from '@element-plus/icons-vue'

// 响应式数据
const summaryData = ref({})

// ECharts 实例
const funnelChartRef = ref(null)
const pieChartRef = ref(null)
const trendChartRef = ref(null)
let funnelChart = null
let pieChart = null
let trendChart = null
let resizeObserver = null

// 金额格式化
const formatMoney = (val) => {
  if (val == null) return '--'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 统计卡片配置
const statCards = computed(() => [
  {
    label: '市场活动',
    mainValue: summaryData.value.effectiveActivityCount ?? '--',
    suffix: summaryData.value.totalActivityCount,
    icon: ShoppingBag,
  },
  {
    label: '线索总数',
    mainValue: summaryData.value.totalClueCount ?? '--',
    suffix: null,
    icon: Phone,
  },
  {
    label: '客户总数',
    mainValue: summaryData.value.totalCustomerCount ?? '--',
    suffix: null,
    icon: User,
  },
  {
    label: '交易总额',
    mainValue: summaryData.value.successTranAmount != null ? formatMoney(summaryData.value.successTranAmount) : '--',
    suffix: summaryData.value.totalTranAmount != null ? formatMoney(summaryData.value.totalTranAmount) : null,
    icon: Money,
  },
])

// 加载概览统计数据
const loadSummary = () => {
  doGet("/api/summary/data", {}).then(resp => {
    if (resp.data.code === 200) {
      summaryData.value = resp.data.data
    }
  })
}

// 销售漏斗图
const loadSaleFunnelChart = () => {
  doGet("/api/saleFunnel/data", {}).then(resp => {
    if (resp.data.code === 200 && funnelChartRef.value) {
      if (funnelChart) funnelChart.dispose()
      funnelChart = echarts.init(funnelChartRef.value)

      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          borderColor: '#dce9e0',
          textStyle: { color: '#294434' },
        },
        series: [{
          name: '销售漏斗',
          type: 'funnel',
          left: '10%',
          right: '10%',
          top: 20,
          bottom: 20,
          width: '80%',
          sort: 'none', // 改为 none 锁死业务流程阶段顺序（线索->客户->交易->成交），不按值大小打乱
          gap: 3,
          label: {
            show: true,
            position: 'inside',
            formatter: '{b}\n{c}',
            fontSize: 13,
            color: '#fff',
          },
          itemStyle: {
            borderWidth: 0,
            borderRadius: 4,
          },
          color: ['#4b7551', '#5c8b63', '#7ba082', '#addbb4'],
          data: resp.data.data || [], // 防空容错
        }]
      }

      funnelChart.setOption(option)
    }
  })
}

// 线索来源饼图
const loadSourcePieChart = () => {
  doGet("/api/sourcePie/data", {}).then(resp => {
    if (resp.data.code === 200 && pieChartRef.value) {
      if (pieChart) pieChart.dispose()
      pieChart = echarts.init(pieChartRef.value)

      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          borderColor: '#dce9e0',
          textStyle: { color: '#294434' },
        },
        legend: {
          bottom: 0,
          textStyle: { color: '#667d6d', fontSize: 12 },
        },
        color: ['#3f6848', '#5c8b63', '#7ba082', '#92b196', '#addbb4', '#d5b381'],
        series: [{
          name: '线索来源',
          type: 'pie',
          radius: ['50%', '78%'],
          center: ['50%', '46%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderColor: '#ffffff',
            borderWidth: 2,
            borderRadius: 4,
          },
          label: { show: false },
          emphasis: {
            label: {
              show: true,
              fontSize: 16,
              fontWeight: 'bold',
            },
            scaleSize: 8,
          },
          data: resp.data.data || [], // 防空容错
        }]
      }

      pieChart.setOption(option)
    }
  })
}

// 月度趋势曲线图
const loadTrendChart = () => {
  doGet("/api/trend/data", {}).then(resp => {
    if (resp.data.code === 200 && trendChartRef.value) {
      if (trendChart) trendChart.dispose()
      trendChart = echarts.init(trendChartRef.value)

      const trendData = resp.data.data || {}
      const monthList = trendData.monthList || []
      const clueNumList = trendData.clueNumList || []
      const customerNumList = trendData.customerNumList || []
      const tranAmountList = trendData.tranAmountList || []

      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          borderColor: '#dce9e0',
          textStyle: { color: '#294434' },
        },
        legend: {
          data: ['线索数', '客户数', '交易额'],
          bottom: 0,
          textStyle: { color: '#667d6d', fontSize: 12 },
        },
        grid: {
          left: '3%',
          right: '3%',
          bottom: '12%',
          top: 48,
          containLabel: true,
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: monthList,
          axisLine: { lineStyle: { color: '#dce7df' } },
          axisTick: { lineStyle: { color: '#dce7df' } },
          axisLabel: { color: '#718277', fontSize: 11 },
        },
        yAxis: [
          {
            type: 'value',
            name: '数量',
            nameGap: 14,
            nameTextStyle: { color: '#718277', fontSize: 12 },
            axisLine: { show: false },
            axisLabel: { color: '#718277', fontSize: 11 },
            splitLine: { lineStyle: { color: '#edf2ee' } },
          },
          {
            type: 'value',
            name: '金额 (¥)',
            nameGap: 14,
            nameTextStyle: { color: '#718277', fontSize: 12 },
            axisLine: { show: false },
            axisLabel: {
              color: '#718277',
              fontSize: 11,
              formatter: value => Number(value).toLocaleString('zh-CN'),
            },
            splitLine: { show: false },
          },
        ],
        series: [
          {
            name: '线索数',
            type: 'line',
            smooth: true,
            data: clueNumList,
            itemStyle: { color: '#addbb4' },
            areaStyle: { color: 'rgba(173, 219, 180, 0.08)' },
            lineStyle: { width: 2 },
            symbol: 'circle',
            symbolSize: 6,
          },
          {
            name: '客户数',
            type: 'line',
            smooth: true,
            data: customerNumList,
            itemStyle: { color: '#76a9c8' },
            lineStyle: { width: 2 },
            symbol: 'circle',
            symbolSize: 6,
          },
          {
            name: '交易额',
            type: 'line',
            smooth: true,
            yAxisIndex: 1,
            data: tranAmountList,
            itemStyle: { color: '#d5b381' },
            lineStyle: { width: 2 },
            symbol: 'circle',
            symbolSize: 6,
          },
        ],
      }

      trendChart.setOption(option)
    }
  })
}

// 响应式处理
const handleResize = () => {
  funnelChart?.resize()
  pieChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  loadSummary()
  loadSaleFunnelChart()
  loadSourcePieChart()
  loadTrendChart()
  
  // 完美防抖、防变形：使用 ResizeObserver 实时感知导航折叠/局部布局挤压，重绘容器坐标
  if (window.ResizeObserver) {
    resizeObserver = new ResizeObserver(() => {
      handleResize()
    })
    const container = document.querySelector('.statistic-dashboard')
    if (container) {
      resizeObserver.observe(container)
    }
  } else {
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  } else {
    window.removeEventListener('resize', handleResize)
  }
  funnelChart?.dispose()
  pieChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.statistic-dashboard {
  --forest-border: #dce8df;
  --forest-text: #294434;
  --forest-muted: #708277;
  --forest-accent: #5f936b;
  --forest-soft: #edf5ef;
  --champagne: #ae8255;
  position: relative;
  min-height: 100%;
  max-width: 1480px;
  margin: 0 auto;
  padding: 4px 0 8px;
  box-sizing: border-box;
  background-image:
    radial-gradient(circle at 4% 2%, rgba(95, 147, 107, 0.08), transparent 23%),
    radial-gradient(circle at 96% 0%, rgba(174, 130, 85, 0.06), transparent 20%);
}

.dashboard-hero {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  margin-bottom: 22px;
  padding: 10px 4px 20px;
  border-bottom: 1px solid var(--forest-border);
}

.dashboard-hero__eyebrow {
  display: block;
  margin-bottom: 8px;
  color: #8d6b48;
  font-family: Georgia, serif;
  font-size: 10px;
  letter-spacing: 3px;
}

.dashboard-hero h2 {
  margin: 0;
  color: var(--forest-text);
  font-family: Georgia, 'Microsoft YaHei', serif;
  font-size: clamp(22px, 2.5vw, 31px);
  letter-spacing: 3px;
}

.dashboard-hero p {
  margin: 8px 0 0;
  color: var(--forest-muted);
  font-size: 13px;
  letter-spacing: 0.6px;
}

.dashboard-hero__status {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 9px;
  padding: 9px 14px;
  border: 1px solid #d7e7db;
  border-radius: 99px;
  background: rgba(245, 250, 246, 0.9);
  color: #617669;
  font-size: 12px;
  box-shadow: 0 5px 16px rgba(48, 85, 58, 0.05);
}

.dashboard-hero__status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #69a675;
  box-shadow: 0 0 0 4px rgba(105, 166, 117, 0.12);
}

/* ===== 统计卡片 ===== */
.stat-cards {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 20px;
  border: 1px solid rgba(210, 225, 215, 0.9);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 24px rgba(36, 75, 47, 0.06), inset 0 1px rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: #bcd6c2;
  box-shadow: 0 15px 32px rgba(36, 75, 47, 0.11);
}

.stat-card__glow {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  opacity: 0.12;
  pointer-events: none;
}

.stat-card--activity .stat-card__glow {
  background: #5c8b63;
}

.stat-card:nth-child(2) .stat-card__glow {
  background: #76a9c8;
}

.stat-card:nth-child(3) .stat-card__glow {
  background: #d5b381;
}

.stat-card:nth-child(4) .stat-card__glow {
  background: #a7835b;
}

.stat-card__icon {
  width: 52px;
  height: 52px;
  border: 1px solid rgba(95, 147, 107, 0.1);
  border-radius: 15px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card--activity .stat-card__icon {
  background: linear-gradient(135deg, #edf6ef, #dcecdf);
  color: #4f865b;
}

.stat-card:nth-child(2) .stat-card__icon {
  background: linear-gradient(135deg, #edf6fa, #dbeaf1);
  color: #4c819d;
}

.stat-card:nth-child(3) .stat-card__icon {
  background: linear-gradient(135deg, #faf5ec, #eee2d1);
  color: #a47a4f;
}

.stat-card:nth-child(4) .stat-card__icon {
  background: linear-gradient(135deg, #f6f1e9, #e9ddcc);
  color: #987047;
}

.stat-card__body {
  flex: 1;
  min-width: 0;
}

.stat-card__label {
  font-size: 13px;
  color: var(--forest-muted);
  margin-bottom: 6px;
  letter-spacing: 0.3px;
}

.stat-card__value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  min-width: 0;
  max-width: 100%;
}

.stat-card__main {
  min-width: 0;
  font-size: clamp(20px, 2vw, 26px);
  font-weight: 700;
  color: var(--forest-text);
  font-family: Georgia, 'Microsoft YaHei', serif;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.stat-card__suffix {
  min-width: 0;
  font-size: 13px;
  color: #8b9c91;
  overflow-wrap: anywhere;
}

/* 金额文本通常最长，空间不足时允许总额整体换到下一行，避免撑破卡片 */
.stat-card:nth-child(4) .stat-card__value {
  flex-wrap: wrap;
  row-gap: 2px;
}

/* ===== 图表区域 ===== */
.charts-area {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card--trend {
  position: relative;
  z-index: 1;
  margin-bottom: 0;
}

.chart-card {
  border: 1px solid rgba(210, 225, 215, 0.9);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 28px rgba(36, 75, 47, 0.065);
  overflow: hidden;
  transition: box-shadow 0.25s ease;
}

.chart-card:hover {
  border-color: #bed7c4;
  box-shadow: 0 16px 34px rgba(36, 75, 47, 0.11);
}

.chart-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 22px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--forest-text);
  letter-spacing: 1px;
}

.chart-card__header .el-icon {
  color: var(--forest-accent);
}

.chart-card__body {
  width: 100%;
  height: 370px;
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-area {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .statistic-dashboard {
    padding: 0 0 8px;
  }

  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-hero__status {
    align-self: flex-start;
  }

  .stat-cards {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: 20px 16px;
  }

  .stat-card__main {
    font-size: 22px;
  }

  .chart-card__header {
    padding-right: 16px;
    padding-left: 16px;
  }
}
</style>
