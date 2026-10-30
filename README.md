# StayCalm

Android app that watches what you type and the browser address bar. If it sees a blocked word it:

1. sends you to the home screen (browser goes to the background)
2. locks the phone
3. after you unlock, shows the calm screen: Crunchyroll / Discord / Reels
4. when you pick one, opens that app and shows "Stay calm sajin..." on top of it

## Install

1. Install Android Studio and open this folder (File > Open). Let Gradle sync.
2. Plug in your phone (USB debugging on) and press Run, or Build > Build APK(s) and install the APK.
3. Open StayCalm > "Open Accessibility settings" > StayCalm > turn it ON.
   - If the switch is greyed out: Settings > Apps > StayCalm > three dots > "Allow restricted settings", then try again.
4. Your phone must have a screen lock (PIN / pattern / fingerprint) so the lock screen actually asks for it.
5. Tap "Preview calm screen" to check it looks right.

## Edit messages, apps, words

Everything is in `app/src/main/java/com/staycalm/app/Config.kt`.
Extra words can also be added inside the app (add-only, so a weak moment can't remove them).

## Limits

- Android cannot force-close another app, so it goes Home and locks instead.
- It matches words in typed text, browser address bars and page titles. It is not a full web filter.
  Pair it with a DNS filter (Settings > Network > Private DNS > `family-filter-dns.cleanbrowsing.org`).
- The app has no internet permission. Nothing it sees leaves the phone.
