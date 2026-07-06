<template>
  <div class="common-layout">
    <el-container>
      <el-aside :width="collapsed ? '64px' : '160px'" class="aside-bar">
        <div class="menu-toggle" @click="collapsed = !collapsed">
          <el-icon><Menu /></el-icon>
        </div>
        <el-menu
          :default-active="activeMenu || undefined"
          class="el-menu-vertical-demo"
          background-color="#F5F7FA"
          text-color="#909399"
          active-text-color="#409EFF"
          :collapse="collapsed"
          @select="handleMenuSelect"
        >
          <el-menu-item index="1">
            <i class="iconfont icon-sousuo2" style="font-size: 26px"></i>
            <span v-if="!collapsed" style="margin-left: 15px; font-size: 15px">{{ lang === "zh" ? "搜索案例" : "Search Cases" }}</span>
          </el-menu-item>
          <el-menu-item index="2"  :disabled="username === '游客'">
            <i class="iconfont icon-shoucang_shixin" style="font-size: 25px"></i>
            <span v-if="!collapsed" style="margin-left: 15px; font-size: 15px">{{ lang === "zh" ? "收藏案件" : "Favorites" }}</span>
          </el-menu-item>
          <el-menu-item index="3"  :disabled="username === '游客'">
            <i class="iconfont icon-lishixiao1" style="font-size: 20px; margin-left: 1px"></i>
            <span v-if="!collapsed" style="margin-left: 15px; font-size: 15px">{{ lang === "zh" ? "历史记录" : "History" }}</span>
          </el-menu-item>
          <el-menu-item index="4" :disabled="username === '游客'">
            <i class="iconfont icon-falvfagui" style="font-size: 22px; color: #409eff; margin-left: 2px"></i>
            <span v-if="!collapsed" style="margin-left: 15px; font-size: 15px">{{ lang === "zh" ? "购买次数" : "Buy credits" }}</span>
          </el-menu-item>
          <el-menu-item index="5">
            <el-icon class="kb-menu-icon"><Collection /></el-icon>
            <span v-if="!collapsed" style="margin-left: 15px; font-size: 15px">{{ lang === "zh" ? "本地知识库" : "Local KB" }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="header-bar">
          <div class="header-title">
            <span style="font-size: 18px; margin-left: 23px">{{ lang === "zh" ? "涉外案例查询分析系统" : "Foreign Case Query & Analysis System" }}</span>
          </div>
          <div class="header-actions">
            <el-switch v-model="switchLang" :active-value="'en'" :inactive-value="'zh'" active-text="EN" inactive-text="中文" style="margin-right: 24px" @change="changeLang" />
            <el-dropdown trigger="hover" placement="bottom-end">
              <span class="avatar-dropdown" style="display: inline-block">
                <div style="height: 32px; width: 32px; border-radius: 50%; background-color: #1883ff; display: flex; justify-content: center; align-items: center">
                  <i class="iconfont icon-yonghu" style="font-size: 20px; color: white; border: none"></i>
                </div>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item disabled>{{ username }}</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">{{ lang === "zh" ? "退出登录" : "Logout" }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="main-content">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { ref, onMounted, computed, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { Collection, Menu } from "@element-plus/icons-vue";
import { useStore } from "vuex";
import { ElNotification } from "element-plus";
import { getAuth, clearAllAuth } from "../utils/authStorage";

export default {
  components: {
    Collection,
    Menu,
  },
  setup() {
    // =============================
    // ✅ 响应式变量定义
    // =============================
    const collapsed = ref(true);
    const activeMenu = ref("1");
    const userEmail = ref(getAuth("userEmail") || "");
    const username = ref(getAuth("username") || "");
    const router = useRouter();
    const route = useRoute();
    const store = useStore();

    // =============================
    // ✅ 计算属性 (lang 依然是只读的，它仅用于显示文本)
    // =============================
    const lang = computed(() => store.getters.lang);

    const switchLang = computed({
      get: () => store.state.lang,
      set: (val) => {
        // 当 switchLang 被修改时，更新 Vuex store
        store.commit("setLang", val);
        localStorage.setItem("lang", val);
      },
    });

    // =============================
    // ✅ 初始化方法
    // =============================
    const initApp = () => {
      userEmail.value = getAuth("userEmail") || "";
      username.value = getAuth("username") || "";
      activeMenu.value = pathToMenu(route.path);
    };

    // =============================
    // ✅ 菜单相关逻辑
    // =============================
    const handleMenuSelect = (index) => {
      const userId = getAuth("userId");
      const isGuest = userId === "0";

      if (index === "1") {
        // 「搜索案例」→ 案例检索页（含多数据源、PDF+AI 摘要），不是首页
        activeMenu.value = "1";
        router.push("/case-query/search");
      } else if (index === "2") {
        if (isGuest) {
          // 🚫 游客访问收藏：不更新 activeMenu，不跳转，只提示
          ElNotification({
            title: lang.value === "zh" ? "提示" : "Notice",
            message: lang.value === "zh" ? "游客用户无法访问收藏夹，请登录后重试。" : "Guest users cannot access favorites, please log in and try again.",
            type: "info",
            duration: 3000,
          });
          // 🔴 不设置 activeMenu.value → 菜单不会高亮
          activeMenu.value = pathToMenu(route.path);
        } else {
          activeMenu.value = "2";
          router.push("/case-query/favorite");
        }
      } else if (index === "3") {
        if (isGuest) {
          ElNotification({
            title: lang.value === "zh" ? "提示" : "Notice",
            message: lang.value === "zh" ? "游客用户无法访问历史记录，请登录后重试。" : "Guest users cannot access history, please log in and try again.",
            type: "info",
            duration: 3000,
          });
          activeMenu.value = pathToMenu(route.path);
        } else {
          activeMenu.value = "3";
          router.push("/case-query/history");
        }
      } else if (index === "4") {
        if (isGuest) {
          ElNotification({
            title: lang.value === "zh" ? "提示" : "Notice",
            message: lang.value === "zh" ? "请先登录后再购买次数。" : "Please log in to purchase credits.",
            type: "info",
            duration: 3000,
          });
          activeMenu.value = pathToMenu(route.path);
        } else {
          activeMenu.value = "4";
          router.push("/case-query/recharge");
        }
      } else if (index === "5") {
        activeMenu.value = "5";
        router.push("/case-query/kb");
      }
    };
    const pathToMenu = (path) => {
      if (path.includes("/case-query/favorite")) return "2";
      if (path.includes("/case-query/history")) return "3";
      if (path.includes("/case-query/recharge")) return "4";
      if (path.includes("/case-query/kb")) return "5";
      if (path.includes("/case-query/search")) return "1";
      // 首页 /case-query/home：不高亮「搜索案例」，避免误以为已在检索页
      return "";
    };

    // =============================
    // ✅ 语言切换逻辑
    // =============================
    const changeLang = (val) => {
      store.commit("setLang", val); // 将新的语言值提交到 Vuex store
    };

    // =============================
    // ✅ 登出逻辑
    // =============================
    const logout = () => {
      clearAllAuth();
      router.push("/login");
    };

    // =============================
    // ✅ 路由监听逻辑
    // =============================
    watch(
      () => route.path,
      (newPath) => {
        activeMenu.value = pathToMenu(newPath);
      },
      { immediate: true }
    );

    // =============================
    // ✅ 生命周期
    // =============================
    onMounted(() => {
      initApp();
    });

    // =============================
    // ✅ 返回模板所需数据
    // =============================
    return {
      collapsed,
      lang, // lang 仍然暴露给模板，用于显示文本（例如菜单项的文本），因为它仍然是一个有用的只读值。
      switchLang, // 新增：暴露给 el-switch 的 v-model。
      activeMenu,
      changeLang,
      userEmail,
      username,
      logout,
      handleMenuSelect,
    };
  },
};
</script>

<style scoped>
.common-layout {
  height: 100vh;
  width: 100vw;
  background: #fff;
}
.aside-bar {
  background: #f5f7fa;
  color: #fff;
  position: relative;
  transition: width 0.2s;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding-bottom: 0;
}
.menu-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  cursor: pointer;
  color: #409eff;
  font-size: 20px;
}
.el-menu-vertical-demo {
  border-right: none;
}
.kb-menu-icon {
  color: #67c23a;
  font-size: 22px;
  margin-left: 2px;
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  height: 64px;
  padding: 0 24px;
  border-bottom: none;
}
.header-title {
  display: flex;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
  color: #222;
}
.header-actions {
  display: flex;
  align-items: center;
}
.main-content {
  background: #fff;
  min-height: calc(100vh - 64px);
  padding: 0;
}
/* 这里保留了 ::v-deep，但 Vue 3 推荐使用 :deep() 伪类选择器，你可以考虑更新 */
::v-deep .el-menu-item {
  border-radius: 10px;
  overflow: hidden;
}
</style>
