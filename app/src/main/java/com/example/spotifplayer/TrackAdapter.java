package com.example.spotifplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class TrackAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> trackNames;
    private final OnTrackActionListener listener;
    private boolean isFavoriteMode; // 🔥

    public interface OnTrackActionListener {
        void onPlayClick(int position);
        void onAddClick(int position);
        void onDeleteClick(int position); // 🔥 Yeni method

    }

    public TrackAdapter(Context context, List<String> trackNames, OnTrackActionListener listener,boolean isFavoriteMode) {
        super(context, R.layout.track_item, trackNames);
        this.context = context;
        this.trackNames = trackNames;
        this.listener = listener;
        this.isFavoriteMode = isFavoriteMode;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.track_item, parent, false);
        }

        TextView trackNameText = convertView.findViewById(R.id.trackNameText);
        ImageButton addButton = convertView.findViewById(R.id.addButton);

        trackNameText.setText(trackNames.get(position));

        trackNameText.setOnClickListener(v -> {
            if (listener != null) listener.onPlayClick(position);
        });

        // 🔁 Butonun görünümünü moda göre değiştir
        if (isFavoriteMode) {
            addButton.setImageResource(android.R.drawable.ic_menu_delete); // Çöp kutusu simgesi
            addButton.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(position);
            });
        } else {
            addButton.setImageResource(android.R.drawable.ic_input_add); // Artı simgesi
            addButton.setOnClickListener(v -> {
                if (listener != null) listener.onAddClick(position);
            });
        }

        return convertView;
    }}

