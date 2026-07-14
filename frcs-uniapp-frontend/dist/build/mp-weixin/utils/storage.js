"use strict";const e=require("../common/vendor.js"),t={get:t=>e.index.getStorageSync(t)||null,set(t,o){e.index.setStorageSync(t,o)},remove(t){e.index.removeStorageSync(t)}};exports.storage=t;
