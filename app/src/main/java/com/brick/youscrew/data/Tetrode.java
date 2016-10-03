package com.brick.youscrew.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import com.brick.youscrew.data.TurnContract.TetrodeEntry;
import com.brick.youscrew.utils.AppInstance;

/**
 * Created by Brick on 02/04/16.
 *
 * In YouScrew, tetrodes only exist as children of Rat objects, and are created when a new rat is
 * added to the database.
 *
 */
public class Tetrode extends DbEntity2 {

    private static final String LOG_TAG = Tetrode.class.getSimpleName();

    public static final int INITIAL_ANGLE_SET = 1;
    public static final int INITIAL_ANGLE_UNSET = 2;

    public static final int EVER_TURNED_YES = 1;
    public static final int EVER_TURNED_NO = 2;

    public static final int TAGGABLE_YES = 1;
    public static final int TAGGABLE_NO = 2;

    public static final int IN_USE_YES = 1;
    public static final int IN_USE_NO = 2;

    public static final int REFERENCE_YES = 1;
    public static final int REFERENCE_NO = 2;

    private long mRatId;
    private Rat mRat;

    public Tetrode() {
        super();
    }

    public Tetrode (Rat parent) {
        this();

        mRat = parent;
        set(TetrodeEntry.COLUMN_RAT_KEY, mRat.getId());

        set(TetrodeEntry.COLUMN_INITIAL_ANGLE, 0d);
        set(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET, INITIAL_ANGLE_UNSET);
        set(TetrodeEntry.COLUMN_EVER_TURNED, EVER_TURNED_NO);
        set(TetrodeEntry.COLUMN_TAGGABLE, TAGGABLE_YES);
        set(TetrodeEntry.COLUMN_IN_USE, IN_USE_YES);

        Log.v(LOG_TAG, "New tetrode created");

    }


    public Tetrode(SQLiteDatabase db, long id) {
        this();

        Cursor c = TurnDbUtils.singleEntryQuery(db, TetrodeEntry.TABLE_NAME, null, id, null);

        mRatId = c.getLong(c.getColumnIndex(TetrodeEntry.COLUMN_RAT_KEY));

        c.close();

        mId = id;
        updateFromDb(db);

    }

    /** Construct from database
     *
     * @param db - SQLite turn database
     * @param parentRat - Rat object that is to be the parent of the Tetrode being constructed
     * @param id        - rowId of the
     */

    public Tetrode (SQLiteDatabase db, Rat parentRat, long id) {
        this();

        mRat = parentRat;
        set(TetrodeEntry.COLUMN_RAT_KEY, mRat.getId());
        mId = id;
        updateFromDb(db);
    }

    public Tetrode (Bundle bundle) {
        super(bundle);
        mRat = bundle.getParcelable(TurnContract.RatEntry.TABLE_NAME);
        set(TetrodeEntry.COLUMN_RAT_KEY, getRat().getId());
    }

    public Bundle toBundle() {
        Bundle bundle = super.toBundle();
        bundle.putParcelable(TurnContract.RatEntry.TABLE_NAME, mRat);
        return bundle;
    }

    public String getTableName() {
        return TetrodeEntry.TABLE_NAME;
    }

    protected void setKeys() {

        mKeys = new String[] {
                TetrodeEntry.COLUMN_RAT_KEY,
                TetrodeEntry.COLUMN_INDEX,
                TetrodeEntry.COLUMN_NAME,
                TetrodeEntry.COLUMN_INITIAL_ANGLE,
                TetrodeEntry.COLUMN_INITIAL_ANGLE_SET,
                TetrodeEntry.COLUMN_IS_REF,
                TetrodeEntry.COLUMN_IN_USE,
                TetrodeEntry.COLUMN_COLOUR,
                TetrodeEntry.COLUMN_EVER_TURNED,
                TetrodeEntry.COLUMN_TAGGABLE,
                TetrodeEntry.COLUMN_COMMENT };

        mTypes = new int[] {
                TYPE_LONG,
                TYPE_INT,
                TYPE_STRING,
                TYPE_DOUBLE,
                TYPE_INT,
                TYPE_INT,
                TYPE_INT,
                TYPE_INT,
                TYPE_INT,
                TYPE_INT,
                TYPE_STRING };

    }

