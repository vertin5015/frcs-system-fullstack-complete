/**
 * 登录态同时写入 sessionStorage 与 localStorage。
 * sessionStorage 随标签页隔离；新标签页打开 /case-reader 时读不到原标签的 sessionStorage，
 * 因此用 localStorage 同步，关闭浏览器后清除需在登出时 clearAllAuth。
 */
const AUTH_KEYS = ["token", "userId", "username", "userEmail", "summaryCredits"];

export function getAuth(key) {
  const a = sessionStorage.getItem(key);
  if (a !== null && a !== "") return a;
  return localStorage.getItem(key);
}

export function setAuth(key, value) {
  if (value === null || value === undefined || value === "") {
    removeAuth(key);
    return;
  }
  const s = String(value);
  sessionStorage.setItem(key, s);
  localStorage.setItem(key, s);
}

export function removeAuth(key) {
  sessionStorage.removeItem(key);
  localStorage.removeItem(key);
}

export function clearAllAuth() {
  AUTH_KEYS.forEach(removeAuth);
}

export function isAuthenticated() {
  return getAuth("token") === "true";
}

/**
 * 旧版曾只把登录态写在 sessionStorage；在已登录页刷新一次即可把同套数据写到 localStorage，
 * 之后新开的案件阅读页等标签才能通过 getAuth 读到登录态。
 */
export function syncSessionAuthToLocal() {
  if (sessionStorage.getItem("token") !== "true") {
    return;
  }
  AUTH_KEYS.forEach((k) => {
    const v = sessionStorage.getItem(k);
    if (v != null) {
      localStorage.setItem(k, v);
    }
  });
}
