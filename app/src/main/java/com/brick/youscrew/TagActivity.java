package com.brick.youscrew;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class TagActivity extends AppCompatActivity {

    public static final String LOG_TAG = TagActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        Log.v(LOG_TAG, "onCreateContextMenu()");
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        super.onContextMenuClosed(menu);
        Log.v(LOG_TAG, "onContextMenuClosed");
    }

    @Override
    public void onBackPressed() {

        TagActivityFragment fragment =
                (TagActivityFragment) getSupportFragmentManager().getFragments().get(0);

        int editMode = fragment.getTagEditMode();
        int turnTime = fragment.getTurnTime();

        Intent data = new Intent()
                .putExtra("editMode", editMode)
                .putExtra("turnTime", turnTime);

        setResult(TagActivityFragment.RESULT_CANCELLED, data);

        Toast.makeText(this, "Tagging cancelled", Toast.LENGTH_SHORT).show();


        finish();

    }
}
