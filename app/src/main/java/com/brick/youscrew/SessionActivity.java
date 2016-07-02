package com.brick.youscrew;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.brick.youscrew.data.Session;

public class SessionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onBackPressed() {
        // Pressing back during an open session will open a dialog asking whether to end it.
        // Pressing back during a closed session just calls the super method

        final SessionActivityFragment fragment = (SessionActivityFragment)
                getSupportFragmentManager().getFragments().get(0);

        if (fragment.isSessionOpen() == Session.OPEN_YES) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setMessage("Finish the session?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fragment.closeSession();
                            SessionActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton("Cancel", null);

            builder.create().show();

        }

        else {
            super.onBackPressed();
        }

    }


}
