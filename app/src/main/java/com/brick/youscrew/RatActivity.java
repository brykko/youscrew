package com.brick.youscrew;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.brick.youscrew.utils.AppInstance;

public class RatActivity extends AppCompatActivity {

    private final String LOG_TAG = RatActivity.class.getSimpleName();

    private boolean mAppInitialized = false;

    SharedPreferences prefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Make sure that the app instance will start afresh when the program starts
        AppInstance.clear();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Log.v(LOG_TAG, "onCreate()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(LOG_TAG, "onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "onResume()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(LOG_TAG, "onStart()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "onDestroy()");
    }

}
