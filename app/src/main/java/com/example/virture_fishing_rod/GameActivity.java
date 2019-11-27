package com.example.virture_fishing_rod;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

public class GameActivity extends AppCompatActivity {
    MediaPlayer player;
    Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        player = MediaPlayer.create(this,R.raw.splash02);
        player.setLooping(true);
        player.start();

        ImageView s = findViewById(R.id.iv2);
        GlideDrawableImageViewTarget gif = new GlideDrawableImageViewTarget(s);
        Glide.with(this).load(R.drawable.ocean).into(gif);
    }
    public void onBackPressed(){
        player.stop();
        intent = new Intent(GameActivity.this, LoadingActivity.class);
        startActivity(intent);
    }
}
