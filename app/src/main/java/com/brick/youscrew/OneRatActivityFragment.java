package com.brick.youscrew;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.Session;
import com.brick.youscrew.data.TurnContract.SessionEntry;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.DatePickerFragment;
import com.brick.youscrew.utils.GeneralUtils;
import com.brick.youscrew.utils.TimePickerFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class OneRatActivityFragment extends Fragment {

    private final String LOG_TAG = OneRatActivityFragment.class.getSimpleName();

    private final int REQUEST_EDIT_RAT_DIALOG = 1;
    private final int REQUEST_DELETE_RAT_DIALOG = 2;
    private final int REQUEST_DELETE_ALL_SESSIONS_DIALOG = 3;

    private final int TARGET_NEW_SESSION = 1;
    private final int TARGET_UPDATE_SESSION = 2;

    // We need to collate some information about most recent session found in the mDb
    private Long    mLastSessionTime;
    private Long    mLastSessionId;

    private SQLiteDatabase mDb;

    private Rat mRat;
    private Session[] mSessions;
    private int mNumSessions;

    private DialogFragment mTimePicker, mDatePicker;

    private ArrayAdapter<String> mSessionAdapter;
    private ArrayList<String> mSessionStrings = new ArrayList<>();

    private int mListItemSelected;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mContext = getContext();

        mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_one_rat, menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_one_rat, menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If the delete rat dialog tells us the rat has been deleted, finish the activity
        switch (requestCode) {
            case REQUEST_DELETE_RAT_DIALOG :
                if (resultCode == TurnDbUtils.DIALOG_OK) {
                    getActivity().finish();
                }
                break;
            case REQUEST_DELETE_ALL_SESSIONS_DIALOG :
                if (resultCode == TurnDbUtils.DIALOG_OK) {
                    refreshSessionStrings();
                }
                break;

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final long ratId = mRat.getId();

        Intent intent;

        switch (item.getItemId() ) {

            case R.id.action_new_session :
                getNewSessionType();
                break;

            case R.id.action_delete_rat :
                TurnDbUtils.ratOpenDeleteDialog(mDb, ratId, this, REQUEST_DELETE_RAT_DIALOG);
                break;

            case R.id.action_delete_rat_sessions :
                TurnDbUtils.sessionOpenDeleteAllDialog(mDb, ratId, this, REQUEST_DELETE_ALL_SESSIONS_DIALOG);
                break;

            case R.id.action_edit_rat :
                TurnDbUtils.ratOpenEditDialog2(this, TurnDbUtils.RECORD_EDIT, mRat, REQUEST_EDIT_RAT_DIALOG);
                break;

            case R.id.action_settings :
                intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.action_export_log :
                GeneralUtils.exportTurningLog(mContext, ratId);
                break;

            case R.id.action_show_summary :
                intent = new Intent(mContext, RatSummaryActivity.class);
                intent.putExtra("rat", mRat);
                startActivity(intent);
                break;

        }

        return true;

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

//        final long id = mSessionIds[mListItemSelected];
        final Session session = mSessions[mListItemSelected];
        final long id = mSessions[mListItemSelected].getId();

        AlertDialog.Builder builder;

        switch (item.getItemId()) {

            case R.id.action_show_summary :
                return true;

            case R.id.action_add_comment :
                editSessionComment(id);
                refreshSessionStrings();
                return true;

            case R.id.action_change_date :
                getUserPickedTime(TARGET_UPDATE_SESSION, id);
                refreshSessionStrings();
                return true;

            case R.id.action_delete_session :
                builder = new AlertDialog.Builder(mContext)
                        .setMessage("Are you sure you want to delete the session? This cannot be undone!")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListItemSelected == 0) {
                                    session.deleteFromDb(mDb);
//                                    TurnDbUtils.sessionDeleteEntry(mDb, id);
                                    Toast.makeText(mContext, "Session deleted", Toast.LENGTH_LONG).show();
                                    refreshSessionStrings();
                                }
                                else {
                                    Toast.makeText(mContext, "Only the most recent session can be deleted", Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                builder.create().show();

                return true;

            default :

                return super.onContextItemSelected(item);
        }

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout first
        View rootView = inflater.inflate(R.layout.fragment_one_rat, container, false);

        // Get the rat Id
        Intent startIntent = getActivity().getIntent();
        mRat = startIntent.getParcelableExtra("rat");

        // Get the listView and populate with the strings
        ListView listView = (ListView) rootView.findViewById(R.id.one_rat_session_listview);

        mSessionAdapter = new ArrayAdapter<>(getContext(), R.layout.list_item_session,
                R.id.list_item_session_textview, mSessionStrings);

        listView.setAdapter(mSessionAdapter);

        // Clicking the session list opens SessionActivity
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(mContext, SessionActivity.class);
                intent.putExtra("session", mSessions[position]);
                startActivity(intent);
            }
        });

        // Long-clicking the list opens the context menu
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mListItemSelected = position;
                return false;
            }
        });

        registerForContextMenu(listView);

        // Get the list of session strings from the mDb
        refreshSessionStrings();

        return rootView;

    }

    @Override
    public void onStart() {
        super.onStart();
        // Set the toolbar contents (this needs to be done after view creation is finished)
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        toolbar.setSubtitle(mRat.getName() + " (" + mRat.getCode() + ") ");
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mDb.isOpen()) {
            mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();
        }

        // Update the session listView
        refreshSessionStrings();
    }

    private void refreshSessionStrings() {

        // Construct the list of strings used by the listView. This also gets the start time
        // for each session
        //
        // STRING FORMAT:
        // <SESSION TIME> <NUM TTS TURNED> <COMMENT>

        mSessionStrings.clear();

        mSessions = mRat.findSessions(mDb, TurnDbUtils.QUERY_ROW_ANY);
        mNumSessions = mSessions.length;

        // If there are any records, get the time and id of the most recent one
        if (mNumSessions != 0) {

            mLastSessionTime = mSessions[0].getLong(SessionEntry.COLUMN_TIME_START);
            mLastSessionId = mSessions[0].getId();
        }
        else {
            mLastSessionId = null;
            mLastSessionTime = null;
        }

        // Now loop through the sessions and create a formatted date string for each
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

        for (int s=0; s<mNumSessions; s++) {

            // Set the calendar's time to the millisec-precision value in the mDb
            date.setTime(mSessions[s].getLong(SessionEntry.COLUMN_TIME_START));

            // Create formatted date string
            String strTime = dateFormat.format(date);
            String strTt = " (" + mSessions[s].getInt(SessionEntry.COLUMN_NUM_TTS_TURNED) + " TTs)";

            String strComm = mSessions[s].getString(SessionEntry.COLUMN_COMMENT);

            if (strComm == null) {
                strComm = "";
            }

            mSessionStrings.add(
                    Integer.toString(mNumSessions - s) + ") " + strTime + strTt + " " + strComm);
        }

        mSessionAdapter.notifyDataSetChanged();

    }

    private Long getLastUsedTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (prefs.contains("lastUserPickedTime")) {
            return prefs.getLong("lastUserPickedTime", -1);
        }
        else {
            return null;
        }
    }

    private void setPickedTime(long timeMillis) {
        // Save the last picked time to the shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        prefs.edit().putLong("lastUserPickedTime", timeMillis).commit();
    }

    private void getNewSessionType() {
        // Shows a dialog requesting user to choose between a real-time and picked-time session

        DialogFragment dialog = new DialogFragment() {

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {

                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                builder.setMessage(R.string.dialog_session_type)

                        // REAL TIME BUTTON
                        // No picker needed, so just start ttActivity
                        .setNegativeButton(R.string.dialog_session_type_real, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startSessionActivity(null);
                            }
                        })

                        // PICKED TIME BUTTON
                        // Start the time picker
                        .setPositiveButton(R.string.dialog_session_type_picked, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getUserPickedTime(TARGET_NEW_SESSION, null);
                            }
                        });
                // Create the AlertDialog object and return it
                return builder.create();

            }


        };

        dialog.show(getActivity().getFragmentManager(), "sessionTypeDialog");


    }

    private void getUserPickedTime(final int timePickTarget, final Long sessionId) {
        // 1) Shows a date picker dialog
        // 2) When the date is picked, a time dialog shows up
        // 3) The the time is picked, ttActivity is launched

        // Create a calendar instance that the dialogs will manipulate
        final Calendar c = Calendar.getInstance();

        mDatePicker = new DatePickerFragment() {

            @Override
            public void onDateSet(DatePicker view, int year, int month, int day) {
                super.onDateSet(view, year, month, day);

                // Set a calendar to the user-picked date
                c.set(year, month, day);

                // Now show the time picker
                mTimePicker.show(getActivity().getFragmentManager(), "timePicker");

            }
        };

        mTimePicker = new TimePickerFragment() {


            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                super.onTimeSet(view, hourOfDay, minute);

                // Get the values previously set in the calendar
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);

                // Set the calendar with the correct time of day
                c.set(year, month, day, hourOfDay, minute);

                long newSessionTime = c.getTimeInMillis();

                long currentTime = System.currentTimeMillis();

                // Check that the picked time is in the past
                if (newSessionTime < currentTime) {

                        // Store the time just chosen in the shared prefs file
                        setPickedTime(newSessionTime);

                        if (timePickTarget == TARGET_NEW_SESSION) {
                            if (mLastSessionTime == null || newSessionTime > mLastSessionTime) {
                                // Start ttActivity
                                startSessionActivity(newSessionTime);
                            }
                            else {
                                Toast.makeText(mContext, "The new session cannot occur before a preexisting session. Please try again.", Toast.LENGTH_LONG).show();
                            }

                        } else if (timePickTarget == TARGET_UPDATE_SESSION) {
                            if (TurnDbUtils.sessionIsTimeValid(mDb, sessionId, newSessionTime)) {
                                // Update the time value for a session and all turn children
                                TurnDbUtils.sessionEditTime(mDb, sessionId, newSessionTime);
                                refreshSessionStrings();
                            }
                            else {
                                Toast.makeText(mContext, "The session time must be between the previous session and the next.", Toast.LENGTH_LONG).show();
                            }
                        }
                }
                else {
                    Toast.makeText(
                            mContext,
                            "You cannot select a time in the future! Please try again.",
                            Toast.LENGTH_LONG).show();
                }

            }
        };

        // Send the start time mode to the dialogs
        Bundle args = new Bundle();

        Long lastUsedTime = getLastUsedTime();
        if (lastUsedTime != null) {
            args.putInt("startTimeMode", DatePickerFragment.START_TIME_MILLIS);
            args.putLong("startValue", lastUsedTime);
        } else {
            args.putInt("startTimeMode", DatePickerFragment.START_TIME_CURRENT);
        }

        mDatePicker.setArguments(args);
        mTimePicker.setArguments(args);

        mDatePicker.show(getActivity().getFragmentManager(), "datePicker");

    }


    private void startSessionActivity(Long timeMillis) {
        // Launch sessionActivity. If argument timeMillis is set, the session will be marked as
        // 'picked-time'; if null, the session will be marked 'real-time'

        // Check that the new session time is after the most recent
        if (mLastSessionTime != null && timeMillis != null && timeMillis <= mLastSessionTime) {
            throw new RuntimeException("Previous session time '" + Long.toString(mLastSessionTime) +
                    "' is not less than new session time " + Long.toString(timeMillis));
        }

        // Create a new session record.
        long sessionId = TurnDbUtils.sessionAddEntry(mDb, mRat.getId(), mLastSessionId, timeMillis, null, null);

        // Create session object from the db record
        Session session = new Session(mRat, mDb, sessionId);

        // Create intent to start SessionActivity.
        Intent intent = new Intent(getContext(), SessionActivity.class);
        intent.putExtra("session", session);
        startActivity(intent);

    }

    private void editSessionComment(final long sessionId) {

        // First get the existing comment
        Cursor c = TurnDbUtils.singleEntryQuery(mDb,
                SessionEntry.TABLE_NAME,
                new String[]{SessionEntry.COLUMN_COMMENT},
                sessionId,
                null);

        if (c.getCount() == 0) {
            throw new RuntimeException("Error retrieving comment from session record");
        }

        String comment = c.getString(c.getColumnIndex(SessionEntry.COLUMN_COMMENT));

        c.close();

        if (comment == null) {
            comment = "";
        }

        final EditText editText = new EditText(getContext());
        editText.setText(comment);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit session comment")
                .setView(editText)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TurnDbUtils.singleEntryUpdate(mDb,
                                        SessionEntry.TABLE_NAME,
                                        sessionId,
                                        SessionEntry.COLUMN_COMMENT,
                                        editText.getText().toString() );

                                refreshSessionStrings();

                            }
                        }
                )
                .show();



    }

}
