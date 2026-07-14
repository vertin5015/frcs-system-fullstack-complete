"use strict";
const common_vendor = require("../../common/vendor.js");
const api_request = require("../../api/request.js");
if (!Math) {
  (CaseCard + Empty)();
}
const CaseCard = () => "../../components/CaseCard.js";
const Empty = () => "../../components/Empty.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "index",
  setup(__props) {
    const favorites = common_vendor.ref([]);
    common_vendor.onMounted(async () => {
      favorites.value = (await api_request.caseApi.list()).slice(0, 1);
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(favorites.value, (item, k0, i0) => {
          return {
            a: item.id,
            b: "9c2e4bd0-0-" + i0,
            c: common_vendor.p({
              ["case-info"]: item
            })
          };
        }),
        b: !favorites.value.length
      }, !favorites.value.length ? {
        c: common_vendor.p({
          text: "暂无收藏案例"
        })
      } : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-9c2e4bd0"]]);
wx.createPage(MiniProgramPage);
