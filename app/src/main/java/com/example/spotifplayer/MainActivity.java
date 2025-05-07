package com.example.spotifplayer;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String CLIENT_ID = "";
    private static final String REDIRECT_URI = "";
    private static final int REQUEST_CODE = 1337;
    private static final String CHANNEL_ID = "player_channel";
    private static final String ACTION_PLAY_PAUSE = "com.example.spotifplayer.PLAY_PAUSE";

    private SpotifyAppRemote spotifyAppRemote;
    private String accessToken;

    private EditText searchEditText;
    private ListView resultsListView;
    private TrackAdapter adapter;
    private final List<String> trackNames = new ArrayList<>();
    private final List<String> trackUris = new ArrayList<>();
    private int currentFavIndex = -1;
    private boolean isInFavoriteMode = false;
    private String lastPlayedUri = "";
    private boolean pendingNextFavorite = false;

    private LinearLayout playerBar;
    private ImageButton playPauseButton, rewindButton, forwardButton, prevButton, nextButton;
    private TextView currentTrackText;
    private SeekBar seekBar;
    private TextView positionText, durationText;
    private TextView headerView;

    private boolean isPlaying = true;
    private boolean userSeeking = false;

    private final Handler seekHandler = new Handler();
    private Runnable updateSeekBar;

    private MediaSessionCompat mediaSession;

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PLAY_PAUSE.equals(intent.getAction())
                    && spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
                MainActivity.this.togglePlayPause();
                MainActivity.this.updatePlaybackState();
            }
        }
    };

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Media Controls", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Spotify media controls");
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) mgr.createNotificationChannel(channel);
        }

        registerReceiver(notificationReceiver, new IntentFilter(ACTION_PLAY_PAUSE));


        ImageButton profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        mediaSession = new MediaSessionCompat(this, "SpotifySession");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        searchEditText = findViewById(R.id.searchEditText);
        resultsListView = findViewById(R.id.resultsListView);
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

        headerView = new TextView(this);
        headerView.setText("\uD83D\uDD25 Top 10 Playlist");
        headerView.setTextSize(22);
        headerView.setPadding(32, 32, 32, 16);
        headerView.setTextColor(getResources().getColor(android.R.color.black));
        resultsListView.addHeaderView(headerView);
        adapter = new TrackAdapter(this, trackNames, new TrackAdapter.OnTrackActionListener() {
            @Override public void onPlayClick(int position) {
                if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
                    spotifyAppRemote.getPlayerApi().play(trackUris.get(position));
                    if (isInFavoriteMode) {
                        currentFavIndex = position;
                    }
                }
            }


            @Override
            public void onAddClick(int position) {
                // Buraya arama modundaki ekleme işlemi zaten var
            }

            @Override
            public void onDeleteClick(int position) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) return;
                String userId = user.getUid();
                String trackUriToDelete = trackUris.get(position);

                db.collection("users")
                        .document(userId)
                        .collection("favorites")
                        .whereEqualTo("trackUri", trackUriToDelete)
                        .get()
                        .addOnSuccessListener(query -> {
                            for (QueryDocumentSnapshot doc : query) {
                                doc.getReference().delete();
                            }
                            Toast.makeText(MainActivity.this, "Favoriden silindi", Toast.LENGTH_SHORT).show();
                            fetchFavoriteTracks();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(MainActivity.this, "Silinemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            }
        },  true); // 🔥 Favori mod açık



        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                fetchTopTracks();
                headerView.setText("\uD83D\uDD25 Top 10 Playlist");
                return true;
            } else if (id == R.id.nav_favorites) {
                fetchFavoriteTracks(); // 🔥 İşte burası önemli
                return true;
            }
            return false;
        });

        resultsListView.setAdapter(adapter);

        accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE).getString("token", null);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (accessToken != null) {
                searchTracks(searchEditText.getText().toString());
                headerView.setText("\uD83D\uDD25 Arama Sonuçları");
            } else {
                Toast.makeText(this, "Token alınamadı", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    fetchTopTracks();
                    headerView.setText("\uD83D\uDD25 Top 10 Playlist");
                }
            }
        });

        if (accessToken != null) fetchTopTracks();

        playPauseButton.setOnClickListener(v -> togglePlayPause());
        rewindButton.setOnClickListener(v -> seekBy(-15000));
        forwardButton.setOnClickListener(v -> seekBy(15000));
        prevButton.setOnClickListener(v -> {
            if (isInFavoriteMode) {
                playPreviousFavorite(); // bunun için ayrıca fonksiyon yazarım istersen
            } else {
                spotifyAppRemote.getPlayerApi().skipPrevious();
            }
        });
        nextButton.setOnClickListener(v -> {
            if (isInFavoriteMode) {
                playNextFavorite(); // bizim listemizde sıradaki
            } else {
                spotifyAppRemote.getPlayerApi().skipNext(); // Spotify playlist’inde sıradaki
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {}
            @Override public void onStartTrackingTouch(SeekBar sb) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                userSeeking = false;
                if (spotifyAppRemote != null && spotifyAppRemote.isConnected())
                    spotifyAppRemote.getPlayerApi().seekTo(sb.getProgress());
            }
        });

        AuthorizationRequest req = new AuthorizationRequest.Builder(
                CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"streaming", "app-remote-control"})
                .build();
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, req);
    }

    @Override protected void onStart() {
        super.onStart();
        accessToken = getSharedPreferences("SpotifyPrefs", MODE_PRIVATE).getString("token", null);
        if (accessToken != null) connectToSpotify();




    }

    @Override protected void onResume() {
        super.onResume();
        if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
            updatePlaybackState();
            startSeekbarUpdater();
        }
    }

    @Override protected void onDestroy() {
        unregisterReceiver(notificationReceiver);
        mediaSession.setActive(false);
        if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
            spotifyAppRemote.getPlayerApi().pause();
            SpotifyAppRemote.disconnect(spotifyAppRemote);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AuthorizationResponse resp = AuthorizationClient.getResponse(resultCode, data);
        if (resp.getType() == AuthorizationResponse.Type.TOKEN) {
            accessToken = resp.getAccessToken();
            getSharedPreferences("SpotifyPrefs", MODE_PRIVATE)
                    .edit().putString("token", accessToken).apply();
            connectToSpotify();
            fetchTopTracks(); // EKLEDİĞİMİZ SATIR
        }
    }
    private void fetchFavoriteTracks() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Giriş yapılmamış", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        isInFavoriteMode = true;
        currentFavIndex = -1;
        lastPlayedUri = "";

        db.collection("users").document(userId).collection("favorites")
                .orderBy("timestamp", Query.Direction.ASCENDING)                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    trackNames.clear();
                    trackUris.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("trackName");
                        String artist = doc.getString("artistName");
                        String uri = doc.getString("trackUri");

                        if (name == null || artist == null || uri == null) continue;

                        trackNames.add(name + " - " + artist);
                        trackUris.add(uri);
                    }

                    adapter = new TrackAdapter(this, trackNames, new TrackAdapter.OnTrackActionListener() {
                        @Override public void onPlayClick(int position) {
                            if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
                                spotifyAppRemote.getPlayerApi().play(trackUris.get(position));
                                currentFavIndex = position;
                                lastPlayedUri = ""; // Yeni çalma başlatıldığında sıfırla
                            }
                        }

                        @Override public void onAddClick(int position) {
                            // Favori modda ekleme yapılmaz
                        }

                        @Override public void onDeleteClick(int position) {
                            String trackUriToDelete = trackUris.get(position);
                            db.collection("users").document(userId).collection("favorites")
                                    .whereEqualTo("trackUri", trackUriToDelete)
                                    .get()
                                    .addOnSuccessListener(query -> {
                                        for (DocumentSnapshot doc : query) {
                                            doc.getReference().delete();
                                        }
                                        Toast.makeText(MainActivity.this, "Favoriden silindi", Toast.LENGTH_SHORT).show();
                                        fetchFavoriteTracks(); // Güncelle
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(MainActivity.this, "Silinemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }, true);

                    resultsListView.setAdapter(adapter);
                    headerView.setText("⭐ Favori Şarkılar");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Favoriler alınamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }




    private void connectToSpotify() {
        ConnectionParams params = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI).showAuthView(true).build();
        SpotifyAppRemote.connect(this, params, new Connector.ConnectionListener() {
            @Override public void onConnected(SpotifyAppRemote remote) {
                spotifyAppRemote = remote;
                subscribeToPlayerUpdates();
                updatePlaybackState();
                startSeekbarUpdater();
            }
            @Override public void onFailure(Throwable e) {
                Log.e("SpotifyRemote", "Error", e);
            }
        });
    }

    private void playPreviousFavorite() {
        if (trackUris == null || trackUris.isEmpty()) return;

        currentFavIndex--;
        if (currentFavIndex < 0) {
            currentFavIndex = trackUris.size() - 1; // başa sardıysa en sona geç
        }

        spotifyAppRemote.getPlayerApi().play(trackUris.get(currentFavIndex));
        lastPlayedUri = trackUris.get(currentFavIndex); // tekrarı engelle
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

                // 🔥 Favori modda ve şarkı sonuna çok yaklaşıldıysa

            }
        });
    }

    private void updatePlaybackState() {
        spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(state -> {
            Track t = state.track;
            if (t != null) {
                int dur = (int) t.duration;
                int pos = (int) state.playbackPosition;
                seekBar.setMax(dur);
                seekBar.setProgress(pos);
                positionText.setText(formatTime(pos));
                durationText.setText(formatTime(dur));
              // showMediaNotification(t.name + " - " + t.artist.name, !state.isPaused, pos, dur);
            }
        });
    }

    private void startSeekbarUpdater() {
        updateSeekBar = new Runnable() {
            @Override public void run() {
                if (spotifyAppRemote != null && !userSeeking) {
                    spotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(st -> {
                        int pos = (int) st.playbackPosition;
                        Track t = st.track;
                        if (t == null) return;

                        int dur = (int) t.duration;
                        seekBar.setMax(dur);
                        seekBar.setProgress(pos);
                        positionText.setText(formatTime(pos));
                        durationText.setText(formatTime(dur));

                        // 🔥 Burada kendimiz kontrol ediyoruz
                        if (isInFavoriteMode && dur - pos <= 2000 && !pendingNextFavorite && !t.uri.equals(lastPlayedUri)) {
                            pendingNextFavorite = true;
                            lastPlayedUri = t.uri;
                            seekHandler.postDelayed(() -> {
                                playNextFavorite();
                                pendingNextFavorite = false;
                            }, 500); // 0.5 saniye sonra sıradakini çal
                        }

                    });
                }

                seekHandler.postDelayed(this, 500); // her 0.5 saniyede kontrol et
            }
        };
        seekHandler.post(updateSeekBar);
    }


    private void searchTracks(String q) {
        Retrofit rf = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create()).build();
        rf.create(SpotifyService.class)
                .searchTracks("Bearer " + accessToken, q, "track", 10)
                .enqueue(new Callback<SearchResponse>() {
                    @Override public void onResponse(Call<SearchResponse> c, Response<SearchResponse> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            trackNames.clear(); trackUris.clear();
                            for (SearchResponse.TrackItem it : r.body().tracks.items) {
                                trackNames.add(it.name + " - " + it.artists.get(0).name);
                                trackUris.add(it.uri);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onFailure(Call<SearchResponse> c, Throwable t) {}
                });
    }

    private void showMediaNotification(String title, boolean playing, int position, int duration) {
        if (mediaSession == null) mediaSession = new MediaSessionCompat(this, "SpotifySession");

        int smallIconRes = getApplicationInfo().icon;
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), getApplicationInfo().icon);

        PendingIntent piToggle = PendingIntent.getBroadcast(this, 0,
                new Intent(ACTION_PLAY_PAUSE), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent piContent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(formatTime(position) + " / " + formatTime(duration))
                .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                        playing ? "Pause" : "Play", piToggle)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setProgress(duration, position, false)
                .setContentIntent(piContent)
                .setOngoing(playing);

        NotificationManager mgr = getSystemService(NotificationManager.class);
        if (mgr != null) mgr.notify(1, nb.build());
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

    private void fetchTopTracks() {
        Retrofit rf = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // 🔁 Favori dışı mod
        isInFavoriteMode = false;
        currentFavIndex = -1;
        lastPlayedUri = "";

        rf.create(SpotifyService.class)
                .getPlaylistTracks("Bearer " + accessToken, "0ej2MqYBdJZ16wcLtZdeQT")
                .enqueue(new Callback<PlaylistTracksResponse>() {
                    @Override
                    public void onResponse(Call<PlaylistTracksResponse> c, Response<PlaylistTracksResponse> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            trackNames.clear();
                            trackUris.clear();

                            int limit = Math.min(10, r.body().items.size());
                            for (int i = 0; i < limit; i++) {
                                PlaylistTracksResponse.TrackItem item = r.body().items.get(i);
                                String name = item.track.name;
                                String artist = item.track.artists.get(0).name;
                                String uri = item.track.uri;
                                trackNames.add(name + " - " + artist);
                                trackUris.add(uri);
                            }

                            adapter = new TrackAdapter(MainActivity.this, trackNames, new TrackAdapter.OnTrackActionListener() {
                                @Override
                                public void onPlayClick(int position) {
                                    if (spotifyAppRemote != null && spotifyAppRemote.isConnected()) {
                                        spotifyAppRemote.getPlayerApi().play(trackUris.get(position));
                                    }
                                }

                                @Override
                                public void onAddClick(int position) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user == null) {
                                        Toast.makeText(MainActivity.this, "Giriş yapmış kullanıcı yok", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String userId = user.getUid();
                                    String trackUri = trackUris.get(position);

                                    db.collection("users").document(userId).collection("favorites")
                                            .whereEqualTo("trackUri", trackUri)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                if (querySnapshot.isEmpty()) {
                                                    Map<String, Object> trackData = new HashMap<>();
                                                    trackData.put("trackUri", trackUri);
                                                    trackData.put("trackName", trackNames.get(position).split(" - ")[0]);
                                                    trackData.put("artistName", trackNames.get(position).split(" - ")[1]);
                                                    trackData.put("timestamp", System.currentTimeMillis()); // 🔥 Burası eklendi


                                                    db.collection("users").document(userId).collection("favorites")
                                                            .add(trackData)
                                                            .addOnSuccessListener(docRef ->
                                                                    Toast.makeText(MainActivity.this, "Favorilere eklendi", Toast.LENGTH_SHORT).show())
                                                            .addOnFailureListener(e ->
                                                                    Toast.makeText(MainActivity.this, "Ekleme başarısız: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                                } else {
                                                    Toast.makeText(MainActivity.this, "Bu şarkı zaten favorilerde", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(MainActivity.this, "Kontrol hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                }

                                @Override
                                public void onDeleteClick(int position) {
                                    // Top listede silme yok
                                }
                            }, false);

                            resultsListView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                            headerView.setText("🔥 Top 10 Playlist");
                        }
                    }

                    @Override
                    public void onFailure(Call<PlaylistTracksResponse> c, Throwable t) {
                        Toast.makeText(MainActivity.this, "Top 10 alınamadı", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void playNextFavorite() {
        if (trackUris == null || trackUris.isEmpty()) return;

        currentFavIndex++;
        if (currentFavIndex >= trackUris.size()) {
            currentFavIndex = 0; // başa sar
        }

        // 🔒 Şarkı ismi görünmeden önce TextView'ı gizle
        currentTrackText.setVisibility(View.INVISIBLE);

        // 🔁 Yeni şarkıyı çal
        String nextUri = trackUris.get(currentFavIndex);
        spotifyAppRemote.getPlayerApi().play(nextUri);

        // ✅ 0.1 saniye sonra TextView'ı tekrar görünür yap
        new Handler().postDelayed(() -> {
            currentTrackText.setVisibility(View.VISIBLE);
        }, 500);
    }


    private String formatTime(int ms) {
        int minutes = (ms / 1000) / 60;
        int seconds = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
} 