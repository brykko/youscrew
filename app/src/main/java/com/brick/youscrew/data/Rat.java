package com.brick.youscrew.data;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.brick.youscrew.data.TurnContract.*;
import com.brick.youscrew.utils.AppInstance;

/**
 * Created by windows on 02.04.2016.
 */
public class Rat extends DbEntity2 {

    private static final String LOG_TAG = Rat.class.getSimpleName();

    public static final int THREAD_DIR_CW = 1;
    public static final int THREAD_DIR_CCW = 2;

    public static final int TETRODES_INDEPENDENT_YES = 1;
    public static final int TETRODES_INDEPENDENT_NO = 2;


    public Rat() {
        super();
    }

    /**
     * Construct from a db record
     *
     * @param db - Turn database
     * @param id - Row id of the record in the rat table from which the Rat object is to be created
     * @return   - Returns a rat object
     */

    public Rat(SQLiteDatabase db, long id) {
        this();
        mId = id;
        updateFromDb(db);
    }

    /**
     * Construct from bundle
     * @param bundle
     */

    public Rat(Bundle bundle) {
        super(bundle);
    }

    public String getTableName() {
        return RatEntry.TABLE_NAME;
    }

    protected void setKeys() {

        mKeys = new String[] {
                RatEntry.COLUMN_NAME,
                RatEntry.COLUMN_CODE,
                RatEntry.COLUMN_NUM_TETRODES,
                RatEntry.COLUMN_TETRODES_INDEPENDENT,
                RatEntry.COLUMN_TURN_MICROMETERS,
                RatEntry.COLUMN_DISPLAY_INDEX,
                RatEntry.COLUMN_SCREW_THREAD_DIR
        };

        mTypes = new int[] {
                TYPE_STRING,
                TYPE_STRING,
                TYPE_INT,
                TYPE_INT,
                TYPE_DOUBLE,
                TYPE_INT,
                TYPE_INT
        };
    }

    /** ------------------------------------------------
    PUBLIC SETTERS AND GETTERS FOR CLASS MEMBER FIELDS
    ------------------------------------------------ */

    public void setName(String name) {
        set(RatEntry.COLUMN_NAME, name);
    }

    public String getName() {
        return getString(RatEntry.COLUMN_NAME);
    }

    public void setCode(String code) {
        set(RatEntry.COLUMN_CODE, code);
    }

    public String getCode() {
        return getString(RatEntry.COLUMN_CODE);
    }

    public void setNumTetrodes(int numTetrodes) {
        set(RatEntry.COLUMN_NUM_TETRODES, numTetrodes);
    }

    public int getNumTetrodes() {
        return getInt(RatEntry.COLUMN_NUM_TETRODES);
    }

    public void setTurnMicrometers(double value) {
        set(RatEntry.COLUMN_TURN_MICROMETERS, value);
    }

    public double getTurnMicrometers() {
        return getDouble(RatEntry.COLUMN_TURN_MICROMETERS);
    }

    public void setDisplayIndex(int value) {
        set(RatEntry.COLUMN_DISPLAY_INDEX, value);
    }

    public int getDisplayIndex() {
        return getInt(RatEntry.COLUMN_DISPLAY_INDEX);
    }

    public void setScrewThreadDir(int value) {
        set(RatEntry.COLUMN_SCREW_THREAD_DIR, value);
    }

    public int getScrewThreadDir() {
        return getInt(RatEntry.COLUMN_SCREW_THREAD_DIR);
    }


    /** ------------------------------------------------
     *  DB METHODS
     ------------------------------------------------ */


    public synchronized void deleteFromDb(SQLiteDatabase db) {

        for (Tetrode tetrode : findTetrodes(db, null)) {
            tetrode.deleteFromDb(db);
        }

        for (Session session : findSessions(db, null)) {
            session.deleteFromDb(db);
        }

        super.deleteFromDb(db);

    }

    public synchronized void deleteAllSessionsFromDb(SQLiteDatabase db) {

    }


