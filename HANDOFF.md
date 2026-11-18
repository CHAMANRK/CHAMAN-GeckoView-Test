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

## Status
**v0.1-geckoview-test: device pe pass.** Baaki (not yet measured): app size, memory, cold-start. Known UI nit: heading status bar ke saath overlap karti hai (edge-to-edge insets), mini browser me fix karna.

## Changelog
- 0.1 CI fix: `android-actions/setup-android@v3` hata diya (obsolete `tools` package pe sdkmanager exit 1). Runner ka preinstalled SDK use hota hai + `yes | sdkmanager --licenses` + packages install. APK copy step ab `*arm64-v8a*release*.apk` pin karta hai (splits se 2 APK banti hain).
- CI fix #2: runner pe `sdkmanager` PATH me nahi tha (exit 127). Ab `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager` (fallback: find) full path se chalta hai.
- CI fix #3: `:app:checkReleaseAarMetadata` fail (13 issues). GeckoView ki androidx deps newer compileSdk/AGP maangti hain. Bumped compileSdk 35->36, AGP 8.7.3->8.11.1, Gradle 8.9->8.14.3, workflow me platforms;android-36 + build-tools;36.0.0 install. Agar phir fail ho to log ki `Dependency '...' requires` lines dekh ke exact version lena.
- CI fix #4: v4 (compileSdk 36/AGP 8.11.1) bhi `checkReleaseAarMetadata` pe fail hua. Ab compileSdk 37 (36.1/37 dono requirements cover), AGP 8.13.2, Kotlin 2.3.10 (`kotlinOptions` -> `compilerOptions`). Workflow trigger: pehle sirf `main` pe tha par repo branch `master` hai, ab har push pe chalta hai. Build log artifact (`build-log`) + failure pe Job Summary me error lines aati hain.
- CI fix #5 (real log mili): sirf 2 issues the: `androidx.core:core` aur `core-ktx` **1.19.1** ko AGP 9.1.0+ chahiye (GeckoView transitive laata hai). compileSdk 37 + AGP 8.13.2 ye baaki sab pass kar chuke the. Fix: `resolutionStrategy.force` se core/core-ktx 1.17.0 pin (app/build.gradle.kts). AGP 9 upgrade baad me alag step (Gradle 9.3+, kotlin-android plugin hatna).
- CI fix #6: AAR check pass. `compileReleaseKotlin` ke 4 errors fix (MainActivity.kt): `singleLine` -> `isSingleLine`, nullable `WebExtension?` null-check, nullable `Throwable?` safe-call. Naye GeckoView/Kotlin 2.3 me ye types nullable hain.
- GeckoView version `160.0.20261104181552` abhi verify nahi hua; agar dependency resolve fail ho to pehle ye check karo.

## Required device tests
- [x] CI build succeeds. — device pe pass
- [x] APK installs on target ARM64 phone. — device pe pass
- [x] GeckoView loads example.com and user-entered URLs. — device pe pass (Instagram logged-in load)
- [x] Built-in extension install succeeds after fresh install. — device pe pass
- [x] Test button appears on ordinary HTTPS pages. — device pe pass
- [x] Cookie API returns a count on a test site with cookies. — device pe pass (www.instagram.com, 9 cookies)
- [x] Native message reaches Android and status updates. — device pe pass
- [x] Restart app; built-in extension remains installed and test button still appears. — device pe pass
- [ ] Check app size, memory use, and cold-start time.

## Not implemented / do not assume
- No full browser UI migration.
- No AMO add-on catalogue or arbitrary extension manager.
- No downloader integration or cookie export.
- No existing V2 ad blocker/download detector migration.
- No proof yet that YouTube/Instagram authenticated media extraction will work.

## Next phase: Cookie Export plan (concept explain ho chuka, build ke liye "kardo/haan" PENDING)

### Flow (user side)
1. Mini browser me site pe login.
2. Tap 1: native **Export** button (page ke andar ka button nahi).
3. Tap 2: confirm dialog: "<site> ki N cookies Downloader ko bhejni hain? Ye tera login session hai."
4. Cookies Downloader tak jati hain; Downloader yt-dlp `--cookies` ke saath use karta hai.

### Technical concept
- Extension `cookies.getAll` se **sirf current site ke domain** ki cookies leta hai (values ke saath, sirf export ke time), Netscape `cookies.txt` banata hai (yt-dlp ka format).
- Export trigger sirf native Android button se; page JS trigger nahi kar sakta.
- Handoff (recommended): explicit Intent sirf `com.chaman.downloader` ko + FileProvider content URI. Public `/sdcard/Download` me file nahi.
- Downloader side: chhota receiver, cookies apni private storage me, site-wise, "Clear cookies" button.
- Safety: user tap ke bina export nahi; cookie values na log me, na upload; handoff ke baad temp file delete; kabhi hardcode/chat me paste nahi.

### Steps (har step device test pass hone par hi agla)
- [ ] **Step 1** (GeckoView Test): native Export button, cookies.txt app cache me, screen pe sirf count + size. Test: file valid Netscape format me bani.
- [ ] **Step 2**: Intent se Downloader ko handoff + Downloader me receiver (Downloader ka alag zip/HANDOFF). Test: login-required link yt-dlp se download hua.
- [ ] **Step 3**: 1-2 tap UI polish + status bar overlap fix (edge-to-edge insets).

### Architecture options (Downloader se kaise jodna hai)
- **A. Alag apps + Intent handoff (recommended abhi):** Downloader halka rehta hai (GeckoView ka size/CI weight uspe nahi aata), dono independent update hote hain, working Downloader ko risk nahi. Con: 2 apps install, handoff ka ek confirm tap.
- **B. Merge: browser ko Downloader ke andar tab banana (baad me, A prove hone ke baad):** ek app, cookies app ke bahar jati hi nahi. Con: GeckoView ki bahut badi native libs se APK size badhta hai, Downloader ka CI/toolchain bhi isi test jaisa heavy ho jayega (AGP 8.13.2, compileSdk 37, Kotlin 2.3, core pin), aur working ffmpeg/yt-dlp build ko risk.
- **C. GeckoView ke bina: Downloader ke andar Android WebView + `CookieManager`:** sabse chhota, merge ka jhanjhat nahi. Con: Google/YouTube login jaise flows embedded WebView ko block kar sakte hain ("browser may not be secure"); isi wajah se GeckoView test kiya gaya. Sirf tab dekhna jab Instagram jaisi sites pe WebView kaafi ho.
- Decision: abhi A, Najeef ka final confirm pending.

### Risks / unknown
- Cookie API chalna != yt-dlp accept karega. YouTube/Instagram cookies rotate kar sakte hain ya user-agent match maang sakte hain. Step 2 test me pata chalega.
- Pehli test site: Najeef se confirm karna baaki (Instagram ya YouTube).
