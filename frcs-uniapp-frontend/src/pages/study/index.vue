<template>
  <view class="study-container">
    <view class="country-tabs">
      <view 
        v-for="country in countries" 
        :key="country"
        class="country-tab"
        :class="{ 'active-country': activeCountry === country }"
        @tap="handleCountryChange(country)"
      >
        {{ country }}
        <view class="active-line" v-if="activeCountry === country"></view>
      </view>
    </view>

    <scroll-view 
      scroll-x 
      class="category-scroll" 
      :scroll-into-view="'cat-' + activeCategoryIndex"
      scroll-with-animation
    >
      <view class="category-tabs">
        <view 
          v-for="(cat, index) in categories" 
          :key="index"
          :id="'cat-' + index"
          class="category-tab"
          :class="{ 'active-category': activeCategoryIndex === index }"
          @tap="handleCategoryClick(index)"
        >
          {{ cat }}
        </view>
      </view>
    </scroll-view>

    <swiper 
      class="content-swiper" 
      :current="activeCategoryIndex" 
      @change="handleSwiperChange"
      duration="300"
    >
      <swiper-item v-for="(cat, index) in categories" :key="index">
        <scroll-view 
          scroll-y 
          class="law-list-scroll"
          @scrolltolower="loadMoreData"
        >
          <view class="list-wrapper">
            <view v-if="getCacheState(activeCountry, cat).list.length === 0 && getCacheState(activeCountry, cat).loading" class="loading-state">
              加载中...
            </view>

            <view 
              class="law-card" 
              v-for="law in getCacheState(activeCountry, cat).list" 
              :key="law.id"
              @tap="handleLawClick(law)"
            >
              <view class="card-left">
                <image class="law-icon" src="/static/icons/tab-book.png" mode="aspectFit" />
                <view class="law-info">
                  <text class="law-title">{{ law.title }}</text>
                  <view class="law-tags">
                    <text class="tag country-tag">{{ law.country }}</text>
                    <text class="tag category-tag">{{ law.category }}</text>
                    <text class="status-text">{{ law.status }}</text>
                  </view>
                </view>
              </view>
              <image class="arrow-icon" src="/static/icons/arrow-right.png" mode="aspectFit" />
            </view>

            <view class="load-more-text" v-if="getCacheState(activeCountry, cat).list.length > 0">
              <text v-if="getCacheState(activeCountry, cat).loading">正在加载更多...</text>
              <text v-else-if="!getCacheState(activeCountry, cat).hasMore">- 到底了 -</text>
            </view>
          </view>
        </scroll-view>
      </swiper-item>
    </swiper>

    <BottomTabBar activeTab="study" />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import BottomTabBar from '@/components/BottomTabBar.vue';
import { fetchLawsMock, type LawItem } from '../../api/mockLaw';
import { onShow } from '@dcloudio/uni-app'

onShow(() => {
  uni.hideTabBar({
    animation: false // 瞬间隐藏，不要动画，避免闪烁
  })
})

// ================= 数据定义 =================
const countries = ['全部国家', '美国', '欧盟', '日本'];
const categories = ['全部', '宪法', '民商法', '刑法', '行政法', '经济法', '诉讼法', '国际法'];

const activeCountry = ref(countries[0]);
const activeCategoryIndex = ref(0);

// ================= 局部缓存管理 =================
// 结构: cache['国家_分类'] = { list: [], page: 1, hasMore: true, loading: false }
type CacheState = {
  list: LawItem[];
  page: number;
  hasMore: boolean;
  loading: boolean;
};

const dataCache = reactive<Record<string, CacheState>>({});

// 生成缓存 Key
const getCacheKey = (country: string, category: string) => `${country}_${category}`;

// 获取某个类别下的状态（如果不存在则初始化空状态）
const getCacheState = (country: string, category: string): CacheState => {
  const key = getCacheKey(country, category);
  if (!dataCache[key]) {
    dataCache[key] = { list: [], page: 1, hasMore: true, loading: false };
  }
  return dataCache[key];
};

// ================= 数据加载逻辑 =================
const fetchListData = async (isLoadMore = false) => {
  const currentCategory = categories[activeCategoryIndex.value];
  const state = getCacheState(activeCountry.value, currentCategory);

  // 如果正在加载，或者非下拉触底但没有更多数据时，阻止重复请求
  if (state.loading || (isLoadMore && !state.hasMore)) return;

  state.loading = true;
  try {
    const res = await fetchLawsMock(activeCountry.value, currentCategory, state.page);
    
    if (isLoadMore) {
      state.list.push(...res.data);
    } else {
      state.list = res.data;
    }
    
    state.hasMore = res.hasMore;
    if (state.hasMore) {
      state.page++;
    }
  } catch (error) {
    console.error('获取法典数据失败:', error);
    uni.showToast({ title: '加载失败', icon: 'none' });
  } finally {
    state.loading = false;
  }
};

