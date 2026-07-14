"use strict";
const common_vendor = require("../../common/vendor.js");
const api_request = require("../../api/request.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "detail",
  setup(__props) {
    const law = common_vendor.ref();
    common_vendor.onLoad(async (query) => {
      law.value = await api_request.lawApi.detail(Number(query == null ? void 0 : query.id));
    });
    return (_ctx, _cache) => {
      var _a, _b;
      return common_vendor.e({
        a: law.value
      }, law.value ? {
        b: common_vendor.t(law.value.title),
        c: common_vendor.t(law.value.content),
        d: common_vendor.t(law.value.interpretation),
        e: common_vendor.t((_a = law.value.revisions) == null ? void 0 : _a.join("、")),
        f: common_vendor.t((_b = law.value.relatedCaseIds) == null ? void 0 : _b.join("、"))
      } : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-8a4aee64"]]);
wx.createPage(MiniProgramPage);