    public synchronized static Rat[] fromDbAllRats(SQLiteDatabase db) {

        Cursor c = db.query(
                RatEntry.TABLE_NAME,
                new String[]{RatEntry._ID},
                null,
                null,
                null,
                null,
                null);

        c.moveToFirst();

//        Cursor c = TurnDbUtils.ratGetEntries(db, null);

        int numRats = c.getCount();

        Rat[] rats = new Rat[numRats];

        for (int r = 0; r < numRats; r++) {
            rats[r] = new Rat(db, c.getLong(c.getColumnIndex(RatEntry._ID)));
            c.moveToNext();
        }

        c.close();

        return rats;

    }

    /**
     * Other methods
     */

    public Tetrode[] findTetrodes(SQLiteDatabase db, Integer ttIndex) {

//        Cursor c = TurnDbUtils.tetrodeGetEntriesByRatId(db, getId(), null);

        // Sort by ascending TT index
        String sortOrder = TetrodeEntry.COLUMN_INDEX + " ASC";

        // If no index given, get all TTs for the specified rat
        String selection;
        String[] selectionArgs;
        if (ttIndex == null) {
            selection = TetrodeEntry.COLUMN_RAT_KEY + " = ? ";
            selectionArgs = new String[]{Long.toString(getId())};
        }
        // Otherwise query using both the ratId AND the tetrode index
        else {
            selection = TetrodeEntry.COLUMN_RAT_KEY + " = ? AND " +
                    TetrodeEntry.COLUMN_INDEX + " = ? ";
            selectionArgs = new String[]{Long.toString(getId()), Integer.toString(ttIndex)};
        }

        Cursor c = db.query(
                TetrodeEntry.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        TurnDbUtils.checkCursor(c, TurnDbUtils.QUERY_ROW_NONEMPTY);

        int numTetrodes = c.getCount();

        Tetrode[] tetrodes = new Tetrode[numTetrodes];

        for (int t = 0; t < numTetrodes; t++) {

            long tetrodeId = c.getLong(c.getColumnIndex(TetrodeEntry._ID));

            tetrodes[t] = new Tetrode(db, this, tetrodeId);

//            Log.v(LOG_TAG, "Found tetrode " + tetrodes[t].getString(TetrodeEntry.COLUMN_NAME)
//                    + ", id " + Long.toString(tetrodes[t].getId()));

            c.moveToNext();

        }

        c.close();

        return tetrodes;

    }

    public Session[] findSessions(SQLiteDatabase db, Integer expectedCode) {

        // Sort temporally, most recent FIRST
        String sortOrder = SessionEntry.COLUMN_TIME_START + " DESC";

        // If no index given, get all TTs for the specified rat
        String selection;
        String[] selectionArgs;

        selection = SessionEntry.COLUMN_RAT_KEY + " = ? ";
        selectionArgs = new String[]{Long.toString(mId)};

        // Query the DB to get all rat records
        Cursor c = db.query(
                SessionEntry.TABLE_NAME,
                new String[] {SessionEntry._ID},
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        // Check the cursor
        TurnDbUtils.checkCursor(c, expectedCode);

        int numSessions = c.getCount();

        Session[] sessions = new Session[numSessions];

        for (int t = 0; t < numSessions; t++) {
            long sessionId = c.getLong(c.getColumnIndex(SessionEntry._ID));
            sessions[t] = new Session(this, db, sessionId);
            c.moveToNext();
        }

        c.close();

        return sessions;

    }

    public Tetrode createTetrode() {
        return new Tetrode(this);
    }


    /** ------------------------------------------------------------------------------------------
     *     PARCELABLE METHODS
     ------------------------------------------------------------------------------------------ */


    // this is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Rat> CREATOR = new Parcelable.Creator<Rat>() {

        public Rat createFromParcel(Parcel in) {
            return new Rat(in.readBundle(getClass().getClassLoader()));
        }

        public Rat[] newArray(int size) {
            return new Rat[size];
        }

    };


}
