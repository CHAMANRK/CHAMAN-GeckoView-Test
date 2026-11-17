browser.runtime.onMessage.addListener(async (message) => {
  if (!message || message.type !== "CHAMAN_COUNT_COOKIES" || !message.url) {
    return;
  }

  try {
    const cookies = await browser.cookies.getAll({ url: message.url });
    // Privacy-by-design: only the count and host are sent to Android.
    // Cookie names, values, domains and session tokens are never transferred.
    const host = new URL(message.url).hostname;
    await browser.runtime.sendNativeMessage("chaman.browser", {
      type: "CHAMAN_COOKIE_COUNT",
      host,
      count: cookies.length
    });
    return { ok: true, count: cookies.length };
  } catch (error) {
    await browser.runtime.sendNativeMessage("chaman.browser", {
      type: "CHAMAN_COOKIE_ERROR",
      error: String(error)
    });
    return { ok: false };
  }
});
