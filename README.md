# CHAMAN GeckoView Test APK

A separate ARM64 proof-of-concept APK. It does **not** replace or modify the existing CHAMAN Browser V2 project.

## What this test proves

1. GeckoView starts and loads web pages.
2. A built-in WebExtension bundled in `app/src/main/assets/chaman_test_extension/` installs using `ensureBuiltIn`.
3. The extension content script runs on ordinary HTTP/HTTPS pages.
4. The extension can call the WebExtension `cookies` API after explicit host permission.
5. The extension sends only the cookie **count** and hostname to Android using GeckoView native messaging. Cookie names and values are deliberately never transferred.

## Build

Push the contents of this project to a separate GitHub repository, then run **Actions → Build CHAMAN GeckoView Test APK → Run workflow**. Download the `CHAMAN-GeckoView-Test-ARM64` artifact.

GeckoView is a large native dependency, so the first CI build may take longer and the APK will be substantially larger than the existing WebView browser.

## Device test

1. Install the APK on an ARM64 Android phone.
2. Open a website where you are already signed in (only if you choose).
3. Tap **Test Chaman Cookie Bridge** shown at the bottom of the page.
4. The app should display the hostname and cookie count.
5. Confirm that the UI reports `Cookie API OK`.

Use a test account/site if possible. The test intentionally never exports or displays cookie values.

## Important limitations

- This is a GeckoView/WebExtension proof-of-concept, not a finished browser.
- It does not include the existing browser's UI, ad blocker, tabs, download detector, or download manager.
- It tests a built-in extension. It does not yet test arbitrary AMO extension installation.
- Cookie API visibility depends on the extension's declared host permissions and the site's cookie attributes.
- A successful cookie count does not prove that every website's authenticated media can be downloaded.
