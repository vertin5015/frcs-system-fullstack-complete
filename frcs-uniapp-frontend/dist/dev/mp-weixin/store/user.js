"use strict";
const common_vendor = require("../common/vendor.js");
const utils_storage = require("../utils/storage.js");
const TOKEN_KEY = "frcs_token";
const USER_KEY = "frcs_user";
const useUserStore = common_vendor.defineStore("user", () => {
  const token = common_vendor.ref(utils_storage.storage.get(TOKEN_KEY) || "");
  const userInfo = common_vendor.ref(utils_storage.storage.get(USER_KEY));
  const isLoggedIn = common_vendor.computed(() => Boolean(token.value));
  function setLogin(sessionToken, user) {
    token.value = sessionToken;
    userInfo.value = user;
    utils_storage.storage.set(TOKEN_KEY, sessionToken);
    utils_storage.storage.set(USER_KEY, user);
  }
  function logout() {
    token.value = "";
    userInfo.value = null;
    utils_storage.storage.remove(TOKEN_KEY);
    utils_storage.storage.remove(USER_KEY);
  }
  return { token, userInfo, isLoggedIn, setLogin, logout };
});
exports.useUserStore = useUserStore;
