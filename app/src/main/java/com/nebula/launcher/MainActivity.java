package com.nebula.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** First UI milestone for Nebula. Game runtime and account providers are not wired yet. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 13, 31);
    private static final int PANEL = Color.rgb(16, 28, 57);
    private static final int BLUE = Color.rgb(57, 139, 255);
    private static final int TEXT = Color.rgb(239, 245, 255);
    private static final int MUTED = Color.rgb(157, 178, 211);
    private LinearLayout content;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
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
        brand.setLetterSpacing(0.18f);
        page.addView(brand);
        TextView title = text(section, 30, TEXT, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(8);
        page.addView(title, titleLp);
        TextView subtitle = text("Tu Minecraft Java, en un solo lugar.", 14, MUTED, false);
        page.addView(subtitle);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(-1, -1);
        contentLp.topMargin = dp(24);
        page.addView(content, contentLp);

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
        card("Bienvenido a Nebula", "Interfaz inicial del launcher. Estamos preparando la integración del motor de Minecraft Java.");
        Button play = button("Preparar instalación");
        play.setEnabled(false);
        content.addView(play, matchTop());
        TextView note = text("El inicio del juego estará disponible cuando integremos y probemos el runtime de MojoLauncher.", 12, MUTED, false);
        LinearLayout.LayoutParams lp = matchTop(); lp.topMargin = dp(10); content.addView(note, lp);
        card("Acceso rápido", "Biblioteca · Mods · Shaders · Resource packs");
    }

    private void showLibrary() {
        base("Biblioteca");
        card("Mods y modpacks", "Aquí conectaremos Modrinth y CurseForge, con filtros estrictos por versión de Minecraft y cargador.");
        card("Shaders", "Exploración e instalación de shader packs compatibles.");
        card("Resource packs", "Exploración e instalación de paquetes de recursos.");
        TextView status = text("Estado: catálogo todavía no conectado.", 12, MUTED, false);
        content.addView(status, matchTop());
    }

    private void showAccounts() {
        base("Cuentas");
        card("Proveedores previstos", "Microsoft · Ely.by · Cuenta local");
        card("Seguridad", "Los tokens y códigos de autenticación no se escribirán en logs ni se mostrarán en pantalla.");
        TextView status = text("Los flujos de autenticación aún no están implementados.", 12, MUTED, false);
        content.addView(status, matchTop());
    }

    private void showSettings() {
        base("Ajustes");
        card("Minecraft", "Versión objetivo inicial: 1.20.1. Los perfiles y cargadores se añadirán al integrar el runtime.");
        card("Acerca de Nebula", "Versión de interfaz 0.1.0 · Prototipo de desarrollo");
    }

    private void card(String heading, String body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(PANEL); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), Color.rgb(34, 58, 101));
        box.setBackground(bg);
        TextView h = text(heading, 17, TEXT, true); box.addView(h);
        TextView b = text(body, 13, MUTED, false);
        LinearLayout.LayoutParams bp = matchTop(); bp.topMargin = dp(7); box.addView(b, bp);
        LinearLayout.LayoutParams lp = matchTop(); lp.bottomMargin = dp(12); content.addView(box, lp);
    }

    private void addNav(LinearLayout nav, String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setTextSize(11); b.setTextColor(TEXT); b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1); lp.setMargins(dp(3), 0, dp(3), 0); nav.addView(b, lp);
        b.setOnClickListener(v -> action.run());
    }

    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE)); return b;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v;
    }
    private LinearLayout.LayoutParams matchTop() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
