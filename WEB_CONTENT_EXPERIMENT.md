# Android WebContent pilot

This worktree points at KMPWright 0.0.3, which is not published. It tests only the Android
live browser. Watch setup, captures, the scheduled worker, and saved snapshots still use
their existing hosts.

Build against the matching local KMPWright worktree:

```sh
./gradlew --include-build "$HOME/Documents/kotlin/wt/kmpwright-webcontent" \
  -PkmpwrightConsumerAgp=9.4.1 -PkmpwrightConsumerKotlin=2.4.21 \
  -PkmpwrightConsumerSerialization=1.12.0 -PkmpwrightConsumerIncludeCompose=true \
  :shared:assembleAndroidDeviceTest
```

The Android 36 (WebView 133) and Android 37.2 (WebView 149) emulators did not
advertise WebContent. They exercise the normal WebView fallback, not retention.
The ChangeDetection run on Android 37.2 passed the three browser error/retry tests
and 22 capture/font regressions with the current local capture fixes overlaid.
To require the actual retention path, run the KMPWright test
`retainedSessionReattachesOrFallsBackAccordingToTheProvider` with the Android
instrumentation argument `requireWebContent=true` on a compatible provider.
