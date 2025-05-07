package com.example.spotifplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("ACTION_PAUSE".equals(action)) {
            SpotifyAppRemoteHolder.pause();
        } else if ("ACTION_PLAY".equals(action)) {
            SpotifyAppRemoteHolder.resume();
        }
    }
}
