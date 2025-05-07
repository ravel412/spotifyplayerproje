package com.example.spotifplayer;

import java.util.List;

public class SearchResponse {
    public TrackResults tracks;

    public static class TrackResults {
        public List<TrackItem> items;
    }

    public static class TrackItem {
        public String name;
        public String uri;
        public List<Artist> artists;
    }

    public static class Artist {
        public String name;
    }
}
