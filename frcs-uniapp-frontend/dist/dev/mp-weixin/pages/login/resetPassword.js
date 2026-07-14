"use strict";
const common_vendor = require("../../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "resetPassword",
  setup(__props) {
    const form = common_vendor.reactive({ email: "", captcha: "", newPassword: "" });
    function submit() {
      common_vendor.index.showToast({ title: "密码重置请求已提交", icon: "none" });
    }
    return (_ctx, _cache) => {
      return {
        a: form.email,
        b: common_vendor.o(($event) => form.email = $event.detail.value, "54"),
        c: form.captcha,
        d: common_vendor.o(($event) => form.captcha = $event.detail.value, "55"),
        e: form.newPassword,
        f: common_vendor.o(($event) => form.newPassword = $event.detail.value, "5f"),
        g: common_vendor.o(submit, "ba")
      };
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-0beea310"]]);
wx.createPage(MiniProgramPage);
