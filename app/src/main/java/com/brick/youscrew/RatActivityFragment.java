package com.brick.youscrew;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.AppInstance;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;

/**
 * NEW VERSION, USES THE RAT CLASS
 */


public class RatActivityFragment extends Fragment {

    public static final int REQUEST_EDIT_RAT_DIALOG = 1;
    public static final int REQUEST_DELETE_SESSIONS_DIALOG = 2;
    public static final int REQUEST_DELETE_RAT_DIALOG = 3;

    private AppInstance mAppInstance;

    // SQLiteOpenHelper
    private TurnDbHelper dbHelper;

    // Adapter for the listView
    private ArrayAdapter<String> ratListAdapter;

    // String ArrayList holds the content of the rat listView
    private ArrayList<String> ratListStrings;

    private int numRats;

    private Rat[] mRats;

    // Field to stored which listView item has just been touched (this is necessary for the context
    // menu callbacks to know which mRatIds to operate on)
    private int mListItemSelected;

    private Context mContext;

    SQLiteDatabase mDb;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mContext = getContext();
        mAppInstance = AppInstance.getInstance(mContext);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_rat, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case (R.id.action_settings) :
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
                break;

            case (R.id.action_new_rat) :
                TurnDbUtils.ratOpenEditDialog2(this, TurnDbUtils.RECORD_NEW, null, REQUEST_EDIT_RAT_DIALOG);
                break;

        }

        return true;

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_rat, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        Rat rat = mRats[mListItemSelected];

        Long ratId = rat.getId();

        switch (item.getItemId() ) {

            case R.id.action_delete_rat :
                TurnDbUtils.ratOpenDeleteDialog2(mDb, rat, this, REQUEST_DELETE_RAT_DIALOG);
                refreshRatList();
                return true;

            case R.id.action_delete_rat_sessions :
                TurnDbUtils.sessionOpenDeleteAllDialog(mDb, ratId, this, REQUEST_DELETE_SESSIONS_DIALOG);
                refreshRatList();
                return true;

            case R.id.action_edit_rat :
                TurnDbUtils.ratOpenEditDialog2(this, TurnDbUtils.RECORD_EDIT, rat, REQUEST_EDIT_RAT_DIALOG);
                return true;

            case R.id.action_export_log :
                GeneralUtils.exportTurningLog(mContext, ratId);
                return true;

            case R.id.action_show_summary :
                Intent intent = new Intent(mContext, RatSummaryActivity.class);
                intent.putExtra("rat", rat);
                startActivity(intent);
                return true;


            default :
                return super.onContextItemSelected(item);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case REQUEST_EDIT_RAT_DIALOG :
                if (resultCode == TurnDbUtils.DIALOG_OK) {
                    refreshRatList();
                }
                break;

            case REQUEST_DELETE_RAT_DIALOG :
                if (resultCode == TurnDbUtils.DIALOG_OK) {
                    refreshRatList();
                }
                break;

        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Get the rat names from the database table
        dbHelper = TurnDbHelper.getInstance(mContext);

        // (FOR TESTING: DELETE THE DATABASE AND CREATE ANEW)
        mDb = dbHelper.getReadableDatabase();

        // Create an ArrayAdapter.
        ratListStrings = new ArrayList<>();

        ratListAdapter =
                new ArrayAdapter<>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_rat, // The name of the layout ID.
                        R.id.list_item_rat_textview, // The ID of the textview to populate.
                        ratListStrings);

        // Update the adapter with rat names from the database
        refreshRatList();

        View rootView = inflater.inflate(R.layout.fragment_rat, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listView_rat);
        listView.setAdapter(ratListAdapter);

        // Enable the listView to generate a context menu when items are long-pressed
        registerForContextMenu(listView);

        // Set the click listener for the rat list items: launch ONERATACTIVITY
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                        if (position < numRats) {
                            Intent intent = new Intent(getContext(), OneRatActivity.class);
                            intent.putExtra("rat", mRats[position]);
//                            intent.putExtra("ratId", mRats[position].getId());
                            startActivity(intent);
                        }
                    }
                }
        );

        // The long click listener just assigns the position of the clicked item to the class
        // instance field mListItemSelected, so that the context menu callbacks know which rat to deal with
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mListItemSelected = position;
                return false;
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        // Every time the view resumes, update the database
        super.onResume();

        // Check that the mDb is still open; if not, re-open it
        if (!mDb.isOpen()) {
            mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();
        }

        refreshRatList();

    }

    private void refreshRatList() {

        ratListStrings.clear();

        mRats = Rat.fromDbAllRats(mDb);
        numRats = mRats.length;

        for (int i=0; i<numRats; i++) {
            ratListStrings.add( mRats[i].getName() + " (" + mRats[i].getCode() + ")");
        }

        ratListAdapter.notifyDataSetChanged();

    }

}
