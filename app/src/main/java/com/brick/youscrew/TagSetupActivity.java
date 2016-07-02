package com.brick.youscrew;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import java.util.List;

public class TagSetupActivity extends AppCompatActivity {

    private String LOG_TAG = TagSetupActivity.class.getSimpleName();
    public boolean mContextMenuShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mContextMenuShowing = true;
        Log.v(LOG_TAG, "onCreateContextMenu()");
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        mContextMenuShowing = false;
        Log.v(LOG_TAG, "onContextMenuClosed");
    }

    @Override
    public void onBackPressed() {
        // Display dialog asking user if they want to save the changes

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Do you want to save changes?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(TagSetupActivity.this, "Changes discarded", Toast.LENGTH_SHORT).show();
                        TagSetupActivity.super.onBackPressed();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Get the fragment. Since there's only one fragment, just get all and
                        // take the first from the list
                        List<Fragment> fragments = getSupportFragmentManager().getFragments();
                        TagSetupActivityFragment fragment = (TagSetupActivityFragment) fragments.get(0);

                        fragment.writeTagUseStateToDb();
                        fragment.writeTagGroupFieldsToDb();
                        Toast.makeText(TagSetupActivity.this, "Tag settings saved", Toast.LENGTH_SHORT).show();
                        TagSetupActivity.super.onBackPressed();
                    }
                });

        builder.create().show();

    }
}
