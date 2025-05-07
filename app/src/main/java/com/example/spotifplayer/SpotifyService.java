package com.example.spotifplayer;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpotifyService {

    @GET("v1/search")
    Call<SearchResponse> searchTracks(
            @Header("Authorization") String auth,
            @Query("q") String query,
            @Query("type") String type,
            @Query("limit") int limit
    );

    @GET("v1/playlists/{playlist_id}/tracks")
    Call<PlaylistTracksResponse> getPlaylistTracks(
            @Header("Authorization") String auth,
            @Path("playlist_id") String playlistId
    );
}
