"use strict";
const common_vendor = require("../../common/vendor.js");
const api_request = require("../../api/request.js");
if (!Math) {
  (SearchBar + CaseCard + BottomTabBar)();
}
const SearchBar = () => "../../components/SearchBar.js";
const CaseCard = () => "../../components/CaseCard.js";
const BottomTabBar = () => "../../components/BottomTabBar.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "index",
  setup(__props) {
    const cases = common_vendor.ref([]);
    common_vendor.onMounted(async () => {
      cases.value = await api_request.caseApi.list();
    });
    function search(keyword) {
      common_vendor.index.navigateTo({ url: `/pages/case/list?keyword=${encodeURIComponent(keyword)}` });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(search, "b7"),
        b: common_vendor.p({
          placeholder: "搜索案例"
        }),
        c: common_vendor.f(cases.value, (item, k0, i0) => {
          return {
            a: item.id,
            b: "2c5296db-1-" + i0,
            c: common_vendor.p({
              ["case-info"]: item
            })
          };
        })
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-2c5296db"]]);
wx.createPage(MiniProgramPage);
