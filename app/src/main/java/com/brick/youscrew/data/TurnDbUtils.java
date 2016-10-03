package com.brick.youscrew.data;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.brick.youscrew.EditRatDialog;
import com.brick.youscrew.R;
import com.brick.youscrew.data.TurnContract.SessionEntry;
import com.brick.youscrew.data.TurnContract.TagEntry;
import com.brick.youscrew.data.TurnContract.TagGroupEntry;
import com.brick.youscrew.data.TurnContract.TurnEntry;
import com.brick.youscrew.utils.AppInstance;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * Some essential methods for writing to, deleting from, and querying the SQLite database.
 *
 * For each table, there are standard methods:
 * "addEntry" - add a single new record to a table, with all mandatory data fields
 *
 * "deleteEntry" - delete a single record, specified by its _id field. Where this record's _is used
 * as a foreign key in other tables, all 'child' records that reference the specified _id will be
 * deleted recursively
 *
 * "getEntries" - basic query to retrieve all entries in the table. Optionally an _id value may be
 * specified to retrieve a single
 *
 */
public final class TurnDbUtils {

    private static final String LOG_TAG = TurnDbUtils.class.getSimpleName();
    private static final boolean LOG_ERRORS = true;
    private static final boolean LOG_CURSOR_COUNT = false;
    private static final boolean LOG_CURSOR_CONTENTS = false;

    // Session time modes
    public static final int TIME_REAL = 1;
    public static final int TIME_PICKED = 2;

//    public static final int THREAD_DIR_CW = 1;
//    public static final int THREAD_DIR_CCW = 2;

    public static final int QUERY_ROW_EMPTY = 1;        // n == 0
    public static final int QUERY_ROW_ONE = 2;          // n == 1
    public static final int QUERY_ROW_TWOPLUS = 3;      // n >= 2
    public static final int QUERY_ROW_NONEMPTY = 4;     // n != 1
    public static final int QUERY_ROW_ONEPLUS = 5;      // n >= 1
    public static final int QUERY_ROW_ANY = 6;          // n >= 0
    public static final int QUERY_ROW_MAXONE = 7;       // n <= 1

    // Consts for specifying all combinations of pre/post tag types with a single integer value
    public static final int SPECIFY_NONE = 1;
    public static final int SPECIFY_PRE = 2;
    public static final int SPECIFY_POST = 3;
    public static final int SPECIFY_BOTH = 4;

    public static final int SINGLE_CONSTRAINT_OFF = 1;
    public static final int SINGLE_CONSTRAINT_ON = 2;

    // Consts indicating the type of tag (pre-turn or post-turn)
    public static final int TAG_TYPE_PRETURN = 1;
    public static final int TAG_TYPE_POSTTURN = 2;
    public static final int TAG_TYPE_PERSISTENT_PRE = 3;
    public static final int TAG_TYPE_PERSISTENT_POST = 4;
    public static final int TAG_TYPE_GENERIC = 5; // use this for settings mode

    public static final int TURNTIME_PRE = 1;
    public static final int TURNTIME_POST = 2;
    public static final int TURNTIME_GENERIC = 3;

    public static final int TAGGED_YES = 1;
    public static final int TAGGED_NO = 2;
    public static final int TAGGED_POSTPONED = 3;

    public static final int IN_USE_YES = 1;
    public static final int IN_USE_NO = 2;
//    public static final int IN_USE_UNDEFINED = 3;

    public static final int DELETED_NO = 1;
    public static final int DELETED_YES = 2;
//    public static final int DELETED_UNDEFINED = 3;

    public static final int TURN_NOT_TURNED = 1;
    public static final int TURN_TURNED = 2;

    public static final int TURN_UNEDITED = 1;
    public static final int TURN_EDITED = 2;

    public static final int FLAG_APPEND = 1;
    public static final int FLAG_REPLACE = 2;

    public static final int INITIAL_ANGLE_SET = 1;
    public static final int INITIAL_ANGLE_UNSET = 2;

    public static final int TETRODE_REF_YES = 1;
    public static final int TETRODE_REF_NO = 2;

//    public static final int TETRODE_TURNED = 1;
//    public static final int TETRODE_NOT_TURNED = 2;

    public static final int TETRODE_TAGGABLE = 1;
    public static final int TETRODE_NONTAGGABLE = 2;

    public static final int RECORD_NEW = 1;
    public static final int RECORD_EDIT = 2;
    public static final int RECORD_DELETE = 3;

    public static final int DIALOG_OK = 1;
    public static final int DIALOG_CANCEL = 2;

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                  RAT TABLE METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////

//    public static long ratAddEntry (SQLiteDatabase db, String name, String code, int numTTs,
//                                    double turnUm, int threadDir) throws SQLiteConstraintException {
//
//        /*
//        ADDS A SINGLE ENTRY TO THE RAT TABLE. ALL FIELDS MUST BE FILLED
//         */
//
//        if (numTTs < 1) {
//            throw new IllegalArgumentException("Illegal number of tetrodes :" + Integer.toString(numTTs));
//        }
//
//        if (turnUm < 0) {
//            throw new IllegalArgumentException("Illegal microns per turn :" + Double.toString(turnUm));
//        }
//
//        if (threadDir != Rat.THREAD_DIR_CW && threadDir != Rat.THREAD_DIR_CCW) {
//            throw new IllegalArgumentException("Illegal thread direction code :" + Integer.toString(threadDir));
//        }
//
//        // Determine the maximum display index that currently exists
//        Cursor cursor = db.rawQuery("SELECT " + RatEntry.COLUMN_DISPLAY_INDEX + " FROM " +
//                RatEntry.TABLE_NAME, null);
//
//        cursor.moveToFirst();
//
//        int maxDisplayIndex = 0;
//        int currentDisplayIndex;
//
//        for(int c=0; c<cursor.getCount(); c++) {
//            currentDisplayIndex = cursor.getInt(cursor.getColumnIndex(RatEntry.COLUMN_DISPLAY_INDEX));
//            if (currentDisplayIndex > maxDisplayIndex);
//            maxDisplayIndex = currentDisplayIndex;
//            cursor.moveToNext();
//        }
//        cursor.close();
//
//        ContentValues values = new ContentValues();
//        values.put(RatEntry.COLUMN_NAME, name);
//        values.put(RatEntry.COLUMN_CODE, code);
//        values.put(RatEntry.COLUMN_NUM_TETRODES, numTTs);
//        values.put(RatEntry.COLUMN_TURN_MICROMETERS, turnUm);
//        values.put(RatEntry.COLUMN_DISPLAY_INDEX, maxDisplayIndex+1);
//        values.put(RatEntry.COLUMN_SCREW_THREAD_DIR, threadDir);
//
//        long rowId = db.insert(TurnContract.RatEntry.TABLE_NAME, null, values);
//
//        checkRowId(rowId);
//
//        AppInstance.getBackupHelper().notifyDatabaseChanged();
//
//        return rowId;
//
//    }
//
//    public static void ratDeleteEntry (SQLiteDatabase db, long ratId) {
//        // Delete a rat entry and all its child tetrode and session records
//
//        ratDeleteTetrodeChildren(db, ratId);
//
//        ratDeleteSessionChildren(db, ratId);
//
//        singleEntryDelete(db, RatEntry.TABLE_NAME, ratId);
//
//    }
//
//    private static void ratDeleteTetrodeChildren(SQLiteDatabase db, long ratId) {
//
//        // Delete all tetrode records linked to a ratId
//
//        // First query the tetrode table for all children
//        Cursor cursor = tetrodeGetEntriesByRatId(db, ratId, null);
//
//        // Determine how many child tags must be deleted
//        int nChildTetrodes = cursor.getCount();
//        int colIdxId = cursor.getColumnIndex(TetrodeEntry._ID);
//        long currentId;
//
//        cursor.moveToFirst();
//
//        // Cycle through the children and delete them
//        for (int r=0; r<nChildTetrodes; r++) {
//            currentId = cursor.getLong(colIdxId);
//            tetrodeDeleteEntry(db, currentId);
//            cursor.moveToNext();
//        }
//
//        cursor.close();
//
//    }
//
//    private static void ratDeleteSessionChildren(SQLiteDatabase db, long ratId) {
//        // Delete all tetrode records linked to a ratId
//
//        // First query the tetrode table for all children
//        Cursor cursor = sessionGetEntriesByRatId(db, ratId, QUERY_ROW_ANY);
//
//        // Determine how many child tags must be deleted
//        int nChild = cursor.getCount();
//        int colIdxId = cursor.getColumnIndex(SessionEntry._ID);
//        long currentId;
//
//        cursor.moveToFirst();
//
//        // Cycle through the children and delete them
//        for (int r=0; r<nChild; r++) {
//            currentId = cursor.getLong(colIdxId);
//            sessionDeleteEntry(db, currentId);
//            cursor.moveToNext();
//        }
//
//        cursor.close();
//
//    }
//
//    public static Cursor ratGetEntries (SQLiteDatabase db, Long ratId) {
//
//        return singleEntryQuery(
//                db,
//                RatEntry.TABLE_NAME,
//                ratFieldNames(),
//                ratId,
//                RatEntry.COLUMN_NAME);
//
//    }
//
//    public static String[] ratFieldNames() {
//        return new String[] {
//                RatEntry._ID,
//                RatEntry.COLUMN_NAME,
//                RatEntry.COLUMN_NUM_TETRODES,
//                RatEntry.COLUMN_TURN_MICROMETERS,
//                RatEntry.COLUMN_CODE,
//                RatEntry.COLUMN_DISPLAY_INDEX,
//                RatEntry.COLUMN_SCREW_THREAD_DIR
//        };
//    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                 TETRODE TABLE METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////

//    public static long tetrodeAddEntry (SQLiteDatabase db, long ratId, int index, String name, int colour, int reference) {
//
//        /*
//        ADDS A SINGLE NEW TETRODE RECORD TO THE TABLE. THE INITIAL_ANGLE AND TOTAL_TURNS FIELDS ARE
//        FILLED WITH ZEROS, SINCE THESE ARE EXPECTED TO BE SET LATER
//         */
//
//        ContentValues values = new ContentValues();
//        values.put(TetrodeEntry.COLUMN_RAT_KEY, ratId);
//        values.put(TetrodeEntry.COLUMN_INDEX, index);
//        values.put(TetrodeEntry.COLUMN_NAME, name);
//        values.put(TetrodeEntry.COLUMN_IN_USE, IN_USE_YES);
//        values.put(TetrodeEntry.COLUMN_IS_REF, reference);
//        values.put(TetrodeEntry.COLUMN_COLOUR, colour);
//        values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE, 0);
//        values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET, INITIAL_ANGLE_UNSET);
//        values.put(TetrodeEntry.COLUMN_EVER_TURNED, TETRODE_NOT_TURNED);
//        values.put(TetrodeEntry.COLUMN_TAGGABLE, TETRODE_TAGGABLE);
//
//        long rowId = db.insert(TetrodeEntry.TABLE_NAME, null, values);
//
//        AppInstance.getBackupHelper().notifyDatabaseChanged();
//
//        return rowId;
//
//    }
//
//    public static void tetrodeSetInitialAngle (SQLiteDatabase db, long tetrodeId, Double initialAngle) {
//        // Set a tetrode's initial angle. If the argument initialAngle is null, the database angle
//        // will be 'unset' (initial_angle is set to 0 deg, and the initial_angle_set field is set to 'unset')
//
//        ContentValues values = new ContentValues();
//
//        int angleSetCode;
//
//        //
//        if (initialAngle == null) {
//            initialAngle = 0.0;
//            angleSetCode = INITIAL_ANGLE_UNSET;
//        }
//        else {
//            angleSetCode = INITIAL_ANGLE_SET;
//        }
//
//        values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE, initialAngle);
//        values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET, angleSetCode);
//
//        int numRecords = db.update(TetrodeEntry.TABLE_NAME,
//                values,
//                TetrodeEntry._ID + " = " + tetrodeId,
//                null);
//
//        if (numRecords!=1) {
//            throw new RuntimeException("Error updating initial angle");
//        }
//
//        AppInstance.getBackupHelper().notifyDatabaseChanged();
//
//    }
//
//    public static void tetrodeDeleteEntry (SQLiteDatabase db, long tetrodeId) {
//        // Delete a tetrode and all its child turn records
//
//        // Get all child turn events
//        Cursor cursor = turnGetEntriesByTetrodeId(db, tetrodeId);
//
//        int nChild = cursor.getCount();
//        int colIdxId = cursor.getColumnIndex(TurnEntry._ID);
//        long currentTurnId;
//
//        // For each child:
//        for (int c=0; c<nChild; c++) {
//            // Get the ID
//            currentTurnId = cursor.getLong(colIdxId);
//            // Delete
//            turnDeleteEntry(db, currentTurnId);
//        }
//
//        // Now delete the parent
//        singleEntryDelete(db, TetrodeEntry.TABLE_NAME, tetrodeId);
//
//    }
//
//    public static Cursor tetrodeGetEntries (SQLiteDatabase db, Long tetrodeId) {
//
//        return singleEntryQuery(
//                db,
//                TetrodeEntry.TABLE_NAME,
//                tetrodeFieldNames(),
//                tetrodeId,
//                TetrodeEntry.COLUMN_INDEX);
//
//    }
//
//    public static Cursor tetrodeGetEntriesByRatId (SQLiteDatabase db, long ratId, Integer ttIndex) {
//
//        // Simple query to get TTs for one rat. Optionally a TT index can be specified to return
//        // a single TT.
//
//        String[] projection = tetrodeFieldNames();
//
//        // Sort by ascending TT index
//        String sortOrder = TetrodeEntry.COLUMN_INDEX + " ASC";
//
//        // If no index given, get all TTs for the specified rat
//        String selection;
//        String[] selectionArgs;
//        if (ttIndex == null) {
//            selection = TetrodeEntry.COLUMN_RAT_KEY + " = ? ";
//            selectionArgs = new String[]{Long.toString(ratId)};
//        }
//        // Otherwise query using both the ratId AND the tetrode index
//        else {
//            selection = TetrodeEntry.COLUMN_RAT_KEY + " = ? AND " +
//                    TetrodeEntry.COLUMN_INDEX + " = ? ";
//            selectionArgs = new String[]{Long.toString(ratId), Integer.toString(ttIndex)};
//        }
//
//        // Query the DB to get all rat records
//        Cursor cursor = db.query(
//                TetrodeEntry.TABLE_NAME,
//                projection,
//                selection,
//                selectionArgs,
//                null,
//                null,
//                sortOrder);
//
//        // Check the cursor; we expect there to be >0 results
//        checkCursor(cursor, QUERY_ROW_NONEMPTY);
//
//        return cursor;
//    }
//
//    public static int tetrodeWasEverTurned(SQLiteDatabase db, long ttId) {
//        // Determine if a tetrode has ever been turned, by finding child turn records and determining
//        // whether turning took place in any of them
//
//        // Find all turn records for current TT
//        Cursor c = turnGetEntriesByTetrodeId(db, ttId);
//        int numTurnRecords = c.getCount();
//
//        int wasEverTurned = TETRODE_NOT_TURNED;
//
//        // Check all turn records
//        for (int t = 0; t < numTurnRecords; t++) {
//            if (c.getInt(c.getColumnIndex(TurnEntry.COLUMN_WAS_TURNED)) == TURN_TURNED) {
//                wasEverTurned = TETRODE_TURNED;
//            }
//            c.moveToNext();
//        }
//
//        c.close();
//
//        return wasEverTurned;
//
//    }
//
//    public static String[] tetrodeFieldNames() {
//
//        return new String[] {
//                TetrodeEntry._ID,
//                TetrodeEntry.COLUMN_RAT_KEY,
//                TetrodeEntry.COLUMN_NAME,
//                TetrodeEntry.COLUMN_INDEX,
//                TetrodeEntry.COLUMN_INITIAL_ANGLE,
//                TetrodeEntry.COLUMN_IN_USE,
//                TetrodeEntry.COLUMN_IS_REF,
//                TetrodeEntry.COLUMN_COLOUR,
//                TetrodeEntry.COLUMN_INITIAL_ANGLE_SET,
//                TetrodeEntry.COLUMN_EVER_TURNED,
//                TetrodeEntry.COLUMN_TAGGABLE,
//                TetrodeEntry.COLUMN_COMMENT
//        };
//    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                 SESSION TABLE METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static long sessionAddEntry(SQLiteDatabase db, long ratId, Long sessionIdLast, Long time, String[] tags, String comment) {

