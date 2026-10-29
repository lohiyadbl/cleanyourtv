# TV Vibe: optional liquid-glass Home extension

This builds an extended copy of the existing `com.gathin.tvhome` launcher. It replaces the large focused-app heading with an icy-blue rain-glass layout inspired by the supplied reference: a large left clock/weather/subscription panel, a four-poster strip, selected-show details, and separate Arcade and voice-assistant cards above the existing dock. Your four streaming dock tiles, app order, theme and existing Home selection are retained. The generated copy lives under ignored `work/tv-vibe/`; the original `launcher/` source is not edited.

Android 10 is supported (minimum Android 8 / SDK 26). No Android upgrade or root is needed.

## Included

- **Blue rain-glass cards:** translucent icy-blue gradient panels, reflective edges, a large clock/weather column, a static decorative rain background, a 200 ms focus lift and scale, and subtle title/poster fades. The rain is an aesthetic texture, not a forecast of rain. Native drawing, no real-time backdrop blur. Turning off system animations or the extension's motion option removes the extension transitions.
- **A broad mix of interests:** all languages and genres by default; optional genre and original-language filters in Your tastes. Named shelves include One More Twist, Under the Surface, Beyond the Ordinary, Easy Evenings, Big-Screen Adrenaline, Watch Together and Match Night. These use your explicit filter and title genres; the launcher does not read your viewing history.
- **Movie/show carousel:** Home shows up to four actual TMDB poster thumbnails when data is available; placeholders are labelled until then. Left/Right on the poster card cycles suggestions with a selection outline, then moves to adjacent cards at either end. OK opens the detailed carousel. In that screen, Left/Right while artwork has focus, Previous/Next buttons, and horizontal swipe gestures browse. Year, genres, IMDb rating when available, and India subscription-streaming providers stay attached to each title. Posters load on demand at thumbnail resolution.
- **Neon Rally:** an original small brick/paddle arcade game. Left/Right moves; OK starts or pauses; Back exits. Three lives, 24 bricks, local score.
- **Boundary Club:** an original timing-cricket mini-game. OK bowls, then OK swings as the ball enters the strike zone. Twelve balls or three wickets finish an innings. Back exits. Both games work offline with only a TV remote, without advertising or purchases.
- **Jarvis / Google Assistant:** a launcher entry point for the TV's installed Google Assistant (`com.google.android.katniss`). The Home card has an orb/HUD appearance, short motion and an off option. Selecting it opens Google Assistant through Android's ASSIST action; long OK opens appearance options. Google handles the actual voice session, listening UI and results. The launcher cannot reskin Google's own activity or animate its orb from private microphone/response state. The remote mic key keeps its firmware's normal Google Assistant behavior. No separate speech-recognition service, speech-to-command router, conversational backend or microphone capture is used. Text title/genre search remains a separate carousel feature.
- **Weather/AQI:** approximate Nerul, Navi Mumbai 400706 coordinates, 19.033 / 73.020. Temperature, humidity, wind, model weather and clearly labelled **US AQI** from Open-Meteo/CAMS. This is a model estimate, not Indian CPCB station data. Select Weather for details and source timestamps. Cached data retains its timestamp; failures are labelled.
- **Subscriptions:** manually enter ISO dates and whether each is an expiry or auto-renewal date. Netflix, Prime Video, JioHotstar, SonyLIV, ZEE5 and YouTube Premium. A past renewal date asks for a manual update; it never assumes payment or accesses streaming login/billing data.

## API setup

Weather and games need no API keys. For recommendations, select the Home recommendation card and enter a personal **TMDB v3 API key** from https://www.themoviedb.org/settings/api. The optional **OMDb API key** from https://www.omdbapi.com/apikey.aspx supplies IMDb ratings. Missing ratings are shown as Unavailable; a TMDB score is never relabelled IMDb. Long OK on the Home movie card reopens key setup. Keys can be removed by clearing the fields and saving.

Availability comes from TMDB/JustWatch for India, can change, and does not prove an active subscription. Availability opens the source page when a browser exists, otherwise lists the apps to search. It does not claim verified direct playback links. There is no viewing-history personalisation or cloud LLM integration.

## Build on the Windows laptop

These source changes must first be present in the laptop's repository checkout. They are not in the previously downloaded pinned source archive. Do not reuse the retired encoded chat installer.