// 触底加载更多
const loadMoreData = () => {
  fetchListData(true);
};

// ================= 交互逻辑 =================
// 点击国家：切换国家，保留类别索引，尝试读取或加载该组合下的数据
const handleCountryChange = (country: string) => {
  if (activeCountry.value === country) return;
  activeCountry.value = country;
  
  const currentCategory = categories[activeCategoryIndex.value];
  const state = getCacheState(country, currentCategory);
  // 如果该组合在缓存中没有数据，则触发加载
  if (state.list.length === 0) {
    fetchListData();
  }
};

// 点击分类：仅改变索引，由 Swiper 联动触发数据拉取
const handleCategoryClick = (index: number) => {
  activeCategoryIndex.value = index;
};

// Swiper 左右滑动触发：同步更新类别索引，尝试读取或加载数据
const handleSwiperChange = (e: any) => {
  const index = e.detail.current;
  activeCategoryIndex.value = index;
  
  const currentCategory = categories[index];
  const state = getCacheState(activeCountry.value, currentCategory);
  if (state.list.length === 0) {
    fetchListData();
  }
};

// 点击法典卡片
const handleLawClick = (law: LawItem) => {
  uni.showToast({
    title: '正在开发中',
    icon: 'none'
  });
};

// ================= 初始化 =================
onMounted(() => {
  fetchListData(); // 首次进入页面加载初始数据
});
</script>

<style lang="scss" scoped>
/* 整体布局 */
.study-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #F5F7FA;
  padding-bottom: env(safe-area-inset-bottom);
}

/* 顶部标题区 */
.header-safe-area {
  padding-top: var(--status-bar-height, 44px);
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  
  .page-title {
    font-size: 16px;
    font-weight: 600;
    color: #333333;
  }
}

/* 1. 国家选择区 (白色底) */
.country-tabs {
  display: flex;
  background-color: #ffffff;
  padding: 0 20rpx;
  height: 88rpx;
  border-bottom: 1rpx solid #E4E7ED;
}

.country-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  color: #666666;
  position: relative;
  transition: all 0.2s;
  
  &.active-country {
    color: #218CFF;
    font-weight: 600;
  }

  .active-line {
    position: absolute;
    bottom: 0;
    width: 40rpx;
    height: 6rpx;
    background-color: #218CFF;
    border-radius: 4rpx;
  }
}

/* 2. 类别选择区 (灰色底，胶囊状横向滚动) */
.category-scroll {
  width: 100%;
  height: 100rpx;
  background-color: #F5F7FA;
  white-space: nowrap;
}

.category-tabs {
  display: inline-flex;
  padding: 20rpx;
  align-items: center;
}

.category-tab {
  padding: 12rpx 32rpx;
  background-color: #ffffff;
  border-radius: 32rpx;
  font-size: 26rpx;
  color: #666666;
  margin-right: 20rpx;
  border: 1rpx solid #E4E7ED;
  transition: all 0.3s;
  flex-shrink: 0;

  &.active-category {
    background-color: #EBF4FF;
    color: #218CFF;
    border-color: #218CFF;
  }
}

/* 3. 法典列表区域 (Swiper 撑满剩余高度) */
.content-swiper {
  flex: 1;
  height: 0;
}

.law-list-scroll {
  height: 100%;
}

.list-wrapper {
  padding: 0 24rpx 140rpx 24rpx; /* 底部留出 tabBar 高度 */
}

.loading-state {
  text-align: center;
  padding: 40rpx;
  color: #999999;
  font-size: 26rpx;
}

/* 卡片样式 */
.law-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx 24rpx;
  margin-bottom: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);
  
  &:active {
    background-color: #FAFAFA;
  }
}

.card-left {
  display: flex;
  align-items: flex-start;
  flex: 1;
  padding-right: 20rpx;
}

.law-icon {
  width: 40rpx;
  height: 40rpx;
  margin-right: 20rpx;
  margin-top: 4rpx;
  opacity: 0.8;
}

.law-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.law-title {
  font-size: 30rpx;
  color: #333333;
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: 12rpx;
  word-break: break-all;
}

.law-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
}

.country-tag {
  background-color: #F0F2F5;
  color: #606266;
}

.category-tag {
  background-color: #EBF4FF;
  color: #218CFF;
}

.status-text {
  font-size: 22rpx;
  color: #67C23A;
  margin-left: auto; /* 靠右对齐 */
}

.arrow-icon {
  width: 32rpx;
  height: 32rpx;
  opacity: 0.4;
}

/* 底部加载状态 */
.load-more-text {
  text-align: center;
  padding: 30rpx 0;
  color: #999999;
  font-size: 24rpx;
}
</style>