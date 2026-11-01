#!/usr/bin/env python3
"""Static validation for the Android TV and conventional launcher entry points."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
android = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
app = manifest.find('application')
assert app is not None
activity = next((e for e in app.findall('activity')
                 if e.get(android + 'name') == '.MainActivity'), None)
assert activity is not None, 'MainActivity missing'
assert activity.get(android + 'exported') == 'true', 'MainActivity must be exported'
filter_categories = []
for f in activity.findall('intent-filter'):
    actions = {e.get(android + 'name') for e in f.findall('action')}
    categories = {e.get(android + 'name') for e in f.findall('category')}
    if 'android.intent.action.MAIN' in actions:
        filter_categories.extend(categories)
for name in ('android.intent.category.LAUNCHER', 'android.intent.category.LEANBACK_LAUNCHER'):
    assert name in filter_categories, f'Missing {name}'
leanback = next((f for f in manifest.findall('uses-feature')
                if f.get(android + 'name') == 'android.software.leanback'), None)
assert leanback is not None
assert leanback.get(android + 'required') == 'false'
gradle = (root / 'app/build.gradle').read_text()
assert re.search(r'\bversionCode\s+2\b', gradle)
assert re.search(r'versionName\s+[\'\"]1\.0\.2[\'\"]', gradle)
print('PASS: Leanback + standard launch, exported MainActivity, TV box support, v1.0.2')
