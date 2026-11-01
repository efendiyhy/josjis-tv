#!/usr/bin/env python3
"""Verify Android TV launchers without hardcoding a particular app version."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

project = Path(__file__).resolve().parents[1]
android = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(project / 'app/src/main/AndroidManifest.xml').getroot()
app = manifest.find('application')
assert app is not None, 'Android manifest requires <application>'

activity = next(
    (element for element in app.findall('activity')
     if element.get(android + 'name') == '.MainActivity'),
    None,
)
assert activity is not None, 'MainActivity missing from AndroidManifest.xml'
assert activity.get(android + 'exported') == 'true', 'MainActivity must be exported'

categories = set()
for intent_filter in activity.findall('intent-filter'):
    actions = {a.get(android + 'name') for a in intent_filter.findall('action')}
    if 'android.intent.action.MAIN' in actions:
        categories.update(c.get(android + 'name') for c in intent_filter.findall('category'))
for name in ('android.intent.category.LAUNCHER', 'android.intent.category.LEANBACK_LAUNCHER'):
    assert name in categories, f'Missing launcher category: {name}'

leanback = next(
    (feature for feature in manifest.findall('uses-feature')
     if feature.get(android + 'name') == 'android.software.leanback'),
    None,
)
assert leanback is not None, 'Missing Android TV leanback uses-feature'
assert leanback.get(android + 'required') == 'false', 'leanback must be optional for TV boxes'

build_gradle = (project / 'app/build.gradle').read_text(encoding='utf-8')
version_code = re.search(r'\bversionCode\s+(\d+)\b', build_gradle)
version_name = re.search(r'\bversionName\s+[\'\"]([^\'\"]+)[\'\"]', build_gradle)
assert version_code, 'No versionCode in app/build.gradle'
assert int(version_code.group(1)) > 0, 'versionCode must be positive'
assert version_name and version_name.group(1).strip(), 'No valid versionName in app/build.gradle'
print(f'PASS: Android TV launchers and version {version_name.group(1)} '
      f'(code {version_code.group(1)})')
