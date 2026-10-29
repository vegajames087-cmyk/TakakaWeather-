# Weather & Rivers — Android home-screen widget (source project)

This is **not an APK**. Build it with Android Studio (JDK 17, Android SDK 35). The project is intentionally dependency-light and uses Java.

1. Unzip the project on a computer.
2. Android Studio → Open → select `TakakaWeatherAndroid`.
3. Allow Gradle sync and install the Android SDK if prompted.
4. Build → Build Bundle(s) / APK(s) → Build APK(s).
5. Find `app/build/outputs/apk/debug/app-debug.apk` and copy it to your phone.
6. On Android open the APK in Files, allow installs from that source when prompted, and install.
7. Long-press an empty home-screen area → Widgets → Weather & Rivers → drag the widget onto your home screen; resize by long-pressing it.
8. Tap the widget to open the dashboard and change location; the widget updates from the saved location.

## Honest limitations
- Native widget shows **one ECMWF feed**, not three separate live provider readings. The in-app web dashboard has ECMWF and Yr data and MetService links.
- Warning strip is a **link/prompt**, not an independently verified live warning feed.
- River gauges and charts are accessed in the dashboard; the native widget does **not** render river-flow charts or real-time m³/s readings.
- Internet is required; refresh interval is approximate (Android may defer updates).
- GPS support in a local WebView can vary by device. Manual location search is recommended.
- This project has not been compiled or device-tested in this environment.
