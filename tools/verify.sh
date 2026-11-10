#!/usr/bin/env bash
# Runs the checks from "Before you push" in docs/DEVELOPMENT.md.
#   tools/verify.sh        format check and JVM tests (a few minutes)
#   tools/verify.sh full   adds iOS compile (macOS), Android lint and release build, web demo, website
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew :shared:spotlessCheck :shared:testAndroidHostTest :shared:desktopTest

if [ "${1:-}" = "full" ]; then
  if [ "$(uname)" = "Darwin" ]; then
    ./gradlew :shared:compileKotlinIosSimulatorArm64
  fi
  ./gradlew :androidApp:lintDebug :androidApp:assembleRelease
  ./gradlew :webDemo:wasmJsBrowserDistribution
  (cd website && pnpm lint && pnpm typecheck && pnpm build)
fi
