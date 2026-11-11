"""Verify real patch outputs reject unsupported clone options and historical patches."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess

root = Path(__file__).resolve().parents[2]
out = root/'local/issue8/options'; out.mkdir(parents=True, exist_ok=True)
records = []

def run(name, command):
    p = subprocess.run([str(x) for x in command], cwd=root, capture_output=True, text=True, timeout=240)
    (out/(name+'.log')).write_text(p.stdout+p.stderr)
    return p

def checked(name, passed):
    records.append({'test': name, 'passed': bool(passed)})
    (out/'results.json').write_text(json.dumps(records, indent=2)+'\n')
    print(name+': '+('PASS' if passed else 'FAIL'), flush=True)
    assert passed, name

for name, package, permissions, providers, reason in [
    ('wrong-package', 'com.quicksearch.bond', 'true', 'true', 'Unsupported clone package'),
    ('permissions-off', 'Default', 'false', 'true', 'Custom permission declarations not isolated'),
    ('providers-off', 'Default', 'true', 'false', 'Provider authority collision'),
]:
    apk = out/(name+'.apk'); result = out/(name+'.json')
    build = run(name+'-build', [root/'scripts/patch-apk.sh', root/'inputs/gemini-base.apk', apk, result,
        '-p', root/'build/gemini-patches.jar', '-p', root/'tools/patches.mpp', '-e', 'Clone app',
        '-O', 'packageName='+package, '-O', 'updatePermissions='+permissions, '-O', 'updateProviders='+providers])
    checked(name+'-fixture-built', build.returncode == 0)
    verify = run(name+'-verify', [root/'scripts/verify-clone-contract.sh', root/'inputs/gemini-base.apk', apk])
    checked(name+'-rejected', verify.returncode != 0 and reason in verify.stdout+verify.stderr)

for i, patch in enumerate(['Account ID Fallback for Clone', 'Account Switch Lock Bypass', 'Account Sync Binder Loop Fix']):
    apk = out/f'experiment-{i}.apk'
    shutil.copyfile(root/'inputs/gemini-base.apk', apk)
    previous = hashlib.sha256(apk.read_bytes()).hexdigest()
    result = run(f'experiment-{i}', [root/'scripts/patch-apk.sh', root/'inputs/google-base.apk', apk, out/f'experiment-{i}.json',
        '--exclusive', '-p', root/'build/gemini-patches.jar', '-e', patch])
    checked(f'experiment-{i}-blocked-before-output', result.returncode != 0 and 'Unsupported historical experiment' in result.stdout+result.stderr
        and hashlib.sha256(apk.read_bytes()).hexdigest() == previous)
