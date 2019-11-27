package com.example.virture_fishing_rod;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;

public class GameActivity extends AppCompatActivity {
    MediaPlayer player;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        player = MediaPlayer.create(this,R.raw.splash02);
        player.setLooping(true);
        player.start();
    }
}
