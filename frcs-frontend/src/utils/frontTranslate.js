/**
 * 前端直连第三方翻译 API（不经过后端翻译接口）。
 *
 * 原文正文仍通过后端既有的 /api/cases/original-proxy 取回（旧后端也有该接口，
 * 它只负责抓取/展示原文，不是后端翻译）；翻译请求全部由浏览器直接发给第三方接口。
 *
 * 默认翻译源（免费、免密钥）：
 *   1. google   —— translate.googleapis.com/translate_a/single（质量较好）
 *   2. mymemory —— api.mymemory.translated.net（CORS 开放，作为兜底）
 * 某个源超时/限流/不可达时自动切下一个；全部失败时由调用方决定保留原文或提示重试。
 * 若以后要接带密钥的服务（百度/有道/DeepL/OpenAI 兼容等），
 * 在 PROVIDERS 里加一个函数并调整 PROVIDER_ORDER 即可，无需改页面代码。
 */

const REQUEST_TIMEOUT_MS = 12000; // 单个翻译源超时，快速切换备用源
const MAX_CHUNK_CHARS = 1200; // 单次请求最大字符数，过长段落先按句子边界拆分
const MAX_TOTAL_CHARS = 30000; // 整篇参与翻译的最大字符数，防止请求过多
const PROVIDER_ORDER = ['google', 'mymemory'];
const PROVIDER_DOWN_MS = 120000; // 某源连续失败后的熔断时间，避免每段都等超时

