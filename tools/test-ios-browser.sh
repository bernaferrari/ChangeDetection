#!/usr/bin/env bash
# WebKit needs an app host; the ordinary simulator task runs a bare test executable.
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :shared:linkDebugTestIosSimulatorArm64
work_dir="$(mktemp -d "${TMPDIR:-/tmp}/changedetection-webkit.XXXXXX")"
simulator=""
cleanup() {
  if [[ -n "$simulator" ]]; then
    xcrun simctl shutdown "$simulator" >/dev/null 2>&1 || true
    xcrun simctl delete "$simulator" >/dev/null 2>&1 || true
  fi
  rm -rf "$work_dir"
}
trap cleanup EXIT
read -r runtime device_type <<< "$(xcrun simctl list runtimes --json | python3 -c '
import json,sys
r = max((r for r in json.load(sys.stdin)["runtimes"] if r["isAvailable"] and r.get("platform") == "iOS"), key=lambda r: tuple(map(int,r["version"].split("."))))
d = next(d for d in r["supportedDeviceTypes"] if d["productFamily"] == "iPhone")
print(r["identifier"],d["identifier"])
')"
simulator="$(xcrun simctl create 'Change Detection WebKit tests' "$device_type" "$runtime")"
xcrun simctl boot "$simulator"
xcrun simctl bootstatus "$simulator" -b >/dev/null
app="$work_dir/CaptureTests.app"
mkdir -p "$app"
cp shared/build/bin/iosSimulatorArm64/debugTest/test.kexe "$app/CaptureTests"
cat > "$app/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>com.bernaferrari.changedetection.capturetests</string>
<key>CFBundleExecutable</key><string>CaptureTests</string>
<key>CFBundleShortVersionString</key><string>1.0</string>
<key>CFBundleVersion</key><string>1</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleSupportedPlatforms</key><array><string>iPhoneSimulator</string></array>
<key>MinimumOSVersion</key><string>16.0</string>
<key>UIDeviceFamily</key><array><integer>1</integer></array>
<key>NSAppTransportSecurity</key><dict><key>NSAllowsLocalNetworking</key><true/></dict>
</dict></plist>
PLIST
codesign --force --sign - "$app" >/dev/null
xcrun simctl install "$simulator" "$app"
xcrun simctl launch --console-pty "$simulator" com.bernaferrari.changedetection.capturetests "--ktest_filter=${IOS_TEST_FILTER:-*DocumentTest*}" 2>&1 | tee "$work_dir/result.log"
# A crash or an empty run must never count as successful verification.
python3 - "$work_dir/result.log" <<'PY'
import re,sys
text=open(sys.argv[1]).read().replace("\r", "")
passed=re.findall(r'^\[  PASSED  \] (\d+) tests?',text,re.M)
assert passed and int(passed[-1]) > 0 and not re.search(r'^\[  FAILED  \]',text,re.M),text
PY
