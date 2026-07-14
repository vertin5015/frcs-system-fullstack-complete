"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "BottomTabBar",
  setup(__props) {
    const tabs = [
      { text: "首页", url: "/pages/home/index" },
      { text: "检索", url: "/pages/case/list" },
      { text: "学法", url: "/pages/study/index" },
      { text: "收藏", url: "/pages/favorite/index" },
      { text: "记录", url: "/pages/history/index" },
      { text: "个人", url: "/pages/user/index" }
    ];
    function navigate(url) {
      common_vendor.index.navigateTo({ url });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(tabs, (tab, k0, i0) => {
          return {
            a: common_vendor.t(tab.text),
            b: tab.url,
            c: common_vendor.o(($event) => navigate(tab.url), tab.url)
          };
        })
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-e3d80f8d"]]);
wx.createComponent(Component);
