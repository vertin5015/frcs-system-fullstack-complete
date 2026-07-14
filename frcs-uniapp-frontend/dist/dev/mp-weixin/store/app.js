"use strict";
const common_vendor = require("../common/vendor.js");
const useAppStore = common_vendor.defineStore("app", () => {
  const currentPage = common_vendor.ref("home");
  const systemConfig = common_vendor.ref({ systemName: "涉外案例查询与法律知识系统" });
  function initialize() {
  }
  function setCurrentPage(page) {
    currentPage.value = page;
  }
  return { currentPage, systemConfig, initialize, setCurrentPage };
});
exports.useAppStore = useAppStore;
