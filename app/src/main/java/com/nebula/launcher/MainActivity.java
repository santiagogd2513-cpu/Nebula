package com.nebula.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nebula.launcher.accounts.AccountSession;
import com.nebula.launcher.accounts.LocalAccountProvider;
import com.nebula.launcher.catalog.ModrinthClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 13, 31);
    private static final int PANEL = Color.rgb(16, 28, 57);
    private static final int BLUE = Color.rgb(57, 139, 255);
    private static final int TEXT = Color.rgb(239, 245, 255);
    private static final int MUTED = Color.rgb(157, 178, 211);
    private static final String PREFS = "nebula_launcher";
    private static final String KEY_VERSION = "minecraft_version";
    private static final String KEY_LOADER = "minecraft_loader";
    private static final String KEY_LOCAL_NAMES = "local_account_names";

    private LinearLayout content;
    private final ModrinthClient modrinth = new ModrinthClient();
    private SharedPreferences preferences;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
    }

    @Override protected void onDestroy() {
        modrinth.close();
        super.onDestroy();
    }

    private void base(String section) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(22));
        scroll.addView(page);

        TextView brand = text("NEBULA", 13, BLUE, true);
        brand.setLetterSpacing(.18f);
        page.addView(brand);

        TextView title = text(section, 30, TEXT, true);
        LinearLayout.LayoutParams titleParams = match();
        titleParams.topMargin = dp(8);
        page.addView(title, titleParams);
        page.addView(text("Tu Minecraft Java, en un solo lugar.", 14, MUTED, false));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = match();
        contentParams.topMargin = dp(24);
        page.addView(content, contentParams);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(12), 0, dp(4));
        addNav(nav, "Inicio", this::showHome);
        addNav(nav, "Biblioteca", this::showLibrary);
        addNav(nav, "Cuentas", this::showAccounts);
        addNav(nav, "Ajustes", this::showSettings);
        page.addView(nav);
        setContentView(scroll);
    }

    private void showHome() {
        base("Inicio");
        card("Bienvenido a Nebula",
                "Tu espacio para preparar Minecraft Java en Android. La interfaz, el catálogo y los perfiles locales ya tienen funciones iniciales.");
        Button play = button("Jugar");
        play.setEnabled(false);
        content.addView(play, match());
        TextView runtime = text("El motor de MojoLauncher todavía no está integrado. Jugar seguirá desactivado hasta que el runtime real esté incorporado y probado.", 12, MUTED, false);
        LinearLayout.LayoutParams runtimeParams = match();
        runtimeParams.topMargin = dp(8);
        content.addView(runtime, runtimeParams);
        card("Biblioteca", "Explora mods, modpacks, shaders y paquetes de recursos en Modrinth.");
        card("Cuentas", "Puedes crear y conservar perfiles locales sin conexión. El acceso Microsoft y Ely.by sigue pendiente.");
        card("Tu configuración", "Minecraft " + selectedVersion() + " · " + selectedLoaderLabel());
    }

    private void showLibrary() {
        base("Biblioteca");
        card("Catálogo Modrinth", "Busca proyectos para tu versión y cargador seleccionados. Toca un resultado para abrir su página oficial.");

        EditText query = edit("Buscar mods, shaders o paquetes");
        content.addView(query, match());

        Spinner type = new Spinner(this);
        String[] labels = {"Mods", "Modpacks", "Shaders", "Resource packs"};
        String[] values = {"mod", "modpack", "shader", "resourcepack"};
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        LinearLayout.LayoutParams typeParams = match();
        typeParams.topMargin = dp(8);
        content.addView(type, typeParams);

        TextView filters = text("Minecraft " + selectedVersion() + " · " + selectedLoaderLabel(), 12, MUTED, false);
        LinearLayout.LayoutParams filterParams = match();
        filterParams.topMargin = dp(8);
        content.addView(filters, filterParams);

        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams resultParams = match();
        resultParams.topMargin = dp(12);
        content.addView(results, resultParams);

        Button search = button("Buscar en Modrinth");
        LinearLayout.LayoutParams searchParams = match();
        searchParams.topMargin = dp(10);
        content.addView(search, searchParams);

        search.setOnClickListener(v -> {
            final String version = selectedVersion();
            final String loader = preferences.getString(KEY_LOADER, "forge");
            final String projectType = values[type.getSelectedItemPosition()];
            search.setEnabled(false);
            results.removeAllViews();
            results.addView(text("Buscando en Modrinth…", 13, MUTED, false));

            modrinth.search(query.getText().toString(), projectType, version, loader, (projects, error) ->
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        search.setEnabled(true);
                        results.removeAllViews();
                        if (error != null) {
                            results.addView(text("No se pudo completar la búsqueda: " + error, 13, MUTED, false));
                            return;
                        }
                        results.addView(text(projects.size() + " resultados · " + version, 12, BLUE, true));
                        if (projects.isEmpty()) {
                            results.addView(text("No se encontraron proyectos con esos filtros.", 13, MUTED, false));
                            return;
                        }
                        for (ModrinthClient.Project project : projects) {
                            addProjectResult(results, project);
                        }
                    }));
        });

        TextView note = text("Las búsquedas muestran metadatos. La instalación automática, resolución de dependencias y validación de archivos aún no están habilitadas.", 12, MUTED, false);
        LinearLayout.LayoutParams noteParams = match();
        noteParams.topMargin = dp(14);
        content.addView(note, noteParams);
    }

    private void addProjectResult(LinearLayout results, ModrinthClient.Project project) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(14), dp(13), dp(14), dp(13));
        GradientDrawable background = new GradientDrawable();
        background.setColor(PANEL);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), Color.rgb(34, 58, 101));
        item.setBackground(background);
        item.setClickable(true);
        item.setFocusable(true);

        item.addView(text(project.title, 15, TEXT, true));
        TextView description = text(project.description, 12, MUTED, false);
        description.setMaxLines(4);
        LinearLayout.LayoutParams descriptionParams = match();
        descriptionParams.topMargin = dp(5);
        item.addView(description, descriptionParams);
        TextView metadata = text(formatDownloads(project.downloads) + " descargas · " + project.projectType + " · Abrir proyecto ↗", 11, BLUE, false);
        LinearLayout.LayoutParams metadataParams = match();
        metadataParams.topMargin = dp(7);
        item.addView(metadata, metadataParams);
        item.setOnClickListener(v -> openProject(project.slug));

        LinearLayout.LayoutParams itemParams = match();
        itemParams.bottomMargin = dp(9);
        results.addView(item, itemParams);
    }

    private void openProject(String slug) {
        if (slug == null || !slug.matches("[A-Za-z0-9_-]+")) {
            Toast.makeText(this, "Este proyecto no tiene un enlace válido.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://modrinth.com/project/" + slug));
        try {
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "No se encontró una aplicación para abrir el enlace.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAccounts() {
        base("Cuentas");
        card("Perfiles locales", "Crea perfiles sin conexión para preparar la interfaz del launcher. No son cuentas autenticadas y no permiten acceder a servicios que exigen inicio de sesión oficial.");

        EditText playerName = edit("Nombre de jugador (3–16 caracteres)");
        playerName.setSingleLine(true);
        content.addView(playerName, match());
        Button add = button("Añadir perfil local");
        LinearLayout.LayoutParams addParams = match();
        addParams.topMargin = dp(9);
        content.addView(add, addParams);

        LinearLayout profiles = new LinearLayout(this);
        profiles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams profilesParams = match();
        profilesParams.topMargin = dp(16);
        content.addView(profiles, profilesParams);
        renderLocalProfiles(profiles);

        add.setOnClickListener(v -> {
            String name = playerName.getText().toString().trim();
            try {
                AccountSession session = new LocalAccountProvider().create(name);
                Set<String> names = new TreeSet<>(preferences.getStringSet(KEY_LOCAL_NAMES, Collections.emptySet()));
                names.add(session.getPlayerName());
                preferences.edit().putStringSet(KEY_LOCAL_NAMES, names).apply();
                playerName.setText("");
                renderLocalProfiles(profiles);
                Toast.makeText(this, "Perfil local añadido.", Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException error) {
                playerName.setError(error.getMessage());
            }
        });

        card("Cuentas en línea", "Microsoft y Ely.by todavía no están habilitadas. Se integrarán mediante OAuth seguro cuando estén configurados y probados sus flujos.");
        card("Protección", "Los tokens se cifran con Android Keystore. No se guardan contraseñas ni se muestran credenciales en pantalla.");
    }

    private void renderLocalProfiles(LinearLayout target) {
        target.removeAllViews();
        Set<String> stored = preferences.getStringSet(KEY_LOCAL_NAMES, Collections.emptySet());
        List<String> names = new ArrayList<>(stored);
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        if (names.isEmpty()) {
            target.addView(text("Todavía no hay perfiles guardados.", 13, MUTED, false));
            return;
        }
        for (String name : names) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(8), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(PANEL);
            bg.setCornerRadius(dp(12));
            row.setBackground(bg);
            TextView label = text(name + "  ·  Local", 14, TEXT, true);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove = button("Quitar");
            remove.setTextSize(11);
            row.addView(remove, new LinearLayout.LayoutParams(-2, dp(42)));
            remove.setOnClickListener(v -> {
                Set<String> updated = new TreeSet<>(preferences.getStringSet(KEY_LOCAL_NAMES, Collections.emptySet()));
                updated.remove(name);
                preferences.edit().putStringSet(KEY_LOCAL_NAMES, updated).apply();
                renderLocalProfiles(target);
            });
            LinearLayout.LayoutParams rowParams = match();
            rowParams.bottomMargin = dp(8);
            target.addView(row, rowParams);
        }
    }

    private void showSettings() {
        base("Ajustes");
        card("Versión de Minecraft", "Configura la versión que se utilizará al consultar el catálogo.");
        EditText version = edit("Ejemplo: 1.20.1");
        version.setSingleLine(true);
        version.setText(selectedVersion());
        content.addView(version, match());

        card("Cargador de mods", "Este ajuste filtra mods y modpacks. Shaders y paquetes de recursos no requieren filtro de cargador.");
        Spinner loader = new Spinner(this);
        String[] loaderLabels = {"Forge", "Fabric", "Quilt", "NeoForge", "Cualquiera"};
        String[] loaderValues = {"forge", "fabric", "quilt", "neoforge", "any"};
        loader.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, loaderLabels));
        String savedLoader = preferences.getString(KEY_LOADER, "forge");
        int selected = 0;
        for (int i = 0; i < loaderValues.length; i++) if (loaderValues[i].equals(savedLoader)) selected = i;
        loader.setSelection(selected);
        content.addView(loader, match());

        Button save = button("Guardar ajustes");
        LinearLayout.LayoutParams saveParams = match();
        saveParams.topMargin = dp(12);
        content.addView(save, saveParams);
        save.setOnClickListener(v -> {
            String value = version.getText().toString().trim();
            if (!value.matches("[0-9]+\\.[0-9]+(\\.[0-9]+)?")) {
                version.setError("Escribe una versión válida, por ejemplo 1.20.1");
                return;
            }
            preferences.edit()
                    .putString(KEY_VERSION, value)
                    .putString(KEY_LOADER, loaderValues[loader.getSelectedItemPosition()])
                    .apply();
            Toast.makeText(this, "Ajustes guardados.", Toast.LENGTH_SHORT).show();
        });

        card("Tema", "Nebula utiliza una interfaz oscura azul. El tema oscuro también se aplica desde el inicio de la actividad para evitar el destello de una pantalla clara al abrir.");
        card("Acerca de", "Nebula · Minecraft Java para Android · Desarrollo en curso.");
    }

    private String selectedVersion() {
        return preferences.getString(KEY_VERSION, "1.20.1");
    }

    private String selectedLoaderLabel() {
        String loader = preferences.getString(KEY_LOADER, "forge");
        if ("any".equals(loader)) return "Cualquier cargador";
        if (loader == null || loader.isEmpty()) return "Forge";
        return Character.toUpperCase(loader.charAt(0)) + loader.substring(1);
    }

    private EditText edit(String hint) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint(hint);
        field.setHintTextColor(MUTED);
        field.setTextColor(TEXT);
        field.setPadding(dp(12), dp(9), dp(12), dp(9));
        field.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));
        return field;
    }

    private String formatDownloads(long downloads) {
        if (downloads >= 1_000_000) return String.format(java.util.Locale.US, "%.1f M", downloads / 1_000_000d);
        if (downloads >= 1_000) return String.format(java.util.Locale.US, "%.1f k", downloads / 1_000d);
        return String.valueOf(downloads);
    }

    private void card(String heading, String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable background = new GradientDrawable();
        background.setColor(PANEL);
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), Color.rgb(34, 58, 101));
        box.setBackground(background);
        box.addView(text(heading, 17, TEXT, true));
        TextView bodyText = text(body, 13, MUTED, false);
        LinearLayout.LayoutParams bodyParams = match();
        bodyParams.topMargin = dp(7);
        box.addView(bodyText, bodyParams);
        LinearLayout.LayoutParams params = match();
        params.bottomMargin = dp(12);
        content.addView(box, params);
    }

    private void addNav(LinearLayout nav, String label, Runnable action) {
        Button b = button(label);
        b.setTextSize(11);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        nav.addView(b, params);
        b.setOnClickListener(v -> action.run());
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
