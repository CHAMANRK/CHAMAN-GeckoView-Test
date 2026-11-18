# HANDOFF — CHAMAN Cookie Browser (GeckoView)

## Real goal (Najeef's plan)
Main purpose: **cookies export** for CHAMAN Downloader. Firefox extensions cookie export ke liye friendly nahi hain, isliye ek chhota mini browser: user login kare, phir **1-2 tap** me cookies export ho jayein (yt-dlp `cookies.txt`).

## Architecture (FINAL, Najeef ne decide kiya)
- **Do alag apps.** Browser Downloader ke andar merge NAHI hoga (GeckoView ka size/CI weight Downloader ko risk deta hai).
- **Downloader = store jaisa.** Downloader me ek "Cookie Browser" button: browser app installed nahi to **Install** (GitHub release ka APK link), installed hai to **Open**. (`<queries>` se installed check.)
- Browser -> Downloader handoff: explicit Intent sirf `com.chaman.downloader` ko + FileProvider content URI. Public `/sdcard/Download` me file nahi. Dono apps same signing key (key chat me nahi, GitHub Secrets).
- Downloader side: chhota receiver, cookies apni private storage me site-wise, "Clear cookies" button. (Downloader ka alag zip/HANDOFF.)

## Browser app rules (Najeef)
- **Allowlist nahi.** Redirect jahan jaye wahi jayega.
- **URL field read-only** (hide nahi). Search/typing band.
- Home pe sirf 2 site cards: **Instagram, YouTube**. (Aur sites baad me, Najeef bole tab.)
- Per-site **cookie domains preset** (sirf export ke liye, navigation pe asar nahi): Instagram `instagram.com`; YouTube `youtube.com` + `google.com`.
- Export sirf **native button** se. Page me koi injected button nahi (content script hata di).
- Cookie values na UI me, na log me, na upload. Keys/tokens kabhi hardcode ya chat me nahi.

## Baseline
- Package: `com.chaman.browser.geckotest`, label "CHAMAN Cookie Browser"
- Version: `0.2-cookie-export` (versionCode 2)
- Engine: GeckoView `160.0.20261104181552`, Min SDK 26, target 35, compileSdk 37, ARM64
- Java 17, AGP 8.13.2, Gradle 8.14.3, Kotlin 2.3.10
- `androidx.core` / `core-ktx` pinned 1.17.0 (`resolutionStrategy.force`), GeckoView 1.19.1 laata hai jo AGP 9.1+ maangta hai. AGP 9 upgrade baad me alag step.

## Implemented in source (0.2, Step 1 + Step 2 saath me)
- **Step 1:** home screen (Instagram/YouTube cards), read-only URL bar (`onLocationChange` se update), Home/Back/Reload buttons, system Back handling, login popups (`onNewSession`) same view me, edge-to-edge insets fix (status bar overlap).
- **Step 2:** native **Export** button. Android extension ko port (`connectNative`) se `EXPORT_COOKIES{site, domains}` bhejta hai. Extension `cookies.getAll({domain})` se cookies leke Netscape `cookies.txt` banata hai (`#HttpOnly_` prefix, session cookie expiry 0) aur port se wapas bhejta hai. Android format validate karta hai, `cacheDir/cookies/<site>.txt` me likhta hai, screen pe sirf **count + size** dikhata hai. Stale cache app start pe delete.
- Extension host permissions sirf `*.instagram.com`, `*.youtube.com`, `*.google.com`.
- Extension background HELLO/READY handshake + auto-retry karta hai (delegate set hone se pehle connect ho to).
- CI: GitHub Actions ARM64 APK artifact (`CHAMAN-GeckoView-Test-ARM64`).

## Status
- v0.1-geckoview-test: **device pe pass** (CI, install, load, extension, cookie count 9 on instagram, native message, restart).
- **v0.2-cookie-export: BUILD HUA, device test PENDING.** CI compile pehli baar chalegi; GeckoView API signatures (`onLocationChange` 4-arg, `onNewSession`, `Port/PortDelegate`) pehli baar use hue. Compile fail ho to log ki `e:` lines bhejo.
- Abhi measure nahi hua: app size, memory, cold-start.

## Required device tests (v0.2)
- [ ] CI build succeeds.
- [ ] APK v0.1 ke upar install hota hai (same package/key).
- [ ] Home pe Instagram + YouTube cards; heading/status bar overlap nahi.
- [ ] URL field read-only (tap pe keyboard nahi), navigation/redirect pe URL update hota hai.
- [ ] Instagram login flow chalta hai, Back button sahi kaam karta hai.
- [ ] Instagram logged-in: Export -> "N cookies · size · Netscape format valid · app cache me saved".
- [ ] YouTube/Google logged-in: Export -> count (youtube.com + google.com dono ki cookies).
- [ ] Logged-out/0 cookies pe sahi message, crash nahi.
- [ ] Page ke andar koi Chaman button nahi dikhta.
- [ ] App restart ke baad Export phir chalta hai (extension reconnect).
- [ ] Check app size, memory use, cold-start time.

## Next steps (har step device test pass hone par hi agla)
- [ ] **Step 3:** Intent handoff + confirm dialog ("<site> ki N cookies Downloader ko bhejni hain? Ye tera login session hai.") + Downloader receiver + Downloader me Install/Open button.
- [ ] **Step 4:** Test: login-required link yt-dlp se download hua.
- [ ] Polish: 1-2 tap flow, extra sites (Najeef bole tab).

## Risks / unknown
- Cookie API chalna != yt-dlp accept karega. YouTube/Instagram cookies rotate kar sakte hain ya user-agent match maang sakte hain. Step 4 me pata chalega.
- Google login GeckoView me block hoga ya nahi: device pe dekhna.

## Changelog
- 0.2: Steps 1+2 (upar). Content script + page button hata di, extension native-port based. Label "CHAMAN Cookie Browser".
- CI fixes #1-#6 (v0.1 tak): setup-android hata ke runner SDK, sdkmanager full path, compileSdk 37 + AGP 8.13.2 + Kotlin 2.3.10, core/core-ktx 1.17.0 pin, Kotlin nullable fixes (`isSingleLine`, `WebExtension?`, `Throwable?`), workflow har push pe, `build-log` artifact + failure summary.
