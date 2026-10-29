#!/usr/bin/env python3
"""Build the optional TV Vibe extension in an isolated generated project; installation is explicit."""
import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--install', action='store_true', help='Install after checks; requires --tv and --adb')
    parser.add_argument('--tv', help='Already authorised ADB serial, for example IP:port')
    parser.add_argument('--adb', type=Path, help='Path to the local native adb executable')
    args = parser.parse_args()
    if args.install and (not args.tv or not args.adb):
        parser.error('--install requires --tv and --adb')
    here = Path(__file__).resolve().parent
    repo = here.parent
    out = repo / 'work' / 'tv-vibe' / ('liquid-' + str(time.time_ns()))
    out.mkdir(parents=True)
    project = out / 'launcher'
    shutil.copytree(repo/'launcher', project, ignore=shutil.ignore_patterns('build', '.gradle', 'local.properties'))
    spec = importlib.util.spec_from_file_location('vibe_patch', here/'src/patch.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.apply(project, here/'src')
    env = os.environ.copy()
    windows = os.name == 'nt'
    if windows:
        cache = Path(os.environ['LOCALAPPDATA'])/'TV-Vibe-build'
        if not (cache/'jdk17/bin/java.exe').is_file() or not (cache/'android-sdk').is_dir():
            raise SystemExit('The portable JDK/Android SDK setup must be completed first.')
        env.update(JAVA_HOME=str(cache/'jdk17'), ANDROID_HOME=str(cache/'android-sdk'),
                   ANDROID_SDK_ROOT=str(cache/'android-sdk'), ANDROID_USER_HOME=str(cache/'android-user'),
                   GRADLE_USER_HOME=str(cache/'gradle'))
        env['PATH'] = str(cache/'jdk17/bin') + os.pathsep + env.get('PATH', '')

    def run(command, **kwargs):
        return subprocess.run([str(x) for x in command], check=True, env=env, **kwargs)

    def executable(path, *arguments, **kwargs):
        if windows and str(path).lower().endswith('.bat'):
            return run([os.environ.get('COMSPEC', r'C:\Windows\System32\cmd.exe'), '/d', '/c', 'call', path, *arguments], **kwargs)
        return run([path, *arguments], **kwargs)

    executable(project/('gradlew.bat' if windows else 'gradlew'), '--no-daemon', '--max-workers=2',
               '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8',
               'testReleaseUnitTest', 'lintRelease', 'assembleRelease', cwd=project)
    apk = project/'app/build/outputs/apk/release/app-release.apk'
    sdk = env.get('ANDROID_HOME') or env.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise SystemExit('Set ANDROID_HOME to verify the APK signature.')
    signer = Path(sdk)/'build-tools/34.0.0'/('apksigner.bat' if windows else 'apksigner')

    def certificate(path):
        text = executable(signer, 'verify', '--print-certs', path, capture_output=True,
                          text=True, encoding='utf-8', errors='replace').stdout
        matches = re.findall(r'Signer #\d+ certificate SHA-256 digest:\s*(\S+)', text)
        if len(matches) != 1:
            raise RuntimeError('Expected one verified signing certificate')
        return matches[0]

    cert = certificate(apk)
    (out/'build.json').write_text(json.dumps({'apk':str(apk), 'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
        'certificate_sha256':cert, 'bytes':apk.stat().st_size}, indent=2)+'\n', encoding='utf-8')
    print('BUILD COMPLETE:', apk, flush=True)
    if not args.install:
        print('No TV changes were made. See README.md for installation and verification.', flush=True)
        return
    adb = args.adb.resolve()

    def device(*command):
        return run([adb, '-s', args.tv, *command], capture_output=True, text=True, encoding='utf-8', errors='replace').stdout

    if device('get-state').strip() != 'device':
        raise RuntimeError('The TV must already be connected and authorised')
    home = device('shell', 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME')
    if 'com.gathin.tvhome/' not in home:
        raise RuntimeError('TV Vibe must already be the default Home; this installer does not change Home')
    (out/'home-before.txt').write_text(home, encoding='utf-8')
    for name, command in [('packages','pm list packages -f --user 0'), ('disabled','pm list packages -d --user 0')]:
        (out/(name+'-before.txt')).write_text(device('shell', command), encoding='utf-8')
    theme = device('shell', 'content read --uri content://com.gathin.tvhome.theme/config.json')
    json.loads(theme)
    (out/'theme-before.json').write_text(theme, encoding='utf-8')
    # Pull the APK actually installed on the TV, rather than assuming a local build matches it.
    paths = device('shell','pm path com.gathin.tvhome').splitlines()
    paths = [line[len('package:'):].strip() for line in paths if line.startswith('package:')]
    if len(paths) != 1:
        raise RuntimeError('Split APK or missing installed launcher; stop without changing it')
    old = out/'previous-launcher.apk'
    run([adb, '-s', args.tv, 'pull', paths[0], old], capture_output=True)
    if certificate(old) != cert:
        raise RuntimeError('Signing certificate differs; keep the installed launcher')
    restore = out/'Restore-previous-launcher.py'
    restore.write_text('import subprocess\n' +
        'subprocess.run(' + repr([str(adb), '-s', args.tv, 'install', '-r', str(old)]) + ',check=True)\n' +
        'subprocess.run(' + repr([str(adb), '-s', args.tv, 'shell', 'am start -W -n com.gathin.tvhome/.MainActivity']) + ',check=True)\n', encoding='utf-8')
    print('Rollback: python "'+str(restore)+'"', flush=True)

    def screenshot(name):
        remote = '/data/local/tmp/tv-vibe-check.png'
        try:
            device('shell','screencap -p '+remote)
            run([adb, '-s', args.tv, 'pull', remote, out/name], capture_output=True)
            if (out/name).stat().st_size == 0:
                raise RuntimeError('TV returned an empty screenshot')
        finally:
            device('shell','rm -f '+remote)

    device('shell','am start -W -n com.gathin.tvhome/.MainActivity')
    time.sleep(2)
    screenshot('before.png')
    try:
        if 'Success' not in device('install', '-r', str(apk)):
            raise RuntimeError('Install did not report Success')
        device('shell','am start -W -n com.gathin.tvhome/.MainActivity')
        time.sleep(3)
        device('shell','input keyevent KEYCODE_HOME')
        time.sleep(2)
        resumed = '\n'.join(line for line in device('shell','dumpsys activity activities').splitlines()
                            if 'mResumedActivity' in line or 'topResumedActivity' in line)
        (out/'focus.txt').write_text(resumed, encoding='utf-8')
        if 'com.gathin.tvhome/' not in resumed:
            raise RuntimeError('Launcher did not hold focus')
    except Exception:
        print('Restoring previous launcher...', flush=True)
        if 'Success' not in device('install', '-r', str(old)):
            raise RuntimeError('Automatic rollback failed; keep the TV on and share the output')
        device('shell','am start -W -n com.gathin.tvhome/.MainActivity')
        raise
    try:
        time.sleep(4)
        screenshot('after.png')
    except Exception as error:
        print('Installed; screenshot failed:', error, flush=True)
    print('INSTALLED. Verify physical remote, layout and reboot persistence. Folder:', out, flush=True)


if __name__ == '__main__':
    main()
