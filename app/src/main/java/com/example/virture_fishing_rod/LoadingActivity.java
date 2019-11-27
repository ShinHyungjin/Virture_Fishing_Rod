package com.example.virture_fishing_rod;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

public class LoadingActivity extends AppCompatActivity {
    Intent intent;
    Button bt;
    MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES, WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_loading);
        bt = findViewById(R.id.button2);
        player = MediaPlayer.create(this,R.raw.entire);
        player.setLooping(true);
        player.start();

        intent = new Intent(LoadingActivity.this, GameActivity.class);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent);
            }
        });


        ImageView s = findViewById(R.id.iv);
        GlideDrawableImageViewTarget gif = new GlideDrawableImageViewTarget(s);
        Glide.with(this).load(R.drawable.loading).into(gif);

    }
}
