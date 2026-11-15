"""Account-free dedicated emulator only; restore exact prior APKs even after failure.

This resets only the prior Google clone's test data. Never use a personal AVD.
The APK backup contains third-party code and must stay in ignored local storage.
"""
from pathlib import Path
import argparse, hashlib, json, re, signal, subprocess

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--serial', required=True)
p.add_argument('--expected-avd', required=True)
p.add_argument('--fixture', type=Path, required=True)
p.add_argument('--probe', type=Path, required=True)
p.add_argument('--split', type=Path, action='append', default=[])
p.add_argument('--output', type=Path, required=True)
p.add_argument('--adb', default='adb')
p.add_argument('--allow-reset-test-clone', action='store_true', required=True)
a = p.parse_args()
assert re.fullmatch(r'emulator-\d+', a.serial), 'A physical device is not allowed'
root = Path(__file__).resolve().parents[1]
out = a.output.resolve()
assert out.is_relative_to(root / 'local'), 'Keep private APK backups under ignored local/'
out.mkdir(parents=True, exist_ok=False)
adb = [a.adb, '-s', a.serial]
pkg = 'com.google.android.googlequicksearchbox.morphe'
probe = 'app.geminipatch.retentiontest'

def command(*args):
    return subprocess.check_output(adb + list(args), text=True, stderr=subprocess.STDOUT, timeout=240)

def digest(file):
    return hashlib.sha256(file.read_bytes()).hexdigest()

def account_free():
    assert not re.search(r'Account \{name=', command('shell', 'dumpsys', 'account')), 'Refusing an authenticated emulator'

assert command('emu', 'avd', 'name').splitlines()[0] == a.expected_avd
assert command('shell', 'getprop', 'ro.build.version.sdk').strip() == '31'
account_free()
assert 'package:' + probe not in command('shell', 'pm', 'list', 'packages', probe).splitlines()
paths = [s.removeprefix('package:') for s in command('shell', 'pm', 'path', pkg).splitlines()]
assert paths, 'A prior test clone is required for exact restoration'
backups, hashes = [], []
for i, path in enumerate(paths):
    file = out / f'before-{i}.apk'
    command('pull', path, str(file))
    assert digest(file) == command('shell', 'sha256sum', path).split()[0]
    backups.append(file)
    hashes.append(digest(file))
apks = [a.fixture, *a.split]
assert all(file.is_file() for file in [*apks, a.probe])

def stop(*_):
    raise SystemExit('Restoring dedicated account-free test clone')

signal.signal(signal.SIGTERM, stop)
if hasattr(signal, 'SIGHUP'):
    signal.signal(signal.SIGHUP, stop)
changed = installed = False
results = {}
try:
    changed = True
    assert 'Success' in command('uninstall', pkg)
    assert 'Success' in command('install-multiple', '--no-incremental', *map(str, apks))
    assert 'Success' in command('install', '--no-incremental', str(a.probe))
    installed = True
    command('shell', 'input', 'keyevent', 'KEYCODE_WAKEUP')
    command('shell', 'wm', 'dismiss-keyguard')
    for phase in ['setup', 'reload', 'cleared', 'native']:
        command('shell', 'am', 'force-stop', pkg)
        text = command('shell', 'am', 'instrument', '-w', '-e', 'phase', phase, probe + '/.RetentionProbe')
        (out / f'{phase}.txt').write_text(text)
        safe = dict(re.findall(r'^INSTRUMENTATION_RESULT: ([A-Za-z]+)=([^\r\n]*)', text, re.M))
        print(json.dumps({'phase': phase, **safe}), flush=True)
        assert safe.get('result') == 'PASS'
        results[phase] = safe
    (out / 'passed.json').write_text(json.dumps({'fixtureSha256': digest(a.fixture), 'probeSha256': digest(a.probe), 'phases': results}, indent=2) + '\n')
finally:
    if installed:
        subprocess.run(adb + ['uninstall', probe], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
    if changed:
        subprocess.run(adb + ['uninstall', pkg], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
        install = 'install' if len(backups) == 1 else 'install-multiple'
        assert 'Success' in command(install, '--no-incremental', *map(str, backups))
        after = [s.removeprefix('package:') for s in command('shell', 'pm', 'path', pkg).splitlines()]
        assert sorted(command('shell', 'sha256sum', path).split()[0] for path in after) == sorted(hashes)
        account_free()
        (out / 'restored.json').write_text(json.dumps({'exactApkSetRestored': True, 'accountFree': True, 'onlyDedicatedTestCloneDataReset': True}) + '\n')
        print('Restored exact prior APK set on dedicated account-free emulator', flush=True)
