"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "LawCard",
  props: {
    lawInfo: {}
  },
  setup(__props) {
    function openDetail(id) {
      common_vendor.index.navigateTo({ url: `/pages/law/detail?id=${id}` });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.t(_ctx.lawInfo.title),
        b: common_vendor.t(_ctx.lawInfo.country),
        c: common_vendor.t(_ctx.lawInfo.category),
        d: common_vendor.t(_ctx.lawInfo.updatedAt || "-"),
        e: common_vendor.o(($event) => openDetail(_ctx.lawInfo.id), "33")
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-4cb33954"]]);
wx.createComponent(Component);
