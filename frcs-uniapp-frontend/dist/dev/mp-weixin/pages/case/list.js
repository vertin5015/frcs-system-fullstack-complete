"use strict";
const common_vendor = require("../../common/vendor.js");
const api_request = require("../../api/request.js");
if (!Math) {
  (SearchBar + FilterBar + CaseCard + Empty)();
}
const SearchBar = () => "../../components/SearchBar.js";
const FilterBar = () => "../../components/FilterBar.js";
const CaseCard = () => "../../components/CaseCard.js";
const Empty = () => "../../components/Empty.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "list",
  setup(__props) {
    const cases = common_vendor.ref([]);
    async function search(keyword = "") {
      cases.value = await api_request.caseApi.list({ keyword });
    }
    common_vendor.onLoad((query) => {
      search(typeof (query == null ? void 0 : query.keyword) === "string" ? query.keyword : "");
    });
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.o(search, "d5"),
        b: common_vendor.o(($event) => search(), "19"),
        c: common_vendor.f(cases.value, (item, k0, i0) => {
          return {
            a: item.id,
            b: "5b946739-2-" + i0,
            c: common_vendor.p({
              ["case-info"]: item
            })
          };
        }),
        d: !cases.value.length
      }, !cases.value.length ? {} : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-5b946739"]]);
wx.createPage(MiniProgramPage);
