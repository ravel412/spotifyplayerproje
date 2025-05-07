package com.example.spotifplayer;

import com.spotify.android.appremote.api.SpotifyAppRemote;

public class SpotifyAppRemoteHolder {
    public static SpotifyAppRemote remote;

    public static void pause() {
        if (remote != null) remote.getPlayerApi().pause();
    }

    public static void resume() {
        if (remote != null) remote.getPlayerApi().resume();
    }
}
