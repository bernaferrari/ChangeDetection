#!/bin/sh
# Rebuild the capture app and recapture the README screens in the app's Blue palette, then export the README art.
#   store-listing/recapture-readme.sh
# Uses the one Resizable_Experimental emulator (emulator-5578); never a physical phone.
set -eu
cd "$(dirname "$0")/.."
serial=${SERIAL:-emulator-5578}
playwright=${PLAYWRIGHT_MODULE:-$HOME/.cache/weakspot-shots/node_modules/playwright}
raw=store-listing/en-US/raw
tmp=$(mktemp -d)

./gradlew :shared:assembleAndroidDeviceTest -q
capture() { uv run -q --with pillow python3 store-listing/capture-emulator.py --serial "$serial" --palette BLUE --out "$tmp" --profile "$1" --theme "$2"; }
capture phone light
capture phone dark
capture tablet10 light
for pair in phone/light:phone/blue phone/dark:phone/blue-dark tablet10/light:tablet10/blue; do
  rm -rf "$raw/${pair#*:}" && mkdir -p "$raw/${pair#*:}" && cp "$tmp/${pair%%:*}"/* "$raw/${pair#*:}/"
done
rm -rf "$tmp"
(cd store-listing && PLAYWRIGHT_MODULE=$playwright node studio/export.cjs --locale en-US --kind readme)
