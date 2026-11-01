package com.josjis.tv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class UserStore {
    private final SharedPreferences prefs;
    public UserStore(Context context) { prefs = context.getSharedPreferences("josjis_local", Context.MODE_PRIVATE); }
    public String token() { return prefs.getString("tmdb_token", "").trim(); }
    public void saveToken(String token) { prefs.edit().putString("tmdb_token", token.trim()).apply(); }
    public boolean favorite(Media item) {
        for (Media m : read("favorites")) if (m.key().equals(item.key())) return true;
        return false;
    }
    public void toggleFavorite(Media item) {
        List<Media> current = read("favorites");
        boolean removed = false;
        for (int i=current.size()-1; i>=0; i--) if (current.get(i).key().equals(item.key())) { current.remove(i); removed = true; }
        if (!removed) current.add(0, item);
        write("favorites", current);
    }
    public void record(Media item) {
        List<Media> current = read("recent");
        for (int i=current.size()-1; i>=0; i--) if (current.get(i).key().equals(item.key())) current.remove(i);
        current.add(0, item);
        if (current.size() > 15) current = new ArrayList<>(current.subList(0, 15));
        write("recent", current);
    }
    public List<Media> favorites() { return read("favorites"); }
    public List<Media> recent() { return read("recent"); }
    private List<Media> read(String key) {
        ArrayList<Media> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(key, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json != null) { Media media = Media.fromJson(json); if (media != null) list.add(media); }
            }
        } catch (Exception ignored) { }
        return list;
    }
    private void write(String key, List<Media> data) {
        JSONArray arr = new JSONArray();
        for (Media m : data) arr.put(m.toJson());
        prefs.edit().putString(key, arr.toString()).apply();
    }
}
