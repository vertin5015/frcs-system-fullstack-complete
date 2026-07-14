"use strict";
const common_vendor = require("../common/vendor.js");
const storage = {
  get(key) {
    return common_vendor.index.getStorageSync(key) || null;
  },
  set(key, value) {
    common_vendor.index.setStorageSync(key, value);
  },
  remove(key) {
    common_vendor.index.removeStorageSync(key);
  }
};
exports.storage = storage;
