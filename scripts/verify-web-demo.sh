#!/usr/bin/env bash
# Builds and verifies the Compose Multiplatform Wasm demo. Mirrors the CI workflow.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/webDemo/build/dist/wasmJs/productionExecutable"

if [[ "${1:-}" != "" && "${1:-}" != "--ci" ]]; then
  echo "Usage: $0 [--ci]" >&2
  exit 1
fi

cd "$ROOT"

echo "==> Building the Compose Multiplatform web demo"
./gradlew :webDemo:wasmJsBrowserDistribution \
  --no-daemon \
  --parallel \
  --build-cache \
  --configuration-cache

echo "==> Verifying the production web demo"
required_files=(
  "index.html"
  "changedetection-demo.js"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$DIST/$file" ]]; then
    echo "Missing required web demo artifact: $DIST/$file" >&2
    exit 1
  fi
done

if ! grep -q 'changedetection-demo.js' "$DIST/index.html"; then
  echo "The web demo HTML does not load its Compose bundle: $DIST/index.html" >&2
  exit 1
fi

wasm_count="$(find "$DIST" -maxdepth 1 -name '*.wasm' | wc -l | tr -d ' ')"
if [[ "$wasm_count" -lt 1 ]]; then
  echo "Expected a WebAssembly artifact in $DIST" >&2
  exit 1
fi

echo "==> Compose Multiplatform web demo is ready"
echo "    dist: $DIST"
echo "    wasm: $wasm_count file(s)"
