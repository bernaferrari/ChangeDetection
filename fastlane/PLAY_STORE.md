# Google Play listing uploads

Install Ruby and Bundler, then run `bundle install`. Authenticate using Google Application Default Credentials authorized for Google Play, or set `PLAY_JSON_KEY` to the path of an authorized Google credential JSON file. Never commit credentials or signing keys.

From the repository root:

```sh
bundle exec fastlane android check_screenshots
bundle exec fastlane android validate_screenshots
bundle exec fastlane android upload_screenshots
bundle exec fastlane android check_store
bundle exec fastlane android validate_store
bundle exec fastlane android upload_store
```

The first command checks local screenshot manifests without contacting Google. Validation lanes use a Play API edit without committing it. Upload lanes leave changes pending review; they do not upload a bundle or submit a release. The store lanes also require `listing.json` for every configured locale. The screenshots lanes leave listing text and icon/feature graphics untouched.

Each lane stages assets in a temporary directory using the locale list in `store-listing/studio.json` and screenshot order in each locale's `assets.json`. Export every configured locale in Screenshot Studio before running a lane; missing or invalid assets stop the upload before Play is changed. Fastlane replaces the screenshot set for each staged language and device size.
