<!-- src/pages/case/list.vue -->
<template>
  <view class="search-container">
    <!-- 顶部状态栏安全区 -->
    <view class="header-safe-area"></view>

    <!-- ================= 顶部固定：搜索与筛选区域 ================= -->
    <view class="top-fixed-section">
      <!-- 搜索框行 -->
      <view class="search-bar-row">
        <!-- 左侧模式切换按钮 -->
        <view class="mode-toggle-btn" @tap="toggleSearchMode">
          <text class="toggle-icon">⇅</text>
          <text class="toggle-text">{{ searchMode === 'case' ? '案例' : '条文' }}</text>
        </view>
        
        <!-- 分割线 -->
        <view class="vertical-divider"></view>

        <!-- 输入框 -->
        <input 
          class="search-input" 
          v-model="keyword" 
          :placeholder="searchMode === 'case' ? '请输入你想查询案件的关键词...' : '请输入您想查询法律条文的关键词...'" 
          placeholder-class="placeholder-style"
          confirm-type="search"
          @confirm="handleSearch"
        />

        <!-- 右侧搜索按钮 (圆形带图标) -->
        <view class="search-submit-btn" @tap="handleSearch">
          <image class="search-icon" src="/static/icons/search-white.png" mode="aspectFit" />
        </view>
      </view>

      <!-- 筛选条件行 -->
      <view class="filter-row">
        <view class="filter-btn" @tap="openFilter('country')">
          <image class="filter-icon" src="/static/icons/earth.png" mode="aspectFit" />
          <text class="filter-text">{{ filters.country }}</text>
          <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
        </view>

        <view class="filter-btn" @tap="openFilter('source')">
          <image class="filter-icon" src="/static/icons/database.png" mode="aspectFit" />
          <text class="filter-text">{{ filters.source }}</text>
          <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
        </view>

        <!-- 仅在案例模式下显示判决时间 -->
        <view class="filter-btn" v-if="searchMode === 'case'" @tap="openFilter('time')">
          <image class="filter-icon" src="/static/icons/calendar.png" mode="aspectFit" />
          <text class="filter-text">{{ filters.time }}</text>
          <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
        </view>
      </view>
    </view>

    <!-- ================= 滚动内容：结果卡片列表 ================= -->
    <scroll-view scroll-y class="list-scroll">
      <view class="content-wrapper">
        
        <!-- 案例卡片列表 -->
        <template v-if="searchMode === 'case'">
          <view 
            class="result-card case-card" 
            v-for="item in caseList" 
            :key="item.id"
            @tap="goToDetail(item.id, 'case')"
          >
            <view class="card-header">
              <view class="header-left">
                <image class="flag-icon" :src="item.flag" mode="aspectFit" />
                <text class="title">{{ item.title }}</text>
              </view>
              <image 
                class="star-icon" 
                :src="item.isFavorite ? '/static/icons/star-filled.png' : '/static/icons/star-outline.png'" 
                mode="aspectFit" 
                @tap.stop="toggleFavorite(item)"
              />
            </view>
            
            <view class="sub-info-row">
              <text class="sub-text">{{ item.court }}</text>
              <text class="sub-text">{{ item.caseNo }}</text>
              <text class="sub-text">{{ item.date }}</text>
            </view>

            <view class="divider-line"></view>

            <view class="summary-box">
              <text class="summary-text">简要介绍：{{ item.summary }}</text>
            </view>

            <view class="card-bottom">
              <image class="nav-arrow" src="/static/icons/arrow-right-blue.png" mode="aspectFit" />
            </view>
          </view>
        </template>

        <!-- 条文卡片列表 -->
        <template v-else>
          <view 
            class="result-card article-card" 
            v-for="item in articleList" 
            :key="item.id"
            @tap="goToDetail(item.id, 'article')"
          >
            <view class="card-header">
              <view class="header-left">
                <image class="flag-icon" :src="item.flag" mode="aspectFit" />
                <text class="title">{{ item.title }}</text>
              </view>
            </view>
            
            <view class="sub-info-row">
              <text class="sub-text">生效日期：{{ item.effectiveDate }}</text>
              <text class="sub-text">最新修订：{{ item.revisedDate }}</text>
            </view>

            <view class="divider-line"></view>

            <view class="summary-box">
              <text class="summary-text">简要介绍：{{ item.summary }}</text>
            </view>

            <view class="card-bottom">
              <image class="nav-arrow" src="/static/icons/arrow-right-blue.png" mode="aspectFit" />
            </view>
          </view>
        </template>

        <!-- 空状态 -->
        <view class="empty-state" v-if="(searchMode === 'case' && caseList.length === 0) || (searchMode === 'article' && articleList.length === 0)">
          <text class="empty-text">暂无相关数据</text>
        </view>

      </view>
    </scroll-view>

    <!-- ================= 底部导航栏 ================= -->
    <BottomTabBar activeTab="search" />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import BottomTabBar from '../../components/BottomTabBar.vue' // 确保使用正确的相对路径

