"use strict";
const common_vendor = require("../common/vendor.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "LoginForm",
  emits: ["submit"],
  setup(__props, { emit: __emit }) {
    const mode = common_vendor.ref("password");
    const form = common_vendor.reactive({ email: "", password: "", captcha: "" });
    const emit = __emit;
    function submit() {
      emit("submit", { ...form, password: mode.value === "password" ? form.password : void 0 });
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.t(mode.value === "password" ? "验证码" : "密码"),
        b: common_vendor.o(($event) => mode.value = mode.value === "password" ? "captcha" : "password", "da"),
        c: form.email,
        d: common_vendor.o(($event) => form.email = $event.detail.value, "2b"),
        e: mode.value === "password"
      }, mode.value === "password" ? {
        f: form.password,
        g: common_vendor.o(($event) => form.password = $event.detail.value, "91")
      } : {
        h: form.captcha,
        i: common_vendor.o(($event) => form.captcha = $event.detail.value, "b5")
      }, {
        j: common_vendor.o(submit, "67")
      });
    };
  }
});
wx.createComponent(_sfc_main);
