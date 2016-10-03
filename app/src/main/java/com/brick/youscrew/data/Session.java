package com.brick.youscrew.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.brick.youscrew.data.TurnContract.SessionEntry;

/**
 * Created by Brick on 02/04/16.
 */
public class Session extends DbEntity2 {

    public static final int FIRST_YES = 1;
    public static final int FIRST_NO = 2;

    public static final int TIME_START = 1;
    public static final int TIME_END = 2;

    public static final int OPEN_YES = 1;
    public static final int OPEN_NO = 2;

    public static final int TIME_MODE_REAL = 1;
    public static final int TIME_MODE_PICKED = 2;

    private long mRatId;

    private Rat mRat;

    public Session() {
        super();
    }

    public Session(Rat parentRat) {
        this();
        mRat = parentRat;
    }

    public Session(SQLiteDatabase db, long id) {
        this();

        mId = id;

        // Get parent rat
        Cursor c = TurnDbUtils.singleEntryQuery(db, getTableName(), null, id, null);
        mRatId = c.getLong(c.getColumnIndex(SessionEntry.COLUMN_RAT_KEY));
        c.close();

//        mRat = new Rat(db, ratId);

        updateFromDb(db);

    }

    public Session(Rat parentRat, SQLiteDatabase db, long id) {
        this(parentRat);

        mId = id;

        updateFromDb(db);

    }

    public Session(Bundle bundle) {
        super(bundle);
        mRat = bundle.getParcelable(TurnContract.RatEntry.TABLE_NAME);
    }

    /** ------------------------------------------------------------------------------------------
     *     OTHER MISC METHODS
     ------------------------------------------------------------------------------------------ */

    public String getTableName() {
        return SessionEntry.TABLE_NAME;
    }

    protected void setKeys() {

        mKeys = new String[] {
                SessionEntry.COLUMN_RAT_KEY,
                SessionEntry.COLUMN_TIME_START,
                SessionEntry.COLUMN_TIME_END,
                SessionEntry.COLUMN_TAGS,
                SessionEntry.COLUMN_NUM_TTS_TURNED,
                SessionEntry.COLUMN_IS_OPEN,
                SessionEntry.COLUMN_TIME_MODE,
                SessionEntry.COLUMN_SESSION_KEY_LAST,
                SessionEntry.COLUMN_IS_FIRST_SESSION,
                SessionEntry.COLUMN_COMMENT };

        mTypes = new int[] {
                TYPE_LONG,
                TYPE_LONG,
                TYPE_LONG,
                TYPE_STRING,
                TYPE_INT,
                TYPE_INT,
                TYPE_INT,
                TYPE_LONG,
                TYPE_INT,
                TYPE_STRING };

    }

    @Override
    protected Bundle toBundle() {
        Bundle bundle = super.toBundle();
        bundle.putParcelable(TurnContract.RatEntry.TABLE_NAME, mRat);
        return bundle;
    }

    public synchronized void deleteFromDb(SQLiteDatabase db) {
        // Delete all turn records for the session
        for (Turn turn : findTurns(db)) {
            turn.deleteFromDb(db);
        }

        // Delete the session itself
        super.deleteFromDb(db);

        // Scan through all tetrodes checking if any other turn records exist.
        // If not, mark the tetrode as unturned.
        for (Tetrode tetrode : getRat().findTetrodes(db, null)) {
            if (tetrode.wasEverTurned(db) == Tetrode.EVER_TURNED_NO) {
                tetrode.markAsUnturned();
                tetrode.writeToDb(db);
            }
        }

    }

    @Override
    public synchronized void updateFromDb(SQLiteDatabase db) {
        super.updateFromDb(db);
//        mRat.updateFromDb(db);
    }

