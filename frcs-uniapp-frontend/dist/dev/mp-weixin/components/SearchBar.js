"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "SearchBar",
  props: /* @__PURE__ */ common_vendor.mergeModels({
    placeholder: { default: "请输入关键词" }
  }, {
    "modelValue": { default: "" },
    "modelModifiers": {}
  }),
  emits: /* @__PURE__ */ common_vendor.mergeModels(["search"], ["update:modelValue"]),
  setup(__props, { emit: __emit }) {
    const keyword = common_vendor.useModel(__props, "modelValue");
    const emit = __emit;
    function submit() {
      emit("search", keyword.value);
    }
    return (_ctx, _cache) => {
      return {
        a: _ctx.placeholder,
        b: common_vendor.o(submit, "12"),
        c: keyword.value,
        d: common_vendor.o(($event) => keyword.value = $event.detail.value, "c6"),
        e: common_vendor.o(submit, "d2")
      };
    };
  }
});
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-6ab8b910"]]);
wx.createComponent(Component);