const PROVIDERS = {};
const RESULT_CACHE = new Map();
const providerFailStreak = {};
const providerDownUntil = {};

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function fetchWithTimeout(url, options) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    return await fetch(url, { ...(options || {}), signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

/**
 * 粗识别正文语言：zh / ja / en / other。
 * 用于“原文已是目标语言则不再调用翻译”的判断，以及 MyMemory 的源语言参数。
 */
export function detectTextLanguage(text) {
  const sample = String(text || '').slice(0, 6000);
  let letters = 0;
  let cjk = 0;
  let kana = 0;
  let latinAccent = 0;
  for (const ch of sample) {
    const code = ch.codePointAt(0);
    const isCjk = (code >= 0x3400 && code <= 0x4dbf) || (code >= 0x4e00 && code <= 0x9fff);
    const isKana = code >= 0x3040 && code <= 0x30ff;
    const isAccent = code >= 0x00c0 && code <= 0x024f;
    if (!isCjk && !isKana && !isAccent && !(code >= 0x41 && code <= 0x7a)) continue;
    letters++;
    if (isCjk) cjk++;
    else if (isKana) kana++;
    else if (isAccent) latinAccent++;
  }
  if (!letters) return 'other';
  if (kana / letters > 0.05) return 'ja';
  if (cjk / letters > 0.35) return 'zh';
  if (latinAccent / letters > 0.02) return 'other'; // 法语/德语等拉丁文变音较多，交给 auto 识别
  return 'en';
}

async function googleTranslate(text, target) {
  const tl = target === 'zh' ? 'zh-CN' : 'en';
  const url =
    'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' +
    tl +
    '&dt=t&q=' +
    encodeURIComponent(text);
  const res = await fetchWithTimeout(url);
  if (!res.ok) throw new Error('google http ' + res.status);
  const data = await res.json();
  const rows = Array.isArray(data) ? data[0] : null;
  if (!Array.isArray(rows)) throw new Error('google 响应格式异常');
  const out = rows
    .map((row) => (Array.isArray(row) && row[0]) || '')
    .join('')
    .trim();
  if (!out) throw new Error('google 空结果');
  return out;
}

async function myMemoryTranslate(text, target) {
  const src = detectTextLanguage(text);
  const sourceCode = src === 'zh' ? 'zh-CN' : src === 'ja' ? 'ja' : 'en';
  const targetCode = target === 'zh' ? 'zh-CN' : 'en-US';
  const body = new URLSearchParams();
  body.set('q', text);
  body.set('langpair', sourceCode + '|' + targetCode);
  const res = await fetchWithTimeout('https://api.mymemory.translated.net/get', {
    method: 'POST',
    // application/x-www-form-urlencoded 属于 CORS 安全类型，不会触发预检
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
    body: body.toString(),
  });
  if (!res.ok) throw new Error('mymemory http ' + res.status);
  const data = await res.json();
  if (data && data.quotaFinished) throw new Error('mymemory 当日配额已用完');
  if (!data || !data.responseData || data.responseStatus !== 200) {
    throw new Error('mymemory 响应异常');
  }
  const out = String(data.responseData.translatedText || '').trim();
  if (!out) throw new Error('mymemory 空结果');
  return out;
}

PROVIDERS.google = googleTranslate;
PROVIDERS.mymemory = myMemoryTranslate;

async function translateChunk(text, target) {
  const cacheKey = target + '|' + text;
  const hit = RESULT_CACHE.get(cacheKey);
  if (hit) return hit;
  let lastError = null;
  for (const name of PROVIDER_ORDER) {
    const fn = PROVIDERS[name];
    if (!fn) continue;
    if (providerDownUntil[name] && Date.now() < providerDownUntil[name]) continue; // 熔断中
    try {
      const out = await fn(text, target);
      providerFailStreak[name] = 0;
      providerDownUntil[name] = 0;
      if (RESULT_CACHE.size > 1200) RESULT_CACHE.clear();
      RESULT_CACHE.set(cacheKey, out);
      return out;
    } catch (err) {
      lastError = err;
      providerFailStreak[name] = (providerFailStreak[name] || 0) + 1;
      const msg = String((err && err.message) || '');
      const fatalNetwork =
        (err && err.name === 'AbortError') ||
        /failed to fetch|network error|fetch failed|networkrequestfailed/i.test(msg);
      if (fatalNetwork || providerFailStreak[name] >= 2) {
        providerDownUntil[name] = Date.now() + PROVIDER_DOWN_MS;
      }
    }
  }
  throw lastError || new Error('没有可用的翻译源');
}

function sentenceEndOffset(s) {
  const m = /[.!?。！？…]["')\]]?\s*$/.exec(s);
  return m ? m.index + m[0].length : -1;
}

/** 段落过长时按句子边界拆成 <= maxChars 的小块 */
function splitIntoChunks(text, maxChars) {
  const source = String(text || '').trim();
  if (!source) return [];
  if (source.length <= maxChars) return [source];
  const chunks = [];
  let start = 0;
  while (start < source.length) {
    let end = Math.min(start + maxChars, source.length);
    if (end < source.length) {
      const slice = source.slice(start, end);
      const cut = sentenceEndOffset(slice);
      if (cut >= maxChars * 0.4) {
        end = start + cut;
      } else {
        const ws = slice.lastIndexOf(' ');
        if (ws > maxChars * 0.4) end = start + ws;
      }
    }
    const piece = source.slice(start, end).trim();
    if (piece) chunks.push(piece);
    start = Math.max(end, start + 1);
  }
  return chunks;
}

async function mapLimit(items, limit, fn) {
  const results = new Array(items.length);
  let cursor = 0;
  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (cursor < items.length) {
      const idx = cursor++;
      results[idx] = await fn(items[idx], idx);
    }
  });
  await Promise.all(workers);
  return results;
}

/**
 * 将段落数组翻译成目标语言（zh / en）。
 *
 * @returns {Promise<{content:string, truncated:boolean, failedCount:number, anyProviderUsed:boolean}>}
 */
export async function translateParagraphs(paragraphs, target) {
  const output = new Array(paragraphs.length);
  const tasks = [];
  let totalChars = 0;
  let skippedAfterCap = false;
  for (let i = 0; i < paragraphs.length; i++) {
    const text = String(paragraphs[i] || '').trim();
    if (!text) {
      output[i] = '';
      continue;
    }
    if (totalChars + text.length > MAX_TOTAL_CHARS) {
      skippedAfterCap = true;
      output[i] = '';
      continue;
    }
    totalChars += text.length;
    tasks.push({ index: i, text, chunks: splitIntoChunks(text, MAX_CHUNK_CHARS) });
  }

  let failedCount = 0;
  let anyProviderUsed = false;
  await mapLimit(tasks, 3, async (task) => {
    const pieces = [];
    for (const chunk of task.chunks) {
      try {
        pieces.push(await translateChunk(chunk, target));
        anyProviderUsed = true;
      } catch (err) {
        failedCount++;
        pieces.push(chunk); // 该块翻译失败时保留原文，避免整篇不可读
      }
      await sleep(80); // 轻微限速，降低被翻译源限流的概率
    }
    output[task.index] = pieces.join(' ').trim();
  });

  const content = output
    .map((t) => String(t || '').trim())
    .filter((t) => t.length > 0)
    .join('\n\n');
  return {
    content,
    truncated: skippedAfterCap,
    failedCount,
    anyProviderUsed,
  };
}
