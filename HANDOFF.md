# HANDOFF — CHAMAN GeckoView Test

## Real goal (Najeef's plan)
Main purpose: **cookies export**, jo CHAMAN Downloader me merge hoga. Firefox extensions deta hai, lekin cookie export ke liye friendly nahi hai. Isliye plan hai ek chhota mini browser banane ka, jo sirf isi kaam ke liye ho: user login kare, phir **1-2 tap** me cookies export ho jayein (Downloader ke liye). Ye GeckoView test us plan ka feasibility step hai.

## Project purpose
Separate proof-of-concept APK to evaluate GeckoView as a possible engine for a future CHAMAN Browser branch. Existing CHAMAN Browser V2 remains untouched.

## Baseline
- Package: `com.chaman.browser.geckotest`
- Version: `0.1-geckoview-test`
- Engine: Mozilla GeckoView `160.0.20261104181552`
- Min SDK: 26
- Target: ARM64 test APK
- Java: 17
- AGP: 8.7.3
- Gradle: 8.9

## Implemented in source
- Minimal URL bar and GeckoView surface.
- GeckoRuntime + GeckoSession startup.
- Built-in WebExtension installation via `ensureBuiltIn`.
- Extension content script adds a visible test button to HTTP/HTTPS pages.
- Extension background script queries `browser.cookies.getAll`.
- Native messaging bridge sends hostname and cookie count only.
- Cookie values and names are not sent to Android.
- GitHub Actions workflow builds and uploads an ARM64 APK artifact.

## Changelog
- 0.1 CI fix: `android-actions/setup-android@v3` hata diya (obsolete `tools` package pe sdkmanager exit 1). Runner ka preinstalled SDK use hota hai + `yes | sdkmanager --licenses` + packages install. APK copy step ab `*arm64-v8a*release*.apk` pin karta hai (splits se 2 APK banti hain).
- GeckoView version `160.0.20261104181552` abhi verify nahi hua; agar dependency resolve fail ho to pehle ye check karo.

## Required device tests
- [ ] CI build succeeds.
- [ ] APK installs on target ARM64 phone.
- [ ] GeckoView loads example.com and user-entered URLs.
- [ ] Built-in extension install succeeds after fresh install.
- [ ] Test button appears on ordinary HTTPS pages.
- [ ] Cookie API returns a count on a test site with cookies.
- [ ] Native message reaches Android and status updates.
- [ ] Restart app; built-in extension remains installed and test button still appears.
- [ ] Check app size, memory use, and cold-start time.

## Not implemented / do not assume
- No full browser UI migration.
- No AMO add-on catalogue or arbitrary extension manager.
- No downloader integration or cookie export.
- No existing V2 ad blocker/download detector migration.
- No proof yet that YouTube/Instagram authenticated media extraction will work.

## Next step after tests
If GeckoView startup + built-in extension + cookie API + native messaging all pass on-device, plan a separate integration spike for secure, explicit cookie handoff to CHAMAN Downloader. Never transfer cookie values without a clear user action and consent.
