"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "index",
  setup(__props) {
    const categories = ["美国", "欧盟", "日本", "韩国"];
    const activeCategory = common_vendor.ref(categories[0]);
    const knowledge = common_vendor.ref(["跨境合同订立要点", "海外数据合规基础", "争议解决条款说明"]);
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(categories, (item, k0, i0) => {
          return {
            a: common_vendor.t(item),
            b: item,
            c: common_vendor.o(($event) => activeCategory.value = item, item)
          };
        }),
        b: common_vendor.t(activeCategory.value),
        c: common_vendor.f(knowledge.value, (item, k0, i0) => {
          return {
            a: common_vendor.t(item),
            b: item
          };
        })
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-95bda29c"]]);
wx.createPage(MiniProgramPage);
