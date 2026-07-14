"use strict";
const common_vendor = require("../../common/vendor.js");
const store_user = require("../../store/user.js");
if (!Math) {
  LoginForm();
}
const LoginForm = () => "../../components/LoginForm.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "login",
  setup(__props) {
    const userStore = store_user.useUserStore();
    function login(payload) {
      const user = { id: 1, username: "法律用户", email: payload.email || "guest@example.com", avatar: "" };
      userStore.setLogin("mock-token", user);
      common_vendor.index.reLaunch({ url: "/pages/home/index" });
    }
    function guestAccess() {
      login({ email: "guest@example.com" });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(login, "88"),
        b: common_vendor.o(guestAccess, "45")
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-cdfe2409"]]);
wx.createPage(MiniProgramPage);
