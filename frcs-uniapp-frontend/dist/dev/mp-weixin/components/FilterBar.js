"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "FilterBar",
  props: {
    countries: { default: () => [] },
    dates: { default: () => [] },
    sources: { default: () => [] }
  },
  emits: ["change"],
  setup(__props, { emit: __emit }) {
    const emit = __emit;
    function notify() {
      emit("change", { country: "", date: "", source: "" });
    }
    return (_ctx, _cache) => {
      return {
        a: common_vendor.o(notify, "73"),
        b: common_vendor.o(notify, "2c"),
        c: common_vendor.o(notify, "dd")
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-f4672e2a"]]);
wx.createComponent(Component);
