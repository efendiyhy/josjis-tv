package com.josjis.tv;

/** Documented frameXTV embed URL builder. Never supplies raw stream URLs. */
public final class FramexLinks {
    private FramexLinks() { }
    public static String embed(int tmdbId, String type, int season, int episode, boolean controls) {
        if (tmdbId < 1) throw new IllegalArgumentException("Invalid TMDB ID");
        if (!"movie".equals(type) && !"tv".equals(type)) throw new IllegalArgumentException("Invalid media type");
        String path = "https://framextv.xyz/embed/" + tmdbId;
        if ("tv".equals(type)) {
            if (season < 1 || episode < 1) throw new IllegalArgumentException("Invalid season or episode");
            path += "/" + season + "/" + episode;
        }
        return path + "?autoplay=1&muted=0&controls=" + (controls ? "1" : "0");
    }
}
