"""Drive the capture emulator for store screenshots (Python 3; Pillow for JPEG output and re-encoding).

An app's capture script keeps only its scene tour:

    import sys
    from pathlib import Path
    sys.path.insert(0, str(Path(__file__).resolve().parent / 'studio'))
    from capture import Device, parser

    args = parser(__doc__).parse_args()
    with Device(args, 'com.example.storecapture', '.MainActivity') as d:
        for profile in d.profiles():
            d.night(False)
            d.launch(clear=True)
            d.shot('01-home')                 # <locale>/raw/<profile>/01-home.png + .nodes.json
            d.tap('Settings')
            d.shot('02-settings')

`Device` refuses anything but an emulator serial and an emulator that still has a display-size override.
While it is open it sets gesture navigation, System UI demo mode (9:41, full battery and Wi-Fi, no
notifications or SIM), font scale 1, heads-up off, the network off (`offline`), animations off
(`animations=False`; otherwise
they are set to 1, whatever an interrupted run left), and the app's language; each profile gets its `wm size`/`wm density` from
profiles.json. Everything it changes is put back on exit, even after a failure.

It also recovers from what goes wrong on a busy emulator: a stale UiAutomation client (an agent-device
`snapshothelper` app_process: "UiAutomationService ... already registered"), an "isn't responding"
dialog (it taps Wait), a slow first draw, and INSTALL_FAILED_INSUFFICIENT_STORAGE (it uninstalls the
app's capture build and trims caches, then installs again). Helper packages that would show up in app
lists (agent-device, SafetyCore, contact keys, verifiers) are hidden with `pm uninstall --user 0`.

Taps are by label (a regular expression, matched in full against text or content description). With a
tap map (the default), the reference locale records the UI tree path of each tapped node in
<default locale>/raw/tap-map.json and other locales replay those paths, so a tour written with English
labels runs in every language. `d.scope` (default "<profile>|<theme>") prefixes each tap's key.

Command line:  python3 capture.py install <apk> [--package p]  |  python3 capture.py reset
"""
import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from uitree import Navigator, nodes_from_xml, set_app_locale, write_sidecar  # noqa: E402

LISTING = Path(__file__).resolve().parent.parent
SERIAL = os.environ.get('CAPTURE_SERIAL', 'emulator-5578')
PNG = b'\x89PNG\r\n\x1a\n'
DEMO = 'com.android.systemui.demo'
SCALES = ('animator_duration_scale', 'transition_animation_scale', 'window_animation_scale')
# Installed by tools or Google Play services rather than by a person; they would show up in app lists.
HELPERS = re.compile(r'^com\.callstack\.agentdevice\..*|.*\.safetycore|.*contactkeys.*|.*\.verifier$')
SYSTEM_DIALOG = re.compile(r".*(isn't responding|keeps stopping|has stopped).*")


