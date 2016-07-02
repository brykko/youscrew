package com.brick.youscrew;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;

import java.util.List;

public class TurnActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_turn);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Determine the TT identity and display in the toolbar
        Intent intent = getIntent();
        String ttName = intent.getStringExtra("ttName");
        toolbar.setSubtitle(ttName);

        // Keep the screen on while this activity is running
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onBackPressed() {
        // Only allow navigation with back button if the tetrode initial angle is not yet set
        // (in which case there is no 'cancel' button on screen

        // Find the fragment and determine whether the tetrode
        List<Fragment> fragments = getSupportFragmentManager().getFragments();

        TurnActivityFragment fragment = (TurnActivityFragment) fragments.get(0);

        final int initialAngleSet = fragment.getInitialAngleSet();
        final int activityMode    = fragment.getActivityMode();

        // If in view mode, just go back
        if (activityMode == TurnActivityFragment.MODE_VIEW) {
            super.onBackPressed();
        }

        // If in edit mode, show a dialog to request confirmation to cancel turning
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("Cancel turning and return to menu?")
                    .setPositiveButton("Back to menu", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TurnActivity.this.setResult(RESULT_CANCELED);
                            TurnActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton("Resume turning", null);
            builder.create().show();
        }
    }
}
