package com.example.spotifplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;

import java.util.ArrayList;
import java.util.Locale;

public class FavoritesActivity extends AppCompatActivity {

    private ListView favoritesListView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> favoriteTracks;
    private ArrayList<String> favoriteUris;
    private TextView headerView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SpotifyAppRemote spotifyAppRemote;
    private static final String CLIENT_ID = "37375ee1cd3a49ffb56518c44bfaff95";
    private static final String REDIRECT_URI = "spotifplayer://callback";

    private LinearLayout playerBar;
    private ImageButton playPauseButton, rewindButton, forwardButton, prevButton, nextButton;
    private TextView currentTrackText, positionText, durationText;
    private SeekBar seekBar;
    private boolean isPlaying = true;
    private boolean userSeeking = false;
    private final Handler seekHandler = new Handler();
    private Runnable updateSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        favoritesListView = findViewById(R.id.favoritesListView);
        favoriteTracks = new ArrayList<>();
        favoriteUris = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, favoriteTracks);

        headerView = new TextView(this);
        headerView.setText("\uD83D\uDC96 Favori Şarkılar");
        headerView.setTextSize(22);
        headerView.setPadding(32, 32, 32, 16);
        headerView.setTextColor(getResources().getColor(android.R.color.black));
        favoritesListView.addHeaderView(headerView);

        favoritesListView.setAdapter(adapter);

        playerBar = findViewById(R.id.playerBar);
        playPauseButton = findViewById(R.id.playPauseButton);
        rewindButton = findViewById(R.id.rewindButton);
        forwardButton = findViewById(R.id.forwardButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        currentTrackText = findViewById(R.id.currentTrackText);
        seekBar = findViewById(R.id.seekBar);
        positionText = findViewById(R.id.positionText);
        durationText = findViewById(R.id.durationText);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_favorites);
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(FavoritesActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_favorites) {
                    return true;
                }
                return false;
            }
        });

        playPauseButton.setOnClickListener(v -> togglePlayPause());
        rewindButton.setOnClickListener(v -> seekBy(-15000));
        forwardButton.setOnClickListener(v -> seekBy(15000));
        prevButton.setOnClickListener(v -> spotifyAppRemote.getPlayerApi().skipPrevious());
        nextButton.setOnClickListener(v -> spotifyAppRemote.getPlayerApi().skipNext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {}
            @Override public void onStartTrackingTouch(SeekBar sb) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                userSeeking = false;
                if (spotifyAppRemote != null && spotifyAppRemote.isConnected())
                    spotifyAppRemote.getPlayerApi().seekTo(sb.getProgress());
            }
        });

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Lütfen giriş yapın.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("favorites")
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String name = doc.getString("trackName");
                        String artist = doc.getString("artistName");
                        String uri = doc.getString("trackUri");
                        if (name != null && artist != null && uri != null) {
                            favoriteTracks.add(name + " - " + artist);
                            favoriteUris.add(uri);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Favoriler alınamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        favoritesListView.setOnItemClickListener((parent, view, position, id) -> {
            if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
                String uri = favoriteUris.get(position - 1); // Header offset
                spotifyAppRemote.getPlayerApi().play(uri);
                Toast.makeText(this, "Çalınıyor: " + favoriteTracks.get(position - 1), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Spotify bağlantısı yok.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote remote) {
                spotifyAppRemote = remote;
                subscribeToPlayerUpdates();
                startSeekbarUpdater();
            }

            @Override
            public void onFailure(Throwable error) {
                Toast.makeText(FavoritesActivity.this, "Spotify'a bağlanılamadı.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
            SpotifyAppRemote.disconnect(spotifyAppRemote);
        }
    }

    private void subscribeToPlayerUpdates() {
        spotifyAppRemote.getPlayerApi().subscribeToPlayerState().setEventCallback(state -> {
            Track t = state.track;
            if (t != null) {
                currentTrackText.setText(t.name + " - " + t.artist.name);
                isPlaying = !state.isPaused;
                playPauseButton.setImageResource(
                        isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                playerBar.setVisibility(View.VISIBLE);
                int pos = (int) state.playbackPosition;
                int dur = (int) t.duration;
                seekBar.setMax(dur);
                seekBar.setProgress(pos);
                positionText.setText(formatTime(pos));
                durationText.setText(formatTime(dur));
            }
        });
    }

    private void startSeekbarUpdater() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (spotifyAppRemote != null && !userSeeking) {
                    spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(st -> {
                        int pos = (int) st.playbackPosition;
                        int dur = seekBar.getMax();
                        seekBar.setProgress(pos);
                        positionText.setText(formatTime(pos));
                        durationText.setText(formatTime(dur));
                    });
                }
                seekHandler.postDelayed(this, 1000);
            }
        };
        seekHandler.post(updateSeekBar);
    }

    private void togglePlayPause() {
        if (spotifyAppRemote == null || !spotifyAppRemote.isConnected()) return;
        if (isPlaying) spotifyAppRemote.getPlayerApi().pause();
        else spotifyAppRemote.getPlayerApi().resume();
        isPlaying = !isPlaying;
        playPauseButton.setImageResource(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void seekBy(int offset) {
        if (spotifyAppRemote == null || !spotifyAppRemote.isConnected()) return;
        spotifyAppRemote.getPlayerApi().getPlayerState()
                .setResultCallback(state -> spotifyAppRemote.getPlayerApi()
                        .seekTo((int) (state.playbackPosition + offset)));
    }

    private String formatTime(int ms) {
        int minutes = (ms / 1000) / 60;
        int seconds = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}
