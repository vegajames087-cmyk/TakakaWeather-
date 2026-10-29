# Weather & Rivers — Golden Bay / Kahurangi Coast

Android app and home-screen widget with a tactical field-instrument interface.

## Features

- Location selector for Tākaka, Pōhara, Collingwood, Tata Beach, Wainui Bay, Aorere Valley, Paturau, Anatori, Tōtaranui and Kahurangi Lighthouse.
- Native forecast requests from Open-Meteo's ECMWF model and MET Norway's Locationforecast API; source errors are shown instead of substituting invented numbers.
- Live Tākaka at Kotinga and Tākaka at Harwoods water level, hourly level change, flow, peak flow, peak water level and peak time from Tasman District Council's official overview table.
- Council-provided 7-day and 30-day river-flow graphs embedded directly in the dashboard, with a link to the council's configurable hourly history table.
- Home-screen widget with current Tākaka ECMWF conditions and the two Tākaka river gauges.
- WorkManager network-constrained widget refresh, scheduled every 30 minutes. Readings are cleared when a refresh fails so the widget does not present stale values as current.
- Dark olive instrument-panel design, muted green telemetry accents, amber labels and high-contrast numeric readouts.

## Build

Open this repository in Android Studio or use the GitHub Actions workflow. It uses JDK 17, Android SDK 35 and Gradle 8.9. The automated build runs the dashboard JavaScript syntax check, unit tests, APK compilation and archive/package checks. The generated debug APK is uploaded as the weather-rivers-debug-apk artifact.

## Data and safety notes

River readings are provisional council telemetry. The council's table timestamps are labelled NZST; during daylight saving, add one hour to interpret local time. River flow and level are not by themselves a river-crossing safety assessment. Do not rely on this app as the sole source for flood, weather or crossing decisions.

- Tākaka at Kotinga: https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/takaka-at-kotinga
- Tākaka at Harwoods: https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/takaka-harwoods
- Official level/flow report: https://www.tasman.govt.nz/my-region/environment/environmental-data/river-flow/water-level-and-flow-report
- Hourly flow table: https://datapages.tasman.govt.nz/hydrodata/flow.aspx
- Weather warnings: https://www.metservice.com/warnings/home

MetService's live numeric values and automated warning state are not represented as integrated API feeds in this build; the app links to the official pages. Live device-side HTTP and actual phone installation still need device-level validation after downloading the build.
