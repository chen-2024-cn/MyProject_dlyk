<template>
  <div class="statistic-dashboard">
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
            <span v-if="card.suffix" class="stat-card__suffix">/ {{ card.suffix }}</span>
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
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}'
        },
        series: [{
          name: '销售漏斗',
          type: 'funnel',
          left: '10%',
          right: '10%',
          top: 20,
          bottom: 20,
          width: '80%',
          sort: 'descending',
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
          color: ['#4caf50', '#66bb6a', '#81c784', '#a5d6a7'],
          data: resp.data.data,
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
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          bottom: 0,
          textStyle: { color: '#666', fontSize: 12 },
        },
        color: ['#43a047', '#66bb6a', '#a5d6a7', '#c8e6c9', '#e8f5e9', '#81c784'],
        series: [{
          name: '线索来源',
          type: 'pie',
          radius: ['50%', '78%'],
          center: ['50%', '46%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderColor: '#fff',
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
          data: resp.data.data,
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

      const { monthList, clueNumList, customerNumList, tranAmountList } = resp.data.data

      const option = {
        tooltip: {
          trigger: 'axis',
        },
        legend: {
          data: ['线索数', '客户数', '交易额'],
          bottom: 0,
          textStyle: { color: '#666', fontSize: 12 },
        },
        grid: {
          left: '3%',
          right: '3%',
          bottom: '12%',
          top: 20,
          containLabel: true,
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: monthList,
          axisLabel: { color: '#666', fontSize: 11 },
        },
        yAxis: [
          {
            type: 'value',
            name: '数量',
            nameTextStyle: { color: '#999', fontSize: 12 },
            axisLabel: { color: '#666', fontSize: 11 },
            splitLine: { lineStyle: { color: '#f0f0f0' } },
          },
          {
            type: 'value',
            name: '金额 (¥)',
            nameTextStyle: { color: '#999', fontSize: 12 },
            axisLabel: { color: '#666', fontSize: 11 },
            splitLine: { show: false },
          },
        ],
        series: [
          {
            name: '线索数',
            type: 'line',
            smooth: true,
            data: clueNumList,
            itemStyle: { color: '#43a047' },
            lineStyle: { width: 2 },
            symbol: 'circle',
            symbolSize: 6,
          },
          {
            name: '客户数',
            type: 'line',
            smooth: true,
            data: customerNumList,
            itemStyle: { color: '#2196f3' },
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
            itemStyle: { color: '#e91e63' },
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
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  funnelChart?.dispose()
  pieChart?.dispose()
  trendChart?.dispose()
})
</script>

<style scoped>
.statistic-dashboard {
  padding: 4px 0;
}

/* ===== 统计卡片 ===== */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 20px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  cursor: default;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.1);
}

.stat-card__glow {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  opacity: 0.08;
  pointer-events: none;
}

.stat-card--activity .stat-card__glow {
  background: #4caf50;
}

.stat-card:nth-child(2) .stat-card__glow {
  background: #2196f3;
}

.stat-card:nth-child(3) .stat-card__glow {
  background: #ff9800;
}

.stat-card:nth-child(4) .stat-card__glow {
  background: #e91e63;
}

.stat-card__icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card--activity .stat-card__icon {
  background: linear-gradient(135deg, #e8f5e9, #c8e6c9);
  color: #2e7d32;
}

.stat-card:nth-child(2) .stat-card__icon {
  background: linear-gradient(135deg, #e3f2fd, #bbdefb);
  color: #1565c0;
}

.stat-card:nth-child(3) .stat-card__icon {
  background: linear-gradient(135deg, #fff3e0, #ffe0b2);
  color: #e65100;
}

.stat-card:nth-child(4) .stat-card__icon {
  background: linear-gradient(135deg, #fce4ec, #f8bbd0);
  color: #c2185b;
}

.stat-card__body {
  flex: 1;
  min-width: 0;
}

.stat-card__label {
  font-size: 13px;
  color: #999;
  margin-bottom: 6px;
  letter-spacing: 0.3px;
}

.stat-card__value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-card__main {
  font-size: 26px;
  font-weight: 700;
  color: #333;
  line-height: 1.2;
}

.stat-card__suffix {
  font-size: 13px;
  color: #bbb;
}

/* ===== 图表区域 ===== */
.charts-area {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card--trend {
  margin-bottom: 0;
}

.chart-card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: box-shadow 0.25s ease;
}

.chart-card:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.1);
}

.chart-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 22px 0;
  font-size: 15px;
  font-weight: 600;
  color: #444;
}

.chart-card__header .el-icon {
  color: #4caf50;
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
  .stat-cards {
    grid-template-columns: 1fr;
  }
}
</style>