        /*
        ADDS A SINGLE NEW RECORD TO THE SESSION TABLE. ONLY THE "RATID" IS A MANDATORY ARGUMENT;
        TAGS WILL BE ADDED IF NOT NULL. THE "TIME" FIELD WILL BE GIVEN THE CURRENT SYSTEM TIME.

        THE FIELD IS_FIRST_SESSION IS AUTOMATICALLY SET BY CHECKING FOR OTHER EXISTING SESSION RECORDS
        WITH THE SAME RATID. IF NONE ARE FOUND, THE NEW SESSION IS DEFINED AS THE FIRST
         */

        // Format a comma-separated string from the array of tags
        String tagsCommaSep;
        if (tags == null) {
            tagsCommaSep = "";
        }
        else {
            tagsCommaSep = makeCommaSepString(tags);
        }

        // If time arg is null, use real time mode
        int timeMode;

        if (time == null) {
            time = getTime();
            timeMode = TIME_REAL;
        }

        // Otherwise, use the given picked time value
        else {
            timeMode = TIME_PICKED;
        }

        // Determine if this is the first session for the specified rat
        boolean isFirstSession = false;
//        Cursor c = sessionGetEntriesByRatId(db, ratId, QUERY_ROW_ANY);
//        int numSessions = c.getCount();
        Rat rat = new Rat(db, ratId);
        Session[] sessions = rat.findSessions(db, QUERY_ROW_ANY);
        int numSessions = sessions.length;

        // If no other sessions found, define this as the first session
        if (numSessions == 0) {
            isFirstSession = true;
            if (sessionIdLast != null) {
                throw new RuntimeException("No other sessions were found but a prior sessionId of ' " +
                        Long.toString(sessionIdLast) + "' was given");
            }
        }
        // If other sessions exist
        else {

            // Check that the most recent session (at the first cursor position) is earlier in time
            // than the session to be added
//            long timeLast = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_TIME_START));

            if (sessions[0].getLong(SessionEntry.COLUMN_TIME_START) >= time) {
                throw new RuntimeException("New session time occurred before the previous session");
            }

            if (sessionIdLast == null) {
                throw new RuntimeException("Null prior sessionId, but other sessions were found");
            }

        }

        Log.v(LOG_TAG, "Number or previous existing sessions = " + Integer.toString(numSessions));

        // Create a new record in the session table
        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_RAT_KEY, ratId);
        values.put(SessionEntry.COLUMN_TIME_START, time);
        values.put(SessionEntry.COLUMN_TIME_MODE, timeMode);
        values.put(SessionEntry.COLUMN_NUM_TTS_TURNED, 0);
        values.put(SessionEntry.COLUMN_TAGS, tagsCommaSep);
        values.put(SessionEntry.COLUMN_IS_OPEN, Session.OPEN_YES);
        values.put(SessionEntry.COLUMN_SESSION_KEY_LAST, sessionIdLast);
        values.put(SessionEntry.COLUMN_IS_FIRST_SESSION, isFirstSession ? Session.FIRST_YES : Session.FIRST_NO);
        values.put(SessionEntry.COLUMN_COMMENT, comment);

        long rowId = db.insert(TurnContract.SessionEntry.TABLE_NAME, null, values);

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return rowId;

    }

