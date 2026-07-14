"use strict";
const common_vendor = require("../../common/vendor.js");
const api_request = require("../../api/request.js");
if (!Math) {
  LawCard();
}
const LawCard = () => "../../components/LawCard.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "list",
  setup(__props) {
    const laws = common_vendor.ref([]);
    common_vendor.onMounted(async () => {
      laws.value = await api_request.lawApi.list();
    });
    return (_ctx, _cache) => {
      return {
        a: common_vendor.f(laws.value, (item, k0, i0) => {
          return {
            a: item.id,
            b: "7d773397-0-" + i0,
            c: common_vendor.p({
              ["law-info"]: item
            })
          };
        })
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-7d773397"]]);
wx.createPage(MiniProgramPage);
