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

    private static Media item(int id, String type, String title, String year, String poster, String backdrop, String description, double rating) {
        return new Media(id, type, title, description, year, poster, backdrop, rating);
    }

    private static final List<Media> DEMO = Arrays.asList(
            item(550, "movie", "Fight Club", "1999", "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg", "/52AfXWuXCHn3UjD17rBruA9f5qb.jpg", "Seorang pekerja kantoran yang lelah dengan hidupnya terseret ke dunia klub pertarungan bawah tanah.", 8.4),
            item(157336, "movie", "Interstellar", "2014", "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg", "/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg", "Misi lintas galaksi dilakukan demi mencari rumah baru bagi umat manusia.", 8.4),
            item(27205, "movie", "Inception", "2010", "/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg", "/s3TBrRGB1iav7gFOCNx3H31MoES.jpg", "Seorang pencuri memasuki mimpi target-targetnya demi mencuri rahasia berharga.", 8.3),
            item(155, "movie", "The Dark Knight", "2008", "/qJ2tW6WMUDux911r6m7haRef0WH.jpg", "/hqkIcbrOHL86UncnHIsHVcVmzue.jpg", "Batman menghadapi Joker dalam konflik besar yang mengguncang Gotham.", 8.5),
            item(438631, "movie", "Dune", "2021", "/d5NXSklXo0qyIYkgV94XAgMIckC.jpg", "/iopYFB1b6Bh7FWZh3onQhph1sih.jpg", "Pewaris muda keluarga bangsawan harus memimpin di planet gurun paling berbahaya.", 7.8),
            item(872585, "movie", "Oppenheimer", "2023", "/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg", "/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg", "Drama biografi tentang ilmuwan yang memimpin pengembangan bom atom.", 8.1),
            item(496243, "movie", "Parasite", "2019", "/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg", "/ApiBzeaa95TNYliSbQ8pJv4Fje7.jpg", "Sebuah keluarga miskin menyusup ke kehidupan keluarga kaya dengan konsekuensi tak terduga.", 8.5),
            item(414906, "movie", "The Batman", "2022", "/74xTEgt7R36Fpooo50r9T25onhq.jpg", "/b0PlSFdDwbyK0cf5RxwDpaOJQvQ.jpg", "Batman memburu pembunuh berantai yang meneror elit Gotham.", 7.8),
            item(361743, "movie", "Top Gun: Maverick", "2022", "/62HCnUTziyWcpDaBO2i1DX17ljH.jpg", "/odJ4hx6g6vBt4lBWKFD1tI8WS4x.jpg", "Pilot legendaris kembali melatih generasi baru untuk misi berbahaya.", 8.3),
            item(76600, "movie", "Avatar: The Way of Water", "2022", "/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg", "/s16H6tpK2utvwDtzZ8Qy4qm5Emw.jpg", "Keluarga Sully harus bertahan hidup saat ancaman baru datang ke Pandora.", 7.6),
            item(603692, "movie", "John Wick: Chapter 4", "2023", "/vZloFAK7NmvMGKE7VkF5UHaz0I.jpg", "/h8gHn0OzBoaefsYseUByqsmEDMY.jpg", "John Wick terus melawan organisasi pembunuh internasional demi kebebasannya.", 7.9),
            item(634649, "movie", "Spider-Man: No Way Home", "2021", "/1g0dhYtq4irTY1GPXvft6k4YLjm.jpg", "/iQFcwSGbZXMkeyKrxbPnwnRo5fl.jpg", "Peter Parker menghadapi kekacauan multisemesta setelah identitasnya terbongkar.", 8.2),
            item(545611, "movie", "Everything Everywhere All at Once", "2022", "/w3LxiVYdWWRvEVdn5RYq6jIqkb1.jpg", "/ss0Os3uWJfQAENILHZUdX8Tt1OC.jpg", "Seorang ibu biasa harus menyelamatkan multisemesta dalam petualangan absurd.", 8.1),
            item(593643, "movie", "The Menu", "2022", "/v31MsWhF9WFh7Qooq6xSBbmJxoG.jpg", "/mSyQoValhBsJdq3JNGXJww2Q5yL.jpg", "Jamuan makan mewah berubah menjadi pengalaman yang menegangkan dan satir.", 7.2),
            item(546554, "movie", "Knives Out", "2019", "/pThyQovXQrw2m0s9x82twj48Jq4.jpg", "/AbRYlvwAKHs0YuyNO6NX9ofq4l6.jpg", "Detektif eksentrik menyelidiki kematian misterius seorang novelis kaya.", 7.8),
            item(916224, "movie", "Suzume", "2022", "/vIeu8WysZrTSFb2uhPViKjX9EcC.jpg", "/fMymfWwgTfXh9RkBCFuaZrdn8Of.jpg", "Seorang gadis remaja harus menutup pintu-pintu yang memicu bencana.", 7.9),
            item(372058, "movie", "Your Name", "2016", "/q719jXXEzOoYaps6babgKnONONX.jpg", "/mMtUybQ6hL24FXo0F3Z4j2KG7kZ.jpg", "Dua remaja yang saling bertukar tubuh mencoba memahami takdir mereka.", 8.4),
            item(129, "movie", "Spirited Away", "2001", "/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg", "/6oaL4DP75yABrd5EbC4H2zq5ghc.jpg", "Seorang gadis kecil terjebak di dunia roh dan harus menyelamatkan orang tuanya.", 8.5),
            item(497698, "movie", "Black Widow", "2021", "/qAZ0pzat24kLdO3o8ejmbLxyOac.jpg", "/keIxh0wPr2Ymj0Btjh4gW7JJ89e.jpg", "Natasha Romanoff kembali menghadapi masa lalunya sebagai mata-mata.", 6.7),
            item(68718, "movie", "Django Unchained", "2012", "/7oWY8VDWW7thTzWh3OKYRkWUlD5.jpg", "/2oZklIzUbvZXXzIFzv7Hi68d6xf.jpg", "Seorang pemburu hadiah membantu budak yang dimerdekakan membalas dendam.", 8.2),

            item(1396, "tv", "Breaking Bad", "2008", "/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg", "/tsRy63Mu5cu8etL1X7ZLyf7UP1M.jpg", "Guru kimia berubah menjadi produsen narkoba demi keluarganya.", 8.9),
            item(66732, "tv", "Stranger Things", "2016", "/49WJfeN0moxb9IPfGn8AIqMGskD.jpg", "/56v2KjBlU4XaOv9rVYEQypROD7P.jpg", "Sekelompok anak terlibat dalam misteri supernatural di kota kecil.", 8.6),
            item(100088, "tv", "The Last of Us", "2023", "/uKvVjHNqB5VmOrdxqAt2F7J78ED.jpg", "/uDgy6hyPd82kOHh6I95FLtLnj6p.jpg", "Joel dan Ellie berjuang bertahan hidup di dunia pascaapokaliptik.", 8.8),
            item(1399, "tv", "Game of Thrones", "2011", "/u3bZgnGQ9T01sWNhyveQz0wH0Hl.jpg", "/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg", "Keluarga-keluarga bangsawan memperebutkan tahta besi.", 8.4),
            item(119051, "tv", "Wednesday", "2022", "/jeGtaMwGxPmQN5xM4ClnwPQcNQz.jpg", "/iHSwvRVsRyxpX7FE7GbviaDvgGZ.jpg", "Wednesday Addams menyelidiki misteri pembunuhan di akademi Nevermore.", 8.1),
            item(93405, "tv", "Squid Game", "2021", "/dDlEmu3EZ0Pgg93K2SVNLCjCSvE.jpg", "/qw3J9cNeLioOLoR68WX7z79aCdK.jpg", "Ratusan orang putus asa mempertaruhkan nyawa demi hadiah besar.", 7.8),
            item(82856, "tv", "The Mandalorian", "2019", "/sWgBv7LV2PRoQgkxwlibdGXKz1S.jpg", "/9ijMGlJKqcslswWUzTEwScm82Gs.jpg", "Seorang pemburu hadiah berkelana di galaksi bersama seorang anak misterius.", 8.5),
            item(84958, "tv", "Loki", "2021", "/voHUmluYmKyleFkTu3lOXQG702u.jpg", "/rzKY7S6sdydVRARmyh36W9T6VYZ.jpg", "Loki terlibat dengan lembaga yang menjaga jalur waktu.", 8.2),
            item(94997, "tv", "House of the Dragon", "2022", "/z2yahl2uefxDCl0nogcRBstwruJ.jpg", "/etj8E2o0Bud0HkONVQPjyCkIvpv.jpg", "Kisah awal perang saudara keluarga Targaryen.", 8.4),
            item(76479, "tv", "The Boys", "2019", "/2zmTngn1tYC1AvfnrFLhxeD82hz.jpg", "/mGVrXeIjyecj6TKmwPVpHlscEmw.jpg", "Kelompok vigilante memburu pahlawan super yang korup.", 8.5),
            item(71446, "tv", "Money Heist", "2017", "/reEMJA1uzscCbkpeRJeTT2bjqUp.jpg", "/gFZriCkpJYsApPZEF3jhxL4yLzG.jpg", "Perampokan besar yang dirancang Profesor mengguncang Spanyol.", 8.2),
            item(70523, "tv", "Dark", "2017", "/5Loqyy2GmLplDxQNRz8hD2lqihU.jpg", "/q4zuJVO0Q8qHCVKscjWgEGRJgY4.jpg", "Hilangnya seorang anak membuka misteri lintas generasi dan waktu.", 8.4),
            item(87739, "tv", "The Queen's Gambit", "2020", "/zU0htwkhNvBQdVSIKB9s6hgVeFK.jpg", "/34OGjFEbHj0E3lE2w0iTUVq0CBz.jpg", "Pecatur muda jenius berjuang meraih puncak sambil melawan kecanduan.", 8.5),
            item(87108, "tv", "Chernobyl", "2019", "/hlLXt2tOPT6RRnjiUmoxyG1LTFi.jpg", "/900tHlUYUkp7Ol04XFSoAaEIXcT0.jpg", "Mini seri tentang bencana nuklir paling terkenal di dunia.", 8.7),
            item(63351, "tv", "Narcos", "2015", "/rTmal9fDbwh5F0waol2hq35U4ah.jpg", "/3be8W4oOHXaOXfTjdbV7MfUf8gM.jpg", "Kebangkitan kartel narkoba Kolombia dan perang melawannya.", 8.0),
            item(37854, "tv", "One Piece", "2023", "/fcXdJlbSdUEeMSJFsXKsznGwwok.jpg", "/9m161GawbY3cWxe6txd1NOHTjd0.jpg", "Luffy dan kru Topi Jerami memulai petualangan mencari harta legendaris.", 8.0)
    );

    public List<Media> sample() {
        return new ArrayList<>(DEMO);
    }

    private List<Media> byType(String type) {
        ArrayList<Media> out = new ArrayList<>();
        for (Media m : DEMO) if (m.type.equals(type)) out.add(m);
        return out;
    }

    private List<Media> picks(int... indexes) {
        ArrayList<Media> out = new ArrayList<>();
        for (int index : indexes) {
            if (index >= 0 && index < DEMO.size()) out.add(DEMO.get(index));
        }
        return out;
    }

    public List<Section> demoSections() {
        ArrayList<Section> sections = new ArrayList<>();
        sections.add(new Section("Sorotan JosJisTV", picks(5, 10, 22, 3, 24, 1, 8, 28)));
        sections.add(new Section("Film Aksi & Sci-Fi", picks(1, 2, 3, 4, 8, 9, 10, 11, 18)));
        sections.add(new Section("Film Drama, Misteri & Thriller", picks(0, 5, 6, 7, 13, 14, 19)));
        sections.add(new Section("Film Anime & Pilihan Santai", picks(15, 16, 17, 9, 12, 18)));
        sections.add(new Section("Serial Populer", picks(20, 21, 22, 23, 24, 25, 26, 27)));
        sections.add(new Section("Serial Lanjutan", picks(28, 29, 30, 31, 32, 33, 34, 35)));
        return sections;
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
                list.add(new Section("Sedang tren minggu ini", parse(call(token, "/trending/all/week"), "movie")));
                list.add(new Section("Film populer", parse(call(token, "/movie/popular"), "movie")));
                list.add(new Section("Serial populer", parse(call(token, "/tv/popular"), "tv")));
                list.add(new Section("Film dengan rating terbaik", parse(call(token, "/movie/top_rated"), "movie")));
                list.addAll(demoSections());
                main.post(() -> cb.done(list, null));
            } catch (Exception e) {
                main.post(() -> cb.done(demoSections(), e.getMessage()));
            }
        });
    }

    public void search(String token, String query, String filter, Callback<List<Media>> cb) {
        if (token.isEmpty()) {
            ArrayList<Media> items = new ArrayList<>();
            for (Media m : DEMO) {
                if ((filter.equals("all") || m.type.equals(filter)) && m.title.toLowerCase().contains(query.toLowerCase())) {
                    items.add(m);
                }
            }
            cb.done(items, null);
            return;
        }
        pool.execute(() -> {
            try {
                String path = "/search/multi?query=" + URLEncoder.encode(query, "UTF-8") + "&include_adult=false";
                ArrayList<Media> items = new ArrayList<>();
                for (Media m : parse(call(token, path), "movie")) {
                    if (filter.equals("all") || filter.equals(m.type)) items.add(m);
                }
                if (items.isEmpty()) {
                    for (Media m : DEMO) {
                        if ((filter.equals("all") || m.type.equals(filter)) && m.title.toLowerCase().contains(query.toLowerCase())) {
                            items.add(m);
                        }
                    }
                }
                main.post(() -> cb.done(items, null));
            } catch (Exception e) {
                ArrayList<Media> items = new ArrayList<>();
                for (Media m : DEMO) {
                    if ((filter.equals("all") || m.type.equals(filter)) && m.title.toLowerCase().contains(query.toLowerCase())) {
                        items.add(m);
                    }
                }
                main.post(() -> cb.done(items, items.isEmpty() ? e.getMessage() : null));
            }
        });
    }

    public void seasons(String token, int id, Callback<List<Integer>> cb) {
        if (token.isEmpty()) { cb.done(Arrays.asList(1, 2, 3, 4, 5), null); return; }
        pool.execute(() -> {
            try {
                JSONObject response = new JSONObject(call(token, "/tv/" + id));
                JSONArray seasons = response.optJSONArray("seasons");
                List<Integer> out = new ArrayList<>();
                if (seasons != null) {
                    for (int i = 0; i < seasons.length(); i++) {
                        int no = seasons.getJSONObject(i).optInt("season_number", 0);
                        if (no > 0) out.add(no);
                    }
                }
                main.post(() -> cb.done(out, null));
            } catch (Exception e) { main.post(() -> cb.done(Arrays.asList(1), e.getMessage())); }
        });
    }

    public void episodes(String token, int id, int season, Callback<List<Episode>> cb) {
        if (token.isEmpty()) { cb.done(new ArrayList<>(), null); return; }
        pool.execute(() -> {
            try {
                JSONObject response = new JSONObject(call(token, "/tv/" + id + "/season/" + season));
                JSONArray eps = response.optJSONArray("episodes");
                List<Episode> out = new ArrayList<>();
                if (eps != null) {
                    for (int i = 0; i < eps.length(); i++) {
                        JSONObject ep = eps.getJSONObject(i);
                        out.add(new Episode(ep.optInt("episode_number", i + 1), ep.optString("name", "Episode " + (i + 1)),
                                ep.optString("overview", ""), ep.optString("still_path", "")));
                    }
                }
                main.post(() -> cb.done(out, null));
            } catch (Exception e) { main.post(() -> cb.done(new ArrayList<>(), e.getMessage())); }
        });
    }

    public void shutdown() { pool.shutdownNow(); }
}
