package com.josjis.tv;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CatalogRepository {
    public interface Callback<T> { void done(T value, String error); }
    public static final class Section {
        public final String title;
        public final List<Media> items;
        Section(String title, List<Media> items) { this.title = title; this.items = items; }
    }
    public static final class Episode {
        public final int number;
        public final String name;
        public final String overview;
        public final String still;
        Episode(int number, String name, String overview, String still) {
            this.number = number; this.name = name; this.overview = overview; this.still = still;
        }
    }
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());

    // A genuinely usable offline/demo catalog (identifiers are TMDB IDs).
    // TMDB credentials are NEVER bundled; without credentials, artwork and current lists are unavailable.
    private static Media demo(int id, String type, String name, String year, String description) {
        return new Media(id, type, name, description, year, "", "", 0);
    }
    private static final List<Media> DEMO = Arrays.asList(
        demo(550,"movie","Fight Club","1999","Seorang pekerja kantoran menghadapi perubahan hidup yang drastis."),
        demo(157336,"movie","Interstellar","2014","Sebuah perjalanan luar angkasa untuk mencari masa depan umat manusia."),
        demo(27205,"movie","Inception","2010","Sekelompok ahli menjelajahi mimpi dan realitas."),
        demo(155,"movie","The Dark Knight","2008","Seorang pahlawan menghadapi ancaman besar terhadap kotanya."),
        demo(438631,"movie","Dune","2021","Pertarungan kekuasaan di planet gurun yang berbahaya."),
        demo(872585,"movie","Oppenheimer","2023","Drama biografi tentang perkembangan teknologi nuklir."),
        demo(496243,"movie","Parasite","2019","Ketegangan antarkelas sosial dalam sebuah keluarga."),
        demo(414906,"movie","The Batman","2022","Misteri kejahatan di kota Gotham."),
        demo(361743,"movie","Top Gun: Maverick","2022","Pilot berpengalaman kembali menjalani misi penuh tantangan."),
        demo(1396,"tv","Breaking Bad","2008","Seorang guru kimia memilih jalan hidup yang berbahaya."),
        demo(66732,"tv","Stranger Things","2016","Sekelompok remaja menghadapi kejadian supernatural."),
        demo(100088,"tv","The Last of Us","2023","Dua orang melakukan perjalanan melewati dunia pascaapokaliptik."),
        demo(1399,"tv","Game of Thrones","2011","Persaingan antarkerajaan untuk menguasai takhta."),
        demo(119051,"tv","Wednesday","2022","Seorang remaja menyelidiki misteri di sekolahnya."),
        demo(93405,"tv","Squid Game","2021","Permainan berisiko tinggi dengan konsekuensi serius."),
        demo(82856,"tv","The Mandalorian","2019","Petualangan pemburu hadiah di luar angkasa.")
    );
    public List<Media> sample() { return new ArrayList<>(DEMO); }
    public List<Section> demoSections() {
        ArrayList<Media> movies = new ArrayList<>(), series = new ArrayList<>();
        for (Media m : DEMO) { if (m.type.equals("movie")) movies.add(m); else series.add(m); }
        return Arrays.asList(new Section("Pilihan untuk Anda · Contoh", DEMO.subList(0, 8)),
                new Section("Film · Contoh", movies), new Section("Serial TV · Contoh", series));
    }
    private String call(String token, String path) throws Exception {
        boolean bearer = token.length() > 50 || token.startsWith("eyJ");
        String endpoint = "https://api.themoviedb.org/3" + path + (path.contains("?") ? "&" : "?")
                + "language=id-ID" + (bearer ? "" : "&api_key=" + URLEncoder.encode(token, "UTF-8"));
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000); conn.setReadTimeout(12000);
        conn.setRequestProperty("Accept", "application/json");
        if (bearer) conn.setRequestProperty("Authorization", "Bearer " + token.replaceFirst("^Bearer\\s+", ""));
        try {
            if (conn.getResponseCode() != 200) throw new Exception("TMDB HTTP " + conn.getResponseCode());
            try (InputStream stream = conn.getInputStream(); ByteArrayOutputStream data = new ByteArrayOutputStream()) {
                byte[] bytes = new byte[8192]; int read;
                while ((read = stream.read(bytes)) != -1) data.write(bytes, 0, read);
                return data.toString(StandardCharsets.UTF_8.name());
            }
        } finally { conn.disconnect(); }
    }
    private List<Media> parse(String raw, String defaultType) throws Exception {
        JSONArray results = new JSONObject(raw).optJSONArray("results");
        ArrayList<Media> items = new ArrayList<>();
        if (results == null) return items;
        for (int i = 0; i < results.length(); i++) {
            JSONObject obj = results.optJSONObject(i);
            if (obj == null) continue;
            if (!obj.has("media_type")) obj.put("media_type", defaultType);
            Media m = Media.fromJson(obj);
            if (m != null && m.id > 0) items.add(m);
        }
        return items;
    }
    public void loadHome(String token, Callback<List<Section>> cb) {
        if (token.isEmpty()) { cb.done(demoSections(), null); return; }
        pool.execute(() -> {
            try {
                List<Section> list = new ArrayList<>();
                list.add(new Section("Sedang tren minggu ini", parse(call(token,"/trending/all/week"),"movie")));
                list.add(new Section("Film populer", parse(call(token,"/movie/popular"),"movie")));
                list.add(new Section("Serial populer", parse(call(token,"/tv/popular"),"tv")));
                list.add(new Section("Film dengan rating terbaik", parse(call(token,"/movie/top_rated"),"movie")));
                main.post(() -> cb.done(list, null));
            } catch (Exception e) { main.post(() -> cb.done(demoSections(), e.getMessage())); }
        });
    }
    public void search(String token, String query, String filter, Callback<List<Media>> cb) {
        if (token.isEmpty()) {
            ArrayList<Media> items = new ArrayList<>();
            for (Media m : DEMO) if ((filter.equals("all") || m.type.equals(filter)) && m.title.toLowerCase().contains(query.toLowerCase())) items.add(m);
            cb.done(items, null); return;
        }
        pool.execute(() -> {
            try {
                String path = "/search/multi?query=" + URLEncoder.encode(query, "UTF-8") + "&include_adult=false";
                ArrayList<Media> items = new ArrayList<>();
                for (Media m : parse(call(token, path), "movie")) if (filter.equals("all") || filter.equals(m.type)) items.add(m);
                main.post(() -> cb.done(items, null));
            } catch (Exception e) { main.post(() -> cb.done(new ArrayList<>(), e.getMessage())); }
        });
    }
    public void seasons(String token, int id, Callback<List<Integer>> cb) {
        if (token.isEmpty()) { cb.done(Arrays.asList(1,2,3,4,5), null); return; }
        pool.execute(() -> {
            try {
                JSONObject response = new JSONObject(call(token,"/tv/" + id));
                JSONArray seasons = response.optJSONArray("seasons");
                List<Integer> out = new ArrayList<>();
                if (seasons != null) for (int i = 0; i < seasons.length(); i++) {
                    int no = seasons.getJSONObject(i).optInt("season_number",0);
                    if (no > 0) out.add(no);
                }
                main.post(() -> cb.done(out, null));
            } catch (Exception e) { main.post(() -> cb.done(Arrays.asList(1), e.getMessage())); }
        });
    }
    public void episodes(String token, int id, int season, Callback<List<Episode>> cb) {
        if (token.isEmpty()) { cb.done(new ArrayList<>(), null); return; }
        pool.execute(() -> {
            try {
                JSONObject response = new JSONObject(call(token, "/tv/"+id+"/season/"+season));
                JSONArray eps = response.optJSONArray("episodes");
                List<Episode> out = new ArrayList<>();
                if (eps != null) for (int i = 0; i < eps.length(); i++) {
                    JSONObject ep = eps.getJSONObject(i);
                    out.add(new Episode(ep.optInt("episode_number",i+1), ep.optString("name","Episode " + (i+1)),
                            ep.optString("overview", ""), ep.optString("still_path", "")));
                }
                main.post(() -> cb.done(out, null));
            } catch (Exception e) { main.post(() -> cb.done(new ArrayList<>(), e.getMessage())); }
        });
    }
    public void shutdown() { pool.shutdownNow(); }
}
