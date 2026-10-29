"""Capture ChangeDetection store screens on the capture emulator.

`node store-listing/studio/recapture.cjs` builds the instrumentation APK (`./gradlew
:shared:assembleAndroidDeviceTest`) and runs this per language and device. For each profile and theme
the studio's capture.py sets display size and density, night mode, gesture navigation and System UI demo
mode (9:41, full battery and Wi-Fi); StoreCaptureTest drives the app on the device and writes full-display
PNGs and layout sidecars, which are pulled into <locale>/raw/<profile>/<theme>/. Everything is reset afterwards.
"""
import json
import sys
from pathlib import Path

here = Path(__file__).resolve().parent
sys.path.insert(0, str(here / 'studio'))
from capture import Device, parser  # noqa: E402

parser = parser(__doc__)
parser.add_argument('--theme', choices=['light', 'dark', 'all'], default='all')
parser.add_argument('--palette', help='App palette to capture in, e.g. ORANGE; defaults to the emulator wallpaper')
parser.add_argument('--style', help='Colour style to capture in, e.g. VIBRANT')
parser.add_argument('--out', type=Path, help='Save into this folder instead of <locale>/raw')
parser.add_argument('--apk', type=Path, help='Use an instrumentation APK from a separate build directory')
args = parser.parse_args()
package = 'com.bernaferrari.changedetection.library.test'
apk = args.apk or here.parent / 'shared/build/outputs/apk/androidTest/shared-androidTest.apk'

# Sample pages use natural-looking but fictional sites; with the network off, nothing can reach a real one.
with Device(args, package, apk=apk, tap_map=None) as d:
    for profile in d.profiles():
        for theme in ('light', 'dark'):
            if args.theme not in ('all', theme):
                continue
            d.night(theme == 'dark')
            d.status_bar()
            result = d.shell('am', 'instrument', '-w', '-e', 'class', 'com.bernaferrari.changedetection.store.StoreCaptureTest',
                             '-e', 'store_locale', d.locale, '-e', 'store_profile', profile, '-e', 'store_theme', theme,
                             *(['-e', 'store_palette', args.palette] if args.palette else []),
                             *(['-e', 'store_style', args.style] if args.style else []),
                             f'{package}/androidx.test.runner.AndroidJUnitRunner', retries=0)
            assert 'OK (1 test)' in result, result
            target = (args.out or d.raw) / profile / theme
            target.mkdir(parents=True, exist_ok=True)
            for old in target.glob('*.png'):
                old.unlink()
            d.adb('pull', f'/sdcard/Android/data/{package}/files/store-capture/{profile}/{theme}/.', str(target))
            for image in target.glob('*.png'):
                pixels = image.read_bytes()
                size = (int.from_bytes(pixels[16:20], 'big'), int.from_bytes(pixels[20:24], 'big'))
                assert pixels.startswith(b'\x89PNG\r\n\x1a\n') and size == (d.width, d.height), (image, size)
                layout = json.loads(image.with_suffix('.nodes.json').read_text())
                assert (layout['width'], layout['height']) == size and layout['nodes'], image
            print(profile, theme, sorted(p.name for p in target.glob('*.png')), flush=True)
