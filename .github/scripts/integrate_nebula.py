from pathlib import Path

root = Path("vendor/MojoLauncher")

# Embed Nebula's Android library into Mojo's only application module.
gradle = root / "app_mojo/build.gradle"
source = gradle.read_text()
source = source.replace('applicationId "git.artdeell.mjlaunch"', 'applicationId "com.nebula.launcher"', 1)
source = source.replace("'git.artdeell.mjlaunch.scoped.gamefolder.debug'", "'com.nebula.launcher.debug.scoped.gamefolder'")
source = source.replace("'git.artdeell.mjlaunch.debug'", "'com.nebula.launcher.debug'")
source = source.replace("'git.artdeell.mjlaunch'", "'com.nebula.launcher'")
source = source.replace("'git.artdeell.mjlaunch.scoped.gamefolder'", "'com.nebula.launcher.scoped.gamefolder'")
if 'implementation project(":app")' not in source:
    marker = "dependencies {"
    if marker not in source:
        raise SystemExit("Cannot locate app_mojo dependencies block")
    source = source.replace(
        marker,
        marker + '\n    // Nebula UI, catalog and profile code is packaged into this APK.\n    implementation project(":app")',
        1,
    )
gradle.write_text(source)

# Preserve Mojo's storage bootstrap and open Nebula after initialization.
test_activity = root / "app_mojo/src/main/java/git/artdeell/mojo/TestStorageActivity.java"
activity_source = test_activity.read_text()
old = "Intent intent =  new Intent(this, LauncherActivity.class);"
new = "Intent intent = new Intent(this, com.nebula.launcher.MainActivity.class);"
if old not in activity_source:
    if new not in activity_source:
        raise SystemExit("Expected TestStorageActivity hand-off not found")
else:
    test_activity.write_text(activity_source.replace(old, new, 1))

# Rebrand the runtime's visible app name.
strings = root / "app_mojo/src/main/res/values/strings.xml"
string_source = strings.read_text()
old_name = '<string name="app_short_name">MJLauncher</string>'
new_name = '<string name="app_short_name">Nebula</string>'
if old_name in string_source:
    strings.write_text(string_source.replace(old_name, new_name, 1))
elif new_name not in string_source:
    raise SystemExit("Expected launcher name resource not found")

# AGP 8 makes resource IDs non-final; avoid using them as Java switch labels.
awt = root / "app_mojo/src/main/java/git/artdeell/mojo/awt/AWTActivity.java"
awt_source = awt.read_text()
old_block = '''        switch (v.getId()) {
            case R.id.installmod_mouse_pri:
                Platform.sendMouseButtonEvent(MotionEvent.BUTTON_PRIMARY, isDown ? 1 : 0, CallbackBridge.getCurrentMods());
                break;
                
            case R.id.installmod_mouse_sec:
                Platform.sendMouseButtonEvent(MotionEvent.BUTTON_SECONDARY, isDown ? 1 : 0, CallbackBridge.getCurrentMods());
                break;
        }
        if(isDown) switch(v.getId()) {
            case R.id.installmod_window_moveup:
                AWTBridge.nativeMoveWindow(0, -10);
                break;
            case R.id.installmod_window_movedown:
                AWTBridge.nativeMoveWindow(0, 10);
                break;
            case R.id.installmod_window_moveleft:
                AWTBridge.nativeMoveWindow(-10, 0);
                break;
            case R.id.installmod_window_moveright:
                AWTBridge.nativeMoveWindow(10, 0);
                break;
        }
'''
new_block = '''        int viewId = v.getId();
        if (viewId == R.id.installmod_mouse_pri) {
            Platform.sendMouseButtonEvent(MotionEvent.BUTTON_PRIMARY, isDown ? 1 : 0, CallbackBridge.getCurrentMods());
        } else if (viewId == R.id.installmod_mouse_sec) {
            Platform.sendMouseButtonEvent(MotionEvent.BUTTON_SECONDARY, isDown ? 1 : 0, CallbackBridge.getCurrentMods());
        }
        if (isDown) {
            if (viewId == R.id.installmod_window_moveup) {
                AWTBridge.nativeMoveWindow(0, -10);
            } else if (viewId == R.id.installmod_window_movedown) {
                AWTBridge.nativeMoveWindow(0, 10);
            } else if (viewId == R.id.installmod_window_moveleft) {
                AWTBridge.nativeMoveWindow(-10, 0);
            } else if (viewId == R.id.installmod_window_moveright) {
                AWTBridge.nativeMoveWindow(10, 0);
            }
        }
'''
if old_block in awt_source:
    awt.write_text(awt_source.replace(old_block, new_block, 1))
elif "int viewId = v.getId();" not in awt_source:
    raise SystemExit("Expected AWTActivity switch block not found")


# Apply Nebula identity to the installed application, not only its in-app title.
import re
import shutil

icon_source = Path("app/src/main/res/drawable/nebula_launcher_icon.xml")
icon_target = root / "app_mojo/src/main/res/drawable/nebula_launcher_icon.xml"
icon_target.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(icon_source, icon_target)

manifest = root / "app_mojo/src/main/AndroidManifest.xml"
manifest_source = manifest.read_text()
application_match = re.search(r"<application\b[^>]*>", manifest_source)
if not application_match:
    raise SystemExit("Cannot locate Mojo application manifest element")
application_tag = application_match.group(0)
application_tag = re.sub(r'\s+android:icon="[^"]*"', "", application_tag)
application_tag = re.sub(r'\s+android:roundIcon="[^"]*"', "", application_tag)
application_tag = application_tag[:-1] + ' android:icon="@drawable/nebula_launcher_icon" android:roundIcon="@drawable/nebula_launcher_icon">'
manifest.write_text(manifest_source[:application_match.start()] + application_tag + manifest_source[application_match.end():])

print("Nebula sources embedded; Mojo storage bootstrap retained; Nebula UI and app identity applied.")