//    public static void sessionDeleteEntry(SQLiteDatabase db, Long sessionId) {
//        // Delete a single session entry and all its child turning entries
//
//        // First check the session is open; we won't allow deletion otherwise
//        Cursor c = sessionGetEntries(db, sessionId);
//
//        Long lastSessionId;
//
//        if (c.isNull(c.getColumnIndex(SessionEntry.COLUMN_SESSION_KEY_LAST))) {
//            lastSessionId = null;
//        }
//        else {
//            lastSessionId = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_SESSION_KEY_LAST));
//        }
//
//        c.close();
//
//        // Get all child turn events
////        c = turnGetEntriesBySessionId(db, sessionId);
//
//        int nChild = c.getCount();
//        int colIdxId = c.getColumnIndex(TurnEntry._ID);
//        int colIdxTetrode = c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY);
//        long currentTurnId;
//
//        long[] tetrodeIds = new long[nChild];
//
//        // For each child turn record:
//        for (int t=0; t<nChild; t++) {
//            // Get the ID
//            tetrodeIds[t] = c.getLong(colIdxTetrode);
//            currentTurnId = c.getLong(colIdxId);
//            turnDeleteEntry(db, currentTurnId);
//            c.moveToNext();
//        }
//
//        c.close();
//
//        // Now delete the parent
//        singleEntryDelete(db, SessionEntry.TABLE_NAME, sessionId);
//
//        int wasEverTurned;
////        int numTurnRecords;
//
//        // Now check prior turn records for all tetrodes to determine whether they've been turned.
//        if (lastSessionId != null) {
//
//            Log.v(LOG_TAG, "Last session ID = " + Long.toString(lastSessionId) + ". scanning prior turn records...");
//
//            // Loop through each tetrode
//            for (int tt = 0; tt < nChild; tt++) {
//
////                wasEverTurned = tetrodeWasEverTurned(db, tetrodeIds[tt]);
//                wasEverTurned = new Tetrode(db, tetrodeIds[tt]).wasEverTurned(db);
////
////                // Find all turn records for current TT
////                c = turnGetEntriesByTetrodeId(db, tetrodeIds[tt]);
////                numTurnRecords = c.getCount();
////
////                wasEverTurned = TETRODE_NOT_TURNED;
////
////                // Check all turn records
////                for (int t = 0; t < numTurnRecords; t++) {
////                    if (c.getInt(c.getColumnIndex(TurnEntry.COLUMN_WAS_TURNED)) == TURN_TURNED) {
////                        wasEverTurned = TETRODE_TURNED;
////                    }
////                    c.moveToNext();
////                }
////
////                c.close();
//
//                // If no turn records exist where the TT was turned, mark the TT record as unturned
//                if (wasEverTurned == TETRODE_NOT_TURNED) {
//                    singleEntryUpdate(db, TetrodeEntry.TABLE_NAME, tetrodeIds[tt], TetrodeEntry.COLUMN_EVER_TURNED, TETRODE_NOT_TURNED);
//                }
//
//            }
//
//
//        }
//
//        // Otherwise, reset the tetrode record to its original state
//
//        else {
//
//            Log.v(LOG_TAG, "First turn session: returning TT to original state");
//
//            ContentValues values = new ContentValues();
//
//            for (int tt = 0; tt < nChild; tt++) {
//
//                values.clear();
//
//                values.put(TetrodeEntry.COLUMN_EVER_TURNED, TETRODE_NOT_TURNED);
//                values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE, 0);
//                values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET, INITIAL_ANGLE_UNSET);
//
//                db.update(
//                        TetrodeEntry.TABLE_NAME,
//                        values,
//                        TetrodeEntry._ID + " = " + Long.toString(tetrodeIds[tt]),
//                        null);
//
//                AppInstance.getBackupHelper().notifyDatabaseChanged();
//
//
//            }
//        }
//
//    }

//    public static void sessionDeleteEntriesByRatId(SQLiteDatabase db, long ratId) {
//        // Delete ALL session entries with the specified ratId
//        // Also deletes all linked turn  events
//
//        // First query the db to get the rowIds of all sessions matching the given ratId
//        Cursor c = sessionGetEntriesByRatId(db, ratId, QUERY_ROW_ANY);
//
//        int s;
//        int numSessions = c.getCount();
//        long sessionId;
//        int colIdxId = c.getColumnIndex(SessionEntry._ID);
//
//        for (s=0; s<numSessions; s++) {
//            sessionId = c.getLong(colIdxId);
//            sessionDeleteEntry(db, sessionId);
//            c.moveToNext();
//        }
//
//        c.close();
//
//
//        // Now we need to mark all of the tetrodes as unturned
//        Tetrode[] tetrodes = new Rat(db, ratId).findTetrodes(db, null);
//
////        c = tetrodeGetEntriesByRatId(db, ratId, null);
//
//        int t;
//        int numTetrodes = c.getCount();
//        long tetrodeId;
//        colIdxId = c.getColumnIndex(TetrodeEntry._ID);
//
//        for (Tetrode tetrode : tetrodes) {
//
////            tetrodeId = c.getLong(colIdxId);
//
//            ContentValues values = new ContentValues();
//            values.put(TetrodeEntry.COLUMN_EVER_TURNED, TETRODE_NOT_TURNED);
//            values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET, INITIAL_ANGLE_UNSET);
//            values.put(TetrodeEntry.COLUMN_INITIAL_ANGLE, 0);
//            int numRecords = db.update(TetrodeEntry.TABLE_NAME, values, TetrodeEntry._ID + " = " + Long.toString(tetrode.getId()), null);
//
//            if (numRecords != 1) {
//                throw new RuntimeException("Error updating tetrode record");
//            }
////            c.moveToNext();
//        }
//
////        c.close();
//
//        AppInstance.getBackupHelper().notifyDatabaseChanged();
//
//    }

//    public static Cursor sessionGetEntries(SQLiteDatabase db, Long sessionId) {
//
//        return singleEntryQuery(
//                db,
//                SessionEntry.TABLE_NAME,
//                sessionFieldNames(),
//                sessionId,
//                SessionEntry.COLUMN_TIME_START);
//
//    }

