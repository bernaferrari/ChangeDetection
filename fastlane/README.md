fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew cask install fastlane`

# Available Actions
## Android
### android test
```
fastlane android test
```
Runs Android host tests, compiles device tests, runs lint, and builds the debug app. Device tests must be executed separately on an authorized device.
### android internal
```
fastlane android internal
```
Publishes a release bundle to Google Play internal testing.
### android deploy
```
fastlane android deploy
```
Publishes a release bundle to Google Play production. Complete the release acceptance gates before using this lane.

Both publishing lanes require `CHANGEDETECTION_SIGNING_PROPERTIES` and `CHANGEDETECTION_PLAY_CREDENTIALS` to point to external credential files. Use verified replacement credentials as described in `plans/credential-remediation.md`, and confirm an unused version code in Play Console. These lanes do not run the quality checks or update the version code.

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
