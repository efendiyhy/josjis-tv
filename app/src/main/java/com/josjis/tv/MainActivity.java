package com.josjis.tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private final CatalogRepository catalog = new CatalogRepository();
    private final ImageLoader images = new ImageLoader();
    private UserStore user;
    private LinearLayout root;
    private FrameLayout stage;
    private String page = "home";
    private Media current;
    private List<CatalogRepository.Section> sections;
    private int season = 1;
    private int episode = 1;
    private LinearLayout episodeList;
    private TextView episodeStatus;
    private TextView seasonValue;
    private TextView episodeValue;
    private String detailKey;
    private TextView homeNavigation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); user = new UserStore(this);
        getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        sections = catalog.demoSections();
        createShell(); showHome();
        // Give the remote an initial focusable element on Android TV / AOSP TV boxes.
        if (homeNavigation != null) homeNavigation.requestFocus();
        refreshCatalog();
    }

    private int d(int v) { return Design.dp(this, v); }
    private LinearLayout vertical() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout horizontal() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : d(w), h < 0 ? h : d(h)); }
    private void space(LinearLayout v, int width) { v.addView(new View(this), lp(width,1)); }
    private void gap(LinearLayout v, int height) { v.addView(new View(this), lp(1,height)); }

    private void createShell() {
        root = vertical(); root.setBackgroundColor(Design.BG);
        LinearLayout bar = horizontal(); bar.setPadding(d(36), d(12), d(36), d(12)); bar.setBackgroundColor(0xF70D0E13);
        TextView logo = Design.text(this,"JOSJIS",29,Design.ACCENT,true);
        logo.setLetterSpacing(0.07f);
        bar.addView(logo, lp(-2,48));
        TextView tv = Design.text(this," TV",19,Design.WHITE,true); bar.addView(tv,lp(-2,48));
        space(bar,32);
        addNav(bar,"Beranda","home"); addNav(bar,"Film","movie"); addNav(bar,"Serial","tv");
        addNav(bar,"Daftar Saya","list"); addNav(bar,"Cari","search"); addNav(bar,"Pengaturan","settings");
        root.addView(bar,lp(-1,72));
        View line = new View(this); line.setBackgroundColor(0xFF20212B); root.addView(line,lp(-1,1));
        stage = new FrameLayout(this); root.addView(stage,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }
    private void addNav(LinearLayout bar, String label, String destination) {
        TextView b = Design.text(this, label, 14, Design.MUTED, true);
        b.setGravity(Gravity.CENTER); b.setPadding(d(13),0,d(13),0);
        b.setFocusable(true); b.setClickable(true);
        if ("home".equals(destination)) homeNavigation = b;
        b.setOnClickListener(v -> navigate(destination));
        b.setOnFocusChangeListener((v, focused) -> {
            b.setTextColor(focused ? Design.BG : Design.MUTED);
            b.setBackground(focused ? Design.rounded(Design.WHITE,d(8)) : null);
        });
        bar.addView(b,lp(-2,44));
    }
    private void navigate(String destination) {
        switch (destination) {
            case "home": showHome(); break;
            case "movie": showFilter("movie"); break;
            case "tv": showFilter("tv"); break;
            case "list": showList(); break;
            case "search": showSearch(); break;
            case "settings": showSettings(); break;
        }
    }
    private void replace(View view) {
        stage.removeAllViews(); stage.addView(view, new FrameLayout.LayoutParams(-1,-1));
    }
    private ScrollView newScroll() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false); return scroll;
    }
    private void refreshCatalog() {
        catalog.loadHome(user.token(), (rows, error) -> {
            sections = rows;
            if (page.equals("home")) showHome();
            if (error != null && page.equals("home")) toast("TMDB tidak dapat diakses. Katalog contoh ditampilkan.");
        });
    }
    private void showHome() {
        page = "home";
        ScrollView scroll = newScroll(); LinearLayout content = vertical();
        Media spotlight = sections.isEmpty() || sections.get(0).items.isEmpty()
                ? catalog.sample().get(0) : sections.get(0).items.get(0);
        content.addView(hero(spotlight),lp(-1,330));
        if (!user.recent().isEmpty()) row(content, "Baru diputar", user.recent());
        for (CatalogRepository.Section section : sections) row(content, section.title, section.items);
        gap(content,50); scroll.addView(content); replace(scroll);
    }
    private View hero(Media item) {
        FrameLayout hero = new FrameLayout(this); hero.setBackground(Design.gradient(0xFF291C29,0xFF090A0E,0));
        ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(photo,new FrameLayout.LayoutParams(-1,-1));
        images.load(photo,item.image("w1280",true));
        View darken = new View(this);
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF090A0E,0xF6090A0E,0x77090A0E,0x99090A0E});
        darken.setBackground(gradient); hero.addView(darken,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout text = vertical(); text.setGravity(Gravity.BOTTOM); text.setPadding(d(54),d(18),d(36),d(35));
        TextView eyebrow = Design.text(this,"JOSJISTV  /  UNGGULAN",13,Design.ACCENT,true);
        eyebrow.setLetterSpacing(0.17f); text.addView(eyebrow,lp(-2,30));
        TextView title = Design.text(this,item.title,44,Design.WHITE,true); title.setMaxLines(2); text.addView(title,lp(-1,-2));
        TextView meta = Design.text(this, item.kind()+"   •   "+item.year+"   •   Katalog TMDB",13,Design.MUTED,false);
        text.addView(meta,lp(-1,28));
        TextView desc = Design.text(this,item.overview,15,Design.WHITE,false); desc.setMaxLines(2);
        LinearLayout.LayoutParams descLp = lp(-1,48); descLp.rightMargin=d(300); text.addView(desc,descLp);
        gap(text,13);
        LinearLayout actions=horizontal();
        actions.addView(Design.button(this,"▶  Putar",true,()->play(item,1,1)),lp(-2,46));
        space(actions,12);
        actions.addView(Design.button(this,"ℹ  Detail Film",false,()->showDetail(item)),lp(-2,46));
        text.addView(actions); hero.addView(text,new FrameLayout.LayoutParams(-1,-1));
        return hero;
    }
    private void row(LinearLayout column, String title, List<Media> items) {
        if (items.isEmpty()) return;
        TextView header = Design.text(this,title,23,Design.WHITE,true);
        header.setPadding(d(40),d(14),0,d(12)); column.addView(header,lp(-1,58));
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setClipToPadding(false); scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = horizontal(); strip.setPadding(d(40),d(4),d(38),d(14));
        for (Media media: items) { strip.addView(tile(media)); space(strip,15); }
        scroll.addView(strip); column.addView(scroll,lp(-1,204)); gap(column,6);
    }
    private View tile(Media item) {
        LinearLayout container = vertical(); container.setFocusable(true); container.setClickable(true);
        container.setBackground(Design.rounded(Design.PANEL,d(13)));
        container.setOnClickListener(v -> showDetail(item));
        container.setOnFocusChangeListener((v, focused) -> {
            GradientDrawable bg = Design.rounded(focused ? 0xFF464A55 : Design.PANEL,d(13));
            bg.setStroke(d(focused ? 2 : 0),focused ? Design.WHITE : Design.PANEL);
            container.setBackground(bg);
            v.animate().scaleX(focused ? 1.055f : 1f).scaleY(focused ? 1.055f : 1f).setDuration(140).start();
        });
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable placeholder = Design.gradient(0xFF3B2839,0xFF171E30,d(10)); frame.setBackground(placeholder);
        ImageView artwork = new ImageView(this); artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(artwork,new FrameLayout.LayoutParams(-1,-1));
        String image = item.image("w500",false); images.load(artwork,image);
        TextView mark = Design.text(this,"J",63,0x66FFFFFF,true); mark.setGravity(Gravity.CENTER);
        frame.addView(mark,new FrameLayout.LayoutParams(-1,-1));
        if (!image.isEmpty()) { mark.setVisibility(View.GONE); }
        TextView kind = Design.text(this,item.kind(),10,Design.WHITE,true);
        kind.setPadding(d(8),0,d(8),0); kind.setBackground(Design.rounded(0xD9E13A4B,d(4)));
        FrameLayout.LayoutParams kp = new FrameLayout.LayoutParams(-2,d(24),Gravity.TOP|Gravity.LEFT);
        kp.leftMargin=d(10); kp.topMargin=d(10); frame.addView(kind,kp);
        container.addView(frame,lp(-1,129));
        TextView title = Design.text(this,item.title,14,Design.WHITE,true); title.setSingleLine(true); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(d(10),d(6),d(10),0); container.addView(title,lp(-1,32));
        TextView info = Design.text(this,item.year.isEmpty() ? item.kind() : item.year,11,Design.MUTED,false);
        info.setPadding(d(10),0,d(10),0); container.addView(info,lp(-1,20));
        container.setLayoutParams(lp(222,183));
        return container;
    }
    private void showFilter(String type) {
        page = type;
        List<Media> all = new ArrayList<>();
        for (CatalogRepository.Section section : sections) for (Media m : section.items)
            if (m.type.equals(type) && !containsKey(all, m.key())) all.add(m);
        if (all.isEmpty()) for(Media m:catalog.sample()) if (m.type.equals(type)) all.add(m);
        ScrollView scroll=newScroll(); LinearLayout body=vertical(); body.setPadding(0,d(25),0,0);
        bigTitle(body,type.equals("movie")?"Jelajahi Film":"Jelajahi Serial TV","Pilih judul menggunakan remote Android TV.");
        gridRows(body, all); scroll.addView(body); replace(scroll);
    }
    private boolean containsKey(List<Media> media, String key) {
        for (Media item : media) if (item.key().equals(key)) return true;
        return false;
    }
    private void gridRows(LinearLayout body, List<Media> media) {
        for(int i=0;i<media.size();i+=4) {
            LinearLayout row=horizontal(); row.setPadding(d(40),d(6),0,d(12));
            for(int j=i;j<Math.min(i+4,media.size());j++) { row.addView(tile(media.get(j)),lp(222,183)); space(row,17); }
            body.addView(row); gap(body,13);
        }
    }
    private void bigTitle(LinearLayout body, String title, String subtitle) {
        TextView t=Design.text(this,title,36,Design.WHITE,true); t.setPadding(d(40),0,d(24),0); body.addView(t,lp(-1,55));
        TextView sub=Design.text(this,subtitle,15,Design.MUTED,false); sub.setPadding(d(40),0,d(24),0); body.addView(sub,lp(-1,36));
        gap(body,15);
    }
    private void showList() {
        page="list"; ScrollView scroll=newScroll(); LinearLayout body=vertical(); body.setPadding(0,d(25),0,0);
        bigTitle(body,"Daftar Saya","Judul tersimpan lokal di perangkat TV ini.");
        if(user.favorites().isEmpty()) message(body,"Daftar masih kosong. Buka detail judul lalu pilih + Daftar Saya.");
        else gridRows(body,user.favorites());
        scroll.addView(body); replace(scroll);
    }
    private void message(LinearLayout column,String msg) {
        TextView t=Design.text(this,msg,16,Design.MUTED,false); t.setPadding(d(40),d(12),d(30),d(12)); column.addView(t,lp(-1,-2));
    }
    private void showSearch() {
        page="search"; ScrollView scroll=newScroll(); LinearLayout body=vertical(); body.setPadding(0,d(25),0,0);
        bigTitle(body,"Cari tontonan","Cari film dan serial dari TMDB (atau katalog contoh jika tanpa token).");
        LinearLayout inputRow=horizontal(); inputRow.setPadding(d(40),0,0,0);
        EditText input=new EditText(this); input.setSingleLine(true); input.setTextColor(Color.WHITE);
        input.setTextSize(18); input.setHintTextColor(Design.MUTED); input.setHint("Judul film atau serial...");
        input.setBackground(Design.rounded(Design.PANEL,d(12))); input.setPadding(d(15),0,d(15),0);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        inputRow.addView(input,lp(500,52)); space(inputRow,14);
        LinearLayout results = vertical();
        Runnable search = () -> {
            String q=input.getText().toString().trim(); results.removeAllViews();
            if(q.isEmpty()) { message(results,"Masukkan kata kunci terlebih dahulu."); return; }
            message(results,"Mencari: " + q + " …");
            catalog.search(user.token(),q,"all",(matches,error)-> {
                if(!page.equals("search")) return;
                results.removeAllViews();
                if(error!=null) { message(results,"Pencarian gagal: "+error); return; }
                if(matches.isEmpty()) message(results,"Tidak ada hasil. Coba judul lainnya.");
                else gridRows(results,matches);
            });
        };
        input.setOnEditorActionListener((v,action,event)->{
            if(action==EditorInfo.IME_ACTION_SEARCH || (event!=null && event.getKeyCode()==KeyEvent.KEYCODE_ENTER)) {
                search.run(); return true;
            } return false;
        });
        inputRow.addView(Design.button(this,"⌕  Cari",true,search),lp(-2,52));
        body.addView(inputRow); gap(body,24); body.addView(results);
        message(results,"Tekan tombol OK untuk membuka keyboard TV.");
        scroll.addView(body); replace(scroll); input.requestFocus();
    }
    private void showSettings() {
        page="settings"; ScrollView scroll=newScroll(); LinearLayout body=vertical(); body.setPadding(0,d(25),d(30),0);
        bigTitle(body,"Pengaturan","Katalog aktual membutuhkan TMDB API Key atau Read Access Token Anda sendiri.");
        TextView label=Design.text(this,"TMDB API Key / Read Access Token",18,Design.WHITE,true);
        label.setPadding(d(40),0,0,0); body.addView(label,lp(-1,42));
        EditText token=new EditText(this); token.setSingleLine(true); token.setText(user.token());
        token.setTextSize(16); token.setTextColor(Color.WHITE); token.setHintTextColor(Design.MUTED);
        token.setHint("Tempel token TMDB di sini"); token.setBackground(Design.rounded(Design.PANEL,d(11)));
        token.setPadding(d(18),0,d(18),0);
        LinearLayout.LayoutParams inputLp=lp(720,56); inputLp.leftMargin=d(40); body.addView(token,inputLp);
        gap(body,16);
        LinearLayout actions=horizontal(); actions.setPadding(d(40),0,0,0);
        actions.addView(Design.button(this,"Simpan & muat ulang katalog",true,()->{
            user.saveToken(token.getText().toString()); toast("Pengaturan disimpan");
            showHome(); refreshCatalog();
        }),lp(-2,54));
        space(actions,15); actions.addView(Design.button(this,"Gunakan katalog contoh",false,()->{
            user.saveToken(""); sections=catalog.demoSections(); showHome();
        }),lp(-2,54));
        body.addView(actions); gap(body,22);
        message(body,"Catatan: token disimpan hanya di penyimpanan lokal aplikasi. Untuk distribusi APK publik, gunakan backend perantara untuk melindungi kredensial Anda.");
        message(body,"FrameXTV: https://framextv.xyz/embed/{tmdbId} — untuk serial: /{tmdbId}/{season}/{episode}.");
        message(body,"This product uses the TMDB API but is not endorsed or certified by TMDB.");
        message(body,"TMDB: https://www.themoviedb.org (data dan artwork). Untuk distribusi publik, ikuti persyaratan atribusi/logo TMDB.");
        message(body,"JosJisTV tidak mengunduh maupun mendekripsi stream. Pemutaran bergantung pada layanan frameXTV dan izin pemilik konten.");
        scroll.addView(body); replace(scroll);
    }
    private void showDetail(Media item) {
        page="detail"; current=item; detailKey=item.key(); season=1; episode=1;
        ScrollView scroll=newScroll(); LinearLayout body=vertical(); body.setPadding(d(44),d(26),d(40),d(35));
        LinearLayout top=horizontal(); top.setGravity(Gravity.TOP);
        FrameLayout poster = new FrameLayout(this); poster.setBackground(Design.gradient(0xFF542A45,0xFF20293E,d(12)));
        ImageView image=new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.addView(image,new FrameLayout.LayoutParams(-1,-1)); images.load(image,item.image("w500",false));
        TextView badge=Design.text(this,"JOSJIS TV",20,0x66FFFFFF,true); badge.setGravity(Gravity.CENTER);
        if(item.poster.isEmpty()) poster.addView(badge,new FrameLayout.LayoutParams(-1,-1));
        top.addView(poster,lp(250,360)); space(top,35);
        LinearLayout details=vertical();
        TextView label=Design.text(this,"JOSJIS ORIGINAL INTERFACE  /  " + item.kind(),13,Design.ACCENT,true);
        details.addView(label,lp(-1,38));
        TextView title=Design.text(this,item.title,40,Design.WHITE,true); title.setMaxLines(2);
        details.addView(title,lp(-1,-2)); gap(details,8);
        String score=item.rating>0 ? "   •   ★ " + String.format(java.util.Locale.US,"%.1f",item.rating) : "";
        details.addView(Design.text(this,item.year+"   •   "+item.kind()+score,15,Design.MUTED,false),lp(-1,35));
        TextView synopsis=Design.text(this,item.overview,17,Design.WHITE,false); synopsis.setMaxLines(8);
        details.addView(synopsis,lp(-1,-2)); gap(details,25);
        LinearLayout buttons=horizontal();
        buttons.addView(Design.button(this,item.type.equals("tv")?"▶  Putar Episode":"▶  Putar Film",true,
                ()->play(item,season,episode)),lp(-2,52)); space(buttons,12);
        buttons.addView(Design.button(this,user.favorite(item)?"✓  Tersimpan":"+  Daftar Saya",false,()->{
            user.toggleFavorite(item); showDetail(item);
        }),lp(-2,52));
        details.addView(buttons);
        top.addView(details,new LinearLayout.LayoutParams(0, -2, 1f)); body.addView(top);
        if(item.type.equals("tv")) {
            gap(body,28); TextView epTitle=Design.text(this,"Pilih Episode",25,Design.WHITE,true);
            body.addView(epTitle,lp(-1,48));
            LinearLayout seasonRow=horizontal();
            seasonRow.addView(Design.button(this,"− Musim",false,()->changeSeason(-1)),lp(-2,47)); space(seasonRow,10);
            seasonValue=Design.text(this,"Musim 1",18,Design.WHITE,true); seasonValue.setGravity(Gravity.CENTER);
            seasonRow.addView(seasonValue,lp(145,47)); space(seasonRow,10);
            seasonRow.addView(Design.button(this,"+ Musim",false,()->changeSeason(1)),lp(-2,47)); space(seasonRow,35);
            seasonRow.addView(Design.button(this,"− Episode",false,()->changeEpisode(-1)),lp(-2,47)); space(seasonRow,10);
            episodeValue=Design.text(this,"Episode 1",18,Design.WHITE,true); episodeValue.setGravity(Gravity.CENTER);
            seasonRow.addView(episodeValue,lp(155,47)); space(seasonRow,10);
            seasonRow.addView(Design.button(this,"+ Episode",false,()->changeEpisode(1)),lp(-2,47));
            body.addView(seasonRow); gap(body,17);
            episodeStatus=Design.text(this,"Mengambil informasi episode...",15,Design.MUTED,false);
            body.addView(episodeStatus,lp(-1,35));
            episodeList=vertical(); body.addView(episodeList);
            loadEpisodes(item);
        }
        scroll.addView(body); replace(scroll);
    }
    private void changeSeason(int delta) {
        season=Math.max(1, Math.min(99,season+delta)); episode=1;
        updateEpisodeLabels(); loadEpisodes(current);
    }
    private void changeEpisode(int delta) { episode=Math.max(1,Math.min(999,episode+delta)); updateEpisodeLabels(); }
    private void updateEpisodeLabels() {
        if(seasonValue!=null) seasonValue.setText("Musim " + season);
        if(episodeValue!=null) episodeValue.setText("Episode " + episode);
    }
    private void loadEpisodes(Media item) {
        int requestedSeason=season;
        episodeStatus.setText(user.token().isEmpty() ? "Mode contoh: pilih nomor episode, lalu Putar Episode." : "Memuat daftar episode...");
        episodeList.removeAllViews();
        if(user.token().isEmpty()) return;
        catalog.episodes(user.token(),item.id,season,(episodes,error)->{
            if(!page.equals("detail") || !detailKey.equals(item.key()) || season!=requestedSeason) return;
            episodeList.removeAllViews();
            if(error!=null) { episodeStatus.setText("Data episode gagal dimuat: " + error); return; }
            episodeStatus.setText(episodes.size()+" episode pada musim "+season);
            for(CatalogRepository.Episode ep : episodes) {
                String name="E"+ep.number+"   "+ep.name;
                TextView button=Design.button(this,name,false,()-> { episode=ep.number; updateEpisodeLabels(); play(item,season,episode); });
                LinearLayout.LayoutParams rowLp=lp(800,50); rowLp.bottomMargin=d(9);
                episodeList.addView(button,rowLp);
            }
        });
    }
    private void play(Media item, int season, int episode) {
        user.record(item);
        Intent intent=new Intent(this,PlayerActivity.class);
        intent.putExtra("id",item.id); intent.putExtra("type",item.type);
        intent.putExtra("title",item.title); intent.putExtra("season",season); intent.putExtra("episode",episode);
        startActivity(intent);
    }
    private void toast(String message) { android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show(); }
    @Override public void onBackPressed() {
        if(page.equals("detail")) showHome(); else if(!page.equals("home")) showHome(); else super.onBackPressed();
    }
    @Override protected void onDestroy() { super.onDestroy(); catalog.shutdown(); images.shutdown(); }
}
