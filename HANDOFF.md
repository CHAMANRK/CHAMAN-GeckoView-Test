# HANDOFF — CHAMAN Cookie Browser (GeckoView)

## Real goal (Najeef's plan)
Main purpose: **cookies export** for CHAMAN Downloader. Firefox extensions cookie export ke liye friendly nahi hain, isliye ek chhota mini browser: user login kare, phir **1-2 tap** me cookies export ho jayein (yt-dlp `cookies.txt`).

## Architecture (FINAL, Najeef ne decide kiya)
- **Do alag apps.** Browser Downloader ke andar merge NAHI hoga (GeckoView ka size/CI weight).
- **Downloader = store jaisa.** Downloader (Settings, baad me welcome page) me "Import Instagram cookies": browser nahi/purana/alag key to Downloader APK download + install karwata hai, phir seedha Instagram login pe kholta hai.
- Browser -> Downloader: Export dabate hi explicit Intent + FileProvider URI, Downloader front me aata hai (= redirect back). Poora contract: **Handshake.md** (dono repo me same copy; badlo to pehle usme likho, dono jagah copy karo).
- Dono apps same permanent keystore se sign hoti hain (Browser repo me bhi `CHAMAN_KEYSTORE_B64`, `CHAMAN_KEYSTORE_PASSWORD` secrets, chat me paste nahi).

## Browser app rules (Najeef)
- **Allowlist nahi.** Redirect jahan jaye wahi jayega.
- **URL field read-only** (hide nahi). Search/typing band.
- Home pe sirf 2 site cards: **Instagram, YouTube** (Downloader se aao to seedha login page, cards skip). Downloader abhi sirf Instagram cookies leta hai. (Aur sites baad me, Najeef bole tab.)
- Per-site **cookie domains preset** (sirf export ke liye, navigation pe asar nahi): Instagram `instagram.com`; YouTube `youtube.com` + `google.com`.
- Export sirf **native button** se. Page me koi injected button nahi (content script hata di).
- Cookie values na UI me, na log me, na upload. Keys/tokens kabhi hardcode ya chat me nahi.

## Baseline
- Package: `com.chaman.browser.geckotest`, label "CHAMAN Cookie Browser"
- Version: `0.3.0-handshake` (versionCode 3). Downloader `MIN_BROWSER_VERSION` = 3
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
- v0.1-geckoview-test: device pe pass.
- v0.2-cookie-export (Step 1+2: home cards, read-only URL, native Export, Netscape cookies.txt): **device pe pass** (Instagram 11, YouTube 46 cookies).
- **v0.3.0-handshake: CODE LIKHA, device test pending** (Downloader v0.4.19-cookies ke saath). Nayi API: `launchMode=singleTask` + `onNewIntent`, LOGIN intent, login-cookie name check (`loggedIn`), FileProvider `${applicationId}.cookies`, IMPORT_COOKIES intent, release signing + GitHub Release CI.
- Pehla CI compile: `FileProvider` / Intent code naya hai; fail ho to log ki `e:` lines.
- Abhi measure nahi hua: app size, memory, cold-start.

## Required device tests (v0.3 + Downloader Step O)
- [ ] Browser repo me do keystore secrets daalo, tag `v0.3.0-handshake` push: CI green + GitHub Release me `CHAMAN-Cookie-Browser.apk`.
- [ ] Purani v0.2 (debug key) device pe ho to Downloader uninstall prompt dikhaye, phir install ho.
- [ ] Downloader Settings > Import Instagram cookies: download %, install, wapas aate hi browser Instagram login pe khule.
- [ ] Login ke bina Export: "login nahi hua" message, kuch save/send nahi.
- [ ] Login ke baad Export: Downloader front me aaye, Toast + Settings me count.
- [ ] Instagram link (login-required) search/download cookies ke saath chale.
- [ ] Standalone browser (launcher se) me cards aur Export ab bhi chalte hain (Downloader install ho to wahin bhejta hai).
- [ ] Downloader install na ho to Export message saaf aaye, crash nahi.

## Next steps
- [ ] Step P (Downloader): welcome page me wahi setup + Skip.
- [ ] Browser size/memory/cold-start measure.

## Risks / unknown
- Cookie API chalna != yt-dlp accept karega. YouTube/Instagram cookies rotate kar sakte hain ya user-agent match maang sakte hain. Step 4 me pata chalega.
- Google login GeckoView me block hoga ya nahi: device pe dekhna.

## Changelog
- 0.2: Steps 1+2 (upar). Content script + page button hata di, extension native-port based. Label "CHAMAN Cookie Browser".
- CI fixes #1-#6 (v0.1 tak): setup-android hata ke runner SDK, sdkmanager full path, compileSdk 37 + AGP 8.13.2 + Kotlin 2.3.10, core/core-ktx 1.17.0 pin, Kotlin nullable fixes (`isSingleLine`, `WebExtension?`, `Throwable?`), workflow har push pe, `build-log` artifact + failure summary.
