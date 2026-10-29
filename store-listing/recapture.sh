#!/bin/sh
# Rebuild the capture app, capture every language and size in the app's Blue palette, and export everything.
#   store-listing/recapture.sh                 # every language
#   store-listing/recapture.sh de-DE pt-BR     # some languages, then export
# English gets phone, both tablets and a dark phone (for the README); other languages get the phone.
# Uses the one Resizable_Experimental emulator (emulator-5578); never a physical phone.
set -eu
cd "$(dirname "$0")/.."
serial=${SERIAL:-emulator-5578}
playwright=${PLAYWRIGHT_MODULE:-$HOME/.cache/weakspot-shots/node_modules/playwright}
locales=${*:-$(python3 -c "import json; print(' '.join(json.load(open('store-listing/studio.json'))['locales']))")}

capture() {
  for try in 1 2 3; do
    uv run -q --with pillow python3 store-listing/capture-emulator.py --serial "$serial" --palette BLUE --locale "$1" --profile "$2" --theme "$3" && return
    echo "retrying $1 $2 $3" >&2
  done
  return 1
}

./gradlew :shared:assembleAndroidDeviceTest -q
for locale in $locales; do
  capture "$locale" phone light
  if [ "$locale" = en-US ]; then
    capture en-US phone dark
    capture en-US tablet7 light
    capture en-US tablet10 light
  fi
done
cd store-listing
for locale in $locales; do PLAYWRIGHT_MODULE=$playwright node studio/export.cjs --locale "$locale"; done
