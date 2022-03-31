package com.prozium.gravityapp;

import android.app.Activity;
import android.media.AudioManager;
import android.os.*;
import android.util.Log;

public class GravityActivity extends Activity {

    GravitySurfaceView gravityView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gravityView = new GravitySurfaceView(this);
        setContentView(gravityView);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gravityView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gravityView.onResume();
    }
}
