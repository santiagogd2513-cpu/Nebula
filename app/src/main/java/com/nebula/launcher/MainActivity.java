package com.nebula.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.nebula.launcher.catalog.ModrinthClient;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(7,13,31), PANEL=Color.rgb(16,28,57), BLUE=Color.rgb(57,139,255);
    private static final int TEXT=Color.rgb(239,245,255), MUTED=Color.rgb(157,178,211);
    private LinearLayout content;
    private final ModrinthClient modrinth=new ModrinthClient();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); showHome();
    }
    private void base(String section) {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout page=new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(22),dp(24),dp(22),dp(22)); scroll.addView(page);
        TextView brand=text("NEBULA",13,BLUE,true); brand.setLetterSpacing(.18f); page.addView(brand);
        TextView title=text(section,30,TEXT,true); LinearLayout.LayoutParams tl=match(); tl.topMargin=dp(8); page.addView(title,tl);
        page.addView(text("Tu Minecraft Java, en un solo lugar.",14,MUTED,false));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams cp=match(); cp.topMargin=dp(24); page.addView(content,cp);
        LinearLayout nav=new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(0,dp(12),0,dp(4));
        addNav(nav,"Inicio",this::showHome); addNav(nav,"Biblioteca",this::showLibrary); addNav(nav,"Cuentas",this::showAccounts); addNav(nav,"Ajustes",this::showSettings);
        page.addView(nav); setContentView(scroll);
    }
    private void showHome() {
        base("Inicio"); card("Bienvenido a Nebula","Interfaz Android en desarrollo. La integración del motor de Minecraft todavía no está lista.");
        Button play=button("Jugar"); play.setEnabled(false); content.addView(play,match());
        content.addView(text("El botón se activará cuando el runtime de MojoLauncher esté integrado y probado.",12,MUTED,false),match());
        card("Acceso rápido","Mods · Modpacks · Shaders · Resource packs");
    }
    private void showLibrary() {
        base("Biblioteca");
        card("Catálogo Modrinth","Busca contenido publicado en Modrinth para Minecraft 1.20.1 y Forge.");
        EditText query=new EditText(this); query.setSingleLine(true); query.setHint("Buscar mods, shaders o paquetes"); query.setHintTextColor(MUTED); query.setTextColor(TEXT); query.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE)); content.addView(query,match());
        Spinner type=new Spinner(this); String[] labels={"Mods","Modpacks","Shaders","Resource packs"}; String[] values={"mod","modpack","shader","resourcepack"};
        type.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels)); LinearLayout.LayoutParams sp=match(); sp.topMargin=dp(8); content.addView(type,sp);
        TextView status=text("Versión: 1.20.1 · Cargador: Forge",12,MUTED,false); LinearLayout.LayoutParams st=match(); st.topMargin=dp(8); content.addView(status,st);
        LinearLayout results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams rp=match(); rp.topMargin=dp(12); content.addView(results,rp);
        Button search=button("Buscar en Modrinth"); LinearLayout.LayoutParams bp=match(); bp.topMargin=dp(10); content.addView(search,bp);
        search.setOnClickListener(v->{
            search.setEnabled(false); results.removeAllViews(); results.addView(text("Buscando…",13,MUTED,false));
            modrinth.search(query.getText().toString(),values[type.getSelectedItemPosition()],"1.20.1","forge",(projects,error)->runOnUiThread(()->{
                search.setEnabled(true); results.removeAllViews();
                if(error!=null){results.addView(text("No se pudo completar la búsqueda: "+error,13,MUTED,false));return;}
                if(projects.isEmpty()){results.addView(text("No se encontraron proyectos con esos filtros.",13,MUTED,false));return;}
                for(ModrinthClient.Project p:projects){
                    LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setPadding(dp(13),dp(12),dp(13),dp(12));
                    GradientDrawable bg=new GradientDrawable(); bg.setColor(PANEL); bg.setCornerRadius(dp(14)); item.setBackground(bg);
                    item.addView(text(p.title,15,TEXT,true)); item.addView(text(p.description,12,MUTED,false));
                    item.addView(text("Descargas: "+p.downloads+" · "+p.projectType,11,MUTED,false));
                    LinearLayout.LayoutParams ip=match(); ip.bottomMargin=dp(8); results.addView(item,ip);
                }
            }));
        });
        card("Instalación","Las búsquedas funcionan. La descarga e instalación segura, dependencias y verificación de archivos todavía están pendientes.");
    }
    private void showAccounts() {
        base("Cuentas"); card("Proveedores","Microsoft · Ely.by · Cuenta local");
        card("Estado","La cuenta local está disponible como base. Los flujos Microsoft y Ely.by necesitan configuración OAuth y pruebas antes de habilitarse.");
        card("Protección","Los tokens se cifran con Android Keystore. No se registran credenciales en logs.");
    }
    private void showSettings() {
        base("Ajustes"); card("Minecraft","Objetivo inicial: Minecraft Java 1.20.1 · Forge.");
        card("Acerca de Nebula","Versión determinada automáticamente por cada compilación de GitHub Actions.");
    }
    private void card(String h,String b) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(PANEL); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1),Color.rgb(34,58,101)); box.setBackground(bg);
        box.addView(text(h,17,TEXT,true)); TextView body=text(b,13,MUTED,false); LinearLayout.LayoutParams bp=match(); bp.topMargin=dp(7); box.addView(body,bp);
        LinearLayout.LayoutParams lp=match(); lp.bottomMargin=dp(12); content.addView(box,lp);
    }
    private void addNav(LinearLayout nav,String label,Runnable action) {
        Button b=button(label); b.setTextSize(11); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1); lp.setMargins(dp(3),0,dp(3),0); nav.addView(b,lp); b.setOnClickListener(v->action.run());
    }
    private Button button(String label) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE)); return b; }
    private TextView text(String s,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private LinearLayout.LayoutParams match(){return new LinearLayout.LayoutParams(-1,-2);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}