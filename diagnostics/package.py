"""Separate diagnostic packaging; never changes the stable feed or release directory."""
from pathlib import Path
from datetime import datetime, timezone
import base64, hashlib, json, re, shutil, sys, zipfile

root = Path(__file__).resolve().parents[1]
metadata = json.loads((root / 'diagnostics/metadata.json').read_text())
version = metadata['version']
assert re.fullmatch(r'\d+\.\d+\.\d+', version)
profile = json.loads((root / 'diagnostics/profiles' / metadata['profile']).read_text())
assert profile['build'] == version
profile_bytes = (json.dumps(profile, indent=2, ensure_ascii=True) + '\n').encode('utf-8')
out = Path(sys.argv[2]).resolve()
assert out == root / 'build/diagnostics'
mode = sys.argv[1]


def token(value):
    """Opaque build code; the version is not readable from a report or the app screen."""
    digest = hashlib.sha256(('geminipatch-diagnostics:' + value).encode('ascii')).digest()
    return base64.b32encode(digest).decode('ascii').lower()[:10]


def quote(s):
    return json.dumps(s, ensure_ascii=True)


def write_jar(path, entries):
    with zipfile.ZipFile(path, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, date_time=(2000, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)


if mode == 'token':
    print(token(sys.argv[3] if len(sys.argv) > 3 else version))
elif mode == 'schema':
    for relative in ['runtime/classes', 'runtime/dex', 'patch-dex']:
        generated = (out / relative).resolve()
        assert generated.is_relative_to(out) and generated != out
        if generated.exists():
            shutil.rmtree(generated)
        generated.mkdir(parents=True)
    lines = ['package app.geminipatch.diagnostics;', 'import java.util.*;', 'public final class Schema {']
    lines.append('public static final String R = ' + quote(token(version)) + ';')
    lines.append('public static final String PROFILE_SHA256 = ' + quote(hashlib.sha256(profile_bytes).hexdigest()) + ';')
    for key, value in [('VERSION', profile['versionName']), ('STATUS', profile['statusType']), ('ERROR_ENUM', profile['errorEnum']),
                       ('CAPABILITIES', profile['capabilitiesType']), ('PUNT', profile['puntType']), ('PUNT_TITLE', profile['puntTitleField']),
                       ('PUNT_BODY', profile['puntBodyField']), ('PUNT_BEHAVIOR', profile['puntBehaviorField'])]:
        lines.append('static final String ' + key + ' = ' + quote(value) + ';')
    lines.append('static final long CODE = ' + profile['versionCode'] + 'L;')
    for name, section in [('ROOTS', 'incomingTypes'), ('PUNT_FIELDS', 'puntFields')]:
        lines.append('static final Map<String,String> ' + name + '; static { Map<String,String> m=new HashMap<String,String>();')
        for k, v in profile[section].items():
            lines.append('m.put(' + quote(k) + ',' + quote(v) + ');')
        lines.extend([name + '=Collections.unmodifiableMap(m);', '}'])
    lines.append('}')
    (out / 'runtime/Schema.java').write_text('\n'.join(lines) + '\n')
    (out / 'resources/diagnostics').mkdir(parents=True, exist_ok=True)
    (out / 'resources/diagnostics/profile.json').write_bytes(profile_bytes)
    # Manager's patch host has no JSON library for patches; this derived resource needs none.
    properties = {k: v for k, v in profile.items() if isinstance(v, str)}
    for section in ['methods', 'incomingTypes', 'expectedSites']:
        properties.update({section + '.' + k: str(v) for k, v in profile[section].items()})
    properties['versionFields.count'] = str(len(profile['versionFields']))
    for i, (field, observer) in enumerate(profile['versionFields'].items()):
        properties['versionFields.' + str(i) + '.field'] = field
        properties['versionFields.' + str(i) + '.observer'] = observer
    properties['capabilityFields.count'] = str(len(profile['capabilityFields']))
    for i, field in enumerate(profile['capabilityFields']):
        properties['capabilityFields.' + str(i)] = field
    for k, v in properties.items():
        assert re.fullmatch(r'[A-Za-z0-9_.]+', k) and v.isascii() and not any(c in v for c in '\r\n\\')
    (out / 'resources/diagnostics/profile.properties').write_text(''.join(k + '=' + v + '\n' for k, v in sorted(properties.items())), encoding='ascii')
elif mode == 'merge':
    entries = {}
    for source in [root / 'build/gemini-patches.jar', out / 'addon.jar']:
        with zipfile.ZipFile(source) as archive:
            for n in archive.namelist():
                if n.endswith('/') or n == 'META-INF/MANIFEST.MF':
                    continue
                assert n not in entries, n
                entries[n] = archive.read(n)
    for f in (out / 'resources').rglob('*'):
        if f.is_file():
            entries[f.relative_to(out / 'resources').as_posix()] = f.read_bytes()
    write_jar(out / 'diagnostics-patches.jar', entries)
elif mode == 'package':
    tag = 'v' + version
    name = 'gemini-microg-patches-' + version + '.mpp'
    description = metadata['description']
    timestamp = str(int(datetime.fromisoformat(metadata['created_at']).replace(tzinfo=timezone.utc).timestamp() * 1000))
    attributes = {'Manifest-Version': '1.0', 'Name': 'Gemini microG patches', 'Description': description, 'Version': version,
                  'Timestamp': timestamp, 'Source': 'https://github.com/ryuya0124/gemini-microg-patches', 'Author': 'ryuya0124',
                  'Website': 'https://github.com/ryuya0124/gemini-microg-patches', 'Patcher-Version': '2.5.0'}
    lines = []
    for k, v in attributes.items():
        line = k + ': ' + v
        line.encode('ascii')
        while len(line) > 70:
            lines.append(line[:70])
            line = ' ' + line[70:]
        lines.append(line)
    entries = {'META-INF/MANIFEST.MF': ('\r\n'.join(lines) + '\r\n\r\n').encode('ascii')}
    with zipfile.ZipFile(out / 'diagnostics-patches.jar') as archive:
        entries.update({n: archive.read(n) for n in archive.namelist() if not n.endswith('/') and n != 'META-INF/MANIFEST.MF'})
    dex = sorted((out / 'patch-dex').glob('classes*.dex'))
    assert dex and dex[0].name == 'classes.dex'
    for f in dex:
        entries[f.name] = f.read_bytes()
    release = out / 'release'
    release.mkdir(exist_ok=True)
    bundle = release / name
    write_jar(bundle, entries)
    feed = {'version': version, 'created_at': metadata['created_at'], 'description': description,
            'download_url': 'https://github.com/ryuya0124/gemini-microg-patches/releases/download/' + tag + '/' + name,
            'page_url': 'https://github.com/ryuya0124/gemini-microg-patches/releases/tag/' + tag}
    (release / 'diagnostics-bundle.json').write_text(json.dumps(feed, indent=2) + '\n')
    (release / 'SHA256SUMS').write_text(''.join(hashlib.sha256(f.read_bytes()).hexdigest() + '  ' + f.name + '\n' for f in [bundle, release / 'diagnostics-bundle.json']))
    print('Packaged', name, 'build code', token(version))
else:
    raise SystemExit('Unknown packaging stage')
