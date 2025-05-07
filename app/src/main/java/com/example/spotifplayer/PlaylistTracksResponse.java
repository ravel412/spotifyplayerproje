package com.example.spotifplayer;

import java.util.List;

public class PlaylistTracksResponse {
    public List<TrackItem> items;

    public static class TrackItem {
        public Track track;
    }

    public static class Track {
        public String name;
        public List<Artist> artists;
        public String uri;
    }

    public static class Artist {
        public String name;
    }
}
