# CHAMAN GeckoView Test APK

A separate ARM64 proof-of-concept APK. It does **not** replace or modify the existing CHAMAN Browser V2 project.

## Real goal

Final purpose: cookies export for CHAMAN Downloader. Firefox extensions support karta hai par cookie export ke liye friendly nahi hai, isliye ek chhota mini browser banana hai jisme login ke baad 1-2 tap me cookies export ho jayein. Ye test usi plan ka feasibility step hai.

## What 0.2 does

1. Home with Instagram / YouTube cards; URL bar is read-only; no allowlist.
2. Native **Export** button asks the built-in WebExtension (over a native port) for the site's cookies.
3. The extension builds a Netscape `cookies.txt`; the app validates it and saves it in its private cache. Screen shows only count and size.
4. No page-injected UI, no cookie values in UI/logs, no network upload.

Handoff to CHAMAN Downloader (Intent) is the next step. See `HANDOFF.md`.

## Build

Push the contents of this project to a separate GitHub repository, then run **Actions → Build CHAMAN GeckoView Test APK → Run workflow**. Download the `CHAMAN-GeckoView-Test-ARM64` artifact.

GeckoView is a large native dependency, so the first CI build may take longer and the APK will be substantially larger than the existing WebView browser.

## Device test

See the checklist in `HANDOFF.md`.