    public Turn[] findTurns(SQLiteDatabase db) {

        Log.v(LOG_TAG, "TABLE NAME = " + getTableName());
        Log.v(LOG_TAG, "getTableName() = " + getTableName());

        if (!existsInDb(db)) {
            throw new RuntimeException("Session id " + getId() + " not found in database, cannot query for Turn records");
        }

        String selection = TurnContract.TurnEntry.COLUMN_SESSION_KEY + " = ? ";
        String[] selectionArgs = new String[]{Long.toString(getId())};

        String sortOrder = TurnContract.TurnEntry.COLUMN_SESSION_KEY + " ASC ";

        Cursor cTurn = db.query(
                TurnContract.TurnEntry.TABLE_NAME,
                new String[] {TurnContract.TurnEntry._ID},
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        // Check cursor; any number of results expected
        TurnDbUtils.checkCursor(cTurn, TurnDbUtils.QUERY_ROW_ANY);

        int numTurns = cTurn.getCount();

        Turn[] turns = new Turn[numTurns];

        for (int t = 0; t < numTurns; t++) {
            long turnId = cTurn.getLong(cTurn.getColumnIndex(TurnContract.TurnEntry._ID));
            turns[t] = new Turn(db, turnId);
            cTurn.moveToNext();
        }

        cTurn.close();

        return turns;
    }

    public Session findLast(SQLiteDatabase db) {
        Long lastSessionId = getLong(SessionEntry.COLUMN_SESSION_KEY_LAST);
        if (lastSessionId != null) {
            return new Session(db, lastSessionId);
        }
        else {
            return null;
        }
    }

    public Session findNext(SQLiteDatabase db) {
        Cursor c = db.query(
                SessionEntry.TABLE_NAME,
                new String[] {SessionEntry._ID},
                SessionEntry.COLUMN_SESSION_KEY_LAST + " = " + Long.toString(mId),
                null,
                null,
                null,
                null);

        if (c.getCount() == 1) {
            Session nextSession = new Session(db, c.getLong(c.getColumnIndex(SessionEntry._ID)));
            c.close();
            return nextSession;
        }
        else {
            c.close();
            return null;
        }

    }

    public Turn findTurnByTetrode(SQLiteDatabase db, Tetrode tetrode) {

        if (!existsInDb(db)) {
            throw new RuntimeException("Session not found in database, cannot query for Turn records");
        }
        if (!tetrode.existsInDb(db)) {
            throw new RuntimeException("Tetrode not found in database, cannot query for Turn records");
        }

        String selection =
                TurnContract.TurnEntry.COLUMN_SESSION_KEY + " = " + Long.toString(getId()) +
                        " AND " + TurnContract.TurnEntry.COLUMN_TETRODE_KEY + " = " + Long.toString(tetrode.getId());

        Cursor c = db.query(
                TurnContract.TurnEntry.TABLE_NAME,
                new String[] {TurnContract.TurnEntry._ID},
                selection,
                null,
                null,
                null,
                null);

        if (c.getCount() == 0) {
            throw new RuntimeException( "Turn query returned no results for tetrode "
                    + tetrode.getString(TurnContract.TetrodeEntry.COLUMN_NAME)
            + " id " + Long.toString(tetrode.getId()) + ", session id " + Long.toString(getId()) );
        }

        c.moveToFirst();

        long turnId = c.getLong(c.getColumnIndex(TurnContract.TurnEntry._ID));

        c.close();

        return new Turn(this, tetrode, db, turnId);

    }

    /** ------------------------------------------------------------------------------------------
     *     GET / SET METHODS
     ------------------------------------------------------------------------------------------ */

    public Rat getRat() {
        if (mRat == null) {
            mRat = new Rat(mDb, mRatId);
        }
        return mRat;
    }


    /** ------------------------------------------------------------------------------------------
     *     PARCELABLE METHODS
     ------------------------------------------------------------------------------------------ */

    // this is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Session> CREATOR = new Parcelable.Creator<Session>() {

        public Session createFromParcel(Parcel in) {
            return new Session(in.readBundle(getClass().getClassLoader()));
        }

        public Session[] newArray(int size) {
            return new Session[size];
        }

    };


}