def parser(description=None, profiles=True):
    """The arguments every capture script takes; add the app's own before parse_args()."""
    p = argparse.ArgumentParser(description=description, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--serial', default=SERIAL, help=f'Dedicated emulator serial (default {SERIAL}); never a physical phone')
    if profiles:
        p.add_argument('--profile', default='all', help='Capture one profile from profiles.json (default: all)')
    p.add_argument('--locale', help='Play locale to capture in, e.g. de-DE (default: the studio default locale)')
    p.add_argument('--only', help='Comma-separated scene names to capture (default: all)')
    return p


class Device:
    def __init__(self, args, package, activity=None, *, foreground=None, tap_map='tap-map.json', battery=100,
                 offline=True, animations=True, locale=True, apk=None, uninstall=False, listing=LISTING):
        """args: parsed `parser()` arguments. package: the capture build's application ID; activity its launch
        activity ('.Main' is relative to the package). foreground: the package whose UI must be on screen
        before a dump counts (default `package`). tap_map: file name under <default>/raw (None: plain label
        taps). offline: Wi-Fi and data off while capturing. animations: False turns them off. locale: set the
        app language (False leaves it to the system). apk: install this first; uninstall=True removes the package
        on exit (a test APK that would otherwise show up in other apps' app lists)."""
        self.serial = getattr(args, 'serial', None) or SERIAL
        if not self.serial.startswith('emulator-'):
            raise SystemExit('Use a dedicated emulator, not a personal device.')
        self.listing = Path(listing)
        config = self.listing / 'studio.json'
        self.studio = json.loads(config.read_text()) if config.exists() else {}
        self.reference = self.studio.get('defaultLocale', 'en-US')
        self.locale = getattr(args, 'locale', None) or self.reference
        if not re.fullmatch(r'[a-z]{2,3}(?:-[A-Za-z0-9]{2,8})*', self.locale):
            raise SystemExit(f'Invalid locale {self.locale}')
        self.raw = self.listing / self.locale / 'raw'
        self.package = package
        self.activity = activity
        self.foreground = foreground or package
        only = getattr(args, 'only', None)
        self.only = set(only.split(',')) if only else None
        self.selected = getattr(args, 'profile', None)
        self.navigator = Navigator(self.listing / self.reference / 'raw' / tap_map,
                                   recording=self.locale == self.reference) if tap_map else None
        self.battery, self.offline, self.keep_animations, self.app_locale, self.apk = battery, offline, animations, locale, apk
        self.uninstall = uninstall
        self.profile = self.theme = None
        self.width = self.height = self.density = 0
        self.scope = None
        self.xml = None
        self.saved = {}
        self.clock_set = False
        self.display = None

    # adb ----------------------------------------------------------------------------------------------
    def adb(self, *words, check=True, timeout=300, retries=2):
        """Run adb against this emulator and return stdout as text. A starved emulator sometimes drops a
        command, so a failure is tried again before it raises."""
        for attempt in range(retries + 1):
            run = subprocess.run(['adb', '-s', self.serial, *map(str, words)], capture_output=True, timeout=timeout)
            if run.returncode == 0 or not check:
                return run.stdout.decode(errors='replace') + run.stderr.decode(errors='replace') * (not check)
            if attempt == retries:
                raise RuntimeError(f"adb {' '.join(map(str, words))} failed: {(run.stdout + run.stderr).decode(errors='replace').strip()}")
            time.sleep(3)

    def shell(self, *words, check=True, **kw):
        return self.adb('shell', *words, check=check, **kw)

    def _uitree_adb(self, *words):  # uitree helpers take a function of adb words
        return self.adb(*words)

    def setting(self, namespace, key, value=None):
        if value is None:
            value = self.shell('settings', 'get', namespace, key).strip()
            return None if value in ('', 'null') else value
        self.shell('settings', 'put', namespace, key, value)

    # lifecycle ----------------------------------------------------------------------------------------
    def __enter__(self):
        qemu = self.shell('getprop', 'ro.kernel.qemu').strip() + self.shell('getprop', 'ro.boot.qemu').strip()
        if '1' not in qemu:
            raise SystemExit(f'{self.serial} is not an emulator.')
        overrides = self.shell('wm', 'size') + self.shell('wm', 'density')
        if 'Override' in overrides:
            raise SystemExit('The emulator still has a display override from an interrupted run: '
                             'python3 store-listing/studio/capture.py reset')
        for key in ('heads_up_notifications_enabled', 'auto_time', *SCALES):
            self.saved[('global', key)] = self.setting('global', key)
        for key in ('font_scale', 'accelerometer_rotation', 'user_rotation'):
            self.saved[('system', key)] = self.setting('system', key)
        try:
            self.shell('input', 'keyevent', 'KEYCODE_WAKEUP')
            self.shell('wm', 'dismiss-keyguard', check=False)
            self.shell('cmd', 'overlay', 'enable-exclusive', '--category', 'com.android.internal.systemui.navbar.gestural')
            self.setting('system', 'font_scale', '1.0')
            self.setting('global', 'sysui_demo_allowed', '1')
            self.setting('global', 'heads_up_notifications_enabled', '0')
            self.hide_helpers()
            self.snooze_notifications()
            if self.offline:
                self.shell('svc', 'wifi', 'disable', check=False)
                self.shell('svc', 'data', 'disable', check=False)
            self.animations(self.keep_animations)  # explicitly: an interrupted run can leave them at 0
            if self.apk:
                self.install(self.apk)
            if self.app_locale:
                set_app_locale(self._uitree_adb, self.package, self.locale)
        except BaseException:
            self.__exit__(*sys.exc_info())
            raise
        return self

    def __exit__(self, *exc):
        if self.navigator:
            self.navigator.save()
        steps = [lambda: self.shell('am', 'force-stop', self.package, check=False)]
        if self.app_locale:
            steps.append(lambda: set_app_locale(self._uitree_adb, self.package, None))
        steps += [lambda: self.demo('exit'),
                  lambda: self.shell('cmd', 'uimode', 'night', 'no', check=False),
                  lambda: self.shell('wm', 'size', 'reset', check=False),
                  lambda: self.shell('wm', 'density', 'reset', check=False)]
        if self.clock_set:  # back to the host's time
            steps.append(lambda: self.shell('cmd', 'alarm', 'set-time', str(int(time.time() * 1000)), check=False))
        defaults = {'heads_up_notifications_enabled': '1', 'auto_time': '1', 'font_scale': '1.0',
                    'accelerometer_rotation': '1', 'user_rotation': '0', **{key: '1' for key in SCALES}}
        for (namespace, key), value in self.saved.items():
            steps.append(lambda n=namespace, k=key, v=value: self.setting(n, k, v or defaults[k]))
        if self.offline:
            steps += [lambda: self.shell('svc', 'wifi', 'enable', check=False),
                      lambda: self.shell('svc', 'data', 'enable', check=False)]
        if self.uninstall:
            steps.append(lambda: self.adb('uninstall', self.package, check=False, retries=0))
        for step in steps:
            try:
                step()
            except Exception as error:  # keep restoring; report what could not be put back
                print(f'cleanup: {error}', file=sys.stderr)
        return False

    # system state -------------------------------------------------------------------------------------
    def hide_helpers(self):
        """Remove tool and Play-services helper packages for the current user (pm install-existing restores)."""
        self.free_uiautomation()
        for line in self.shell('pm', 'list', 'packages').split():
            name = line.removeprefix('package:')
            if HELPERS.fullmatch(name):
                self.shell('pm', 'uninstall', '--user', '0', name, check=False)

    def free_uiautomation(self):
        """Stop a client that still holds UiAutomation (agent-device's snapshothelper app_process, an old dump)."""
        for line in self.shell('ps', '-A', '-o', 'PID,ARGS', check=False).splitlines()[1:]:
            pid, _, command = line.strip().partition(' ')
            if 'snapshothelper' in command or 'uiautomator' in command:
                self.shell('kill', pid, check=False)
        self.shell('am', 'force-stop', 'com.callstack.agentdevice.snapshothelper', check=False)

    def snooze_notifications(self):
        """Snooze other apps' notifications for a week, so none reach the status bar or shade."""
        for key in self.shell('cmd', 'notification', 'list', check=False).split():
            if key.count('|') >= 2 and f'|{self.package}|' not in key:
                self.shell(f"cmd notification snooze --for 604800000 '{key}'", check=False)

    def install(self, apk, package=None):
        """Install an APK; on INSUFFICIENT_STORAGE uninstall the package (the capture build) and trim caches, then retry."""
        package = package or self.package
        for attempt in range(3):
            out = self.adb('install', '-r', '-t', str(apk), check=False, retries=0)
            if 'Success' in out:
                return
            if 'INSUFFICIENT_STORAGE' in out and attempt == 0:
                print(f'Low storage: reinstalling {package} from scratch', file=sys.stderr)
                self.adb('uninstall', package, check=False, retries=0)
            elif 'INSUFFICIENT_STORAGE' in out and attempt == 1:
                self.shell('pm', 'trim-caches', '64G', check=False)
            elif 'UPDATE_INCOMPATIBLE' in out and attempt == 0:  # signed by another machine's debug key
                self.adb('uninstall', package, check=False, retries=0)
            else:
                raise RuntimeError(f'Install of {apk} failed: {out.strip()}')
        raise RuntimeError(f'Install of {apk} failed: {out.strip()}')

    def demo(self, command, **extras):
        words = ['am', 'broadcast', '-a', DEMO, '--es', 'command', command]
        for key, value in extras.items():
            words += ['--es', key, str(value)]
        self.shell(*words, check=False)

    def status_bar(self):
        """Demo mode: 9:41, battery, full Wi-Fi, no mobile signal, SIM, airplane icon or notifications."""
        self.demo('exit')
        self.demo('enter')
        self.demo('clock', hhmm='0941')
        self.demo('battery', level=self.battery, plugged='false')
        self.demo('notifications', visible='false')
        self.demo('network', wifi='show', level=4, fully='true', mobile='hide', sims=0, airplane='hide')

    def resize(self, width, height, density, settle=2):
        self.demo('exit')
        self.shell('wm', 'size', f'{width}x{height}')
        self.shell('wm', 'density', density)
        self.width, self.height, self.density = width, height, density
        time.sleep(settle)
        self.status_bar()

    def use(self, profile, width, height, density):
        """Switch to a capture size by hand (profiles() does this for every profile in profiles.json)."""
        self.profile = profile
        self.resize(width, height, density)

    def profiles(self, only=None):
        """Yield each capture profile's name (profiles.json entries with captureWidth/Height/Density) after
        setting its display size, density and status bar. only: names to keep (default: --profile)."""
        only = only or ([] if self.selected in (None, 'all') else self.selected.split(','))
        config = json.loads((self.listing / 'profiles.json').read_text())
        unknown = set(only) - set(config)
        if unknown:
            raise SystemExit(f'No profile {", ".join(sorted(unknown))} in profiles.json')
        for name, p in config.items():
            if 'captureDensity' not in p or (only and name not in only):
                continue
            self.scope = None
            self.use(name, p['captureWidth'], p['captureHeight'], p['captureDensity'])
            yield name

    def night(self, on, settle=2):
        self.shell('cmd', 'uimode', 'night', 'yes' if on else 'no')
        self.theme = 'dark' if on else 'light'
        time.sleep(settle)

    def animations(self, on):
        for key in SCALES:
            self.setting('global', key, '1' if on else '0')

    def set_clock(self, hhmm='09:41'):
        """Set the emulator's clock to today at hhmm (matching the status bar); the host time comes back on exit."""
        import datetime
        hour, minute = map(int, hhmm.split(':'))
        moment = datetime.datetime.now().replace(hour=hour, minute=minute, second=0, microsecond=0)
        self.clock_set = True
        self.setting('global', 'auto_time', '0')
        self.shell('cmd', 'alarm', 'set-time', str(int(moment.timestamp() * 1000)))

    def rotate(self, landscape, settle=4):
        self.setting('system', 'accelerometer_rotation', '0')
        self.setting('system', 'user_rotation', '1' if landscape else '0')
        time.sleep(settle)

    # app ----------------------------------------------------------------------------------------------
    def component(self, activity=None):
        activity = activity or self.activity
        if '/' in activity:
            return activity
        return f'{self.package}/{self.package + activity if activity.startswith(".") else activity}'

    def launch(self, activity=None, clear=False, wait=4, **extras):
        """Force-stop and start the app; clear=True wipes its data first (the app language is set again).
        Extras become intent extras (bool --ez, int --ei, otherwise --es)."""
        self.shell('am', 'force-stop', self.package)
        if clear:
            self.shell('pm', 'clear', self.package)
            if self.app_locale:
                set_app_locale(self._uitree_adb, self.package, self.locale)
        words = ['am', 'start', '-W', '-n', self.component(activity)]
        for key, value in extras.items():
            flag = '--ez' if isinstance(value, bool) else '--ei' if isinstance(value, int) else '--es'
            words += [flag, key, str(value).lower() if isinstance(value, bool) else str(value)]
        self.shell(*words)
        time.sleep(wait)

    def app_files(self):
        return self.shell('run-as', self.package, 'ls', 'files', check=False, retries=0)

    def top_activity(self):
        return self.shell('dumpsys', 'activity', 'activities').split('topResumedActivity=', 1)[-1][:200]

    # UI tree ------------------------------------------------------------------------------------------
    def dump(self, foreground=True, timeout=60):
        """Read the UI tree (self.xml) and return its nodes. Waits for the app to draw, taps Wait on an
        "isn't responding" dialog and frees UiAutomation from a stale client."""
        deadline = time.time() + timeout
        problem = 'no UI tree'
        while True:
            self.shell('rm', '-f', '/sdcard/studio-capture.xml', check=False, retries=0)
            out = self.shell('uiautomator', 'dump', '--compressed', '/sdcard/studio-capture.xml', check=False, retries=0)
            if 'already registered' in out or 'ERROR' in out:
                problem = out.strip()
                self.free_uiautomation()
            else:
                xml = self.shell('cat', '/sdcard/studio-capture.xml', check=False, retries=0)
                if xml.lstrip().startswith('<?xml'):
                    nodes = nodes_from_xml(xml)
                    wait = next((n for n in nodes if n.get('id') == 'android:id/aerr_wait' or
                                 (n.get('pkg') == 'android' and n.get('text') == 'Wait')), None)
                    if wait:
                        self.tap_box(wait['b'])
                        problem = 'an "isn\'t responding" dialog'
                        time.sleep(3)
                        continue
                    self.xml = xml
                    if not foreground or any(n.get('pkg') == self.foreground for n in nodes):
                        return nodes
                    problem = f'{self.foreground} is not in the foreground'
            if time.time() > deadline:
                raise AssertionError(f'{self.profile}: {problem}')
            time.sleep(1.5)

    @staticmethod
    def matcher(label, text_only=False, within=None):
        pattern = re.compile(label) if isinstance(label, str) else label
        return pattern, lambda n: ((within is None or within(n['b'])) and
                                   bool(pattern.fullmatch(n.get('text', '')) or
                                        (not text_only and pattern.fullmatch(n.get('desc', '')))))

    def find(self, label, text_only=False, within=None, key=None, attempts=6, pause=1.2):
        """The node to tap for `label`: recorded (reference locale) or replayed (others) through the tap map.
        within(bounds) narrows the match by position; key replaces the label in the tap map key."""
        pattern, matches = self.matcher(label, text_only, within)
        scope = self.scope or f'{self.profile}|{self.theme}'
        for attempt in range(attempts):
            self.dump()
            try:
                if self.navigator:
                    return self.navigator.resolve(f'{scope}|{key or pattern.pattern}', self.xml, matches)
                node = next((n for n in nodes_from_xml(self.xml) if matches(n)), None)
                if node is None:
                    raise AssertionError(f'No "{pattern.pattern}" on screen')
                return node
            except AssertionError:
                if attempt == attempts - 1:
                    raise
                time.sleep(pause)

    def present(self, label, text_only=False, within=None):
        """Whether `label` is on screen now (by label in every locale, not through the tap map)."""
        matches = self.matcher(label, text_only, within)[1]
        return any(matches(n) for n in self.dump())

    def wait_for(self, label, timeout=40, text_only=False, within=None):
        matches = self.matcher(label, text_only, within)[1]
        deadline = time.time() + timeout
        while time.time() < deadline:
            node = next((n for n in self.dump() if matches(n)), None)
            if node:
                return node
            time.sleep(2)
        raise AssertionError(f'{label} never appeared')

    def tap_box(self, b):
        self.shell('input', 'tap', (b[0] + b[2]) // 2, (b[1] + b[3]) // 2)

    def tap(self, label, wait=1.5, **find):
        node = self.find(label, **find)
        self.tap_box(node['b'])
        time.sleep(wait)
        return node

    def back(self, wait=1.2):
        self.key('KEYCODE_BACK', wait)

    def key(self, code, wait=1):
        self.shell('input', 'keyevent', code)
        time.sleep(wait)

    def type(self, text, wait=1):
        self.shell('input', 'text', text.replace(' ', '%s'))
        time.sleep(wait)

    def swipe(self, x1, y1, x2, y2, duration=300, wait=1.2):
        self.shell('input', 'swipe', int(x1), int(y1), int(x2), int(y2), duration)
        time.sleep(wait)

    def scroll(self, fraction=.5, up=False, x=.5, start=.8, duration=300, wait=1.2):
        """Swipe by `fraction` of the screen height from `start` (fractions; x across the width)."""
        a, b = start, start - fraction
        if up:
            a, b = b, a
        self.swipe(self.width * x, self.height * a, self.width * x, self.height * b, duration, wait)

    def drag(self, distance, wait=1.2):
        """A slow drag scrolls exactly `distance` pixels (no fling)."""
        start = int(self.height * .8)
        self.swipe(self.width // 2, start, self.width // 2, start - distance, 1400, wait)

    def scroll_to(self, label, top, attempts=12):
        """Scroll until the element labelled `label` sits near `top` (a fraction of the screen height)."""
        matches = self.matcher(label)[1]
        for _ in range(attempts):
            node = next((n for n in self.dump() if matches(n)), None)
            if node:
                y = node['b'][1]
                if y <= self.height * top + 40:
                    if y > self.height * top + 8:
                        self.drag(int(y - self.height * top))
                    return
                self.drag(min(int(y - self.height * top), int(self.height * .55)))
            else:
                self.drag(int(self.height * .5))
        raise AssertionError(f'Could not scroll to {label}')

    # captures -----------------------------------------------------------------------------------------
    def screencap(self):
        """The display as PNG bytes (the emulator's second display adds a warning before the PNG header)."""
        if self.display is None:
            ids = re.search(r'Display (\d+) \(HWC display 0\)', self.shell('dumpsys', 'SurfaceFlinger', '--display-id', check=False))
            self.display = ids.group(1) if ids else ''
        run = subprocess.run(['adb', '-s', self.serial, 'exec-out', 'screencap', '-p', *(['-d', self.display] if self.display else [])],
                             capture_output=True, timeout=120, check=True)
        start = run.stdout.find(PNG)
        if start < 0:
            raise RuntimeError(f'screencap returned no PNG: {run.stdout[:200]!r}')
        return run.stdout[start:]

    def target(self, name):
        if isinstance(name, Path):
            return name
        if '/' in name or '.' in name:
            return self.raw / name
        return self.raw / self.profile / f'{name}.png'

    def shot(self, *names, settle=1.2, animated=False, check=None, either=False):
        """Capture the screen to each target and return their paths. A plain name is
        <locale>/raw/<profile>/<name>.png; a relative path with a folder or extension sits under
        <locale>/raw; a Path is used as is. Files under raw get a .nodes.json layout sidecar.
        animated: read the layout with animations off, then let the real motion run and grab a frame.
        check(nodes) can refuse a screen; either=True accepts the size in either orientation.
        Skipped (returns []) when --only does not list the first name's stem."""
        paths = [self.target(name) for name in names]
        if self.only and paths[0].stem not in self.only:
            return []
        time.sleep(settle)
        nodes = self.dump()
        xml = self.xml
        dialog = next((n for n in nodes if n.get('pkg') == 'android' and SYSTEM_DIALOG.fullmatch(n.get('text', ''))), None)
        if dialog:
            raise AssertionError(f'{self.profile}/{paths[0].name}: a system dialog is on screen: {dialog["text"]}')
        if check and check(nodes) is False:
            raise AssertionError(f'{self.profile}/{paths[0].name}: the screen failed its check')
        if animated:
            self.animations(True)
            time.sleep(4.5)
        try:
            png = self.screencap()
        finally:
            if animated:
                self.animations(False)
        size = int.from_bytes(png[16:20], 'big'), int.from_bytes(png[20:24], 'big')
        expected = {(self.width, self.height)} | ({(self.height, self.width)} if either else set())
        if size not in expected:
            raise AssertionError(f'{self.profile}/{paths[0].name}: got {size[0]}x{size[1]}, expected {self.width}x{self.height}')
        image = None
        for path in paths:
            path.parent.mkdir(parents=True, exist_ok=True)
            if image is None:
                from io import BytesIO
                from PIL import Image
                image = Image.open(BytesIO(png)).convert('RGB')
            if path.suffix.lower() in ('.jpg', '.jpeg'):
                image.save(path, quality=95, subsampling=0)
            else:
                image.save(path, optimize=True)
            if path.resolve().is_relative_to(self.raw.resolve()):
                write_sidecar(path, xml, *size)
            print(path.relative_to(self.listing.parent) if path.resolve().is_relative_to(self.listing.parent) else path, flush=True)
        return paths


def main():
    p = argparse.ArgumentParser(description='Capture-emulator chores outside a capture run.')
    p.add_argument('command', choices=['install', 'reset'])
    p.add_argument('apk', nargs='?')
    p.add_argument('--package', help='Application ID inside the APK (uninstalled first when storage runs out)')
    p.add_argument('--serial', default=SERIAL)
    args = p.parse_args()
    device = Device(argparse.Namespace(serial=args.serial), args.package or '', locale=False, tap_map=None, offline=False)
    if args.command == 'install':
        if not args.apk or not args.package:
            p.error('install needs an APK path and --package')
        device.hide_helpers()
        device.install(args.apk)
        print(f'Installed {args.package}')
    else:  # an interrupted run can leave display overrides, demo mode, dark mode or no animations behind
        device.demo('exit')
        for words in (('wm', 'size', 'reset'), ('wm', 'density', 'reset'), ('cmd', 'uimode', 'night', 'no'),
                      ('svc', 'wifi', 'enable'), ('svc', 'data', 'enable')):
            device.shell(*words, check=False)
        for key in SCALES:
            device.setting('global', key, '1')
        device.setting('global', 'heads_up_notifications_enabled', '1')
        device.setting('global', 'auto_time', '1')
        print(f'{args.serial} reset')


if __name__ == '__main__':
    main()
