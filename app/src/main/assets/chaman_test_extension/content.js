(() => {
  if (window.__chamanCookieBridgeTestAdded) return;
  window.__chamanCookieBridgeTestAdded = true;

  const button = document.createElement("button");
  button.textContent = "Test Chaman Cookie Bridge";
  button.setAttribute("type", "button");
  Object.assign(button.style, {
    position: "fixed",
    zIndex: "2147483647",
    right: "12px",
    bottom: "12px",
    padding: "12px 14px",
    border: "1px solid #d7b56d",
    borderRadius: "12px",
    background: "#20242b",
    color: "#ffffff",
    font: "600 13px system-ui",
    boxShadow: "0 4px 18px rgba(0,0,0,.35)"
  });

  button.addEventListener("click", async () => {
    button.textContent = "Checking cookies…";
    try {
      const result = await browser.runtime.sendMessage({
        type: "CHAMAN_COUNT_COOKIES",
        url: location.href
      });
      button.textContent = result && result.ok
        ? `Cookie API OK · ${result.count} found`
        : "Cookie test failed";
    } catch (_) {
      button.textContent = "Cookie bridge error";
    }
  });

  document.documentElement.appendChild(button);
})();
