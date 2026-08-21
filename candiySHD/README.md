# candiySHD

Samsung Health Data SDK based test module for Galaxy Watch data synced through Samsung Health.

## Modules

- `:candiySHD`: Kotlin library wrapper around `samsung-health-data-api-1.1.0.aar`.
- `:candiySHDExample`: Test app that requests Samsung Health permissions, reads watch data, and displays a summary.

## Test flow

1. Install Samsung Health on the test phone and sign in.
2. Pair a Galaxy Watch and confirm data is synced into Samsung Health.
3. Enable Samsung Health Data SDK developer mode for local testing, or register the app package/signature with Samsung for distribution.
4. Build and install `:candiySHDExample`.
5. Tap `Request permissions`.
6. Tap `Load Galaxy Watch data`.

The test app reads recent data using `ReadSourceFilter.fromDeviceType(DeviceGroup.WATCH)` for heart rate, blood oxygen, and sleep. Steps use Samsung Health aggregate data because the SDK exposes steps through `DataType.StepsType.TOTAL`.

## Build

```bash
./gradlew :candiySHDExample:assembleDebug
```

Debug APK:

```text
candiySHDExample/build/outputs/apk/debug/candiySHDExample-debug.apk
```
