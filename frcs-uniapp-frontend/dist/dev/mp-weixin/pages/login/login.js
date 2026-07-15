"use strict";
const common_vendor = require("../../common/vendor.js");
const common_assets = require("../../common/assets.js");
const store_user = require("../../store/user.js");
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "login",
  setup(__props) {
    const userStore = store_user.useUserStore();
    const loginMode = common_vendor.ref("password");
    const isAgreed = common_vendor.ref(false);
    const countdown = common_vendor.ref(0);
    let timer = null;
    const formData = common_vendor.reactive({
      email: "",
      password: "",
      code: ""
    });
    const showResetModal = common_vendor.ref(false);
    const resetCountdown = common_vendor.ref(0);
    let resetTimer = null;
    const resetForm = common_vendor.reactive({
      email: "",
      code: "",
      newPassword: ""
    });
    const switchMode = (mode) => {
      if (loginMode.value !== mode) {
        loginMode.value = mode;
        formData.email = "";
        formData.password = "";
        formData.code = "";
      }
    };
    const encryptPassword = (pwd) => {
      return btoa(encodeURIComponent(pwd));
    };
    const sendCode = () => {
      if (!formData.email) {
        return common_vendor.index.showToast({ title: "请先填写邮箱", icon: "none" });
      }
      if (countdown.value > 0)
        return;
      common_vendor.index.showToast({ title: "验证码已发送", icon: "success" });
      countdown.value = 59;
      timer = setInterval(() => {
        if (countdown.value > 0) {
          countdown.value--;
        } else {
          if (timer)
            clearInterval(timer);
        }
      }, 1e3);
    };
    const executeLogin = (payload) => {
      const user = {
        id: 1,
        username: payload.loginType === "wechat" ? "微信用户" : "法律用户",
        email: payload.email || "guest@example.com",
        avatar: ""
      };
      userStore.setLogin("mock-token", user);
      common_vendor.index.showToast({ title: "登录成功", icon: "success" });
      setTimeout(() => {
        common_vendor.index.reLaunch({ url: "/pages/home/index" });
      }, 500);
    };
    const handleLogin = () => {
      if (!isAgreed.value) {
        return common_vendor.index.showToast({ title: "请先阅读并同意用户协议与隐私政策", icon: "none" });
      }
      if (!formData.email) {
        return common_vendor.index.showToast({ title: "请输入邮箱", icon: "none" });
      }
      const payload = { email: formData.email };
      if (loginMode.value === "password") {
        if (!formData.password) {
          return common_vendor.index.showToast({ title: "请输入密码", icon: "none" });
        }
        payload.password = encryptPassword(formData.password);
        payload.loginType = "password";
      } else {
        if (!formData.code) {
          return common_vendor.index.showToast({ title: "请输入验证码", icon: "none" });
        }
        payload.code = formData.code;
        payload.loginType = "code";
      }
      executeLogin(payload);
    };
    const handleWechatLogin = () => {
      if (!isAgreed.value) {
        return common_vendor.index.showToast({ title: "请先阅读并同意用户协议与隐私政策", icon: "none" });
      }
      common_vendor.index.login({
        provider: "weixin",
        success: (res) => {
          console.log("微信登录 code:", res.code);
          executeLogin({ email: "wechat_user", loginType: "wechat", code: res.code });
        },
        fail: () => {
          common_vendor.index.showToast({ title: "微信登录失败", icon: "none" });
        }
      });
    };
    const guestAccess = () => {
      executeLogin({ email: "guest@example.com", loginType: "guest" });
    };
    const closeResetModal = () => {
      showResetModal.value = false;
      resetForm.email = "";
      resetForm.code = "";
      resetForm.newPassword = "";
      if (resetTimer)
        clearInterval(resetTimer);
      resetCountdown.value = 0;
    };
    const sendResetCode = () => {
      if (!resetForm.email) {
        return common_vendor.index.showToast({ title: "请先填写邮箱", icon: "none" });
      }
      if (resetCountdown.value > 0)
        return;
      common_vendor.index.showToast({ title: "验证码已发送", icon: "success" });
      resetCountdown.value = 59;
      resetTimer = setInterval(() => {
        if (resetCountdown.value > 0) {
          resetCountdown.value--;
        } else {
          if (resetTimer)
            clearInterval(resetTimer);
        }
      }, 1e3);
    };
    const handleResetPassword = () => {
      if (!resetForm.email || !resetForm.code || !resetForm.newPassword) {
        return common_vendor.index.showToast({ title: "请填写完整信息", icon: "none" });
      }
      const encryptedNewPwd = encryptPassword(resetForm.newPassword);
      console.log("提交重置:", resetForm.email, resetForm.code, encryptedNewPwd);
      common_vendor.index.showToast({ title: "密码重置成功", icon: "success" });
      setTimeout(() => {
        closeResetModal();
      }, 1e3);
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_assets._imports_0,
        b: common_vendor.n(loginMode.value === "password" ? "active" : ""),
        c: common_vendor.n(loginMode.value === "password" ? "text-active" : ""),
        d: common_vendor.o(($event) => switchMode("password"), "a6"),
        e: common_vendor.n(loginMode.value === "code" ? "active" : ""),
        f: common_vendor.n(loginMode.value === "code" ? "text-active" : ""),
        g: common_vendor.o(($event) => switchMode("code"), "47"),
        h: formData.email,
        i: common_vendor.o(($event) => formData.email = $event.detail.value, "cd"),
        j: loginMode.value === "password"
      }, loginMode.value === "password" ? {
        k: formData.password,
        l: common_vendor.o(($event) => formData.password = $event.detail.value, "50")
      } : {}, {
        m: loginMode.value === "code"
      }, loginMode.value === "code" ? {
        n: formData.code,
        o: common_vendor.o(($event) => formData.code = $event.detail.value, "b2"),
        p: common_vendor.t(countdown.value > 0 ? `${countdown.value}s后重试` : "获取验证码"),
        q: countdown.value > 0,
        r: common_vendor.o(sendCode, "95")
      } : {}, {
        s: common_vendor.o(($event) => showResetModal.value = true, "3d"),
        t: common_vendor.o(handleLogin, "4b"),
        v: common_vendor.o(handleWechatLogin, "2a"),
        w: common_vendor.o(guestAccess, "53"),
        x: common_vendor.n(isAgreed.value ? "active" : ""),
        y: common_vendor.o(($event) => isAgreed.value = !isAgreed.value, "ff"),
        z: showResetModal.value
      }, showResetModal.value ? {
        A: common_vendor.o(closeResetModal, "0f"),
        B: resetForm.email,
        C: common_vendor.o(($event) => resetForm.email = $event.detail.value, "e4"),
        D: resetForm.code,
        E: common_vendor.o(($event) => resetForm.code = $event.detail.value, "d3"),
        F: common_vendor.t(resetCountdown.value > 0 ? `${resetCountdown.value}s后重试` : "获取验证码"),
        G: resetCountdown.value > 0,
        H: common_vendor.o(sendResetCode, "a7"),
        I: resetForm.newPassword,
        J: common_vendor.o(($event) => resetForm.newPassword = $event.detail.value, "79"),
        K: common_vendor.o(handleResetPassword, "ad")
      } : {});
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-cdfe2409"]]);
wx.createPage(MiniProgramPage);
