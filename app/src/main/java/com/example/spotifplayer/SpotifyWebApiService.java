package com.example.spotifplayer;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface SpotifyWebApiService {
    @GET("v1/me/player")
    Call<PlayerResponse> getPlayerState(@Header("Authorization") String authHeader);
}
