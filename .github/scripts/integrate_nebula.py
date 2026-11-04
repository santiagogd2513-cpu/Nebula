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


# Replace the legacy launcher chrome with a Nebula-branded shell. The upstream
# fragments, installers, instance manager, preferences and launch pipeline remain
# the real implementation; only the outer activity chrome is rebranded.
launcher_layout = root / "app_mojo/src/main/res/layout/activity_mojo_ui.xml"
launcher_layout.write_text("""<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/transparent">

    <TextView
        android:id="@+id/nebula_runtime_title"
        android:layout_width="0dp"
        android:layout_height="@dimen/_52sdp"
        android:gravity="center_vertical"
        android:paddingStart="@dimen/_18sdp"
        android:text="NEBULA"
        android:textColor="#FFFFFFFF"
        android:textSize="18sp"
        android:textStyle="bold"
        android:letterSpacing="0.16"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toStartOf="@+id/setting_button"
        app:layout_constraintTop_toTopOf="parent" />

    <ImageButton
        android:id="@+id/setting_button"
        android:layout_width="@dimen/_52sdp"
        android:layout_height="@dimen/_52sdp"
        android:background="?attr/selectableItemBackground"
        android:src="@drawable/ic_px_sliders"
        android:contentDescription="Ajustes de Nebula"
        android:scaleType="fitCenter"
        android:paddingVertical="@dimen/_8sdp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/container_fragment"
        android:name="git.artdeell.mojo.fragments.MainMenuFragment"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintBottom_toTopOf="@+id/progress_layout"
        app:layout_constraintTop_toBottomOf="@+id/nebula_runtime_title" />

    <com.kdt.mcgui.ProgressLayout
        android:id="@+id/progress_layout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
""")

# Add a dedicated optimization entry point to the engine's complete preferences.
pref_main = root / "app_mojo/src/main/res/xml/pref_main.xml"
pref_source = pref_main.read_text()
shortcut = '''
        <Preference
            android:key="nebula_optimization_shortcut"
            android:title="@string/nebula_optimization_title"
            android:summary="@string/nebula_optimization_summary"
            android:fragment="git.artdeell.mojo.prefs.screens.LauncherPreferenceVideoFragment"
            android:icon="@drawable/ic_px_speed"/>
'''
if "nebula_optimization_shortcut" not in pref_source:
    anchor = '<PreferenceCategory\n        android:title="@string/preference_category_main_categories"'
    if anchor not in pref_source:
        raise SystemExit("Cannot locate main settings category for optimization shortcut")
    pref_source = pref_source.replace(anchor, shortcut + "\n    " + anchor, 1)
    pref_main.write_text(pref_source)

strings = root / "app_mojo/src/main/res/values/strings.xml"
strings_source = strings.read_text()
if "nebula_optimization_title" not in strings_source:
    strings_source = strings_source.replace(
        "</resources>",
        '    <string name="nebula_optimization_title">Optimización</string>\n'
        '    <string name="nebula_optimization_summary">Resolución, renderizador y opciones de rendimiento</string>\n'
        "</resources>",
        1
    )
    strings.write_text(strings_source)

print("Nebula runtime shell and optimization settings entry integrated.")


# Rebrand the engine's main menu and all settings category labels for Nebula.
# Keep the upstream view IDs and click handlers so its real install/launch pipeline remains intact.
import xml.etree.ElementTree as ET

for values_file in [
    root / "app_mojo/src/main/res/values/strings.xml",
    root / "app_mojo/src/main/res/values-es/strings.xml",
]:
    if not values_file.exists():
        continue
    tree = ET.parse(values_file)
    resources = tree.getroot()
    replacements = {
        "mcl_tab_wiki": "Guía de Nebula",
        "mcl_button_social_media": "Comunidad",
        "mcl_option_customcontrol": "Controles de Minecraft",
        "main_install_jar_file": "Instalar archivo Java (.jar)",
        "main_share_logs": "Compartir registro de Minecraft",
        "mcl_button_open_directory": "Archivos de Minecraft",
        "main_play": "JUGAR",
        "preference_video_title": "Gráficos y rendimiento",
        "preference_video_description": "Resolución, renderizador y opciones visuales",
        "preference_control_title": "Controles",
        "preference_control_description": "Personaliza los controles táctiles y el gamepad",
        "preference_java_title": "Java y memoria",
        "preference_java_description": "Memoria, argumentos y entorno Java",
        "preference_misc_title": "Ajustes generales",
        "preference_misc_description": "Opciones generales de Nebula",
        "preference_experimental_title": "Funciones avanzadas",
        "preference_experimental_description": "Opciones experimentales del motor",
        "preference_category_main_categories": "Personalización de Nebula",
    }
    for element in resources.findall("string"):
        key = element.attrib.get("name")
        if key in replacements:
            element.text = replacements[key]
    ET.indent(tree, space="    ")
    tree.write(values_file, encoding="utf-8", xml_declaration=True)

# Keep both portrait and landscape launcher menus aligned with Nebula branding.
# The IDs are deliberately preserved because MainMenuFragment binds to them.
for layout_path in [
    root / "app_mojo/src/main/res/layout/fragment_launcher.xml",
    root / "app_mojo/src/main/res/layout-land/fragment_launcher.xml",
]:
    if not layout_path.exists():
        raise SystemExit("Missing launcher main-menu layout: " + str(layout_path))
    layout = layout_path.read_text()
    layout = layout.replace('@string/mcl_tab_wiki', '"Guía de Nebula"')
    layout = layout.replace('@string/mcl_button_social_media', '"Comunidad"')
    layout = layout.replace('@string/mcl_option_customcontrol', '"Controles de Minecraft"')
    layout = layout.replace('@string/main_install_jar_file', '"Instalar archivo Java (.jar)"')
    layout = layout.replace('@string/main_share_logs', '"Compartir registro de Minecraft"')
    layout = layout.replace('@string/mcl_button_open_directory', '"Archivos de Minecraft"')
    layout = layout.replace('@string/main_play', '"JUGAR"')
    layout_path.write_text(layout)

print("Nebula main menu and settings labels rebranded in portrait and landscape.")
