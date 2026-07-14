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
    const records = common_vendor.ref([]);
    common_vendor.onMounted(async () => {
      records.value = await api_request.caseApi.list();
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.f(records.value, (item, k0, i0) => {
          return {
            a: item.id,
            b: "530ef1ab-0-" + i0,
            c: common_vendor.p({
              ["case-info"]: item
            })
          };
        }),
        b: !records.value.length
      }, !records.value.length ? {
        c: common_vendor.p({
          text: "暂无浏览记录"
        })
      } : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-530ef1ab"]]);
wx.createPage(MiniProgramPage);