// ================= 状态数据 =================
// 搜索模式: 'case' (案例) | 'article' (条文)
const searchMode = ref<'case' | 'article'>('case')
const keyword = ref('')

// 筛选条件状态
const filters = reactive({
  country: '国家',
  source: '数据源',
  time: '判决时间'
})

// ================= 模拟数据 =================
const caseList = ref([
  {
    id: 1,
    flag: '/static/icons/flag-us.png',
    title: '跨境合同纠纷案件',
    court: 'XX法院',
    caseNo: '案号XXXXX',
    date: '20XX/XX/XX',
    summary: '本案涉及跨国贸易中的管辖权争议与合同违约责任认定，核心焦点在于国际贸易术语解释通则的适用...',
    isFavorite: true
  },
  {
    id: 2,
    flag: '/static/icons/flag-us.png',
    title: '跨境合同纠纷案件',
    court: 'XX法院',
    caseNo: '案号XXXXX',
    date: '20XX/XX/XX',
    summary: '针对跨国知识产权技术转让合同的效力纠纷，法院驳回了原告的诉讼请求...',
    isFavorite: false
  },
  {
    id: 3,
    flag: '/static/icons/flag-us.png',
    title: '跨境合同纠纷案件',
    court: 'XX法院',
    caseNo: '案号XXXXX',
    date: '20XX/XX/XX',
    summary: '双方在跨国并购中的对赌协议触发纠纷，仲裁条款的独立性被重新审查...',
    isFavorite: false
  }
])

const articleList = ref([
  {
    id: 101,
    flag: '/static/icons/flag-us.png',
    title: '《XX法典》第五十一条',
    effectiveDate: '19XX/XX/XX',
    revisedDate: '20XX/XX/XX',
    summary: '本条款规定了在涉外民商事案件中，如何确认涉外合同的准据法以及最密切联系原则的适用标准...'
  },
  {
    id: 102,
    flag: '/static/icons/flag-us.png',
    title: '《XX法典》第五十一条',
    effectiveDate: '19XX/XX/XX',
    revisedDate: '20XX/XX/XX',
    summary: '明确了外国法院判决的承认与执行程序，以及拒绝承认的法定例外情形...'
  },
  {
    id: 103,
    flag: '/static/icons/flag-us.png',
    title: '《XX法典》第五十一条',
    effectiveDate: '19XX/XX/XX',
    revisedDate: '20XX/XX/XX',
    summary: '对于涉外知识产权侵权的惩罚性赔偿基数计算方法进行了详细补充...'
  }
])

// ================= 生命周期 =================
onLoad((query: any) => {
  if (query && query.keyword) {
    keyword.value = decodeURIComponent(query.keyword)
    handleSearch()
  }
})

// ================= 交互方法 =================

// 切换搜索模式 (案例 / 条文)
const toggleSearchMode = () => {
  searchMode.value = searchMode.value === 'case' ? 'article' : 'case'
  // 重置时间筛选（条文不需要判决时间）
  filters.time = '判决时间'
  // 可以在这里触发重新请求列表接口的逻辑
}

// 触发搜索
const handleSearch = () => {
  console.log(`执行搜索, 模式:${searchMode.value}, 关键词:${keyword.value}, 筛选:`, filters)
  uni.showToast({ title: '加载中...', icon: 'loading' })
  // 这里接入实际的网络请求 caseApi.list(...)
}

// 打开底部菜单筛选
const openFilter = (type: 'country' | 'source' | 'time') => {
  let itemList: string[] = []
  
  if (type === 'country') itemList = ['全部国家', '美国', '欧盟', '日本']
  if (type === 'source') itemList = ['全部数据源', '官方法院库', '学术期刊', '第三方平台']
  if (type === 'time') itemList = ['判决时间', '近一个月', '近半年', '近一年']

  uni.showActionSheet({
    itemList,
    success: (res) => {
      filters[type] = itemList[res.tapIndex]
      handleSearch() // 选择后自动触发刷新
    }
  })
}

