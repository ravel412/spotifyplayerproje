package com.example.spotifplayer;

import java.util.List;

public class PlayerResponse {
    public boolean is_playing;
    public int progress_ms;
    public Track item;

    public static class Track {
        public String name;
        public int duration_ms;
        public List<Artist> artists;
    }

    public static class Artist {
        public String name;
    }
}
