/**
 * 从后端既有的 /api/cases/original-proxy 获取原文正文段落。
 *
 * 该接口在旧/新后端都存在，只负责抓取并展示原文（可读 HTML），不是后端翻译。
 * 这样译文功能可以不依赖后端新增的任何接口：正文仍走原有代理，翻译在前端完成。
 */
import { paths } from '../api/path';

const POLL_INTERVAL_MS = 3000;
const paragraphCache = new Map(); // url -> paragraphs，避免同一案例重复轮询

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export function originalProxyUrl(rawUrl, retry) {
  const u = String(rawUrl || '').trim();
  if (!u) return '';
  const query = new URLSearchParams();
  query.set('url', u);
  if (retry) query.set('retry', 'true');
  return `${paths.originalProxy}?${query.toString()}`;
}

function parseArticleParagraphs(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const article = doc.querySelector('article');
  if (!article) return null; // 非正文页（正在抓取/占位页等）

  const paragraphs = [];
  let buffer = [];
  const flush = () => {
    const joined = buffer
      .map((s) => String(s || '').replace(/\s+/g, ' ').trim())
      .filter(Boolean)
      .join(' ')
      .trim();
    if (joined) paragraphs.push(joined);
    buffer = [];
  };

  // 后端可读页里：同一自然段的多行是连续的 p/h4，段与段之间用空 div 分隔
  for (const node of article.children) {
    const tag = (node.tagName || '').toUpperCase();
    const text = (node.textContent || '').replace(/\s+/g, ' ').trim();
    if (tag === 'DIV') {
      if (text) buffer.push(text); // 有内容的 div 也当作正文行
      else flush(); // 空 div 视为段落分隔
      continue;
    }
    if (/^H[1-6]$/.test(tag) || tag === 'P' || tag === 'LI') {
      if (text) buffer.push(text);
      continue;
    }
    if (text) buffer.push(text);
  }
  flush();
  return paragraphs;
}

/**
 * 获取原文段落；后端还没抓到正文时按 3s 间隔轮询，直到成功或超时。
 *
 * @returns {Promise<{ok:boolean, paragraphs?:string[], error?:string, message?:string}>}
 *   error: no-url | http | loading-timeout | unavailable | empty
 */
export async function fetchOriginalParagraphs(rawUrl, options = {}) {
  const timeoutMs = options.timeoutMs || 90 * 1000;
  const onStatus = typeof options.onStatus === 'function' ? options.onStatus : () => {};
  const url = String(rawUrl || '').trim();
  if (!url) {
    return { ok: false, error: 'no-url', message: '缺少原始文书链接' };
  }

  const cached = paragraphCache.get(url);
  if (cached) {
    return { ok: true, paragraphs: cached };
  }

  const deadline = Date.now() + timeoutMs;
  let attempt = 0;
  let usedRetry = false;
  while (Date.now() < deadline) {
    attempt++;
    onStatus(`正在获取原文正文（第 ${attempt} 次）…`);
    let html = '';
    try {
      const res = await fetch(originalProxyUrl(url, usedRetry), { cache: 'no-store' });
      if (!res.ok) {
        return { ok: false, error: 'http', message: `原文代理请求失败（HTTP ${res.status}）` };
      }
      html = await res.text();
    } catch (err) {
      return { ok: false, error: 'http', message: '原文代理请求失败，请检查网络或后端服务' };
    }

    const paragraphs = parseArticleParagraphs(html);
    if (paragraphs) {
      if (paragraphs.length > 0) {
        paragraphCache.set(url, paragraphs);
        return { ok: true, paragraphs };
      }
      return { ok: false, error: 'empty', message: '后端返回的原文正文为空，暂时无法翻译' };
    }

    const looksUnavailable =
      html.includes('原文暂时无法在窗口内显示') || html.includes('原文加载失败');
    if (looksUnavailable) {
      if (!usedRetry) {
        usedRetry = true; // 清除后端记录的旧失败，重新触发抓取
        continue;
      }
      return {
        ok: false,
        error: 'unavailable',
        message: '后端暂时未能抓取到该原文（目标站点可能拒绝访问），可稍后重试',
      };
    }

    const looksLoading =
      html.includes('原文正在抓取') || /<meta[^>]+http-equiv=["']refresh["']/i.test(html);
    if (!looksLoading) {
      return {
        ok: false,
        error: 'http',
        message: '后端返回了无法识别的页面，无法提取原文正文',
      };
    }

    await sleep(Math.min(POLL_INTERVAL_MS, Math.max(500, deadline - Date.now())));
  }

  return {
    ok: false,
    error: 'loading-timeout',
    message: '等待后端抓取原文超时，请先在左侧「官方原文」打开一次再切换译文',
  };
}
