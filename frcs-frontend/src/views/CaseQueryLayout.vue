<template>
  <div class="common-layout">
    <el-container>
      <el-aside :width="collapsed ? '72px' : '216px'" class="aside-bar">
        <button class="menu-toggle" type="button" :aria-label="collapsed ? (lang === 'zh' ? '展开导航' : 'Expand navigation') : (lang === 'zh' ? '收起导航' : 'Collapse navigation')" :aria-expanded="!collapsed" @click="collapsed = !collapsed">
          <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
        </button>
        <el-menu :default-active="activeMenu" class="el-menu-vertical-demo" :collapse="collapsed" @select="handleMenuSelect">
          <el-menu-item index="0"><el-icon><House /></el-icon><template #title>{{ lang === "zh" ? "首页" : "Home" }}</template></el-menu-item>
          <el-menu-item index="1"><el-icon><Search /></el-icon><template #title>{{ lang === "zh" ? "搜索案例" : "Search Cases" }}</template></el-menu-item>
          <el-menu-item index="2" :disabled="username === '游客'"><el-icon><Star /></el-icon><template #title>{{ lang === "zh" ? "收藏案件" : "Favorites" }}</template></el-menu-item>
          <el-menu-item index="3" :disabled="username === '游客'"><el-icon><Clock /></el-icon><template #title>{{ lang === "zh" ? "历史记录" : "History" }}</template></el-menu-item>
          <el-menu-item index="4" :disabled="username === '游客'"><el-icon><CreditCard /></el-icon><template #title>{{ lang === "zh" ? "购买次数" : "Buy credits" }}</template></el-menu-item>
          <el-menu-item index="5"><el-icon><Collection /></el-icon><template #title>{{ lang === "zh" ? "本地知识库" : "Local KB" }}</template></el-menu-item>
          <el-menu-item index="6"><el-icon><ChatDotRound /></el-icon><template #title>{{ lang === "zh" ? "智能问答" : "Agent" }}</template></el-menu-item>
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
                <div style="height: 32px; width: 32px; border-radius: 50%; background-color: var(--frcs-primary); display: flex; justify-content: center; align-items: center">
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
import { ChatDotRound, Clock, Collection, CreditCard, Expand, Fold, House, Search, Star } from "@element-plus/icons-vue";
import { useStore } from "vuex";
import { ElNotification } from "element-plus";
import { getAuth, clearAllAuth } from "../utils/authStorage";

export default {
  components: {
    ChatDotRound,
    Clock,
    Collection,
    CreditCard,
    Expand,
    Fold,
    House,
    Search,
    Star,
  },
  setup() {
    // =============================
    // ✅ 响应式变量定义
    // =============================
    const collapsed = ref(true);
    const activeMenu = ref("0");
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

      if (index === "0") {
        activeMenu.value = "0";
        router.push("/case-query/home");
      } else if (index === "1") {
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
      } else if (index === "6") {
        activeMenu.value = "6";
        router.push("/case-query/agent");
      }
    };
    const pathToMenu = (path) => {
      if (path.includes("/case-query/favorite")) return "2";
      if (path.includes("/case-query/history")) return "3";
      if (path.includes("/case-query/recharge")) return "4";
      if (path.includes("/case-query/kb")) return "5";
      if (path.includes("/case-query/agent")) return "6";
      if (path.includes("/case-query/search")) return "1";
      if (path.includes("/case-query/home") || path === "/case-query") return "0";
      return "0";
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
.common-layout { min-height:100vh; width:100%; background:var(--frcs-bg); }
.aside-bar { background:linear-gradient(180deg,#1c1917 0%,#292524 100%); color:#fff; position:relative; transition:width .28s cubic-bezier(.22,.61,.36,1); min-height:100vh; overflow:hidden; display:flex; flex-direction:column; align-items:stretch; }
.menu-toggle { width:100%; height:64px; display:flex; align-items:center; justify-content:center; flex:0 0 64px; cursor:pointer; color:rgba(255,255,255,.84); font-size:20px; border:0; border-bottom:1px solid rgba(255,255,255,.1); background:transparent; transition:background-color .22s ease,color .22s ease; }
.menu-toggle:hover { background:rgba(255,255,255,.08); color:#fff; }
.el-menu-vertical-demo { width:100%; border-right:none; background:transparent !important; padding:12px 8px; }
:deep(.el-menu-item) { display:flex; align-items:center; gap:12px; width:100%; height:48px; margin:5px 0; padding:0 14px !important; border-radius:10px; overflow:hidden; color:rgba(255,255,255,.68); transition:background-color .22s ease,color .22s ease,box-shadow .22s ease; }
:deep(.el-menu-item .el-icon) { flex:0 0 22px; width:22px; height:22px; margin:0 !important; color:currentColor; font-size:20px; }
:deep(.el-menu-item span) { margin:0 !important; white-space:nowrap; }
:deep(.el-menu--collapse .el-menu-item) { justify-content:center; padding:0 !important; }
:deep(.el-menu--collapse .el-menu-item .el-icon) { margin:0 !important; }
:deep(.el-menu-item:hover) { background:rgba(255,255,255,.1); color:#fff; }
:deep(.el-menu-item.is-active) { background:rgba(214,168,93,.18); color:#f2d39a; box-shadow:inset 3px 0 var(--frcs-accent); }
:deep(.el-menu-item.is-disabled) { opacity:.36; }
:deep(.el-menu) { background:transparent; }
.header-bar { display:flex; justify-content:space-between; align-items:center; background:rgba(255,255,255,.94); height:72px; padding:0 clamp(16px,3vw,34px); border-bottom:1px solid var(--frcs-border); backdrop-filter:blur(12px); }
.header-title { display:flex; align-items:center; gap:12px; font-size:18px; font-weight:700; color:var(--frcs-primary); letter-spacing:-.02em; }
.header-title::before { content:'§'; display:grid; place-items:center; width:30px; height:30px; border-radius:9px; color:#fff; background:var(--frcs-accent); font-family:Georgia,serif; font-size:18px; }
.header-actions { display:flex; align-items:center; gap:18px; }
.avatar-dropdown > div { background:var(--frcs-primary) !important; box-shadow:0 4px 12px rgba(28,25,23,.18); }
.main-content { background:var(--frcs-bg); min-height:calc(100vh - 72px); padding:0; }
@media (max-width:768px) { .header-bar { height:62px; padding:0 14px; } .header-title span { margin-left:0 !important; font-size:14px !important; max-width:200px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; } .header-actions { gap:8px; } .header-actions .el-switch { margin-right:4px !important; } .aside-bar { width:72px !important; } .main-content { min-height:calc(100vh - 62px); } }
</style>
