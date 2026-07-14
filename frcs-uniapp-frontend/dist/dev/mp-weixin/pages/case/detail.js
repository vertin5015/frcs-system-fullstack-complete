"use strict";
const common_vendor = require("../../common/vendor.js");
const api_request = require("../../api/request.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "detail",
  setup(__props) {
    const caseInfo = common_vendor.ref();
    common_vendor.onLoad(async (query) => {
      caseInfo.value = await api_request.caseApi.detail(Number(query == null ? void 0 : query.id));
    });
    function favorite() {
      common_vendor.index.showToast({ title: "已收藏", icon: "success" });
    }
    function openOriginal() {
      common_vendor.index.showToast({ title: "原文链接待接入", icon: "none" });
    }
    return (_ctx, _cache) => {
      var _a;
      return common_vendor.e({
        a: caseInfo.value
      }, caseInfo.value ? {
        b: common_vendor.t(caseInfo.value.title),
        c: common_vendor.t(caseInfo.value.type),
        d: common_vendor.t(caseInfo.value.court),
        e: common_vendor.t(caseInfo.value.caseNumber),
        f: common_vendor.t(caseInfo.value.date),
        g: common_vendor.t(caseInfo.value.summary),
        h: common_vendor.t(caseInfo.value.courtOpinion),
        i: common_vendor.t((_a = caseInfo.value.legalProvisions) == null ? void 0 : _a.join("、")),
        j: common_vendor.o(favorite, "18"),
        k: common_vendor.o(openOriginal, "90")
      } : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-dc481587"]]);
wx.createPage(MiniProgramPage);
