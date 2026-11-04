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
import android.widget.PopupMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.nebula.launcher.accounts.AccountSession;
import com.nebula.launcher.accounts.LocalAccountProvider;
import com.nebula.launcher.accounts.LocalProfileNames;
import com.nebula.launcher.catalog.ModrinthClient;
import com.nebula.launcher.catalog.CatalogImageLoader;
import com.nebula.launcher.catalog.NebulaModInstaller;

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
    private static final String KEY_ACTIVE_NAME = "active_local_account";
    private static final String KEY_INSTALL_VERSION = "install_minecraft_version";
    private static final String KEY_INSTALL_TYPE = "install_profile_type";
    private final ExecutorService versionExecutor = Executors.newSingleThreadExecutor();

    private LinearLayout content;
    private final ModrinthClient modrinth = new ModrinthClient();
    private final CatalogImageLoader imageLoader = new CatalogImageLoader();
    private SharedPreferences preferences;
    private int screenGeneration = 0;
    private String pendingInstallType = "vanilla";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        showHome();
    }

    @Override protected void onDestroy() {
        modrinth.close();
        imageLoader.close();
        versionExecutor.shutdownNow();
        super.onDestroy();
    }

    private void base(String section) {
        screenGeneration++;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(22));
        scroll.addView(page);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setOrientation(LinearLayout.HORIZONTAL);

        Button menuButton = button("☰");
        menuButton.setTextSize(22);
        menuButton.setContentDescription("Abrir navegación");
        menuButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        topBar.addView(menuButton, new LinearLayout.LayoutParams(dp(52), dp(48)));
        menuButton.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, menuButton);
            menu.getMenu().add("Inicio").setOnMenuItemClickListener(item -> { showHome(); return true; });
            menu.getMenu().add("Instalar Minecraft").setOnMenuItemClickListener(item -> { showInstallations(); return true; });
            menu.getMenu().add("Biblioteca").setOnMenuItemClickListener(item -> { showLibrary(); return true; });
            menu.getMenu().add("Ajustes").setOnMenuItemClickListener(item -> { showSettings(); return true; });
            menu.show();
        });

        TextView brand = text("NEBULA", 14, BLUE, true);
        brand.setLetterSpacing(.18f);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0, -2, 1);
        brandParams.leftMargin = dp(12);
        topBar.addView(brand, brandParams);

        String activeAccount = preferences.getString(KEY_ACTIVE_NAME, "");
        Button accountButton = button(activeAccount.isEmpty() ? "●" : activeAccount.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
        accountButton.setTextSize(18);
        accountButton.setContentDescription("Cuentas y perfiles");
        accountButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        topBar.addView(accountButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        accountButton.setOnClickListener(v -> {
            PopupMenu accounts = new PopupMenu(this, accountButton);
            Set<String> saved = LocalProfileNames.sanitize(preferences.getStringSet(KEY_LOCAL_NAMES, Collections.emptySet()));
            List<String> sorted = new ArrayList<>(saved);
            Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
            if (sorted.isEmpty()) {
                accounts.getMenu().add("Sin cuentas guardadas").setEnabled(false);
            } else {
                for (String name : sorted) {
                    accounts.getMenu().add(name).setOnMenuItemClickListener(item -> {
                        preferences.edit().putString(KEY_ACTIVE_NAME, name).apply();
                        showHome();
                        return true;
                    });
                }
            }
            accounts.getMenu().add("＋ Crear cuenta").setOnMenuItemClickListener(item -> { showCreateAccountMenu(); return true; });
            accounts.getMenu().add("Administrar cuentas").setOnMenuItemClickListener(item -> { showAccounts(); return true; });
            accounts.show();
        });
        page.addView(topBar);

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

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void openMinecraftRuntime() {
        // The Mojo runtime is embedded in this same APK; never launch a second package.
        Intent runtimeIntent = new Intent();
        runtimeIntent.setClassName(this, "git.artdeell.mojo.LauncherActivity");
        startActivity(runtimeIntent);
    }

    private void showHome() {
        base("Inicio");
        card("Bienvenido a Nebula",
                "Tu espacio para preparar Minecraft Java en Android. La interfaz, el catálogo y los perfiles locales ya tienen funciones iniciales.");
        Button play = button("Entrar a Minecraft");
        play.setEnabled(true);
        play.setOnClickListener(v -> openMinecraftRuntime());
        content.addView(play, match());
        TextView runtime = text("Nebula incluye el motor de Minecraft integrado en esta misma aplicación. Desde él puedes instalar y ejecutar Minecraft Vanilla, Forge, Fabric y otros cargadores compatibles.", 12, MUTED, false);
        LinearLayout.LayoutParams runtimeParams = match();
        runtimeParams.topMargin = dp(8);
        content.addView(runtime, runtimeParams);
        card("Biblioteca", "Explora mods, modpacks, shaders y paquetes de recursos en Modrinth.");
        String activeName = preferences.getString(KEY_ACTIVE_NAME, "");
        card("Perfil seleccionado", activeName.isEmpty() ? "Todavía no has seleccionado un perfil local." : activeName + " · Perfil local sin autenticación en línea.");
        card("Cuentas", "Puedes crear, seleccionar y conservar perfiles locales sin conexión. El acceso Microsoft y Ely.by sigue pendiente.");
        card("Tu configuración", "Minecraft " + selectedVersion() + " · " + selectedLoaderLabel());
    }

    private void showInstallations() {
        base("Instalar Minecraft");
        card("Versiones oficiales", "Nebula consulta el manifiesto público de Mojang para mostrar versiones reales de Minecraft Java. Selecciona una versión y el tipo de instalación que quieres preparar.");

        TextView versionLabel = text("Versión de Minecraft", 16, TEXT, true);
        content.addView(versionLabel, match());
        Spinner versions = new Spinner(this);
        versions.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Cargando versiones oficiales…"}));
        LinearLayout.LayoutParams versionParams = match();
        versionParams.topMargin = dp(8);
        content.addView(versions, versionParams);

        TextView optiLabel = text("Gráficos y rendimiento", 17, TEXT, true);
        LinearLayout.LayoutParams optiLabelParams = match();
        optiLabelParams.topMargin = dp(20);
        content.addView(optiLabel, optiLabelParams);
        Button optifine = button("OptiFine");
        content.addView(optifine, match());
        optifine.setOnClickListener(v -> selectInstallType("optifine", "OptiFine"));

        Button vanilla = button("Vanilla · Minecraft original");
        LinearLayout.LayoutParams vanillaParams = match();
        vanillaParams.topMargin = dp(8);
        content.addView(vanilla, vanillaParams);
        vanilla.setOnClickListener(v -> selectInstallType("vanilla", "Vanilla"));

        TextView loaderHeading = text("Cargador de mods", 19, TEXT, true);
        LinearLayout.LayoutParams loaderHeadingParams = match();
        loaderHeadingParams.topMargin = dp(22);
        content.addView(loaderHeading, loaderHeadingParams);
        content.addView(text("Instalaciones separadas para mods. El orden sigue tu preferencia.", 12, MUTED, false), match());

        String[] loaderNames = {"Forge", "Fabric", "NeoForge", "Quilt"};
        String[] loaderKeys = {"forge", "fabric", "neoforge", "quilt"};
        for (int i = 0; i < loaderNames.length; i++) {
            final String key = loaderKeys[i];
            Button loaderButton = button(loaderNames[i]);
            LinearLayout.LayoutParams loaderParams = match();
            loaderParams.topMargin = dp(8);
            content.addView(loaderButton, loaderParams);
            loaderButton.setOnClickListener(v -> selectInstallType(key, loaderNames[loaderKeysIndex(key, loaderKeys, loaderNames)]));
        }

        TextView selected = text("Selecciona una edición para ver el resumen.", 13, BLUE, true);
        LinearLayout.LayoutParams selectedParams = match();
        selectedParams.topMargin = dp(18);
        content.addView(selected, selectedParams);
        Button prepare = button("Preparar instalación");
        prepare.setEnabled(false);
        LinearLayout.LayoutParams prepareParams = match();
        prepareParams.topMargin = dp(10);
        content.addView(prepare, prepareParams);
        content.addView(text("La selección queda guardada en Nebula. El motor integrado administra la instalación y el lanzamiento de Minecraft y sus cargadores compatibles.", 12, MUTED, false), match());

        final String[] selectedVersion = {preferences.getString(KEY_INSTALL_VERSION, "1.20.1")};
        pendingInstallType = preferences.getString(KEY_INSTALL_TYPE, "vanilla");
        prepare.setOnClickListener(v -> {
            String version = versions.getSelectedItem() == null ? selectedVersion[0] : versions.getSelectedItem().toString();
            preferences.edit().putString(KEY_INSTALL_VERSION, version).putString(KEY_INSTALL_TYPE, pendingInstallType).apply();
            selected.setText("Selección guardada: Minecraft " + version + " · " + pendingInstallType
                    + ". Abre el motor integrado para completar la instalación.");
            openMinecraftRuntime();
        });
        Runnable updateSelection = () -> {
            String version = versions.getSelectedItem() == null ? selectedVersion[0] : versions.getSelectedItem().toString();
            selected.setText("Seleccionado: Minecraft " + version + " · " + pendingInstallType);
            prepare.setEnabled(versions.getAdapter() != null && versions.getAdapter().getCount() > 0
                    && versions.getSelectedItem() != null);
        };
        versions.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (parent.getItemAtPosition(position) != null) {
                    selectedVersion[0] = parent.getItemAtPosition(position).toString();
                    updateSelection.run();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { prepare.setEnabled(false); }
        });
        // Restore the saved type visually in the summary; the user can change it with any option above.
        selected.setText("Seleccionado: Minecraft " + selectedVersion[0] + " · " + pendingInstallType);
        fetchMinecraftVersions(versions, selectedVersion, updateSelection);
    }

    private int loaderKeysIndex(String key, String[] keys, String[] labels) {
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(key)) return i;
        return 0;
    }

    private void selectInstallType(String type, String label) {
        pendingInstallType = type;
        preferences.edit().putString(KEY_INSTALL_TYPE, type).apply();
        Toast.makeText(this, label + " seleccionado. Pulsa Preparar instalación para continuar en el motor integrado.", Toast.LENGTH_SHORT).show();
    }

    private void fetchMinecraftVersions(Spinner spinner, String[] selectedVersion, Runnable onLoaded) {
        final int token = screenGeneration;
        versionExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");
                int status = connection.getResponseCode();
                if (status != 200) throw new java.io.IOException("HTTP " + status);
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (body.length() + line.length() > 2_000_000) throw new java.io.IOException("El manifiesto es demasiado grande");
                        body.append(line);
                    }
                }
                JSONObject manifest = new JSONObject(body.toString());
                JSONArray entries = manifest.getJSONArray("versions");
                ArrayList<String> ids = new ArrayList<>();
                for (int i = 0; i < entries.length(); i++) {
                    JSONObject item = entries.getJSONObject(i);
                    if ("release".equals(item.optString("type"))) ids.add(item.getString("id"));
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || token != screenGeneration) return;
                    if (ids.isEmpty()) {
                        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                                new String[]{"No hay versiones disponibles"}));
                        return;
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ids);
                    spinner.setAdapter(adapter);
                    int restore = ids.indexOf(selectedVersion[0]);
                    if (restore >= 0) spinner.setSelection(restore);
                    onLoaded.run();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || token != screenGeneration) return;
                    spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                            new String[]{"No se pudieron cargar las versiones"}));
                    Toast.makeText(this, "No se pudo consultar el manifiesto de Mojang.", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void showLibrary() {
        base("Biblioteca");
        card("Explora el catálogo", "Al entrar se muestran los proyectos populares. Usa los filtros para elegir mods, modpacks, shaders o paquetes de recursos.");
        EditText query = edit("Buscar en todo Modrinth");
        content.addView(query, match());
        Spinner type = new Spinner(this);
        String[] labels = {"Mods", "Modpacks", "Shaders", "Resource packs"};
        String[] values = {"mod", "modpack", "shader", "resourcepack"};
        type.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        LinearLayout.LayoutParams typeParams = match(); typeParams.topMargin = dp(8); content.addView(type, typeParams);
        TextView filters = text("Minecraft " + selectedVersion() + " · " + selectedLoaderLabel(), 12, MUTED, false);
        LinearLayout.LayoutParams filterParams = match(); filterParams.topMargin = dp(8); content.addView(filters, filterParams);
        query.setSingleLine(true);
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        Button search = button("Aplicar búsqueda y filtros");
        LinearLayout.LayoutParams searchParams = match(); searchParams.topMargin = dp(10); content.addView(search, searchParams);
        LinearLayout results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams resultParams = match(); resultParams.topMargin = dp(12); content.addView(results, resultParams);
        Button more = button("Cargar más proyectos");
        more.setVisibility(View.GONE);
        LinearLayout.LayoutParams moreParams = match(); moreParams.topMargin = dp(8); content.addView(more, moreParams);
        final int[] offset = {0};
        final int[] total = {0};
        final boolean[] loading = {false};
        final int screenToken = screenGeneration;
        final int[] requestGeneration = {0};
        Runnable[] loadPage = new Runnable[1];
        loadPage[0] = () -> {
            if (loading[0]) return;
            loading[0] = true; search.setEnabled(false); more.setEnabled(false);
            if (offset[0] == 0) results.removeAllViews();
            results.addView(text(offset[0] == 0 ? "Cargando proyectos…" : "Cargando más proyectos…", 13, MUTED, false));
            final int requestToken = requestGeneration[0];
            String version=selectedVersion(), loader=preferences.getString(KEY_LOADER,"forge");
            String projectType=values[type.getSelectedItemPosition()];
            modrinth.search(query.getText().toString(), projectType, version, loader, offset[0], (projects,totalHits,error) ->
                runOnUiThread(() -> {
                    if(isFinishing()||isDestroyed()||screenToken!=screenGeneration||requestToken!=requestGeneration[0])return;
                    loading[0]=false;search.setEnabled(true);more.setEnabled(true);
                    if(offset[0]==0)results.removeAllViews();
                    if(error!=null){results.addView(text("No se pudo cargar el catálogo: "+error,13,MUTED,false));more.setVisibility(View.GONE);return;}
                    total[0]=totalHits;
                    if(offset[0]==0)results.addView(text(totalHits+" proyectos disponibles · ordenados por relevancia",12,BLUE,true));
                    if(projects.isEmpty()&&offset[0]==0){results.addView(text("No se encontraron proyectos con esos filtros.",13,MUTED,false));}
                    for(ModrinthClient.Project project:projects)addProjectResult(results,project);
                    offset[0]+=projects.size();
                    more.setVisibility(offset[0]<total[0]&&!projects.isEmpty()?View.VISIBLE:View.GONE);
                    if(offset[0]<total[0]&&!projects.isEmpty())more.setText("Cargar más · "+offset[0]+" de "+total[0]);
                }));
        };
        search.setOnClickListener(v->{requestGeneration[0]++;loading[0]=false;offset[0]=0;loadPage[0].run();});
        query.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                search.performClick();
                return true;
            }
            return false;
        });
        more.setOnClickListener(v->loadPage[0].run());
        loadPage[0].run();
        TextView note=text("Las fichas muestran datos oficiales de Modrinth. La instalación automática y la resolución segura de archivos y dependencias todavía no están habilitadas.",12,MUTED,false);
        LinearLayout.LayoutParams noteParams=match();noteParams.topMargin=dp(14);content.addView(note,noteParams);
    }

    private void addProjectResult(LinearLayout results, ModrinthClient.Project project) {
        LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(12),dp(12),dp(12),dp(12));
        GradientDrawable bg=new GradientDrawable();bg.setColor(PANEL);bg.setCornerRadius(dp(16));bg.setStroke(dp(1),Color.rgb(34,58,101));item.setBackground(bg);
        ImageView icon=new ImageView(this);icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setContentDescription(project.title + " icon");
        LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(64),dp(64));iconParams.rightMargin=dp(12);item.addView(icon,iconParams);
        imageLoader.load(project.iconUrl,icon,Color.rgb(28,43,72));
        LinearLayout details=new LinearLayout(this);details.setOrientation(LinearLayout.VERTICAL);
        item.addView(details,new LinearLayout.LayoutParams(0,-2,1));
        details.addView(text(project.title,17,TEXT,true));
        TextView desc=text(project.description,12,MUTED,false);desc.setMaxLines(3);
        LinearLayout.LayoutParams descParams=match();descParams.topMargin=dp(4);details.addView(desc,descParams);
        TextView meta=text(formatDownloads(project.downloads)+" descargas · "+project.projectType,11,BLUE,false);
        LinearLayout.LayoutParams metaParams=match();metaParams.topMargin=dp(6);details.addView(meta,metaParams);
        item.setClickable(true);item.setFocusable(true);
        item.setContentDescription("Abrir ficha de " + project.title + ". " + project.description);
        item.setOnClickListener(v->showProjectDetails(project));
        LinearLayout.LayoutParams itemParams=match();itemParams.bottomMargin=dp(9);results.addView(item,itemParams);
    }

    private void showProjectDetails(ModrinthClient.Project project) {
        base("Proyecto");
        Button back=button("← Volver a la Biblioteca");
        content.addView(back,match());
        back.setOnClickListener(v->showLibrary());
        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.HORIZONTAL);hero.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams heroParams=match();heroParams.topMargin=dp(16);content.addView(hero,heroParams);
        if(project.iconUrl!=null&&!project.iconUrl.isEmpty()){
            ImageView icon=new ImageView(this);icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            icon.setContentDescription("Icono de " + project.title);
            hero.addView(icon,new LinearLayout.LayoutParams(dp(92),dp(92)));imageLoader.load(project.iconUrl,icon,Color.rgb(28,43,72));
        }
        LinearLayout heading=new LinearLayout(this);heading.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headingParams=new LinearLayout.LayoutParams(0,-2,1);headingParams.leftMargin=dp(14);hero.addView(heading,headingParams);
        heading.addView(text(project.title,23,TEXT,true));
        TextView authorLabel=text(project.author.isEmpty()?"Cargando autores…":"Por "+project.author,13,BLUE,true);
        heading.addView(authorLabel);
        heading.addView(text(formatDownloads(project.downloads)+" descargas",11,MUTED,false));
        card("Descripción",project.description);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams=match();infoParams.bottomMargin=dp(12);content.addView(info,infoParams);
        info.addView(text("Dependencias",18,TEXT,true));
        info.addView(text("Consultando requisitos y autores…",13,MUTED,false));
        LinearLayout bodySection=new LinearLayout(this);bodySection.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodySectionParams=match();bodySectionParams.bottomMargin=dp(12);content.addView(bodySection,bodySectionParams);
        LinearLayout gallery=new LinearLayout(this);gallery.setOrientation(LinearLayout.VERTICAL);
        content.addView(gallery,match());
        final int detailScreenToken = screenGeneration;
        modrinth.getProject(project.id,(details,error)->runOnUiThread(()->{
            if(isFinishing()||isDestroyed()||detailScreenToken!=screenGeneration)return;
            info.removeAllViews();gallery.removeAllViews();
            if(error!=null){info.addView(text("No se pudieron cargar los detalles completos: "+error,13,MUTED,false));return;}
            String authors=details.authors.isEmpty()?(project.author.isEmpty()?"Autor no especificado":project.author):android.text.TextUtils.join(", ",details.authors);
            authorLabel.setText("Por "+authors);
            info.addView(text("Dependencias",18,TEXT,true));
            if(details.dependencies.isEmpty())info.addView(text("No requiere dependencias declaradas.",13,MUTED,false));
            else for(ModrinthClient.Dependency dep:details.dependencies){
                String requirement=dep.isRequired()?"Obligatoria":("optional".equals(dep.type)?"Opcional":dep.type);
                String dependencyName=!dep.title.isEmpty()?dep.title:
                        (!dep.projectId.isEmpty()?dep.projectId:
                        (!dep.versionId.isEmpty()?"Versión específica ("+dep.versionId+")":"Dependencia declarada"));
                info.addView(text("• "+dependencyName+" · "+requirement,13,MUTED,false));
            }
            if(!details.body.trim().isEmpty()){
                bodySection.addView(text("Información del proyecto",18,TEXT,true));
                TextView bodyText=text(details.body,13,MUTED,false);
                LinearLayout.LayoutParams bodyTextParams=match();bodyTextParams.topMargin=dp(7);bodySection.addView(bodyText,bodyTextParams);
            }
            if(!details.gallery.isEmpty()){
                gallery.addView(text("Imágenes del proyecto",18,TEXT,true));
                for(ModrinthClient.GalleryImage image:details.gallery){
                    ImageView picture=new ImageView(this);picture.setAdjustViewBounds(true);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    picture.setContentDescription(image.title.isEmpty() ? "Imagen de " + project.title : image.title);
                    LinearLayout.LayoutParams p=match();p.topMargin=dp(10);gallery.addView(picture,p);
                    imageLoader.load(image.url,picture,Color.rgb(18,29,52));
                    if(image.featured)gallery.addView(text("Imagen destacada",11,BLUE,true));
                    if(!image.title.isEmpty())gallery.addView(text(image.title,12,TEXT,true));
                    if(!image.description.isEmpty())gallery.addView(text(image.description,12,MUTED,false));
                }
            }
        }));
        Button install=button("⬇ Instalar en Minecraft");
        LinearLayout.LayoutParams installParams=match();installParams.topMargin=dp(12);content.addView(install,installParams);
        install.setOnClickListener(v -> {
            String loader = preferences.getString(KEY_LOADER, "forge");
            if ("any".equals(loader)) {
                Toast.makeText(this, "Selecciona un cargador concreto en Ajustes para instalar este mod.", Toast.LENGTH_LONG).show();
                return;
            }
            NebulaModInstaller.install(this, project.id, selectedVersion(), loader);
        });
        Button official=button("Ver página del proyecto ↗");
        LinearLayout.LayoutParams officialParams=match();officialParams.topMargin=dp(8);content.addView(official,officialParams);
        official.setOnClickListener(v->openProject(project.slug));
    }

    private void openProject(String slug) {
        if(slug==null||!slug.matches("[A-Za-z0-9_-]+")){Toast.makeText(this,"Este proyecto no tiene un enlace válido.",Toast.LENGTH_SHORT).show();return;}
        Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse("https://modrinth.com/project/"+slug));
        try{startActivity(intent);}catch(Exception error){Toast.makeText(this,"No se encontró una aplicación para abrir el enlace.",Toast.LENGTH_SHORT).show();}
    }

    private void showAccounts() {
        base("Cuentas");
        card("Tus cuentas de Minecraft", "Administra perfiles locales y, cuando estén integrados, inicia sesión con proveedores compatibles.");
        Button create = button("+ Crear cuenta");
        LinearLayout.LayoutParams createParams = match();
        createParams.bottomMargin = dp(12);
        content.addView(create, createParams);
        create.setOnClickListener(v -> showCreateAccountMenu());

        content.addView(text("Perfiles guardados", 18, TEXT, true), match());
        LinearLayout profiles = new LinearLayout(this);
        profiles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams profilesParams = match();
        profilesParams.topMargin = dp(10);
        content.addView(profiles, profilesParams);
        renderLocalProfiles(profiles);

        card("Inicio de sesión en línea", "Microsoft y Ely.by aparecerán en el menú de creación cuando sus flujos OAuth seguros estén implementados. Por ahora no se aceptan contraseñas ni tokens manuales.");
        card("Seguridad", "Los tokens de futuras sesiones se guardarán cifrados con Android Keystore. Los perfiles locales no autentican contra los servidores oficiales.");
    }

    private void showCreateAccountMenu() {
        base("Crear cuenta");
        card("Elige el tipo de cuenta", "Puedes crear ahora un perfil local sin conexión. Los proveedores en línea se habilitarán cuando sus procesos oficiales de autenticación estén completos.");
        Button local = button("Crear perfil local");
        LinearLayout.LayoutParams localParams = match();
        localParams.bottomMargin = dp(10);
        content.addView(local, localParams);
        local.setOnClickListener(v -> showCreateLocalProfile());

        Button microsoft = button("Microsoft · Próximamente");
        microsoft.setEnabled(false);
        LinearLayout.LayoutParams microsoftParams = match();
        microsoftParams.bottomMargin = dp(10);
        content.addView(microsoft, microsoftParams);

        Button ely = button("Ely.by · Próximamente");
        ely.setEnabled(false);
        content.addView(ely, match());

        Button back = button("Volver a Cuentas");
        LinearLayout.LayoutParams backParams = match();
        backParams.topMargin = dp(18);
        content.addView(back, backParams);
        back.setOnClickListener(v -> showAccounts());
    }

    private void showCreateLocalProfile() {
        base("Perfil local");
        card("Crear perfil sin conexión", "Elige un nombre de jugador de Minecraft. Este perfil sirve para preparar la interfaz, pero no verifica una cuenta oficial ni permite entrar a servidores que exijan autenticación.");

        EditText playerName = edit("Nombre de jugador (3–16 caracteres)");
        playerName.setSingleLine(true);
        content.addView(playerName, match());

        Button add = button("Guardar perfil local");
        LinearLayout.LayoutParams addParams = match();
        addParams.topMargin = dp(10);
        content.addView(add, addParams);
        add.setOnClickListener(v -> {
            String name = playerName.getText().toString().trim();
            try {
                AccountSession session = new LocalAccountProvider().create(name);
                Set<String> names = LocalProfileNames.withAdded(
                        preferences.getStringSet(KEY_LOCAL_NAMES, Collections.emptySet()),
                        session.getPlayerName());
                SharedPreferences.Editor editor = preferences.edit().putStringSet(KEY_LOCAL_NAMES, names);
                if (preferences.getString(KEY_ACTIVE_NAME, "").isEmpty()) {
                    editor.putString(KEY_ACTIVE_NAME, session.getPlayerName());
                }
                editor.apply();
                Toast.makeText(this, "Perfil local creado.", Toast.LENGTH_SHORT).show();
                showAccounts();
            } catch (IllegalArgumentException error) {
                playerName.setError(error.getMessage());
            }
        });

        Button back = button("Cancelar");
        LinearLayout.LayoutParams backParams = match();
        backParams.topMargin = dp(8);
        content.addView(back, backParams);
        back.setOnClickListener(v -> showCreateAccountMenu());
    }

    private void renderLocalProfiles(LinearLayout target) {
        target.removeAllViews();
        Set<String> stored = LocalProfileNames.sanitize(preferences.getStringSet(KEY_LOCAL_NAMES, Collections.emptySet()));
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
            boolean active = name.equals(preferences.getString(KEY_ACTIVE_NAME, ""));
            TextView label = text(name + (active ? "  ·  Seleccionado" : "  ·  Local"), 14, active ? BLUE : TEXT, true);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            row.setOnClickListener(v -> {
                preferences.edit().putString(KEY_ACTIVE_NAME, name).apply();
                renderLocalProfiles(target);
            });
            Button remove = button("Quitar");
            remove.setTextSize(11);
            row.addView(remove, new LinearLayout.LayoutParams(-2, dp(42)));
            remove.setOnClickListener(v -> {
                Set<String> updated = LocalProfileNames.without(preferences.getStringSet(KEY_LOCAL_NAMES, Collections.emptySet()), name);
                SharedPreferences.Editor editor = preferences.edit().putStringSet(KEY_LOCAL_NAMES, updated);
                if (name.equals(preferences.getString(KEY_ACTIVE_NAME, ""))) {
                    editor.putString(KEY_ACTIVE_NAME, LocalProfileNames.resolveActive(updated, ""));
                }
                editor.apply();
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
