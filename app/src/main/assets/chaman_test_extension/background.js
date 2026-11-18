// CHAMAN Cookie Export - background script.
// No content script and no page-visible UI: only the native app can trigger an export,
// over the native port. Cookie values are only read at export time and go straight
// to the Android app. They are never logged or sent to any network.
const NATIVE_APP = "chaman.browser";
const DOMAIN_RE = /^[a-z0-9.-]+$/i;

let port = null;
let helloTimer = null;

function connect() {
  if (port) return;
  let p;
  try {
    p = browser.runtime.connectNative(NATIVE_APP);
  } catch (_) {
    setTimeout(connect, 2000);
    return;
  }
  port = p;
  p.onMessage.addListener(onNativeMessage);
  p.onDisconnect.addListener(() => {
    clearTimeout(helloTimer);
    if (port === p) port = null;
    setTimeout(connect, 2000);
  });
  // Handshake: the app answers READY. If it never does (delegate not set yet), retry.
  helloTimer = setTimeout(() => {
    if (port === p) {
      port = null;
      try { p.disconnect(); } catch (_) {}
      setTimeout(connect, 1000);
    }
  }, 3000);
  p.postMessage({ type: "HELLO" });
}

function onNativeMessage(msg) {
  if (!msg || typeof msg !== "object") return;
  if (msg.type === "READY") {
    clearTimeout(helloTimer);
    return;
  }
  if (msg.type === "EXPORT_COOKIES") {
    exportCookies(msg);
  }
}

function cleanField(value) {
  return String(value).replace(/[\t\r\n]/g, "");
}

function toNetscapeLine(c) {
  const includeSub = !c.hostOnly;
  let domain = c.domain;
  if (includeSub && !domain.startsWith(".")) domain = "." + domain;
  if (c.httpOnly) domain = "#HttpOnly_" + domain;
  const expiry = (c.session || !c.expirationDate) ? 0 : Math.floor(c.expirationDate);
  return [
    domain,
    includeSub ? "TRUE" : "FALSE",
    c.path || "/",
    c.secure ? "TRUE" : "FALSE",
    expiry,
    cleanField(c.name),
    cleanField(c.value)
  ].join("\t");
}

async function exportCookies(msg) {
  const site = String(msg.site || "");
  try {
    const domains = Array.isArray(msg.domains) ? msg.domains : [];
    if (!domains.length || !domains.every((d) => typeof d === "string" && DOMAIN_RE.test(d))) {
      throw new Error("bad domains");
    }
    const unique = new Map();
    for (const domain of domains) {
      const list = await browser.cookies.getAll({ domain });
      for (const c of list) {
        if (!c.name) continue;
        unique.set(`${c.domain}|${c.path}|${c.name}|${c.storeId}`, c);
      }
    }
    const lines = Array.from(unique.values())
      .sort((a, b) => (a.domain + a.name).localeCompare(b.domain + b.name))
      .map(toNetscapeLine);
    const text = "# Netscape HTTP Cookie File\n" + lines.join("\n") + (lines.length ? "\n" : "");
    port.postMessage({ type: "COOKIES_TXT", site, count: lines.length, text });
  } catch (error) {
    if (port) port.postMessage({ type: "COOKIES_ERROR", site, error: String(error) });
  }
}

connect();
