package com.brick.youscrew;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.Session;
import com.brick.youscrew.data.Tetrode;
import com.brick.youscrew.data.Turn;
import com.brick.youscrew.data.TurnContract.SessionEntry;
import com.brick.youscrew.data.TurnContract.TetrodeEntry;
import com.brick.youscrew.data.TurnContract.TurnEntry;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.graphics.FilledCircle;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class SessionActivityFragment extends Fragment {

    private final String LOG_TAG = SessionActivityFragment.class.getSimpleName();

    private final int SCREW_SIZE_PIXELS = 150;

    public static final int TURNED_REQUEST = 3;

    TTAdapter ttListAdapter;
    ArrayList<String> ttListStrings = new ArrayList<>();
    ListView ttListView;

    private Rat         mRat;
    private Tetrode[]   mTetrodes;
    private Turn[]      mTurns;
    private Session     mSession;

    private boolean     mUiInitialized = false;

    private int         mTtSelectedIndex;

    SQLiteDatabase mDb;

    private Context mContext;
    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_session, container, false);

        mContext = getContext();

        // Open the database
        mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();

        // Retrieve info about the rat and current session
        getDataFromDb();

        return mRootView;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_session, menu);

        MenuItem buttonOpen = menu.findItem(R.id.action_open_session);
        MenuItem buttonDelete = menu.findItem(R.id.action_delete_session);
        MenuItem buttonFinish = menu.findItem(R.id.action_finish_session);

        // 'Open session' button is only enabled when the session is:
        // 1) The most recent session
        // 2) Not currently open
        int isOpen = mSession.getInt(SessionEntry.COLUMN_IS_OPEN);

        if (isOpen == Session.OPEN_YES || TurnDbUtils.sessionIsMostRecent(mDb, mSession.getId())) {
            buttonOpen.  setEnabled(false);
            buttonOpen.  setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            buttonFinish.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        // 'Finish session' button is only enabled when the session is currently open
        if (isOpen == Session.OPEN_NO) {
            buttonFinish.setEnabled(false);
            buttonFinish.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            buttonOpen.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        // 'Delete session' button is only enabled when the session is currently closed AND it is
        // the most recent session
        if (isOpen == Session.OPEN_NO || !TurnDbUtils.sessionIsMostRecent(mDb, mSession.getId())) {
            buttonDelete.setEnabled(false);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_delete_session :
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setMessage("Are you sure you want to delete the current session?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSession.deleteFromDb(mDb);
//                                TurnDbUtils.sessionDeleteEntry(mDb, mSession.getId());
                                getActivity().finish();
                                Toast.makeText(mContext, "Session deleted", Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                builder.create().show();
                break;

            case R.id.action_open_session :

                // Mark the session as open, both in the scope of this fragment...
                mSession.set(SessionEntry.COLUMN_IS_OPEN, Session.OPEN_YES);
                mSession.writeToDb(mDb);

                // ... and also in the database record
                TurnDbUtils.singleEntryUpdate(mDb, SessionEntry.TABLE_NAME, mSession.getId(),
                        SessionEntry.COLUMN_IS_OPEN, Session.OPEN_YES);

                // Reset the UI contents for an open session
                setUpUi();

                // Force re-creation of options menu
                getActivity().invalidateOptionsMenu();
                break;

            case R.id.action_settings :
                Intent intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.action_finish_session :
                mSession.set(SessionEntry.COLUMN_IS_OPEN, Session.OPEN_NO);
                closeSession();
                getActivity().finish();
                break;

        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_session, menu);

        MenuItem itemTagPost = menu.findItem(R.id.action_edit_tags_post);
        MenuItem itemUndoTurn = menu.findItem(R.id.action_undo_turn);

        if (mTurns[mTtSelectedIndex].getInt(TurnEntry.COLUMN_WAS_TURNED) == Turn.TURNED_NO) {
            itemTagPost.setEnabled(false);
            itemUndoTurn.setEnabled(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final Turn turn = mTurns[mTtSelectedIndex];
        final Tetrode tetrode = mTetrodes[mTtSelectedIndex];

        AlertDialog.Builder builder;
        final EditText text;
        Cursor c;

        switch (item.getItemId()) {

            case R.id.action_edit_tags_pre :
                GeneralUtils.getTags(this, TurnDbUtils.TAG_TYPE_PRETURN, turn.getId(), TagActivityFragment.MODE_UPDATE);
                refreshTTList();
                break;

            case R.id.action_edit_tags_post :
                GeneralUtils.getTags(this, TurnDbUtils.TAG_TYPE_POSTTURN, turn.getId(), TagActivityFragment.MODE_UPDATE);
                refreshTTList();
                break;

            case R.id.action_edit_comment :

                text = new EditText(mContext);
                text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                // Get the current comment, if there is one
                c = TurnDbUtils.turnGetEntries(mDb, turn.getId());
                String comment = c.getString(c.getColumnIndex(TurnEntry.COLUMN_COMMENT));
                c.close();
                text.setText(comment);

                // Show a dialog that
                builder = new AlertDialog.Builder(mContext)
                        .setMessage("Enter comment for TT" + Integer.toString(mTtSelectedIndex+1))
                        .setView(text)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TurnDbUtils.singleEntryUpdate(
                                        mDb,
                                        TurnEntry.TABLE_NAME,
                                        turn.getId(),
                                        TurnEntry.COLUMN_COMMENT,
                                        text.getText().toString());

                                // Update UI to display the comment
                                refreshTTList();

                            }
                        });
                builder.create().show();

                break;

            case R.id.action_edit_tetrode_tag :

                text = new EditText(mContext);
                text.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                // Get the current comment, if there is one

                text.setText(tetrode.getString(TetrodeEntry.COLUMN_COMMENT));

                // Show a dialog that
                builder = new AlertDialog.Builder(mContext)
                        .setMessage("Enter tag for TT" + Integer.toString(mTtSelectedIndex+1))
                        .setView(text)
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                tetrode.set(TetrodeEntry.COLUMN_COMMENT, text.getText().toString());
                                tetrode.writeToDb(mDb);

                                dialog.dismiss();

                                // Update UI to display the comment
                                refreshTTList();

                            }
                        });
                builder.create().show();

                break;

            case R.id.action_undo_turn :

                String nullString = null;

                ContentValues values = new ContentValues();
                values.put(TurnEntry.COLUMN_END_ANGLE, turn.getDouble(TurnEntry.COLUMN_START_ANGLE));
                values.put(TurnEntry.COLUMN_WAS_TURNED, Turn.TURNED_NO);
                values.put(TurnEntry.COLUMN_TAG_ID_POST, nullString);
                mDb.update(TurnEntry.TABLE_NAME, values, TurnEntry._ID + " = " + Long.toString(turn.getId()), null);

                tetrode.updateFromDb(mDb);

                if (tetrode.wasEverTurned(mDb) == Tetrode.EVER_TURNED_NO) {
//                if (TurnDbUtils.tetrodeWasEverTurned(mDb, tetrode.getId()) == Tetrode.EVER_TURNED_NO) {
//                if (tetrode.getInt(TetrodeEntry.COLUMN_EVER_TURNED) == Tetrode.EVER_TURNED_NO)
                    mTetrodes[mTtSelectedIndex].set(TetrodeEntry.COLUMN_EVER_TURNED, Tetrode.EVER_TURNED_NO);
                }

                refreshTTList();

        }

        return true;

    }

    @Override
    public void onResume() {
        super.onResume();

        // Need to wait for activity to set up action bar before we call setUpUi() here.
        if (!mUiInitialized) {
            setUpUi();
            mUiInitialized = true;
        }

        refreshTTList();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // Get the relevant Turn and update to reflect any changes that have just occurred
        Turn turn = mTurns[mTtSelectedIndex];
        turn.updateFromDb(mDb);

        Log.v(LOG_TAG, "Result received, code=" + Integer.toString(requestCode) +
                " , result=" + Integer.toString(resultCode));

        if (requestCode == TURNED_REQUEST) {

            switch (resultCode) {

                // OK, return to SessionActivity
                case Activity.RESULT_OK:
                    Log.v(LOG_TAG, "Result received: OK, RETURN TO TTACTIVITY");
                    break;

                // Turning was cancelled
                case Activity.RESULT_CANCELED:
                    Log.v(LOG_TAG, "Result received: CANCELLED");
                    break;

                // OK, start turning next TT
                case TurnActivityFragment.RESULT_NEXT_TT:
                    Log.v(LOG_TAG, "Result received: OK, NEXT TETRODE");
                    if (++mTtSelectedIndex < mTetrodes.length) {
                        startTurnActivity(mTtSelectedIndex, TurnActivityFragment.MODE_TURN);
                    }
                    break;

            }

        }

        else if (requestCode == TagActivityFragment.REQUEST_TAG) {

            Log.v(LOG_TAG, "Tag request code received");

            if (resultCode == TagActivityFragment.RESULT_OK) {
                Log.v(LOG_TAG, "Marking as edited");
                turn.set(TurnEntry.COLUMN_WAS_EDITED, Turn.EDITED_YES);
                turn.writeToDb(mDb);
            }

        }
    }

    public void setUpUi() {

        // Set up UI features appropriately for a closed or open session

        ttListAdapter =
                new TTAdapter(
                        getContext(),
                        R.layout.list_item_tt,
                        R.id.tt_list_item,
                        ttListStrings);

        ttListView = (ListView) mRootView.findViewById(R.id.listView_tt);
        ttListView.setAdapter(ttListAdapter);

        // Set the appropriate item click listeners for the TT list
        int isOpen = mSession.getInt(SessionEntry.COLUMN_IS_OPEN);

        if (isOpen == Session.OPEN_YES) {
            setUpOpenSessionFeatures();
        }
        else if (isOpen == Session.OPEN_NO) {
            setUpClosedSessionFeatures();
        }

        // Set the listView long click listener - this just gets the index of the item touched,
        // so that
        ttListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mTtSelectedIndex = position;
                return false;
            }
        });

        registerForContextMenu(ttListView);

    }

    private void setUpOpenSessionFeatures() {

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle("Session is open");

        // Called during onCreateView; assigns
        ttListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Record the index of the selected tetrode (when TurnActivity returns
                // we'll need this)
                mTtSelectedIndex = position;

                startTurnActivity(mTtSelectedIndex, TurnActivityFragment.MODE_TURN);

            }
        });
    }

    private void setUpClosedSessionFeatures() {

        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle("Session is closed");


        // Called during onCreateView; assigns
        ttListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Record the index of the selected tetrode (when TurnActivity returns
                // we'll need this)
                mTtSelectedIndex = position;

                startTurnActivity(mTtSelectedIndex, TurnActivityFragment.MODE_VIEW);

            }
        });

    }

    private void getDataFromDb() {

        // 1) Retrieve rat / session-specific info from db
        // 2) Get rowIds for all tetrodes
        // 3) Create new turn records for the current session

        mSession = getActivity().getIntent().getParcelableExtra("session");

        if (mSession.getId()==-1 || mSession.getRat().getId()==-1) {
            throw new RuntimeException("Invalid session or rat Id");
        }

        // Determine whether turn entries exist for the current session
        mRat = mSession.getRat();

        mTetrodes = mRat.findTetrodes(mDb, null);

        // Look for turn records
        mTurns = mSession.findTurns(mDb);

        Log.v(LOG_TAG, "Number of tetrode records found = " + Integer.toString(mTetrodes.length));

        Log.v(LOG_TAG, "Number of turn records found = " + Integer.toString(mTurns.length));

        boolean makeNewTurnRecords = mTurns.length == 0;

        if (makeNewTurnRecords) {
            mTurns = new Turn[mTetrodes.length];
        }
        else {
            if (mTurns.length != mTetrodes.length) {
                throw new RuntimeException("Number of Tetrodes = " + Integer.toString(mTetrodes.length) +
                " while number of Turns = " + Integer.toString(mTurns.length));
            }
        }

        // For each tetrode, get the record ID
        for (int i = 0; i < mTetrodes.length; i++) {

            // Also get the turn IDs
            if (makeNewTurnRecords) {

//                // Make blank entries if they don't yet exist
                long turnId = TurnDbUtils.turnAddBlankEntry(mDb, mTetrodes[i].getId(), mSession.getId());

                Log.v(LOG_TAG, "New blank turn entry Id + " + Long.toString(turnId));

                // Create array of Turn objs for the new session and write into the db
                mTurns[i] = new Turn(mSession, mTetrodes[i], mDb, turnId);

            }

            else {
                mTurns[i] = mSession.findTurnByTetrode(mDb, mTetrodes[i]);
            }

            Log.v(LOG_TAG, mTetrodes[i].getString(TetrodeEntry.COLUMN_NAME) +  " turn Id = " + Long.toString(mTurns[i].getId()));

        }

    }

    private void refreshTTList() {

        // Get TT-specific info from db and populate member arrays

        Log.v(LOG_TAG, "Refreshing TT list");

        // Construct a string array containing the rat name from each record
        ttListStrings.clear();

        for (int i=0; i<mTetrodes.length; i++) {
            mTurns[i].updateFromDb(mDb);
            ttListStrings.add("dummyText");
        }

        ttListAdapter.notifyDataSetChanged();

    }

    private void startTurnActivity(int ttIndex, int activityMode) {
        Log.v(LOG_TAG, "Starting new TurnActivity");

        // Put some useful info into the starting intent for turnActivity
        Intent intent = new Intent(getContext(), TurnActivity.class);
//        intent.putExtra("turnId", mTurns[ttIndex].getId());

        intent.putExtra("turn", mTurns[ttIndex]);
        intent.putExtra("activityMode", activityMode);
        startActivityForResult(intent, TURNED_REQUEST);
    }


    private Bitmap scaleImageToView(ImageView imageView, Bitmap bitmap) {
        // Scale a bitmap to fill an imageview

        RectF rectSrc = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectDst = new RectF(0, 0, SCREW_SIZE_PIXELS, SCREW_SIZE_PIXELS);

        Matrix resize = new Matrix();
        resize.setRectToRect(rectSrc, rectDst, Matrix.ScaleToFit.CENTER);

        // Return a new bitmap scaled with the matrix
        Bitmap bitmapResized = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), resize, false);

        // Log the resizing info
        Log.v(LOG_TAG, "Resized image from: H" +
                Integer.toString(bitmap.getHeight()) + " W" +
                Integer.toString(bitmap.getWidth()) + " to H" +
                Integer.toString(bitmapResized.getHeight()) + " W" +
                Integer.toString(bitmapResized.getWidth()));

        return bitmapResized;

    }

    public void closeSession() {

        // If no TTs were turned, delete the session
        boolean anyRecordsEdited = false;
        int numRecordsTurned = 0;
        for (int t = 0; t < mTetrodes.length; t++) {

            String ttName = mTetrodes[t].getString(TetrodeEntry.COLUMN_NAME);

            if (mTurns[t].getInt(TurnEntry.COLUMN_WAS_EDITED) == Turn.EDITED_YES) {
                anyRecordsEdited = true;
                Log.v(LOG_TAG, ttName + " EDITED ");
            }
            else {
                Log.v(LOG_TAG, ttName + " NOT EDITED ");
            }

            if (mTurns[t].getInt(TurnEntry.COLUMN_WAS_EDITED) == Turn.TURNED_YES){
                numRecordsTurned++;
                Log.v(LOG_TAG, ttName + " TURNED ");
            }
            else {
                Log.v(LOG_TAG, ttName + " NOT TURNED ");
            }
        }

        Long timeMillis = null;
        if (mSession.getInt(SessionEntry.COLUMN_TIME_MODE) == Session.TIME_MODE_PICKED) {
            timeMillis = mSession.getLong(SessionEntry.COLUMN_TIME_START);
        }
        TurnDbUtils.sessionFinish(mDb, mSession.getId(), timeMillis, numRecordsTurned);


        if (!anyRecordsEdited) {
            Log.v(LOG_TAG, "No turn records edited; deleting session");

            mSession.deleteFromDb(mDb);
//            TurnDbUtils.sessionDeleteEntry(mDb, mSession.getId());

            // Show toast to indicate session record deleted
            Toast.makeText(
                    getContext(),
                    "No editing occured: session discarded",
                    Toast.LENGTH_LONG)
                    .show();
        }

    }

    private class TTAdapter extends ArrayAdapter<String> {

        ArrayList<String> mStrings;
        LayoutInflater mInflater;
        int mResource;
        int mListItemResource;

        public TTAdapter(Context context, int resource, int textViewResourceId, ArrayList<String> strings) {
            super(context, resource, textViewResourceId, strings);

            mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mResource = resource;
            mListItemResource = textViewResourceId;

        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View listItem = mInflater.inflate(mResource, parent, false);

            Turn turn = mTurns[position];

            Resources res = getResources();

//            ImageView ttIndexIcon = (ImageView) listItem.findViewById(R.id.tt_index_icon);
//
//            float[] x = {-40, -20, 10, 30};
//            float[] y = {23, -15, 40, 19};
//            Line line = new Line(mContext, x, y);
//            line.setColor(Color.RED);
//            line.setStrokeWidth(5);
//            line.setDotFilled(true);
//            line.setDotRadius(2);
//
//            ttIndexIcon.setImageDrawable(line);

            // Create the TT number icon
            FilledCircle ttIndexIcon = (FilledCircle) listItem.findViewById(R.id.tt_index_icon);
            ttIndexIcon.setColor(Color.DKGRAY);
            ttIndexIcon.setDiameter(30);

            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            paint.setTextSize(GeneralUtils.dip2Pix(mContext, 15));
            ttIndexIcon.setText(Integer.toString(position+1), paint);

            // Change list item BG colour if TT was turned
            if (turn.getInt(TurnEntry.COLUMN_WAS_TURNED) == TurnDbUtils.TURN_TURNED) {
                listItem.setBackgroundColor(res.getColor(R.color.colorSessionListItemTurned));
            }

            // Set icon col according to whether ref or not
            if (mTetrodes[position].getInt(TetrodeEntry.COLUMN_IS_REF) == Tetrode.REFERENCE_YES) {
                ttIndexIcon.setColor(res.getColor(R.color.colorTtReference));
            }
            else if (mTetrodes[position].getInt(TetrodeEntry.COLUMN_IS_REF) == Tetrode.REFERENCE_NO) {
                ttIndexIcon.setColor(res.getColor(R.color.colorTtNormal));
            }

            // Set the turn text
            TextView text = (TextView) listItem.findViewById(R.id.tt_textview_turns_this_session);
            double val = GeneralUtils.roundToDecimalPlace(turn.getTurnsThisSession(), 2);
            text.setText(String.format("%.2f T", val));

            // Set the tag text
            text = (TextView) listItem.findViewById(R.id.tt_textview_tag);
            text.setText(mTetrodes[position].getString(TetrodeEntry.COLUMN_COMMENT));

            // Set the depth text
            text = (TextView) listItem.findViewById(R.id.tt_textview_depth);
            val = GeneralUtils.roundToDecimalPlace(turn.getDepth(), 0);
            text.setText(String.format("%.0f %sm", val, "\u03bc"));

            // Set the comment text
            text = (TextView) listItem.findViewById(R.id.tt_textview_comment);
            text.setText(turn.getString(TurnEntry.COLUMN_COMMENT));

            // Set the tag circle fill
            FilledCircle circle = (FilledCircle) listItem.findViewById(R.id.marker_tag_pre);
            circle.setDiameter(8);
            if (!turn.hasTagsPre()) {
                circle.getPaint().setStyle(Paint.Style.STROKE);
            }

            circle = (FilledCircle) listItem.findViewById(R.id.marker_tag_post);
            circle.setDiameter(8);
            if (!turn.hasTagsPost()) {
                circle.getPaint().setStyle(Paint.Style.STROKE);
            }

            return listItem;

        }

    }

    public int isSessionOpen() {
        return mSession.getInt(SessionEntry.COLUMN_IS_OPEN);
    }


}