// 点击星星收藏/取消收藏 (仅案例)
const toggleFavorite = (item: any) => {
  item.isFavorite = !item.isFavorite
  uni.showToast({ 
    title: item.isFavorite ? '收藏成功' : '已取消收藏', 
    icon: 'none' 
  })
}

// 跳转详情页
const goToDetail = (id: number, mode: 'case' | 'article') => {
  const url = mode === 'case' 
    ? `/pages/case/detail?id=${id}` 
    : `/pages/study/detail?id=${id}` // 假设条文详情在 study 目录下
  uni.navigateTo({ url })
}
</script>

<style scoped lang="scss">
/* ================= 整体页面样式 ================= */
.search-container {
  height: 100vh;
  background-color: #F5F7FA;
  display: flex;
  flex-direction: column;
}

.header-safe-area {
  padding-top: var(--status-bar-height, 44px);
  background-color: #F5F7FA;
}

/* ================= 顶部固定：搜索与筛选区域 ================= */
.top-fixed-section {
  background-color: #ffffff;
  border-radius: 0 0 32rpx 32rpx;
  padding: 20rpx 32rpx 30rpx 32rpx;
  box-shadow: 0 8rpx 20rpx rgba(33, 140, 255, 0.06);
  z-index: 10;
}

/* 搜索框行 */
.search-bar-row {
  display: flex;
  align-items: center;
  background-color: #F5F7FA;
  border-radius: 40rpx;
  height: 80rpx;
  padding: 0 10rpx 0 30rpx;
  margin-bottom: 24rpx;
  border: 2rpx solid #EBF4FF;
}

.mode-toggle-btn {
  display: flex;
  align-items: center;
  padding-right: 16rpx;
  
  .toggle-icon {
    color: #218CFF;
    font-size: 28rpx;
    margin-right: 6rpx;
  }
  .toggle-text {
    color: #218CFF;
    font-size: 28rpx;
    font-weight: 500;
  }
}

.vertical-divider {
  width: 2rpx;
  height: 30rpx;
  background-color: #DCDFE6;
  margin-right: 20rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #333333;
}

.placeholder-style {
  color: #B0B4C1;
  font-size: 26rpx;
}

.search-submit-btn {
  width: 64rpx;
  height: 64rpx;
  background-color: #218CFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &:active {
    opacity: 0.8;
  }

  .search-icon {
    width: 32rpx;
    height: 32rpx;
  }
}

/* 筛选条件行 */
.filter-row {
  display: flex;
  justify-content: flex-start;
  gap: 20rpx;
}

.filter-btn {
  display: flex;
  align-items: center;
  background-color: #F5F7FA;
  padding: 12rpx 20rpx;
  border-radius: 12rpx;
  
  &:active {
    background-color: #EBF4FF;
  }

  .filter-icon {
    width: 26rpx;
    height: 26rpx;
    margin-right: 8rpx;
  }

  .filter-text {
    font-size: 24rpx;
    color: #333333;
    margin-right: 6rpx;
  }

  .arrow-icon {
    width: 20rpx;
    height: 20rpx;
  }
}

/* ================= 滚动内容：结果卡片列表 ================= */
.list-scroll {
  flex: 1;
  height: 0;
  
  .content-wrapper {
    padding: 30rpx 24rpx 140rpx 24rpx; /* 底部留白给 BottomTabBar */
  }
}

/* 通用卡片样式 */
.result-card {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
  position: relative;
  
  &:active {
    background-color: #FAFAFA;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
  padding-right: 20rpx;
  
  .flag-icon {
    width: 36rpx;
    height: 36rpx;
    border-radius: 50%;
    margin-right: 16rpx;
  }
  
  .title {
    font-size: 32rpx;
    font-weight: 600;
    color: #333333;
    line-height: 1.4;
  }
}

.star-icon {
  width: 40rpx;
  height: 40rpx;
}

.sub-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  
  .sub-text {
    font-size: 24rpx;
    color: #999999;
  }
}

.divider-line {
  height: 1rpx;
  background-color: #E4E7ED;
  margin-bottom: 20rpx;
}

.summary-box {
  margin-bottom: 20rpx;
  
  .summary-text {
    font-size: 26rpx;
    color: #666666;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3; /* 最多显示3行 */
    overflow: hidden;
  }
}

.card-bottom {
  display: flex;
  justify-content: flex-end;
  
  .nav-arrow {
    width: 32rpx;
    height: 32rpx;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}
</style>