    /** ----------------------------------------------
     * PUBLIC SET/GET METHODS
     ---------------------------------------------- */

    public long getId() {
        return mId;
    }

    public Rat getRat() {
        if (mRat == null) {
            mRat = new Rat(mDb, mRatId);
        }
        return mRat;
    }

    @Override
    public synchronized void updateFromDb(SQLiteDatabase db) {
        super.updateFromDb(db);
        if (mRat != null) {
            mRat.updateFromDb(db);
        }
    }

    public void setInitialAngle (Double initialAngle) {
        // Set a tetrode's initial angle. If the argument initialAngle is null, the database angle
        // will be 'unset' (initial_angle is set to 0 deg, and the initial_angle_set field is set to 'unset')

        int angleSetCode;

        //
        if (initialAngle == null) {
            initialAngle = 0.0;
            angleSetCode = INITIAL_ANGLE_UNSET;
        }
        else {
            angleSetCode = INITIAL_ANGLE_SET;
        }

        set(TetrodeEntry.COLUMN_INITIAL_ANGLE, initialAngle);
        set(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET, angleSetCode);

    }

    /**
     * DATABASE METHODS
     */

    public synchronized void deleteFromDb(SQLiteDatabase db) {

            for (Turn turn : findTurns(db)) {
                turn.deleteFromDb(db);
            }

        super.deleteFromDb(db);

    }


    public Turn[] findTurns(SQLiteDatabase db) {

        String selection = TurnContract.TurnEntry.COLUMN_TETRODE_KEY + " = ? ";
        String[] selectionArgs = new String[]{Long.toString(mId)};

        // Order results by descending _id (most recent first).
        // The "time" column can't be used, since turn records where no turning
        // took place will have zero as their time value.
        String sortOrder = TurnContract.TurnEntry._ID + " DESC ";

        Cursor c = db.query(
                TurnContract.TurnEntry.TABLE_NAME,
                null,


                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        // Check cursor; any number of results expected
        TurnDbUtils.checkCursor(c, TurnDbUtils.QUERY_ROW_ANY);

        int numTurns = c.getCount();

        Turn[] turns = new Turn[numTurns];

        for (int t = 0; t < numTurns; t++) {

            long turnId = c.getLong(c.getColumnIndex(TurnContract.TurnEntry._ID));

            // Create parent session object
            long sessionId = c.getLong(c.getColumnIndex(TurnContract.TurnEntry.COLUMN_SESSION_KEY));
            Session session = new Session(mRat, db, sessionId);

            turns[t] = new Turn(session, this, db, turnId);

            Log.v(LOG_TAG, "Found turn id" + Long.toString(turns[t].getId()));

            c.moveToNext();

        }

        c.close();

        return turns;

    }

    public int wasEverTurned(SQLiteDatabase db) {

        Turn[] allTurns = findTurns(db);

        int wasEverTurned = EVER_TURNED_NO;

        for (Turn turn : allTurns) {
            if (turn.getInt(TurnContract.TurnEntry.COLUMN_WAS_TURNED) == Turn.TURNED_YES) {
                wasEverTurned = EVER_TURNED_YES;
            }
        }

        return wasEverTurned;

    }

    public void markAsUnturned() {
        set(TetrodeEntry.COLUMN_EVER_TURNED, EVER_TURNED_NO);
        set(TetrodeEntry.COLUMN_INITIAL_ANGLE, 0d);
        set(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET, INITIAL_ANGLE_UNSET);
    }

    /**
     * PARCELABLE METHODS
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBundle(toBundle());
    }

    // this is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Tetrode> CREATOR = new Parcelable.Creator<Tetrode>() {

        public Tetrode createFromParcel(Parcel in) {
            return new Tetrode(in.readBundle(getClass().getClassLoader()));
        }

        public Tetrode[] newArray(int size) {
            return new Tetrode[size];
        }

    };


}
