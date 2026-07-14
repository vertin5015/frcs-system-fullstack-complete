"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "CaseCard",
  props: {
    caseInfo: {}
  },
  setup(__props) {
    function openDetail(id) {
      common_vendor.index.navigateTo({ url: `/pages/case/detail?id=${id}` });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.t(_ctx.caseInfo.country),
        b: common_vendor.t(_ctx.caseInfo.title),
        c: common_vendor.t(_ctx.caseInfo.englishTitle),
        d: common_vendor.t(_ctx.caseInfo.court),
        e: common_vendor.t(_ctx.caseInfo.date),
        f: common_vendor.t(_ctx.caseInfo.aiSummaryStatus === "completed" ? "已生成" : "待生成"),
        g: common_vendor.o(($event) => openDetail(_ctx.caseInfo.id), "f4")
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-90e3e9b3"]]);
wx.createComponent(Component);
