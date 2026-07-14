"use strict";
const common_vendor = require("../../common/vendor.js");
const store_user = require("../../store/user.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "index",
  setup(__props) {
    const userStore = store_user.useUserStore();
    const user = common_vendor.computed(() => userStore.userInfo);
    function recharge() {
      common_vendor.index.showToast({ title: "充值功能待接入", icon: "none" });
    }
    function goStudy() {
      common_vendor.index.navigateTo({ url: "/pages/study/index" });
    }
    function logout() {
      userStore.logout();
      common_vendor.index.reLaunch({ url: "/pages/login/login" });
    }
    return (_ctx, _cache) => {
      var _a, _b, _c;
      return {
        a: ((_a = user.value) == null ? void 0 : _a.avatar) || "/static/default-avatar.png",
        b: common_vendor.t(((_b = user.value) == null ? void 0 : _b.username) || "未登录用户"),
        c: common_vendor.t(((_c = user.value) == null ? void 0 : _c.email) || "-"),
        d: common_vendor.o(recharge, "fd"),
        e: common_vendor.o(goStudy, "f6"),
        f: common_vendor.o(logout, "d4")
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-642c545b"]]);
wx.createPage(MiniProgramPage);