//    public static Cursor sessionGetEntriesByRatId(SQLiteDatabase db, long ratId, Integer expectedCode) {
//
//        // We want to retrieve all fields
//        String[] projection = sessionFieldNames();
//
//        // Sort temporally, most recent FIRST
//        String sortOrder = SessionEntry.COLUMN_TIME_START + " DESC";
//
//        // If no index given, get all TTs for the specified rat
//        String selection;
//        String[] selectionArgs;
//
//        selection = SessionEntry.COLUMN_RAT_KEY + " = ? ";
//        selectionArgs = new String[]{Long.toString(ratId)};
//
//        // Query the DB to get all rat records
//        Cursor cursor = db.query(
//                SessionEntry.TABLE_NAME,
//                projection,
//                selection,
//                selectionArgs,
//                null,
//                null,
//                sortOrder);
//
//
//        // Check the cursor
//        checkCursor(cursor, expectedCode);
//
//        return cursor;
//
//    }

    public static void sessionFinish(SQLiteDatabase db, long sessionId, Long time, int numTetrodesTurned) {
        // Updates the fields needed at the end of a session

        // If input time is null, use system time
        if (time == null) {
            time = getTime();
        }

        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_NUM_TTS_TURNED, numTetrodesTurned);
        values.put(SessionEntry.COLUMN_TIME_END, time);
        values.put(SessionEntry.COLUMN_IS_OPEN, Session.OPEN_NO);

        int numRecords = db.update(
                SessionEntry.TABLE_NAME,
                values,
                SessionEntry._ID + " = " + Long.toString(sessionId),
                null);

        if (numRecords!=1) {
            throw new RuntimeException(
                    "Session record with ID '" + Long.toString(sessionId) + "' not found");
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

    }

    public static void sessionEditTime(SQLiteDatabase db, long sessionId, Long time) {
        // Update a session and all child turn entries with a user-picked time

        // First check the specified time is valid
        if (!sessionIsTimeValid(db, sessionId, time)) {
            throw new RuntimeException("Specified time was not valid: it must be more than the " +
                    "previous session time and less than the following session time");
        }

        // Update the session record
        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_TIME_START, time);
        values.put(SessionEntry.COLUMN_TIME_END, time);
        values.put(SessionEntry.COLUMN_TIME_MODE, TIME_PICKED);

        int numRecords = db.update(
                SessionEntry.TABLE_NAME,
                values,
                SessionEntry._ID + " = " + Long.toString(sessionId),
                null);

        if (numRecords != 1) {
            throw new RuntimeException("Error updating session record");
        }

        // Now update times of all turn records with this session Id
        values.clear();
        values.put(TurnEntry.COLUMN_TIME, time);

        db.update(
                TurnEntry.TABLE_NAME,
                values,
                TurnEntry.COLUMN_SESSION_KEY + " = " + Long.toString(sessionId),
                null);

        AppInstance.getBackupHelper().notifyDatabaseChanged();

    }

    public static boolean sessionIsTimeValid(SQLiteDatabase db, long sessionId, long time) {
        // Checks whether a time value is valid for a particular session (i.e.,
        // whether it falls between the previous and following session)

        // Check that the previous session has a smaller time value
        Session session = new Session(db, sessionId);

        Session lastSession = session.findLast(db);
        Session nextSession = session.findNext(db);

        if (lastSession != null && lastSession.getLong(SessionEntry.COLUMN_TIME_START) >= time) {
            return false;
        }

        if (nextSession != null && nextSession.getLong(SessionEntry.COLUMN_TIME_START) <= time) {
            return false;
        }

        return true;

//        Long lastSessionId = session.getLong(SessionEntry.COLUMN_SESSION_KEY_LAST);

//        Cursor c = sessionGetEntries(db, sessionId);
//        Long lastSessionId = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_SESSION_KEY_LAST));
//        c.close();

//        // Check if the last session
//        if (lastSessionId != null) {
//
//            Session lastSession = session.findLast(db);
//            long lastSessionTime = lastSession.getLong(SessionEntry.COLUMN_TIME_START);
//
////            c = sessionGetEntries(db, lastSessionId);
////            long lastSessionTime = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_TIME_START));
////            c.close();
//
//            if (lastSessionTime >= time) {
//                return false;
//            }
//        }
//
//        // Search for the session that comes after the session being edited (if it exists)
//        // and check its time is allowed
//        Cursor c = db.query(
//                SessionEntry.TABLE_NAME,
//                new String[] {SessionEntry.COLUMN_TIME_END},
//                SessionEntry.COLUMN_SESSION_KEY_LAST + " = " + Long.toString(sessionId),
//                null,
//                null,
//                null,
//                null);
//
//        if (c.getCount() != 0) {
//            long nextSessionTime = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_TIME_START));
//            c.close();
//
//            if (nextSessionTime <= time) {
//                return false;
//            }
//        }
//
//        // If we get to here, time time must be OK
//        return true;

    }

    public static boolean sessionIsMostRecent(SQLiteDatabase db, long sessionId) {
        // Checks if the specified session record is the most recent for the rat

        // Search for a session record that points to this sessionId
        Cursor c = db.query(
                SessionEntry.TABLE_NAME,
                new String[] {},
                SessionEntry.COLUMN_SESSION_KEY_LAST + " = " + Long.toString(sessionId),
                null,
                null,
                null,
                null);

        int numRecords = c.getCount();

        c.close();

        return numRecords > 0;

    }

    public static void sessionSetTags(SQLiteDatabase db, long sessionId, String[] tags, int flagCode) {
        // Set the "tags" field for a specified session

        // Find the specified record using its Id
        Session session = new Session(db, sessionId);
//        Cursor cursor = sessionGetEntries(db, sessionId);

//        int colIdxTag = cursor.getColumnIndex(SessionEntry.COLUMN_TAGS);

//        String tagStringOriginal = cursor.getString(colIdxTag);

        String tagStringOriginal = session.getString(SessionEntry.COLUMN_TAGS);
        String tagStringNew = makeCommaSepString(tags);

        // If appending flags, join the two
        if (flagCode == FLAG_APPEND) {
            tagStringNew = makeCommaSepString(new String[] {tagStringOriginal, tagStringNew});
        }

        ContentValues values = new ContentValues();
        values.put(SessionEntry.COLUMN_TAGS, tagStringNew);

        // Update the database
        int nUpdated = db.update(
                SessionEntry.TABLE_NAME,
                values,
                SessionEntry._ID + " = ? ",
                new String[] {Long.toString(sessionId)} );

        // Check the number of updated records. Should be 1.
        if (nUpdated!=1) {
            throw new RuntimeException(
                    "Error: " + Integer.toString(nUpdated) + " records updated. Only 1 expected");
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

    }

    public static String[] sessionFieldNames() {
        return new String[] {
                SessionEntry._ID,
                SessionEntry.COLUMN_NUM_TTS_TURNED,
                SessionEntry.COLUMN_TIME_START,
                SessionEntry.COLUMN_TIME_END,
                SessionEntry.COLUMN_IS_OPEN,
                SessionEntry.COLUMN_TAGS,
                SessionEntry.COLUMN_RAT_KEY,
                SessionEntry.COLUMN_TIME_MODE,
                SessionEntry.COLUMN_SESSION_KEY_LAST,
                SessionEntry.COLUMN_IS_FIRST_SESSION,
                SessionEntry.COLUMN_COMMENT
        };
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                 TURN TABLE METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /*

    WORKFLOW FOR TURN RECORDS:

    1) turnAddBlankEntry - entries must ONLY be created this way, since it checks for prior records
                           and makes sure that starting angles and persistent tags are inherited

    2) turnUpdateAngles  - angles must ONLY be set here. startAngle is optional, and only needs
                           to be set if the user deliberately registers a different start angle
                           from the one inherited from the previous turn record

    3) turnSetTags

     */

    public static long turnAddBlankEntry(SQLiteDatabase db, long tetrodeId, long sessionId) {
        /*
        ADDS A SINGLE NEW BLANK RECORD TO THE TURN TABLE, WITH ONLY THE MANDATORY
        KEYS FILLED IN. TURNS SHOULD BE CREATED FOR ALL TTS AT THE START OF EACH SESSION,
        THEN DATA FIELDS SHOULD BE UPDATED LATER WHEN TURNING IS PERFORMED
         */

        ContentValues values = new ContentValues();

        // Set all mandatory fields
        values.put(TurnEntry.COLUMN_TETRODE_KEY, tetrodeId);
        values.put(TurnEntry.COLUMN_SESSION_KEY, sessionId);
        values.put(TurnEntry.COLUMN_WAS_TURNED, TURN_NOT_TURNED);
        values.put(TurnEntry.COLUMN_WAS_EDITED, TURN_UNEDITED);


        long turnId = db.insert(TurnEntry.TABLE_NAME, null, values);
        checkRowId(turnId);

//        Long turnLastId = turnGetLastId(db, turnId);
        Turn lastTurn = new Turn(db, turnId).findLastTurn(db);

        // If a prior turn ID exists
        if (lastTurn == null) {
            Log.d(LOG_TAG, "No prior turn record found");
        }
        else {

//            Cursor c = turnGetEntries(db, turnLastId);

//            double lastEndAngle = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_END_ANGLE));
            double lastEndAngle = lastTurn.getDouble(TurnEntry.COLUMN_END_ANGLE);

            // Set the new start AND end angle to the previous end angle
            values.clear();
            values.put(TurnEntry.COLUMN_START_ANGLE, lastEndAngle);
            values.put(TurnEntry.COLUMN_END_ANGLE, lastEndAngle);

            // Set the new persistent tags to the previous POST-TURN persistent tags
//            String tagIdString = c.getString(c.getColumnIndex(TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST));
            String tagIdString = lastTurn.getString(TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST);
            values.put(TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE, tagIdString);
            values.put(TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST, tagIdString);

//            c.close();

            // Apply the update
            int numRows = db.update(
                    TurnEntry.TABLE_NAME,
                    values,
                    TurnEntry._ID + " = " + Long.toString(turnId),
                    null);

            if (numRows != 1) {
                throw new RuntimeException("Error updating TurnEntry record");
            }

        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return turnId;

    }


//    public static int turnSetAngles(SQLiteDatabase db, long turnId, double startAngle, double endAngle) {
//
//        /*
//        Updates an existing turn record for the following fields
//        - startAngle
//        - endAngle
//        - was_edited (set to TRUE)
//        - was_turned
//         */
//
//        // Determine the turning time. For this we get the parent session record, and determine whether
//        // it's a picked-time session (in which case all turn events will have the same time), or
//        // a real-time session (in which case call turn events will be stamped with the current
//        // system time when this method is called
//
//        long sessionId = turnGetSessionId(db, turnId);
//        Long time;
//
//        Cursor c = sessionGetEntries(db, sessionId);
//        int sessionTimeMode = c.getInt(c.getColumnIndex(SessionEntry.COLUMN_TIME_MODE));
//
//        if (sessionTimeMode == TurnDbUtils.TIME_PICKED) {
//            time = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_TIME_START));
//        }
//        else if (sessionTimeMode == TurnDbUtils.TIME_REAL) {
//            time = getTime();
//        }
//        else {
//            Log.e(LOG_TAG, "Invalid time mode");
//            time = null;
//        }
//
//        // If the start and end angle match, set the turned field to true
//        int wasTurned = startAngle == endAngle ? TURN_NOT_TURNED : TURN_TURNED;
//
//        ContentValues values = new ContentValues();
//
//        values.put(TurnEntry.COLUMN_START_ANGLE, startAngle);
//        values.put(TurnEntry.COLUMN_END_ANGLE, endAngle);
//        values.put(TurnEntry.COLUMN_WAS_TURNED, wasTurned);
//        values.put(TurnEntry.COLUMN_WAS_EDITED, TURN_EDITED);
//        values.put(TurnEntry.COLUMN_TIME, time);
//
//        String whereClause = TurnEntry._ID + " = " + Long.toString(turnId);
//        int numRecords = db.update(TurnEntry.TABLE_NAME, values, whereClause, null);
//
//        if (numRecords!=1) {
//            throw new RuntimeException("Error updating turn record");
//        }
//
//        AppInstance.getBackupHelper().notifyDatabaseChanged();
//
//        return numRecords;
//
//    }

//    public static void turnUndoTurning(SQLiteDatabase db, long turnId) {
//
//        Cursor c = turnGetEntries(db, turnId);
//        double startAngle = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_START_ANGLE));
//        long tetrodeId =    c.getLong(  c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY));
//
//        ContentValues values = new ContentValues();
//
//        // Reset the turn record to the original state where end angle = start angle
//        values.put(TurnEntry.COLUMN_END_ANGLE, startAngle);
//
//        // Mark as unturned
//        values.put(TurnEntry.COLUMN_WAS_TURNED, TURN_NOT_TURNED);
//
//        // Erase any post-turning tags
//        values.put(TurnEntry.COLUMN_TAG_ID_POST, new String());
//
//        int numRecords = db.update(
//                TurnEntry.TABLE_NAME,
//                values,
//                TurnEntry._ID + " = "  + Long.toString(turnId),
//                null);
//
//        if (numRecords != 1) {
//            throw new RuntimeException("Error updating turn record");
//        }
//
//        // After undoing the turn for the specified record, check if the tetrode was turned before.
//        // If so, mark the tetrode record as never turned
//        if (tetrodeWasEverTurned(db, tetrodeId) == TETRODE_NOT_TURNED) {
//            singleEntryUpdate(db, TetrodeEntry.TABLE_NAME, tetrodeId, TetrodeEntry.COLUMN_EVER_TURNED, TETRODE_NOT_TURNED);
//        }
//
//        AppInstance.getBackupHelper().notifyDatabaseChanged();
//
//    }

//    public static void turnSetComment(SQLiteDatabase db, long turnId, String comment) {
//        singleEntryUpdate(db, TurnEntry.TABLE_NAME, turnId, TurnEntry.COLUMN_COMMENT, comment);
//    }
//
//    public static void turnDeleteEntry(SQLiteDatabase db, long turnId) {
//        // Delete a single turn entry.
//        // DO NOT DELETE TURN ENTRIES UNLESS DELETING A WHOLE SESSION!!!
//
//        singleEntryDelete(db, TurnEntry.TABLE_NAME, turnId);
//    }

    public static void turnSetTagIds(SQLiteDatabase db, long turnId, ArrayList<Long> tagIds, int tagType, int flag) {
        // Set the "tags" fields for a specified turn record. Either/both of "tagIdsPre" and "tagIdsPost"
        // may be null, in which case their values will not be set in the table.

        // If append flag is set, we need to retrieve the existing tags
        String oldCsv = null;
        String dbColumnName;

        switch (tagType) {
            case TAG_TYPE_PRETURN:
                dbColumnName = TurnEntry.COLUMN_TAG_ID_PRE;
                break;
            case TAG_TYPE_POSTTURN:
                dbColumnName = TurnEntry.COLUMN_TAG_ID_POST;
                break;
            case TAG_TYPE_PERSISTENT_PRE:
                dbColumnName = TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE;
                break;
            case TAG_TYPE_PERSISTENT_POST:
                dbColumnName = TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST;
                break;
            default:
                throw new RuntimeException("Invalid tag type");
        }

        // If appending, we need to query the database for preexisting tags
        // and merge them with the new ones
        if (flag==FLAG_APPEND) {

            Cursor cursor = turnGetEntries(db, turnId);
            int colIdx = cursor.getColumnIndex(dbColumnName);
            oldCsv = cursor.getString(colIdx);

        }
        else if (flag!=FLAG_REPLACE) {
            Log.e(LOG_TAG, "Invalid append flag");
        }

        // Convert to CSV (calls ArrayList method)
        String csv = makeCommaSepString(tagIds);
        if (flag==FLAG_APPEND) {
            csv = mergeCommaSepStrings(oldCsv, csv);
        }
        Log.v(LOG_TAG, "Tag ID CSV string: " + csv);

        // Update the database
        singleEntryUpdate(db, TurnEntry.TABLE_NAME, turnId, dbColumnName, csv);

    }

    public static Cursor turnGetEntries(SQLiteDatabase db, Long turnId) {

        // Retrieve turn entries, with optional query by turnId (if null, all entries retrieved)
        return singleEntryQuery(
                db,
                TurnEntry.TABLE_NAME,
                turnFieldNames(),
                turnId,
                TurnEntry.COLUMN_TIME);

    }

//    public static Cursor turnGetEntriesByTetrodeId(SQLiteDatabase db, long tetrodeId) {
//
//        String selection = TurnEntry.COLUMN_TETRODE_KEY + " = ? ";
//        String[] selectionArgs = new String[]{Long.toString(tetrodeId)};
//
//        // Order results by time (most recent first)
//        String sortOrder = TurnEntry.COLUMN_TIME + " DESC ";
//
//        Cursor cursor = db.query(
//                TurnEntry.TABLE_NAME,
//                turnFieldNames(),
//                selection,
//                selectionArgs,
//                null,
//                null,
//                sortOrder);
//
//        // Check cursor; any number of results expected
//        checkCursor(cursor, QUERY_ROW_ANY);
//
//        return cursor;
//    }

//    public static Cursor turnGetEntriesBySessionId(SQLiteDatabase db, long sessionId) {
//
//        String[] projection = turnFieldNames();
//
//        String selection = TurnEntry.COLUMN_SESSION_KEY + " = ? ";
//        String[] selectionArgs = new String[]{Long.toString(sessionId)};
//
//        String sortOrder = TurnEntry.COLUMN_SESSION_KEY + " ASC ";
//
//        Cursor cursor = db.query(
//                TurnEntry.TABLE_NAME,
//                projection,
//                selection,
//                selectionArgs,
//                null,
//                null,
//                sortOrder);
//
//        // Check cursor; any number of results expected
//        checkCursor(cursor, QUERY_ROW_ANY);
//
//        return cursor;
//
//    }

    public static Cursor turnGetEntriesByTimeRange(SQLiteDatabase db, long timeLowerVal, long timeUpperVal) {
        // Retrieve all entries occurring within the specified time range

        String[] projection = turnFieldNames();

        String selection = TurnEntry.COLUMN_TIME + " >= ? AND " + TurnEntry.COLUMN_TIME + " <= ? ";
        String[] selectionArgs = new String[]{Long.toString(timeLowerVal), Long.toString(timeUpperVal)};

        String sortOrder = TurnEntry.COLUMN_TIME + " ASC ";

        Cursor cursor = db.query(
                TurnEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        // Check cursor; any number of results expected
        checkCursor(cursor, QUERY_ROW_ANY);

        return cursor;
    }

    public static Cursor turnGetLastEntryBeforeTime(SQLiteDatabase db, long tetrodeId, Long time,
                                                    boolean inclusive) {
        // Returns the last turn entry for a specified tetrode before a specified time.

        String timeOperand;
        if (inclusive) {
            timeOperand = " <= ? ";
        }
        else {
            timeOperand = " < ? ";
        }

        // If time is null, use current system time
        if (time == null) {
            time = getTime();
        }

        String selection = TurnEntry.COLUMN_TETRODE_KEY + " = ? " +
                "AND " + TurnEntry.COLUMN_TIME + timeOperand;

        String[] selectionArgs = new String[]{
                Long.toString(tetrodeId),
                Long.toString(time)};

        // Order results by time (most recent first)
        String sortOrder = TurnEntry.COLUMN_TIME + " DESC ";

        Cursor cursor = db.query(
                TurnEntry.TABLE_NAME,
                turnFieldNames(),
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        // Check cursor; we expect 0 or 1 results
        checkCursor(cursor, QUERY_ROW_MAXONE);

        return cursor;
    }

    public static long turnGetSessionId(SQLiteDatabase db, long turnId) {
        Cursor c = turnGetEntries(db, turnId);
        long sessionId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_SESSION_KEY));
        checkRowId(sessionId);
        c.close();
        return sessionId;
    }

    public static long turnGetTetrodeId(SQLiteDatabase db, long turnId) {
        Cursor c = turnGetEntries(db, turnId);
        long tetrodeId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY));
        checkRowId(tetrodeId);
        c.close();
        return tetrodeId;
    }

//    public static Long turnGetLastId(SQLiteDatabase db, long turnId) {
//        // Returns the _id of the last registered turn record before the
//        // specified record
//
//        // Get the session ID for the turn record
//        Cursor c = turnGetEntries(db, turnId);
//        long sessionId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_SESSION_KEY));
//        long tetrodeId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY));
//        c.close();
//
//        // Get the session record and retrieve the id for the referenced PREVIOUS session
//        c = sessionGetEntries(db, sessionId);
//        int isFirstSession = c.getInt(c.getColumnIndex(SessionEntry.COLUMN_IS_FIRST_SESSION));
//
//        // If the turn record is from the very first session, don't bother to look
//        if (isFirstSession==1) {
//            c.close();
//            return null;
//        }
//
//
//        // Otherwise, query for the previous turn record
//        else {
//
//            long lastSessionId = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_SESSION_KEY_LAST));
//
//            // Now query the db for turn records matching lastSessionId and tetrodeId
//            Cursor result = db.query(
//                    TurnEntry.TABLE_NAME,
//                    turnFieldNames(),
//                    TurnEntry.COLUMN_SESSION_KEY + " = ? AND " + TurnEntry.COLUMN_TETRODE_KEY + " = ? ",
//                    new String[]{Long.toString(lastSessionId), Long.toString(tetrodeId)},
//                    null,
//                    null,
//                    null,
//                    null
//            );
//
//            result.moveToFirst();
//
//            // Check that a single result was returned
//            if (result.getCount() != 1) {
//                throw new RuntimeException("No prior turn records found");
//            }
//
//            c.close();
//
//            return result.getLong(result.getColumnIndex(TurnEntry._ID));
//
//        }
//
//    }

//    public static Cursor turnGetLastRecord(SQLiteDatabase db, long turnId) {
//        // Retrieve the last turn record before the specified record
//
//        Long lastTurnId = turnGetLastId(db, turnId);
//
//        return singleEntryQuery(db, TurnEntry.TABLE_NAME, turnFieldNames(), lastTurnId, null);
//
//    }

    public static boolean turnHasTags(SQLiteDatabase db, long turnId, int tagType) {

        Cursor c = turnGetEntries(db, turnId);
        String column = null;

        switch (tagType) {
            case TAG_TYPE_PRETURN :
                column = TurnEntry.COLUMN_TAG_ID_PRE;
                break;
            case TAG_TYPE_POSTTURN :
                column = TurnEntry.COLUMN_TAG_ID_POST;
                break;
            case TAG_TYPE_PERSISTENT_PRE :
                column = TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE;
                break;
            case TAG_TYPE_PERSISTENT_POST :
                column = TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST;
                break;
            default :
                throw new RuntimeException("Invalid tagType code");
        }

        String tagIdCsv = c.getString(c.getColumnIndex(column));

        c.close();

        return tagIdCsv.length() > 0;

    }

//    public static double turnGetDepth(SQLiteDatabase db, long turnId, int turnTime) {
//
//        if (turnTime != TURNTIME_PRE && turnTime != TURNTIME_POST) {
//            throw new IllegalArgumentException("Argument prePostCode must correspond to " +
//            "TURNTIME_PRE or TURNTIME_POST");
//        }
//
//        Cursor c = turnGetEntries(db, turnId);
//
//        long tetrodeId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY));
//
//        double angle;
//
//        if (turnTime == TURNTIME_PRE) {
//            angle = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_END_ANGLE));
//        }
//        else {
//            angle = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_START_ANGLE));
//        }
//        c.close();
//
//        Cursor cTetrode = tetrodeGetEntries(db, tetrodeId);
//        double angle0 = cTetrode.getDouble(c.getColumnIndex(TetrodeEntry.COLUMN_INITIAL_ANGLE));
//        long ratId = cTetrode.getLong(c.getColumnIndex(TetrodeEntry.COLUMN_RAT_KEY));
//        cTetrode.close();
//
//        Rat rat = new Rat(db, ratId);
//        int threadDir = rat.getScrewThreadDir();
//        double micronsPerTurn = rat.getTurnMicrometers();
//
////        Cursor cRat = ratGetEntries(db, ratId);
////        int threadDir = cRat.getInt(cRat.getColumnIndex(RatEntry.COLUMN_SCREW_THREAD_DIR));
////        double micronsPerTurn = cRat.getDouble(cRat.getColumnIndex(RatEntry.COLUMN_TURN_MICROMETERS));
////        cRat.close();
//
//        if (threadDir == THREAD_DIR_CCW) {
//            return (angle - angle0) / 360d * micronsPerTurn;
//        }
//        else {
//            return (angle0 - angle) / 360d * micronsPerTurn;
//        }
//
//    }

    public static String[] turnFieldNames() {
        return new String[] {
                TurnEntry._ID,
                TurnEntry.COLUMN_TETRODE_KEY,
                TurnEntry.COLUMN_SESSION_KEY,
                TurnEntry.COLUMN_START_ANGLE,
                TurnEntry.COLUMN_END_ANGLE,
                TurnEntry.COLUMN_TAG_ID_PRE,
                TurnEntry.COLUMN_TAG_ID_POST,
                TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE,
                TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST,
                TurnEntry.COLUMN_WAS_TURNED,
                TurnEntry.COLUMN_WAS_EDITED,
                TurnEntry.COLUMN_TIME,
                TurnEntry.COLUMN_COMMENT};
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                 TAG GROUP TABLE METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static long tagGroupAddEntry(SQLiteDatabase db, String name, int mandatory,
                                        String colour, Integer displayIndex, int inUse,
                                        int singleConstraint, String hintText) {
//        ADDS A SINGLE NEW RECORD TO THE TAG GROUP TABLE. ALL ARGS REQUIRED.

        ContentValues values = new ContentValues();

        if (hintText != null && hintText.length() > 60) {
            throw new RuntimeException("Hint text cannot be more than 60 chars long");
        }

        if (singleConstraint != SINGLE_CONSTRAINT_OFF && singleConstraint != SINGLE_CONSTRAINT_ON) {
            throw new RuntimeException("Invalid single constraint value");
        }

        // If no display index given, use the value 1 higher than the current maximum of the values
        // in the db (taking only extant records)
        boolean displayIndexNull = false;

        if (displayIndex == null) {

            displayIndexNull = true;

            Cursor c = tagGroupGetEntries(db, null, DELETED_NO);

            int numGroups = c.getCount();
            int maxDisplayIndex = 0;
            int currentDisplayIndex;

            for (int i = 0; i < numGroups; i++) {
                currentDisplayIndex = c.getInt(c.getColumnIndex(TagGroupEntry.COLUMN_DISPLAY_INDEX));
                if (currentDisplayIndex > maxDisplayIndex) {
                    maxDisplayIndex = currentDisplayIndex;
                }
                c.moveToNext();
            }

            c.close();

            displayIndex = maxDisplayIndex + 1;

        }

        // Add all mandatory fields
        values.put(TagGroupEntry.COLUMN_NAME, name);
        values.put(TagGroupEntry.COLUMN_MANDATORY, mandatory);
        values.put(TagGroupEntry.COLUMN_COLOUR, colour);
        values.put(TagGroupEntry.COLUMN_DISPLAY_INDEX, displayIndex);
        values.put(TagGroupEntry.COLUMN_IN_USE, inUse);
        values.put(TagGroupEntry.COLUMN_SINGLE_CONSTRAINT, singleConstraint);
        values.put(TagGroupEntry.COLUMN_HINT_TEXT, hintText);
        values.put(TagGroupEntry.COLUMN_DELETED, DELETED_NO);

        long rowId = db.insert(TagGroupEntry.TABLE_NAME, null, values);

        // If display index was not null, check that all display indices are consistent
        if (!displayIndexNull) {
            displayIndexEdit(db, TagGroupEntry.TABLE_NAME, rowId, null, null, displayIndex);
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return rowId;

    }

    public static void tagGroupDeleteEntry(SQLiteDatabase db, long tagGroupId) {
        // Delete a tag group and all of its child tags

        // First get all child tags of the group
        Cursor cursor = tagGetEntriesByTagGroupId(db, tagGroupId, null);
        cursor.moveToFirst();

        // Determine how many child tags must be deleted
        int nChildTags = cursor.getCount();
        int colIdxTagId = cursor.getColumnIndex(TagEntry._ID);

        // Cycle through the children and delete them
        for (int r=0; r<nChildTags; r++) {
            tagDeleteEntry(db, cursor.getLong(colIdxTagId), false);
            cursor.moveToNext();
        }

//        singleEntryDelete(db, TagGroupEntry.TABLE_NAME, tagGroupId);
        singleEntryUpdate(
                db,
                TagGroupEntry.TABLE_NAME,
                tagGroupId, TagGroupEntry.COLUMN_DELETED,
                DELETED_YES);

    }

    public static Cursor tagGroupGetEntriesInUse(SQLiteDatabase db, int turnTime) {
        // Get all extant tag groups with the in_use field set to 1. Results ranked by display index.

        assert (turnTime == TURNTIME_PRE || turnTime == TURNTIME_POST);

        String selection =
                TagGroupEntry.COLUMN_DELETED + " = ? AND "
                        + "( " + TagGroupEntry.COLUMN_IN_USE + " = ? OR "
                        + TagGroupEntry.COLUMN_IN_USE + " = ? )";

        String[] selectionArgs;

        if (turnTime == TurnDbUtils.TAG_TYPE_PRETURN) {
            selectionArgs = new String[]{
                    Integer.toString(DELETED_NO),
                    Integer.toString(SPECIFY_PRE),
                    Integer.toString(SPECIFY_BOTH)
            };
        }

        else {
            selectionArgs = new String[]{
                    Integer.toString(DELETED_NO),
                    Integer.toString(SPECIFY_POST),
                    Integer.toString(SPECIFY_BOTH)
            };

        }

        return db.query(
                TagGroupEntry.TABLE_NAME,
                tagGroupFieldNames(),
                selection,
                selectionArgs,
                null,
                null,
                TagGroupEntry.COLUMN_DISPLAY_INDEX);

    }

    public static Cursor tagGroupGetEntries(SQLiteDatabase db, Long tagGroupId, Integer deletedCode) {

        // Query by row id
        if (tagGroupId != null) {
            if (deletedCode == null) {
                return singleEntryQuery(
                        db,
                        TagGroupEntry.TABLE_NAME,
                        tagGroupFieldNames(),
                        tagGroupId,
                        TagGroupEntry.COLUMN_DISPLAY_INDEX);
            }
            else {
                throw new IllegalArgumentException("When argument 'tagGroupId' is not-null, argument " +
                        "'deletedCode' must be null");
            }
        }

        // Query by deletion state
        else {

            String selection, sortBy;

            if (deletedCode == null) {
                selection = null;
            }
            else if (deletedCode == DELETED_NO || deletedCode == DELETED_YES) {
                selection = TagGroupEntry.COLUMN_DELETED + " = " + Integer.toString(deletedCode);
            }
            else {
                throw new IllegalArgumentException("Argument 'deletedCode' must correspond to " +
                        "DELETED_YES or DELETED_NO");
            }

            Cursor c = db.query(
                    TagGroupEntry.TABLE_NAME,
                    tagGroupFieldNames(),
                    selection,
                    null,
                    null,
                    null,
                    TagGroupEntry.COLUMN_DISPLAY_INDEX);

            c.moveToFirst();
            return c;
        }
    }

    public static String[] tagGroupFieldNames() {
        return new String[] {
                TagGroupEntry._ID,
                TagGroupEntry.COLUMN_NAME,
                TagGroupEntry.COLUMN_MANDATORY,
                TagGroupEntry.COLUMN_COLOUR,
                TagGroupEntry.COLUMN_IN_USE,
                TagGroupEntry.COLUMN_SINGLE_CONSTRAINT,
                TagGroupEntry.COLUMN_DISPLAY_INDEX,
                TagGroupEntry.COLUMN_HINT_TEXT,
                TagGroupEntry.COLUMN_DELETED};
    }

    public static void tagSetDefaults(Context context) {
        // Clear existing tags and tag groups from the db and write the default set

        SQLiteDatabase db = TurnDbHelper.getInstance(context).getReadableDatabase();

        // First clear the tag and tag group tables

        clearTable(db, TagEntry.TABLE_NAME);
        clearTable(db, TagGroupEntry.TABLE_NAME);

        Resources res = context.getResources();

        // Load the xml tag names
        String[] tagGroupNames = res.getStringArray(R.array.tag_group_names);
        String[] tagGroupColours = res.getStringArray(R.array.tag_group_colours);
        String[] tagGroupHints = res.getStringArray(R.array.tag_group_hints);
        int[] tagGroupMandatory = res.getIntArray(R.array.tag_group_mandatory);
        int[] tagGroupInUse = res.getIntArray(R.array.tag_group_in_use);
        int[] tagGroupSingleConstraint = res.getIntArray(R.array.tag_group_single_constraint);

        int nGroups = tagGroupNames.length;

        long[] tagGroupRowIds = new long[nGroups];

        // Check all arrays are the same size
        assert(tagGroupNames.length == tagGroupColours.length
                && tagGroupColours.length == tagGroupMandatory.length
                && tagGroupColours.length == tagGroupSingleConstraint.length
                && tagGroupColours.length == tagGroupHints.length);

        Long tagRowId;

        // Go through the groups and add the records
        for (int n=0; n<nGroups; n++) {
            tagGroupRowIds[n] = tagGroupAddEntry(
                    db,
                    tagGroupNames[n],
                    tagGroupMandatory[n],
                    tagGroupColours[n],
                    n,
                    tagGroupInUse[n],
                    tagGroupSingleConstraint[n],
                    tagGroupHints[n]);

            Log.v(LOG_TAG, "Adding tag group '" + tagGroupNames[n] + "', rowId"
                    + Long.toString(tagGroupRowIds[n]));

        }

        // Now get the XML tag list. This is specified as a series of comma-separated strings,
        // in the format <groupNumber,name>, e.g. "2,amp100". These strings must be split into the
        // int and string components.

        String[] tags = context.getResources().getStringArray(R.array.tag_names);

        nGroups = tags.length;
        long currentTagGroupId;
        String currentTagName;

        for (int n=0; n<nGroups; n++) {

            String[] splitString = tags[n].split(",");

            currentTagGroupId = tagGroupRowIds[Integer.parseInt(splitString[0])-1];

            currentTagName = splitString[1];

            tagRowId = tagAddEntry(db, currentTagGroupId, currentTagName);

            Log.v(LOG_TAG, "Adding tag '" + currentTagName + "' to group "
                    + Long.toString(currentTagGroupId) + ", rowId "
                    + Long.toString(tagRowId));

        }


    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                 TAG TABLE METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static long tagAddEntry(SQLiteDatabase db, long tagGroupId, String name) {

        // Determine the maximum display index that currently exists within the current group
        Cursor cursor = db.query(
                TagEntry.TABLE_NAME,
                new String[]{TagEntry.COLUMN_DISPLAY_INDEX},
                TagEntry.COLUMN_TAG_GROUP_KEY + " = " + Long.toString(tagGroupId),
                null,
                null,
                null,
                TagEntry.COLUMN_DISPLAY_INDEX + " DESC",
                "1");

        int maxDisplayIndex = -1;

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            maxDisplayIndex = cursor.getInt(cursor.getColumnIndex(TagEntry.COLUMN_DISPLAY_INDEX));
        }

        cursor.close();

        ContentValues values = new ContentValues();

        values.put(TagEntry.COLUMN_TAG_GROUP_KEY, tagGroupId);
        values.put(TagEntry.COLUMN_NAME, name);
        values.put(TagEntry.COLUMN_IN_USE, IN_USE_YES);
        values.put(TagEntry.COLUMN_DISPLAY_INDEX, maxDisplayIndex + 1);
        values.put(TagEntry.COLUMN_DELETED, DELETED_NO);

        long rowId = db.insert(TagEntry.TABLE_NAME, null, values);

        checkRowId(rowId);

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return rowId;

    }

    public static void tagDeleteEntry(SQLiteDatabase db, long tagId, boolean reorderDisplayIndices) {

        // Get the tag's parent tagGroup id
        Cursor c = tagGetEntries(db, tagId, null);
        long tagGroupId = c.getLong(c.getColumnIndex(TagEntry.COLUMN_TAG_GROUP_KEY));
        c.close();

        // Set the deleted field to true
        singleEntryUpdate(db, TagEntry.TABLE_NAME, tagId, TagEntry.COLUMN_DELETED, DELETED_YES);
//        singleEntryDelete(db, TagEntry.TABLE_NAME, tagId);

        if (reorderDisplayIndices) {

            // Re-jig the display indeces within the tag group
            String selectionString = TagEntry.COLUMN_TAG_GROUP_KEY + " = " + Long.toString(tagGroupId);
            closeDisplayIndexGaps(db, TagEntry.TABLE_NAME, selectionString);

        }
    }

    public static Cursor tagGetEntries(SQLiteDatabase db, Long tagId, Integer deletedCode) {

        // If we want a single specified entry, use the single entry query method.
        // (deletion state must be null; it makes no sense to use this criterion to query
        // a single record by its id)
        if (tagId != null) {
            if (deletedCode == null) {
                return singleEntryQuery(
                        db,
                        TagEntry.TABLE_NAME,
                        tagFieldNames(),
                        tagId,
                        TagEntry.COLUMN_DISPLAY_INDEX);
            }
            else {
                throw new IllegalArgumentException("When argument 'tagId' is non-null, argument " +
                "'deletedCode' must be null");
            }
        }

        // If tagId is NOT defined, retrieve all records matching the deletion code
        else {

            String selection;

            if (deletedCode == null) {
                selection = null;
            }
            else if (deletedCode == DELETED_NO || deletedCode == DELETED_YES) {
                selection = TagEntry.COLUMN_DELETED + " = " + Integer.toString(deletedCode);
            }
            else {
                throw new IllegalArgumentException("Argument 'deletedCode' must correspond to " +
                        "DELETED_YES or DELETED_NO");
            }

            Cursor c = db.query(
                    TagEntry.TABLE_NAME,
                    tagFieldNames(),
                    selection,
                    null,
                    null,
                    null, null);

            c.moveToFirst();
            return c;
        }
    }

    public static Cursor tagGetEntriesInUse(SQLiteDatabase db, int tagType) {
        // Gets all extant tag entries marked as in use by the user

        String[] projection = tagFieldNames();

        String selection = TagEntry.COLUMN_IN_USE + " = " + Integer.toString(IN_USE_YES)
                + TagEntry.COLUMN_DELETED + " = " + Integer.toString(DELETED_NO);

        Cursor cursor = db.query(
                TagEntry.TABLE_NAME,
                projection,
                selection,
                null,
                null,
                null,
                null);

        // Check cursor; we expect any number of results
        checkCursor(cursor, QUERY_ROW_ANY);

        return cursor;

    }

    public static Cursor tagGetEntriesByTagGroupId(SQLiteDatabase db, long tagGroupId, Integer inUse) {
        // Query all tags within a specified tagGroup. No restrictions on the deletion state of the
        // tags returned

        String[] projection = tagFieldNames();

        String selection;
        String[] selectionArgs;

        // If inUse arg is null, get all results for the specified group key
        if (inUse == null) {
            selection = TagEntry.COLUMN_TAG_GROUP_KEY + " = ? ";
            selectionArgs = new String[]{Long.toString(tagGroupId)};
        }
        // Otherwise, add the inUse criterion to the query
        else {

            if (inUse != TurnDbUtils.IN_USE_YES && inUse != TurnDbUtils.IN_USE_NO) {
                throw new IllegalArgumentException("Invalid tag in-use code: must correspond to " +
                "IN_USE_YES or IN_USE_NO");
            }

            selection = TagEntry.COLUMN_TAG_GROUP_KEY + " = ? AND "
                    + TagEntry.COLUMN_IN_USE + " = ?";

            String inUseStr = Integer.toString(inUse);
            selectionArgs = new String[]{Long.toString(tagGroupId), inUseStr};

        }

        Cursor cursor = db.query(
                TagEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                TagEntry.COLUMN_DISPLAY_INDEX,
                null,
                null);

        // Check cursor; we expect any number of results
        checkCursor(cursor, QUERY_ROW_ANY);

        return cursor;

    }

    public static String[] tagFieldNames() {
        return new String[] {
                TagEntry._ID,
                TagEntry.COLUMN_TAG_GROUP_KEY,
                TagEntry.COLUMN_NAME,
                TagEntry.COLUMN_IN_USE,
                TagEntry.COLUMN_DISPLAY_INDEX,
                TagEntry.COLUMN_DELETED};
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                 DIALOG / ACTIVITY METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////\-

//    public static void ratOpenEditDialog(Fragment targetFragment, int recordMode, Long ratId, int requestCode) {
//        // Method to open a EditRatDialog instance from a fragment
//        FragmentManager manager = targetFragment.getActivity().getSupportFragmentManager();
//        EditRatDialog dialog = new EditRatDialog();
//        Bundle args = new Bundle();
//        args.putInt("recordMode", recordMode);
//        if (ratId != null) {
//            args.putLong("ratId", ratId);
//        }
//        dialog.setArguments(args);
//        dialog.setTargetFragment(targetFragment, requestCode);
//        dialog.show(manager, "editRatDialog");
//    }

    public static void ratOpenEditDialog2(Fragment targetFragment, int recordMode, Rat rat, int requestCode) {
        // Method to open a EditRatDialog instance from a fragment
        FragmentManager manager = targetFragment.getActivity().getSupportFragmentManager();
        EditRatDialog dialog = new EditRatDialog();
        Bundle args = new Bundle();
        args.putInt("recordMode", recordMode);
        args.putParcelable("rat", rat);
        dialog.setArguments(args);
        dialog.setTargetFragment(targetFragment, requestCode);
        dialog.show(manager, "editRatDialog");
    }

    public static void ratOpenDeleteDialog(final SQLiteDatabase db, final long ratId,
                                           final Fragment targetFragment, final int requestCode) {

        AlertDialog.Builder builder = new AlertDialog.Builder(targetFragment.getContext())
                .setMessage("Are you sure you want to delete the animal? This cannot be undone!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Rat(db, ratId).deleteFromDb(db);
//                        TurnDbUtils.ratDeleteEntry(db, ratId);
                        targetFragment.onActivityResult(requestCode, DIALOG_OK, null);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        targetFragment.onActivityResult(requestCode, DIALOG_CANCEL, null);
                    }
                });

        builder.create().show();
    }

    public static void ratOpenDeleteDialog2(final SQLiteDatabase db, final Rat rat,
                                           final Fragment targetFragment, final int requestCode) {

        AlertDialog.Builder builder = new AlertDialog.Builder(targetFragment.getContext())
                .setMessage("Are you sure you want to delete the animal? This cannot be undone!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        TurnDbUtils.ratDeleteEntry(db, ratId);
                        rat.deleteFromDb(db);
                        targetFragment.onActivityResult(requestCode, DIALOG_OK, null);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        targetFragment.onActivityResult(requestCode, DIALOG_CANCEL, null);
                    }
                });

        builder.create().show();
    }

    public static void sessionOpenDeleteAllDialog(final SQLiteDatabase db, final long ratId,
                                                  final Fragment targetFragment, final int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(targetFragment.getContext())
                .setMessage("Are you sure you want to delete all turning sessions? This cannot " +
                        "be undone!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Session[] allSessions = new Rat(db, ratId).findSessions(db, null);
                        for (Session session : allSessions) {
                            session.deleteFromDb(db);
                        }
//                        TurnDbUtils.sessionDeleteEntriesByRatId(db, ratId);
                        Toast.makeText(targetFragment.getContext(), "All sessions deleted", Toast.LENGTH_LONG).show();
                        targetFragment.onActivityResult(requestCode, DIALOG_OK, null);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        targetFragment.onActivityResult(requestCode, DIALOG_CANCEL, null);
                    }
                });
        builder.create().show();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    //                                 HELPER METHODS
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////\-


    public static void checkCursor(Cursor cursor, Integer expectedCode) {

        // Checks the contents of a cursor

        cursor.moveToFirst();
        int nRecords = cursor.getCount();
        int nColumns = cursor.getColumnCount();

        // Check the number of returned results against the expected number.
        // If expectedCode is null, checking is skipped.

        if (LOG_ERRORS && expectedCode!=null) {

            switch (expectedCode) {
                case QUERY_ROW_EMPTY:
                    if (nRecords > 0)
                        resultNumberError(nRecords, 0);
                    break;

                case QUERY_ROW_ONE:
                    if (nRecords != 1)
                        resultNumberError(nRecords, 1);
                    break;

                case QUERY_ROW_NONEMPTY:
                    if (nRecords == 0)
                        resultNumberError(nRecords, 0);
                    break;

                case QUERY_ROW_ONEPLUS:
                    if (nRecords == 0)
                        resultNumberError(nRecords, "1 or more ");
                    break;

                case QUERY_ROW_TWOPLUS:
                    if (nRecords < 2)
                        resultNumberError(nRecords,"2 or more");
                    break;

                case QUERY_ROW_ANY:
                    // Anything is allowed: no error
                    break;
            }

        }

        // Log the number of records
        if (LOG_CURSOR_COUNT)
            Log.v(LOG_TAG, "Cursor with " + Integer.toString(nRecords) + " rows returned");

        // Log the detailed content of the cursor
        if (LOG_CURSOR_CONTENTS) {

            for (int r=0; r<nRecords; r++) {

                String string = "Cursor contents: ";

                cursor.moveToPosition(r);

                for (int c = 0; c < nColumns; c++) {
                    string += cursor.getColumnName(c) + ":" + cursorValueToString(cursor, c) + ", ";
                }

                Log.v(LOG_TAG, string);

            }
        }

        // Return the cursor to the first position
        cursor.moveToFirst();

    }

    public static void checkRowId(long rowId) {
        // If record insertion failed, the returned rowId will be -1.
        // This method throws a runtime exception whenever this happens.
        if (rowId==-1) {
            throw new RuntimeException("Invalid rowID");
        }
    }

    public static Cursor singleEntryQuery(SQLiteDatabase db, String tableName, String[] fieldNames,
                                           Long id, String orderBy) {
        /*
        QUERIES A TABLE BY ITS _ID FIELD. IF _ID IS NULL, ALL RECORDS WILL BE RETRIEVED
         */

        String selection;
        String[] selectionArgs;
        int expectedCode;

        if (id == null) {
            // No rat id: just get all records in table
            selection = null;
            selectionArgs = null;
            expectedCode = QUERY_ROW_ANY; // Table can have any number of records
        }
        else {
            selection = BaseColumns._ID + " = ? ";
            selectionArgs = new String[] {Long.toString(id)};
            expectedCode = QUERY_ROW_ONE; // We expect one result
        }

        Cursor cursor = db.query(
                tableName,
                fieldNames,
                selection,
                selectionArgs,
                null,
                null,
                orderBy);

        checkCursor(cursor, expectedCode);

        return cursor;
    }

    public static void displayIndexEdit(SQLiteDatabase db, String tableName, Long rowId, String groupColumn, Long groupRowId, int newDisplayIndex) {
        // Change the display index if a tag group (and reposition all others to accommodate

        final String COLUMN_DISPLAY_INDEX = "display_index";

        if (newDisplayIndex < 0) {
            throw new RuntimeException("Display index must be a non-negative integer");
        }

        String[] projection = new String[] {BaseColumns._ID, COLUMN_DISPLAY_INDEX};

        // Get the current display index of the record to be shifted
        Cursor c = TurnDbUtils.singleEntryQuery(db, tableName, projection, rowId, null);
        int currentDisplayIndex = c.getInt(c.getColumnIndex(COLUMN_DISPLAY_INDEX));
        c.close();

        int adjustmentFactor;

        // Determine whether the new index is less than or greater than the first
        if (newDisplayIndex > currentDisplayIndex) {
            adjustmentFactor = 0;
        }
        else if (newDisplayIndex < currentDisplayIndex) {
            adjustmentFactor = 1;
        }
        else {
            // If the index hasn't change, don't bother doing anything
            return;
        }


        // Get all tag group records, APART FROM the one of interest

        String selection;
        String[] selectionArgs;

        // If no grouping key is given, get all records from the table
        if (groupRowId == null) {
            selection = BaseColumns._ID + " != ?";
            selectionArgs = new String[] {Long.toString(rowId)};
        }
        // Otherwise, get only records with the given grouping it
        else {
            selection = BaseColumns._ID + " != ? AND " + groupColumn + " = ?";
            selectionArgs = new String[] {Long.toString(rowId), Long.toString(groupRowId)};
        }

        c =  db.query(
                tableName,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_DISPLAY_INDEX);

        int numRecords = c.getCount();
        long[] rowIds = new long[numRecords];
        int[] displayIndices = new int[numRecords];
        int maxDisplayIndex = 0;
        int insertionIndex = 0;
        int extractionIndex = 0;

        boolean insertionPointFound = false;
        boolean extractionPointFound = false;

        c.moveToFirst();

        for (int g = 0; g < numRecords; g++) {

            displayIndices[g] = c.getInt(c.getColumnIndex(COLUMN_DISPLAY_INDEX));
            rowIds[g] = c.getLong(c.getColumnIndex(BaseColumns._ID));

            if (displayIndices[g] > maxDisplayIndex) {
                maxDisplayIndex = displayIndices[g];
            }

            // If the current record's display index >= the new index,
            // mark this record as the insertion point (i.e. the record being moved will be put BEFORE it)
            if (!insertionPointFound && displayIndices[g] + adjustmentFactor > newDisplayIndex) {
                insertionPointFound = true;
                insertionIndex = g;
            }

            // If the current record's display index >= the existing index,
            // mark this record as the extraction point (i.e. all records at/beyond this point
            // will have their display index shifted down by 1
            if (!extractionPointFound && displayIndices[g] + adjustmentFactor > currentDisplayIndex) {
                extractionPointFound = true;
                extractionIndex = g;
            }

            c.moveToNext();

        }


        // Check that the new display index is contiguous with the current display indices of the existing records
        if (newDisplayIndex > maxDisplayIndex + 1) {
            throw new RuntimeException("The new display index cannot exceed the number of existing records +1");
        }

        // For all records with DIs above the extraction point, shift their display indices DOWN by 1
        if (extractionPointFound) {

            c.moveToPosition(extractionIndex);

            for (int g = extractionIndex; g < numRecords; g++) {
                TurnDbUtils.singleEntryUpdate(
                        db,
                        tableName,
                        rowIds[g],
                        COLUMN_DISPLAY_INDEX,
                        --displayIndices[g]
                );

                Log.v(LOG_TAG, "Moving record ID " + Long.toString(rowIds[g]) +
                        " DOWN from index " + Integer.toString(displayIndices[g] + 1) + " to " +
                        Integer.toString(displayIndices[g]));

                c.moveToNext();

            }
        }

        // For all records with DIs above the insertion point, shift their display indices UP by 1
        if (insertionPointFound) {

            c.moveToPosition(insertionIndex);

            for (int g = insertionIndex; g < numRecords; g++) {
                TurnDbUtils.singleEntryUpdate(
                        db,
                        tableName,
                        rowIds[g],
                        COLUMN_DISPLAY_INDEX,
                        ++displayIndices[g]);

                Log.v(LOG_TAG, "Moving record ID " + Long.toString(rowIds[g]) +
                        " UP from index " + Integer.toString(displayIndices[g] - 1) + " to " +
                        Integer.toString(displayIndices[g]));

                c.moveToNext();

            }
        }

        c.close();


        // Finally, update the display index of the record of interest
        TurnDbUtils.singleEntryUpdate(
                db,
                tableName,
                rowId,
                COLUMN_DISPLAY_INDEX,
                newDisplayIndex);

        Log.v(LOG_TAG, tableName + " record id " + Long.toString(rowId) + " moved from DI " +
        Integer.toString(currentDisplayIndex) + " to " + Integer.toString(newDisplayIndex));
    }

    public static void closeDisplayIndexGaps(SQLiteDatabase db, String tableName, String selectionString) {

        final String displayIndexColumn = "display_index";

        // Get ALL records for the given table, by display index (ascending

        Cursor c = db.query(
                tableName,
                new String[] {displayIndexColumn, BaseColumns._ID},
                selectionString,
                null,
                null,
                null,
                displayIndexColumn
        );

        long numRecords = c.getCount();

        if (numRecords > 0) {

            long currentId;

            int colIdxId = c.getColumnIndex(BaseColumns._ID);

            // Scan through all the records. We expect to find display indices that equal the
            // records' positions in the cursor. Whenever a discrepancy is found, all following
            // values will be shifted down by 1.

            c.moveToFirst();

            // Loop through the records, giving them display index values equal to their order
            // in the query results (they're ranked by display index already, so all this will
            // do is to remove the gaps)
            for (long r = 0; r < numRecords; r++) {
                currentId = c.getLong(colIdxId);
                singleEntryUpdate(db, tableName, currentId, displayIndexColumn, r);
                c.moveToNext();
            }

        }

        c.close();

    }

    public static int singleEntryDelete(SQLiteDatabase db, String tableName, long id) {
        // Delete a single table entry by its id

        Log.v(LOG_TAG, "Deleting " + tableName + " entry ID " + Long.toString(id));

        int numDeleted = db.delete(
                tableName,
                BaseColumns._ID + " = ?",
                new String[]{Long.toString(id)});

        if (numDeleted != 1) {
            throw new RuntimeException("Error, record was not found");
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return numDeleted;

    }

    public static int singleEntryUpdate(SQLiteDatabase db, String tableName, long rowId,
                                         String column, String value) {

        checkRowId(rowId);

        ContentValues values = new ContentValues();
        values.put(column, value);

        int numRows = db.update(tableName, values, "_ID = " + Long.toString(rowId), null);

        if (numRows != 1) {
            throw new RuntimeException(
                    "Updating affected " + Integer.toString(numRows) + " records (1 expected)");
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return numRows;

    }

    public static int singleEntryUpdate(SQLiteDatabase db, String tableName, long rowId,
                                        String column, Integer value) {

        checkRowId(rowId);

        ContentValues values = new ContentValues();
        values.put(column, value);

        int numRows = db.update(tableName, values, "_ID = " + Long.toString(rowId), null);

        if (numRows != 1) {
            throw new RuntimeException(
                    "Updating affected " + Integer.toString(numRows) + " records (1 expected)");
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return numRows;

    }

    public static int singleEntryUpdate(SQLiteDatabase db, String tableName, long rowId,
                                        String column, Long value) {

        checkRowId(rowId);

        ContentValues values = new ContentValues();
        values.put(column, value);

        int numRows = db.update(tableName, values, "_ID = " + Long.toString(rowId), null);

        if (numRows != 1) {
            throw new RuntimeException(
                    "Updating affected " + Integer.toString(numRows) + " records (1 expected)");
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return numRows;

    }

    public static int singleEntryUpdate(SQLiteDatabase db, String tableName, long rowId,
                                        String column, Double value) {

        checkRowId(rowId);

        ContentValues values = new ContentValues();
        values.put(column, value);

        int numRows = db.update(tableName, values, "_ID = " + Long.toString(rowId), null);

        if (numRows != 1) {
            throw new RuntimeException(
                    "Updating affected " + Integer.toString(numRows) + " records (1 expected)");
        }

        AppInstance.getBackupHelper().notifyDatabaseChanged();

        return numRows;

    }

    private static void resultNumberError(int numResults, int numExpected) {

        Log.e(LOG_TAG, "Query error: " + Integer.toString(numExpected) + " results expected, " +
                Integer.toString(numResults) + " retrieved.");

    }

    private static void resultNumberError(int numResults, String expected) {

        Log.e(LOG_TAG, "Query error: " + expected + " results expected, " +
                Integer.toString(numResults) + " retrieved.");

    }

    public static String cursorValueToString(Cursor cursor, int colIdx) {

        int type = cursor.getType(colIdx);

        switch (type) {

            case Cursor.FIELD_TYPE_FLOAT:
                return Float.toString(cursor.getFloat(colIdx));

            case Cursor.FIELD_TYPE_INTEGER:
                return Integer.toString(cursor.getInt(colIdx));

            case Cursor.FIELD_TYPE_STRING:
                return cursor.getString(colIdx);

        }

        return null;

    }

    public static String makeCommaSepString(String[] stringArr) {

        StringBuilder result = new StringBuilder();

        int numStrings = stringArr.length;

        for (int n=0; n<numStrings; n++) {

            result.append(stringArr[n]);

            if (n < numStrings-1) {
                result.append(",");
            }

        }

        return result.toString();

    }

    public static String makeCommaSepString(Integer[] intArr) {
        // Make comma-sep string from int array. This method just
        // creates a string array from the integers, then calls the
        // String[] makeCommaSepString method.

        String[] stringArr = new String[intArr.length];

        for (int n=0; n<intArr.length; n++) {
            stringArr[n] = Integer.toString(intArr[n]);
        }
        return makeCommaSepString(stringArr);

    }

    public static String makeCommaSepString(Long[] longArr) {
        // Make comma-sep string from int array. This method just
        // creates a string array from the integers, then calls the
        // String[] makeCommaSepString method.

        String[] stringArr = new String[longArr.length];

        for (int n=0; n<longArr.length; n++) {
            stringArr[n] = Long.toString(longArr[n]);
        }
        return makeCommaSepString(stringArr);

    }

    public static String makeCommaSepString(ArrayList<Long> list) {
        return makeCommaSepString(list.toArray(new Long[list.size()]));
    }

    public static String mergeCommaSepStrings(String str1, String str2) {
        // Merges two comma-separated strings, into a single comma-separated
        // string without duplicate values

        HashSet<String> hSet = new HashSet<>();

        StringTokenizer st = new StringTokenizer(str1, ",");
        while (st.hasMoreTokens()) {
            hSet.add(st.nextToken());
        }

        st = new StringTokenizer(str2, ",");
        while (st.hasMoreTokens()) {
            hSet.add(st.nextToken());
        }

        // Convert the hash set to a string array
        String[] uniqueStrings = hSet.toArray(new String[hSet.size()]);

        // Now regenerate the comma-sep string
        return makeCommaSepString(uniqueStrings);

    }

    public static String[] commaSepStringToStrings(String commaSep) {
        // Convert a comma-separated string into a string array
        return commaSep.split("\\s*,\\s*");
    }

    public static int[] commaSepStringToInts(String commaSep) {
        // Convert a comma-separated string into an int array

        String[] strings = commaSep.split("\\s*,\\s*");
        int[] ints = new int[strings.length];

        for (int n=0; n<strings.length; n++) {
            ints[n] = Integer.parseInt(strings[n]);
        }

        return ints;
    }

    public static long[] commaSepStringToLongs(String commaSep) {
        // Convert a comma-separated string into an int array


        if (commaSep.length() > 0) {
            String[] strings = commaSep.split(",");
            long[] longs = new long[strings.length];

            for (int n = 0; n < strings.length; n++) {
                longs[n] = GeneralUtils.longValueOf(strings[n]);
            }

            return longs;
        }
        else {
            return new long[0];
        }
    }

    private static long getTime() {
        // Get the millisecond-precision time value
        return System.currentTimeMillis();
    }

    public static void clearTable(SQLiteDatabase db, String tableName) {
        db.execSQL("delete from " + tableName);
        AppInstance.getBackupHelper().notifyDatabaseChanged();
    }

    public static String colorToHexString(SQLiteDatabase db, int color) {
        return "#" + Integer.toHexString(color).substring(2);
    }

}
