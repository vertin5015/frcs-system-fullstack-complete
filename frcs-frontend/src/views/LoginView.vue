<template>
  <div class="login-bg">
    <div style="position: absolute; top: 23%; left: 5%; height: 50px; width: 720px; display: flex; justify-content: center; align-items: center">
      <span style="font-size: 30px; font-weight: 600; color: #595959; white-space: nowrap; display: inline-block; max-width: 100%">
        {{ lang === "zh" ? "涉外案例查询分析系统" : "Foreign-related case query and analysis system" }}
      </span>
    </div>
    <div style="position: absolute; top: 28%; left: 5%; height: 350px; width: 720px; display: flex; justify-content: center; align-items: center">
      <i class="iconfont icon-sifachu gradient-text" style="font-size: 270px; color: icon-sifapaimai;"></i>
    </div>
    <div class="center-box">
      <div class="login-card">
        <div style="display: flex; align-items: center; justify-content: center; width: 100%; height: 70px">
          <div style="height: 78px; width: 78px; background-color: white; border-radius: 50%; display: flex; justify-content: center; align-items: center">
            <i class="iconfont icon-falvfagui" style="font-size: 50px; color: #409eff"></i>
          </div>
        </div>
        <h2 class="welcome">{{ isLogin ? text.welcome : text.registerTitle }}</h2>

        <!-- 登录 -->
        <el-form v-if="isLogin" :model="form" class="login-form">
          <el-radio-group v-model="loginMode" size="small" class="login-mode-row">
            <el-radio-button label="password">{{ text.modePassword }}</el-radio-button>
            <el-radio-button label="code">{{ text.modeCode }}</el-radio-button>
          </el-radio-group>
          <el-form-item>
            <el-input v-model="form.email" :placeholder="text.emailShort" clearable />
          </el-form-item>
          <template v-if="loginMode === 'password'">
            <el-form-item>
              <el-input v-model="form.password" :placeholder="text.password" show-password clearable />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item>
              <div class="code-row">
                <el-input v-model="form.code" :placeholder="text.codePlaceholder" maxlength="6" clearable />
                <el-button type="primary" plain :disabled="cooldownLogin > 0 || !form.email" @click="handleSendLoginCode">
                  {{ cooldownLogin > 0 ? text.resendAfter(cooldownLogin) : text.sendCode }}
                </el-button>
              </div>
            </el-form-item>
          </template>
          <el-form-item>
            <el-button type="primary" class="login-btn" @click="onPrimaryLogin">{{ text.login }}</el-button>
          </el-form-item>
          <div class="extra-links">
            <a class="signup-link" href="#" @click.prevent="openForgot">{{ text.forgot }}</a>
            <span class="link-sep">|</span>
            <a class="signup-link" href="#" @click.prevent="openChange">{{ text.changePwd }}</a>
          </div>
        </el-form>

        <!-- 注册 -->
        <el-form v-else :model="form" class="login-form">
          <el-form-item>
            <el-input v-model="form.username" :placeholder="text.username" clearable maxlength="8" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.email" :placeholder="text.email" clearable />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" :placeholder="text.password" show-password clearable />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.confirm" :placeholder="text.confirm" show-password clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" @click="onRegister">{{ text.register }}</el-button>
          </el-form-item>
        </el-form>

        <div class="signup-tip">
          <span v-if="isLogin">
            {{ text.noAccount }}
            <a class="signup-link" @click="isLogin = false">{{ text.toRegister }}</a>
          </span>
          <span v-else>
            {{ text.hasAccount }}
            <a class="signup-link" @click="isLogin = true">{{ text.toLogin }}</a>
          </span>
        </div>
      </div>
    </div>

    <el-dialog v-model="showForgot" :title="text.forgotTitle" width="420px" destroy-on-close @closed="resetForgot">
      <el-form label-position="top">
        <el-form-item :label="text.emailShort">
          <el-input v-model="forgot.email" clearable />
        </el-form-item>
        <el-form-item :label="text.verifyCode">
          <div class="code-row">
            <el-input v-model="forgot.code" maxlength="6" clearable />
            <el-button type="primary" plain :disabled="cooldownForgot > 0 || !forgot.email" @click="handleSendResetCode">
              {{ cooldownForgot > 0 ? text.resendAfter(cooldownForgot) : text.sendCode }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item :label="text.newPassword">
          <el-input v-model="forgot.newPassword" show-password clearable />
        </el-form-item>
        <el-form-item :label="text.confirmNew">
          <el-input v-model="forgot.confirm" show-password clearable />
        </el-form-item>
        <el-button type="primary" class="login-btn" @click="submitForgot">{{ text.submitReset }}</el-button>
      </el-form>
    </el-dialog>

    <el-dialog v-model="showChange" :title="text.changeTitle" width="420px" destroy-on-close @closed="resetChange">
      <el-form label-position="top">
        <el-form-item :label="text.emailShort">
          <el-input v-model="changeForm.email" clearable />
        </el-form-item>
        <el-form-item :label="text.oldPassword">
          <el-input v-model="changeForm.oldPassword" show-password clearable />
        </el-form-item>
        <el-form-item :label="text.newPassword">
          <el-input v-model="changeForm.newPassword" show-password clearable />
        </el-form-item>
        <el-form-item :label="text.confirmNew">
          <el-input v-model="changeForm.confirm" show-password clearable />
        </el-form-item>
        <el-button type="primary" class="login-btn" @click="submitChange">{{ text.submitChange }}</el-button>
      </el-form>
    </el-dialog>

    <div style="position: absolute; left: 40px; top: 30px; z-index: 10; display: flex; align-items: center">
      <el-switch
        v-model="langSwitch"
        size="large"
        :active-value="'en'"
        :inactive-value="'zh'"
        style="--el-switch-on-color: #409eff; --el-switch-off-color: #409eff"
        active-text="English"
        inactive-text="中文"
      />
    </div>
    <div
      style="
        cursor: pointer;
        position: absolute;
        right: 40px;
        top: 35px;
        z-index: 10;
        display: flex;
        align-items: center;
        justify-content: center;
        height: 30px;
        width: 120px;
        border-radius: 25px;
        background-color: rgb(236, 245, 255, 0.9);
        border: 2px solid #409eff;
      "
      @click="VisiterLogin"
    >
      <i class="iconfont icon-iconfonthuiyuan" style="font-size: 25px; color: #409eff; position: relative; top: 2.5px; left: -6px; cursor: pointer" @click="VisiterLogin"></i>
      <span style="position: relative; top: -1px; font-size: 16px; color: #409eff; cursor: pointer" @click="VisiterLogin">{{ lang === "zh" ? "游客" : "Guest" }}</span>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, onUnmounted } from "vue";
