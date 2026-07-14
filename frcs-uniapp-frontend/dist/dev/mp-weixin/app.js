"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const store_app = require("./store/app.js");
if (!Math) {
  "./pages/login/login.js";
  "./pages/login/register.js";
  "./pages/login/resetPassword.js";
  "./pages/home/index.js";
  "./pages/case/list.js";
  "./pages/case/detail.js";
  "./pages/law/list.js";
  "./pages/law/detail.js";
  "./pages/study/index.js";
  "./pages/favorite/index.js";
  "./pages/history/index.js";
  "./pages/user/index.js";
}
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "App",
  setup(__props) {
    common_vendor.onLaunch(() => {
      store_app.useAppStore().initialize();
    });
    return () => {
    };
  }
});
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  app.use(common_vendor.createPinia());
  return { app };
}
createApp().app.mount("#app");
exports.createApp = createApp;
