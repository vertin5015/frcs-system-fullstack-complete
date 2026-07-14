"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "register",
  setup(__props) {
    const form = common_vendor.reactive({ username: "", email: "", password: "", confirmPassword: "" });
    function submit() {
      common_vendor.index.showToast({ title: "注册信息已提交", icon: "none" });
    }
    return (_ctx, _cache) => {
      return {
        a: form.username,
        b: common_vendor.o(($event) => form.username = $event.detail.value, "c7"),
        c: form.email,
        d: common_vendor.o(($event) => form.email = $event.detail.value, "fa"),
        e: form.password,
        f: common_vendor.o(($event) => form.password = $event.detail.value, "fd"),
        g: form.confirmPassword,
        h: common_vendor.o(($event) => form.confirmPassword = $event.detail.value, "bc"),
        i: common_vendor.o(submit, "cf")
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-12565c11"]]);
wx.createPage(MiniProgramPage);
