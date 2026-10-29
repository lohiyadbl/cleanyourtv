from pathlib import Path
import shutil

def apply(project, payload):
    java = project / 'app/src/main/java/com/gathin/tvhome'
    for name in ('VibeDashboard.java', 'VibeRules.java', 'VibeGlass.java', 'VibeTaste.java', 'VibeGameRules.java', 'VibeGameActivity.java', 'VibeHubActivity.java', 'VibeRain.java', 'VibePosters.java', 'VibeAssistant.java', 'VibeJarvisOrb.java'):
        shutil.copy2(payload/name, java/name)
    tests = project/'app/src/test/java/com/gathin/tvhome'
    tests.mkdir(parents=True,exist_ok=True)
    for name in ('VibeRulesTest.java', 'VibeFeaturesTest.java'):
        shutil.copy2(payload/name,tests/name)
    main=java/'MainActivity.java'
    s=main.read_text(encoding='utf-8')
    def replace(old,new):
        nonlocal s
        if s.count(old)!=1: raise RuntimeError('Unsupported source; patch refused: '+old[:60])
        s=s.replace(old,new)
    replace('    @Override\n    protected void onCreate(Bundle savedInstanceState) {',
            '    private VibeDashboard vibeDashboard;\n\n    @Override\n    protected void onCreate(Bundle savedInstanceState) {')
    replace('        if (savedInstanceState == null && !openLauncherSettingsIfAsked(getIntent())) openInputsIfAsked(getIntent());',
            '        vibeDashboard = new VibeDashboard(this, () -> focusTile(lastFocused % COLUMNS), () -> firstTopButton().requestFocus());\n'
            '        if (savedInstanceState == null && !openLauncherSettingsIfAsked(getIntent())) openInputsIfAsked(getIntent());')
    replace('        super.onResume();','        super.onResume();\n        if (vibeDashboard != null) vibeDashboard.resume();')
    replace('        super.onPause();','        super.onPause();\n        if (vibeDashboard != null) vibeDashboard.pause();')
    replace('        super.onDestroy();','        super.onDestroy();\n        if (vibeDashboard != null) vibeDashboard.close();')
    replace('                focusTile(singleRow ? lastFocused : lastFocused % COLUMNS);',
            '                if (vibeDashboard != null) vibeDashboard.focus();\n                else focusTile(singleRow ? lastFocused : lastFocused % COLUMNS);')
    replace('        int count = adapter.appCount();\n        int target;',
            '        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && position < COLUMNS && vibeDashboard != null) { vibeDashboard.focus(); return true; }\n'
            '        int count = adapter.appCount();\n        int target;')
    replace('            if (grid.getPaddingTop() != want) {',
            '            int[] gridAt=new int[2],rootAt=new int[2]; grid.getLocationInWindow(gridAt);root.getLocationInWindow(rootAt);\n'
            '            want=Math.max(want,Math.round(350*density)-(gridAt[1]-rootAt[1]));\n'
            '            if (grid.getPaddingTop() != want) {')
    main.write_text(s,encoding='utf-8')
    manifest=project/'app/src/main/AndroidManifest.xml'
    s=manifest.read_text(encoding='utf-8').replace('<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />',
        '<uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />')
    s=s.replace('android:allowBackup="true"','android:allowBackup="false"')
    s=s.replace('</application>', '<activity android:name=".VibeHubActivity" android:exported="false" android:screenOrientation="landscape" />\n        <activity android:name=".VibeGameActivity" android:exported="false" android:screenOrientation="landscape" />\n    </application>')
    manifest.write_text(s,encoding='utf-8')
    gradle=project/'app/build.gradle'
    s=gradle.read_text(encoding='utf-8').replace("versionName '3.1'", "versionName '3.1-vibe-liquid'")
    gradle.write_text(s,encoding='utf-8')