import { ElMessage } from "element-plus";
import api from "../api/index";
import { useRouter } from "vue-router";
import { getAuth, setAuth, removeAuth } from "../utils/authStorage";

const validateEmail = (email) => /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(email);
const validatePassword = (password) => /^(?=.*[A-Za-z])(?=.*\d)[\w!@#$%^&*()_+\-=.]{8,}$/.test(password);
const validateUsername = (username) => username.length <= 8 && /^[a-zA-Z0-9_]+$/.test(username);

export default {
  name: "LoginView",
  setup() {
    const isLogin = ref(true);
    const loginMode = ref("password");
    const form = ref({
      username: "",
      email: "",
      password: "",
      confirm: "",
      code: "",
    });
    const showForgot = ref(false);
    const showChange = ref(false);
    const forgot = ref({ email: "", code: "", newPassword: "", confirm: "" });
    const changeForm = ref({ email: "", oldPassword: "", newPassword: "", confirm: "" });
    const cooldownLogin = ref(0);
    const cooldownForgot = ref(0);
    let timerLogin = null;
    let timerForgot = null;

    const lang = ref(localStorage.getItem("lang") || "zh");
    const langSwitch = computed({
      get: () => lang.value,
      set: (v) => (lang.value = v),
    });
    watch(lang, (val) => localStorage.setItem("lang", val));

    const text = computed(() => {
      const zh = {
        title: "涉外案例查询分析系统",
        welcome: isLogin.value ? "用户登录" : "用户注册",
        registerTitle: "用户注册",
        username: "用户名",
        email: "注册邮箱",
        emailShort: "邮箱",
        password: "密码",
        confirm: "确定密码",
        login: "登录",
        register: "用户注册",
        noAccount: "还没有账号 ?",
        toRegister: "去注册",
        hasAccount: "已有账号 ?",
        toLogin: "去登录",
        emailError: "请输入正确的邮箱格式",
        pwdNotMatch: "两次密码不一致",
        pwdWeak: "密码需至少8位且包含字母和数字",
        regSuccess: "注册成功",
        loginSuccess: "登录成功",
        loginFail: "登录失败，请检查邮箱和密码",
        inputLogin: "请输入邮箱和密码",
        inputCodeLogin: "请输入邮箱和验证码",
        usernameInvalid: "用户名长度不超过8字符,且只能包含字母、数字和下划线",
        usernameRequired: "请输入用户名",
        modePassword: "密码登录",
        modeCode: "验证码登录",
        sendCode: "获取验证码",
        resendAfter: (s) => `${s}s`,
        codePlaceholder: "6位验证码",
        forgot: "忘记密码",
        changePwd: "修改密码",
        forgotTitle: "忘记密码",
        changeTitle: "修改密码",
        verifyCode: "邮箱验证码",
        newPassword: "新密码",
        oldPassword: "当前密码",
        confirmNew: "确认新密码",
        submitReset: "重置密码",
        submitChange: "确认修改",
        codeSent: "验证码已发送",
        resetOk: "密码已重置，请使用新密码登录",
        changeOk: "密码已修改，请使用新密码登录",
        devCodeHint: "（开发模式）验证码：",
      };
      const en = {
        title: "Foreign Case System",
        welcome: isLogin.value ? "User Login" : "Sign up",
        registerTitle: "Sign up",
        username: "Username",
        email: "Email",
        emailShort: "Email",
        password: "Password",
        confirm: "Confirm Password",
        login: "Login",
        register: "Sign up",
        noAccount: "No account yet?",
        toRegister: "Sign up",
        hasAccount: "Already have an account?",
        toLogin: "Login",
        emailError: "Please enter a valid email",
        pwdNotMatch: "Passwords do not match",
        pwdWeak: "Password must be at least 8 characters and contain letters and numbers",
        regSuccess: "Registration successful",
        loginSuccess: "Login successful",
        loginFail: "Login failed, please check your email and password",
        inputLogin: "Please enter email and password",
        inputCodeLogin: "Please enter email and verification code",
        usernameInvalid: "Username must be max 8 characters and contain only letters, numbers and underscores",
        usernameRequired: "Please enter username",
        modePassword: "Password",
        modeCode: "Verification code",
        sendCode: "Send code",
        resendAfter: (s) => `${s}s`,
        codePlaceholder: "6-digit code",
        forgot: "Forgot password",
        changePwd: "Change password",
        forgotTitle: "Forgot password",
        changeTitle: "Change password",
        verifyCode: "Verification code",
        newPassword: "New password",
        oldPassword: "Current password",
        confirmNew: "Confirm new password",
        submitReset: "Reset password",
        submitChange: "Update password",
        codeSent: "Code sent",
        resetOk: "Password reset. Sign in with your new password.",
        changeOk: "Password updated. Sign in with your new password.",
        devCodeHint: "(Dev) Code:",
      };
      return lang.value === "zh" ? zh : en;
    });

    const router = useRouter();

    const applySession = (response, email) => {
      ElMessage.success(text.value.loginSuccess);
      const username = response.data.username || " ";
      const userId = response.data.userId;
      setAuth("username", username);
      setAuth("userId", userId);
      setAuth("userEmail", email);
      setAuth("token", "true");
      if (response.data.summaryCredits != null && response.data.summaryCredits !== undefined) {
        setAuth("summaryCredits", String(response.data.summaryCredits));
      } else {
        removeAuth("summaryCredits");
      }
      router.push("/case-query");
    };

    const startCooldown = (which) => {
      if (which === "login") {
        if (timerLogin) clearInterval(timerLogin);
        timerLogin = setInterval(() => {
          cooldownLogin.value--;
          if (cooldownLogin.value <= 0) {
            clearInterval(timerLogin);
            timerLogin = null;
          }
        }, 1000);
      } else {
        if (timerForgot) clearInterval(timerForgot);
        timerForgot = setInterval(() => {
          cooldownForgot.value--;
          if (cooldownForgot.value <= 0) {
            clearInterval(timerForgot);
            timerForgot = null;
          }
        }, 1000);
      }
    };

    const handleSendLoginCode = async () => {
      if (!validateEmail(form.value.email)) {
        ElMessage.warning(text.value.emailError);
        return;
      }
      const r = await api.sendAuthCode(form.value.email, "LOGIN");
      if (r.code === 200) {
        ElMessage.success(text.value.codeSent);
        if (r.data?.devCode) {
          ElMessage.info(`${text.value.devCodeHint} ${r.data.devCode}`);
        }
        const sec = r.data?.cooldownSeconds ?? 60;
        cooldownLogin.value = sec;
        startCooldown("login");
      }
    };

    const handleSendResetCode = async () => {
      if (!validateEmail(forgot.value.email)) {
        ElMessage.warning(text.value.emailError);
        return;
      }
      const r = await api.sendAuthCode(forgot.value.email, "RESET");
      if (r.code === 200) {
        ElMessage.success(text.value.codeSent);
        if (r.data?.devCode) {
          ElMessage.info(`${text.value.devCodeHint} ${r.data.devCode}`);
        }
        const sec = r.data?.cooldownSeconds ?? 60;
        cooldownForgot.value = sec;
        startCooldown("forgot");
      }
    };

    const onPrimaryLogin = async () => {
      if (loginMode.value === "password") {
        if (!form.value.email || !form.value.password) {
          ElMessage.warning(text.value.inputLogin);
          return;
        }
        try {
          const response = await api.login(form.value.email, form.value.password);
          if (response.code === 200) {
            applySession(response, form.value.email);
          }
        } catch (error) {
          const msg =
            error?.response?.data?.message ||
            error?.serverMessage ||
            error?.message ||
            text.value.loginFail;
          ElMessage.error(msg);
        }
      } else {
        if (!form.value.email || !form.value.code) {
          ElMessage.warning(text.value.inputCodeLogin);
          return;
        }
        try {
          const response = await api.loginByCode(form.value.email, form.value.code.trim());
          if (response.code === 200) {
            applySession(response, form.value.email);
          }
        } catch (error) {
          const msg = error?.response?.data?.message || error?.serverMessage || error?.message || text.value.loginFail;
          ElMessage.error(msg);
        }
      }
    };

    const openForgot = () => {
      forgot.value.email = form.value.email || "";
      showForgot.value = true;
    };

    const openChange = () => {
      changeForm.value.email = form.value.email || getAuthEmail();
      showChange.value = true;
    };

    const getAuthEmail = () => getAuth("userEmail") || "";

    const resetForgot = () => {
      forgot.value = { email: "", code: "", newPassword: "", confirm: "" };
      cooldownForgot.value = 0;
    };

    const resetChange = () => {
      changeForm.value = { email: "", oldPassword: "", newPassword: "", confirm: "" };
    };

    const submitForgot = async () => {
      if (!validateEmail(forgot.value.email)) {
        ElMessage.warning(text.value.emailError);
        return;
      }
      if (!forgot.value.code || !forgot.value.newPassword) {
        ElMessage.warning(lang.value === "zh" ? "请填写验证码和新密码" : "Enter code and new password");
        return;
      }
      if (forgot.value.newPassword !== forgot.value.confirm) {
        ElMessage.error(text.value.pwdNotMatch);
        return;
      }
      if (!validatePassword(forgot.value.newPassword)) {
        ElMessage.error(text.value.pwdWeak);
        return;
      }
      const r = await api.resetPasswordByCode(forgot.value.email, forgot.value.code.trim(), forgot.value.newPassword);
      if (r.code === 200) {
        ElMessage.success(text.value.resetOk);
        showForgot.value = false;
        isLogin.value = true;
        loginMode.value = "password";
        form.value.email = forgot.value.email;
        form.value.password = "";
      }
    };

    const submitChange = async () => {
      if (!validateEmail(changeForm.value.email)) {
        ElMessage.warning(text.value.emailError);
        return;
      }
      if (!changeForm.value.oldPassword || !changeForm.value.newPassword) {
        ElMessage.warning(lang.value === "zh" ? "请填写完整" : "Fill all fields");
        return;
      }
      if (changeForm.value.newPassword !== changeForm.value.confirm) {
        ElMessage.error(text.value.pwdNotMatch);
        return;
      }
      if (!validatePassword(changeForm.value.newPassword)) {
        ElMessage.error(text.value.pwdWeak);
        return;
      }
      const r = await api.changePasswordApi(changeForm.value.email, changeForm.value.oldPassword, changeForm.value.newPassword);
      if (r.code === 200) {
        ElMessage.success(text.value.changeOk);
        showChange.value = false;
        form.value.email = changeForm.value.email;
        form.value.password = "";
      }
    };

    const VisiterLogin = () => {
      setAuth("username", "游客");
      setAuth("userId", "0");
      removeAuth("userEmail");
      removeAuth("summaryCredits");
      setAuth("token", "true");
      router.push("/case-query");
    };

    const onRegister = async () => {
      if (!form.value.username) {
        ElMessage.error(text.value.usernameRequired);
        return;
      }
      if (!validateUsername(form.value.username)) {
        ElMessage.error(text.value.usernameInvalid);
        return;
      }
      if (!validateEmail(form.value.email)) {
        ElMessage.error(text.value.emailError);
        return;
      }
      if (form.value.password !== form.value.confirm) {
        ElMessage.error(text.value.pwdNotMatch);
        return;
      }
      if (!validatePassword(form.value.password)) {
        ElMessage.error(text.value.pwdWeak);
        return;
      }
      try {
        const response = await api.register(form.value.username, form.value.email, form.value.password);
        if (response.code === 200) {
          ElMessage.success(text.value.regSuccess);
          form.value.email = "";
          form.value.password = "";
          form.value.confirm = "";
          isLogin.value = true;
        } else {
          ElMessage.error(response.message || (lang.value === "zh" ? "注册失败" : "Registration failed"));
        }
      } catch (error) {
        const msg =
          error?.serverMessage ||
          error?.response?.data?.message ||
          error?.message ||
          (lang.value === "zh" ? "注册失败" : "Registration failed");
        ElMessage.error(msg);
      }
    };

    onUnmounted(() => {
      if (timerLogin) clearInterval(timerLogin);
      if (timerForgot) clearInterval(timerForgot);
    });

    return {
      isLogin,
      loginMode,
      form,
      showForgot,
      showChange,
      forgot,
      changeForm,
      cooldownLogin,
      cooldownForgot,
      onPrimaryLogin,
      onRegister,
      VisiterLogin,
      lang,
      langSwitch,
      text,
      handleSendLoginCode,
      handleSendResetCode,
      openForgot,
      openChange,
      submitForgot,
      submitChange,
      resetForgot,
      resetChange,
    };
  },
};
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  background: linear-gradient(to bottom right, rgb(121, 187, 255), rgb(217, 236, 255), rgb(217, 236, 255), rgb(121, 187, 255), rgb(217, 236, 255));
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.center-box {
  width: 100vw;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding-right: 248px;
  z-index: 2;
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 2px 24px 0 rgba(60, 60, 60, 0.1);
  padding: 40px 32px 32px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
  position: relative;
}
.welcome {
  font-size: 1.6rem;
  font-weight: 600;
  margin-bottom: 18px;
  color: #595959;
  letter-spacing: 1px;
}
.login-mode-row {
  width: 100%;
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}
.login-form {
  width: 100%;
}
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
  align-items: center;
}
.code-row .el-input {
  flex: 1;
}
.login-btn {
  width: 100%;
  font-size: 1.1rem;
  letter-spacing: 1px;
  background: #409eff;
  border: none;
}
.extra-links {
  margin-top: 8px;
  text-align: center;
  font-size: 0.95rem;
  color: #666;
}
.link-sep {
  margin: 0 8px;
  color: #ccc;
}
.signup-tip {
  margin-top: 18px;
  color: #888;
  font-size: 0.98rem;
  text-align: center;
}
.signup-link {
  color: #409eff;
  margin-left: 4px;
  text-decoration: underline;
  cursor: pointer;
}
@media (max-width: 900px) {
  .center-box {
    width: 100vw;
  }
  .login-card {
    margin: 0 auto;
  }
}
.gradient-text {
  background: linear-gradient(to right bottom, #0c6fff, #b6fbff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  color: transparent;
}
</style>
