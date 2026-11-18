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
- AGP: 8.13.2 (same as Mozilla firefox-main)
- Gradle: 8.14.3
- Kotlin: 2.3.10
- compileSdk: 37 (targetSdk 35), `android.suppressUnsupportedCompileSdk=37` in gradle.properties

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
- CI fix #2: runner pe `sdkmanager` PATH me nahi tha (exit 127). Ab `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager` (fallback: find) full path se chalta hai.
- CI fix #3: `:app:checkReleaseAarMetadata` fail (13 issues). GeckoView ki androidx deps newer compileSdk/AGP maangti hain. Bumped compileSdk 35->36, AGP 8.7.3->8.11.1, Gradle 8.9->8.14.3, workflow me platforms;android-36 + build-tools;36.0.0 install. Agar phir fail ho to log ki `Dependency '...' requires` lines dekh ke exact version lena.
- CI fix #4: v4 (compileSdk 36/AGP 8.11.1) bhi `checkReleaseAarMetadata` pe fail hua. Ab compileSdk 37 (36.1/37 dono requirements cover), AGP 8.13.2, Kotlin 2.3.10 (`kotlinOptions` -> `compilerOptions`). Workflow trigger: pehle sirf `main` pe tha par repo branch `master` hai, ab har push pe chalta hai. Build log artifact (`build-log`) + failure pe Job Summary me error lines aati hain.
- CI fix #5 (real log mili): sirf 2 issues the: `androidx.core:core` aur `core-ktx` **1.19.1** ko AGP 9.1.0+ chahiye (GeckoView transitive laata hai). compileSdk 37 + AGP 8.13.2 ye baaki sab pass kar chuke the. Fix: `resolutionStrategy.force` se core/core-ktx 1.17.0 pin (app/build.gradle.kts). AGP 9 upgrade baad me alag step (Gradle 9.3+, kotlin-android plugin hatna).
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