The earlier portable toolchain is reused from `%LOCALAPPDATA%\TV-Vibe-build`: JDK 17, Android SDK 34 / build-tools 34.0.0 and the same Android debug signing key. From the repository root:

```powershell
python .\tv-vibe\build.py
```

This runs release unit tests, lint, assembly and signature verification. It prints the APK path. It makes no TV changes.

To install an update on an already authorised TV, supply its ADB serial and the actual local adb.exe path:

```powershell
python .\tv-vibe\build.py --install --tv "<TV-IP>:5555" --adb "C:\path\to\platform-tools\adb.exe"
```

The helper requires TV Vibe to be the current default Home. It saves package/disabled lists, current Home and theme; pulls the APK actually installed on the TV for rollback; compares its signing certificate with the new build; and saves a readable Python restore command. It then screenshots Home, updates in place, injects Home and checks the resumed activity. Install/focus failure triggers rollback. It does not disable packages, change the default Home, reset theme/order, or enable accessibility key helpers. Empty/failed post-install screenshots are reported separately from installation.

On Linux/macOS, provide JDK 17, `ANDROID_HOME`, SDK platform 34 and build-tools 34.0.0. Run the same Python helper. No source or APK downloads occur in the helper; Gradle can fetch its pinned wrapper and dependencies normally.

## Check on the physical TV

1. Press the real Home button. Confirm weather, movie details, subscriptions and Play & Ask fit without covering the dock or header.
2. Up from the dock reaches Weather. Weather Down reaches subscription dates, and Right reaches posters. Left/Right on the poster card first browses recommendations; the ends leave for adjacent cards. Posters Down reaches Arcade; the selected-show card Down reaches the voice card. Down from the lower cards returns to the dock. Up reaches the appropriate card above or the top controls. Check Inputs, Settings, Back, held Volume Up and all streaming apps.
3. Enter API keys locally. Confirm live data, source labels, movie posters, selected genre/language, actual IMDb ratings and India availability. Check error/offline states too. Network/API success was not verifiable in the cloud environment.
4. Try both games with the physical remote. Confirm start/pause/exit and sensible cricket timing. These have not yet been play-tested on the Flipkart TV.
5. Select Jarvis and verify that the actual Google Assistant opens. Test the physical remote mic button separately. Google controls its listening/results appearance; the launcher skin does not replace that UI or remap the mic button.
6. Enter a subscription date and check its countdown. Restart through Android's Restart menu and confirm Home, dates, preferences and app launch still work. This remains unverified on this TV.
7. If needed, run the printed `python "...Restore-previous-launcher.py"` command. Keep that folder. Same application ID, versionCode and signing certificate preserve app data across rollback.
8. Once all work is complete, close/revoke debugging and verify the ADB port is closed as required by the repository's TV workflow. That is not done automatically during development.

## Permissions, data and performance

This extension adds INTERNET to the existing launcher's permissions. No new recording, location, accessibility, boot or background service is added. It opens the installed Google Assistant and does not capture voice or use a separate recognizer. Android backup remains disabled; API keys, dates and recommendation caches are app-private and excluded from git.

HTTPS endpoints: `api.open-meteo.com`, `air-quality-api.open-meteo.com`, `api.themoviedb.org`, `www.omdbapi.com`, `image.tmdb.org`. The selected city, API key, genre/language/title query or requested poster is sent only to the relevant provider, which can see the requesting IP. No ad or analytics SDK is included. User-clicked availability links open `https://www.themoviedb.org/`.

Weather refreshes every 30 minutes and recommendations every six hours while Home is visible, or on an explicit refresh. New requests stop when the screen is paused; an existing request may finish under its timeout. Requests and response sizes are bounded. Posters use one worker and one pending request, replacing obsolete queued requests. Games redraw only when playing; assistant motion stops off-screen and has an off option. Real frame rate, memory usage and visual quality still need checks on the Android 10 SW4H hardware.

## Validation in this workspace

The combined launcher/extension build passed 294 JVM tests, Android release lint with zero errors, release assembly, and APK signature verification. Tests cover original Home behavior plus AQI boundaries, dates, genre/language query construction, carousel wrapping and cricket scoring/end conditions. No actual TV install, Google Assistant launch/mic success, live API success, performance claim or reboot persistence is asserted by those checks. No firmware is changed.

## Kodi

Kodi installation and JioTV configuration are deferred at the owner's request.
