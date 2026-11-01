package com.josjis.tv;

import org.json.JSONObject;

public final class Media {
    public final int id;
    public final String type;
    public final String title;
    public final String overview;
    public final String year;
    public final String poster;
    public final String backdrop;
    public final double rating;

    public Media(int id, String type, String title, String overview, String year, String poster, String backdrop, double rating) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.overview = overview;
        this.year = year;
        this.poster = poster;
        this.backdrop = backdrop;
        this.rating = rating;
    }

    public String key() { return type + ":" + id; }
    public String kind() { return "tv".equals(type) ? "SERIAL" : "FILM"; }
    public String image(String size, boolean backdropImage) {
        String path = backdropImage ? backdrop : poster;
        if (path == null || path.isEmpty() || path.equals("null") || !path.startsWith("/")) return "";
        return "https://image.tmdb.org/t/p/" + size + path;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id); json.put("type", type); json.put("title", title);
            json.put("overview", overview); json.put("year", year);
            json.put("poster_path", poster); json.put("backdrop_path", backdrop);
            json.put("vote_average", rating);
        } catch (Exception ignored) { }
        return json;
    }

    public static Media fromJson(JSONObject obj) {
        String type = obj.optString("media_type", obj.optString("type", "movie"));
        if (!"tv".equals(type) && !"movie".equals(type)) return null;
        String title = "tv".equals(type) ? obj.optString("name", obj.optString("title", "Tanpa judul"))
                : obj.optString("title", obj.optString("name", "Tanpa judul"));
        String date = "tv".equals(type) ? obj.optString("first_air_date") : obj.optString("release_date");
        String year = obj.optString("year", date.length() >= 4 ? date.substring(0, 4) : "");
        return new Media(obj.optInt("id"), type, title,
                obj.optString("overview", "Sinopsis belum tersedia."), year,
                obj.optString("poster_path", ""), obj.optString("backdrop_path", ""),
                obj.optDouble("vote_average", 0));
    }
}
