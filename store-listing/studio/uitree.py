"""Helpers for Android capture scripts (Python 3, no dependencies).

- `nodes_from_xml(xml)`: a uiautomator dump as the studio's layout sidecar format: every node with its
  tree path ("0.2.1"), bounds [l, t, r, b] in screen pixels, and its id/description/text/class.
- `write_sidecar(image_path, xml)`: save that next to a capture as <name>.nodes.json, so callouts drawn on
  the default locale's capture can follow the same UI element in every other language.
- `set_app_locale(adb, package, locale)`: switch one app's language (Android 13+ per-app locales) without
  touching the emulator's system language.
- `Navigator`: tap by label in the default locale and record each tapped node's tree path; replay the same
  paths in other locales, so a capture flow is written once in English and runs in every language.
"""
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

BOUNDS = re.compile(r'\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]')


def nodes_from_xml(xml):
    root = ET.fromstring(xml)
    nodes = []

    def visit(element, path):
        for i, child in enumerate(element.findall('node')):
            child_path = f'{path}.{i}' if path else str(i)
            match = BOUNDS.match(child.get('bounds', ''))
            if match:
                node = {'path': child_path, 'b': [int(v) for v in match.groups()]}
                for key, attribute in (('id', 'resource-id'), ('desc', 'content-desc'), ('text', 'text'), ('cls', 'class'), ('pkg', 'package')):
                    if child.get(attribute):
                        node[key] = child.get(attribute)
                nodes.append(node)
            visit(child, child_path)

    visit(root, '')
    return nodes


def write_sidecar(image_path, xml, width=None, height=None):
    image_path = Path(image_path)
    data = {'source': 'uiautomator', 'nodes': nodes_from_xml(xml)}
    if width and height:
        data['width'], data['height'] = width, height
    sidecar = image_path.with_name(image_path.stem + '.nodes.json')
    sidecar.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    return sidecar


def set_app_locale(adb, package, locale):
    """adb: a function taking shell words. Empty or None locale resets the app to the system language."""
    adb('shell', 'cmd', 'locale', 'set-app-locales', package, '--user', 'current', '--locales', locale or '')
    result = adb('shell', 'cmd', 'locale', 'get-app-locales', package, '--user', 'current')
    if isinstance(result, bytes):
        result = result.decode()
    match = re.search(r'are \[([^\]]*)\]', result)
    if not match or match.group(1) != (locale or ''):
        raise RuntimeError(f'App language did not switch to {locale or "system"}: {result}')


class Navigator:
    """Record taps in the reference locale; replay them by tree path in every other locale.

    key: what identifies a tap within a run (profile, theme and label are a good start); taps with the
    same key are numbered in order. The map is a JSON file kept next to the captures.
    """

    def __init__(self, map_file, recording):
        self.file = Path(map_file)
        self.recording = recording
        self.paths = json.loads(self.file.read_text()) if self.file.exists() else {}
        if not recording and not self.paths:
            raise SystemExit(f'No tap map at {self.file}: capture the default locale first.')
        self.seen = {}

    def resolve(self, key, xml, matches):
        """matches(node_dict) -> bool finds the node in the reference locale. Returns the node to tap."""
        n = self.seen.get(key, 0)
        slot = f'{key}#{n}'
        nodes = nodes_from_xml(xml)
        if self.recording:
            node = next((node for node in nodes if matches(node)), None)
            if node is None:
                raise AssertionError(f'Missing control: {key}')
            self.paths[slot] = node['path']
            self.seen[key] = n + 1
            return node
        path = self.paths.get(slot)
        if path is None:
            raise AssertionError(f'No reference tap for {slot}: capture this flow, profile and theme in the default locale first.')
        node = next((node for node in nodes if node['path'] == path), None)
        if node is None:
            raise AssertionError(f'Tap {slot} has no node at {path}: the UI tree differs from the default locale')
        self.seen[key] = n + 1
        return node

    def save(self):
        if self.recording:
            self.file.parent.mkdir(parents=True, exist_ok=True)
            self.file.write_text(json.dumps(self.paths, indent=1, sort_keys=True) + '\n')
