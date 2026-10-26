"""Capture ChangeDetection store screens on a dedicated, resizable emulator.

Builds nothing: run `./gradlew :shared:assembleAndroidDeviceTest` first. For each device profile and
theme this sets the emulator's display size and density, Android night mode, gesture navigation, and
System UI demo mode (9:41, full battery and Wi-Fi), runs StoreCaptureTest, and pulls the full-display
PNGs and layout sidecars into <locale>/raw/<profile>/<theme>/. Display overrides and demo mode are reset afterwards.
"""
import argparse
import json
import subprocess
import time
import sys
import re
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True, help='Dedicated emulator serial; never a personal device')
parser.add_argument('--profile', choices=['phone', 'tablet7', 'tablet10', 'all'], default='all')
parser.add_argument('--theme', choices=['light', 'dark', 'all'], default='all')
parser.add_argument('--locale', default='en-US', help='Per-app language and output folder, e.g. de-DE')
parser.add_argument('--apk', type=Path, help='Use an instrumentation APK from a separate build directory')
args = parser.parse_args()
if not re.fullmatch(r'[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*', args.locale):
    parser.error('Invalid locale')
assert args.serial.startswith('emulator-'), 'Use a dedicated emulator, not a personal device.'

here = Path(__file__).resolve().parent
repo = here.parent
sys.path.insert(0, str(here / 'studio'))
from uitree import set_app_locale
apk = args.apk or repo / 'shared/build/outputs/apk/androidTest/shared-androidTest.apk'
package = 'com.bernaferrari.changedetection.library.test'
profiles = json.loads((here / 'profiles.json').read_text())


def adb(*parts, check=True):
    return subprocess.run(['adb', '-s', args.serial, *parts], check=check, capture_output=True, text=True).stdout


def demo(*extra):
    adb('shell', 'am', 'broadcast', '-a', 'com.android.systemui.demo', '--es', 'command', *extra)


assert 'Override' not in adb('shell', 'wm', 'size') + adb('shell', 'wm', 'density'), 'Start from an emulator with no display overrides.'
adb('install', '-r', '-t', str(apk))
try:
    set_app_locale(adb, package, args.locale)
    adb('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', 'com.android.internal.systemui.navbar.gestural')
    adb('shell', 'settings', 'put', 'global', 'sysui_demo_allowed', '1')
    for name, profile in profiles.items():
        if name == 'desktop' or args.profile not in ('all', name):
            continue
        for theme in ('light', 'dark'):
            if args.theme not in ('all', theme):
                continue
            demo('exit')
            adb('shell', 'cmd', 'uimode', 'night', 'yes' if theme == 'dark' else 'no')
            adb('shell', 'wm', 'size', f"{profile['captureWidth']}x{profile['captureHeight']}")
            adb('shell', 'wm', 'density', str(profile['captureDensity']))
            time.sleep(2)
            demo('enter')
            demo('clock', '--es', 'hhmm', '0941')
            demo('battery', '--es', 'level', '100', '--es', 'plugged', 'false')
            demo('notifications', '--es', 'visible', 'false')
            demo('network', '--es', 'wifi', 'show', '--es', 'level', '4', '--es', 'fully', 'true', '--es', 'mobile', 'hide', '--es', 'sims', '0')
            result = adb('shell', 'am', 'instrument', '-w', '-e', 'class', 'com.bernaferrari.changedetection.store.StoreCaptureTest',
                         '-e', 'store_locale', args.locale, '-e', 'store_profile', name, '-e', 'store_theme', theme, f'{package}/androidx.test.runner.AndroidJUnitRunner')
            assert 'OK (1 test)' in result, result
            target = here / args.locale / 'raw' / name / theme
            target.mkdir(parents=True, exist_ok=True)
            for old in target.glob('*.png'):
                old.unlink()
            adb('pull', f'/sdcard/Android/data/{package}/files/store-capture/{name}/{theme}/.', str(target))
            for image in target.glob('*.png'):
                pixels = image.read_bytes()
                dimensions = (int.from_bytes(pixels[16:20], 'big'), int.from_bytes(pixels[20:24], 'big'))
                assert pixels.startswith(b'\x89PNG\r\n\x1a\n') and dimensions == (profile['captureWidth'], profile['captureHeight']), (image, dimensions)
                layout = json.loads(image.with_suffix('.nodes.json').read_text())
                assert (layout['width'], layout['height']) == dimensions and layout['nodes'], image
            print(name, theme, sorted(p.name for p in target.glob('*.png')), flush=True)
finally:
    adb('shell', 'am', 'force-stop', package, check=False)
    set_app_locale(adb, package, None)
    demo('exit')
    adb('shell', 'cmd', 'uimode', 'night', 'no', check=False)
    adb('shell', 'wm', 'size', 'reset', check=False)
    adb('shell', 'wm', 'density', 'reset', check=False)
