package com.brick.youscrew.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.brick.youscrew.data.TurnContract.TurnEntry;
import com.brick.youscrew.utils.AppInstance;

/**
 * Created by windows on 03.04.2016.
 */
public class Turn extends DbEntity2 {

    public static final int TURNED_NO = 1;
    public static final int TURNED_YES = 2;
    public static final int TURNED_BACKWARDS = 3;

    public static final int EDITED_NO = 1;
    public static final int EDITED_YES = 2;

    private Session mSession;
    private Tetrode mTetrode;


    public Turn() {
        super();
    }

    public Turn(Session parentSession, Tetrode parentTetrode) {
        this();
        mSession = parentSession;
        mTetrode = parentTetrode;
    }

    public Turn(SQLiteDatabase db, long id) {
        this();
        Cursor c = TurnDbUtils.singleEntryQuery(
                db,
                TurnEntry.TABLE_NAME,
                null,
                id,
                null);

        long sessionId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_SESSION_KEY));
        long tetrodeId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY));

        c.close();

        mSession = new Session(db, sessionId);
        mTetrode = new Tetrode(db, tetrodeId);
        mId = id;

        updateFromDb(db);

    }

    public Turn(Session parentSession, Tetrode parentTetrode, SQLiteDatabase db, long id) {
        this(parentSession, parentTetrode);
        mId = id;
        updateFromDb(db);
    }

    public Turn(Bundle bundle) {
        super(bundle);
        mSession = bundle.getParcelable(TurnContract.SessionEntry.TABLE_NAME);
        mTetrode = bundle.getParcelable(TurnContract.TetrodeEntry.TABLE_NAME);

    }

    protected void setKeys() {

        mKeys = new String[]{
                TurnEntry.COLUMN_SESSION_KEY,
                TurnEntry.COLUMN_TETRODE_KEY,
                TurnEntry.COLUMN_TIME,
                TurnEntry.COLUMN_START_ANGLE,
                TurnEntry.COLUMN_END_ANGLE,
                TurnEntry.COLUMN_TAG_ID_PRE,
                TurnEntry.COLUMN_TAG_ID_POST,
                TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE,
                TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST,
                TurnEntry.COLUMN_WAS_TURNED,
                TurnEntry.COLUMN_WAS_EDITED,
                TurnEntry.COLUMN_COMMENT
        };

        mTypes = new int[]{
                TYPE_LONG,
                TYPE_LONG,
                TYPE_LONG,
                TYPE_DOUBLE,
                TYPE_DOUBLE,
                TYPE_STRING,
                TYPE_STRING,
                TYPE_STRING,
                TYPE_STRING,
                TYPE_INT,
                TYPE_INT,
                TYPE_STRING};

    }

    public String getTableName() {
        return TurnEntry.TABLE_NAME;
    }

    @Override
    protected Bundle toBundle() {
        Bundle bundle = super.toBundle();
        bundle.putParcelable(mSession.getTableName(), mSession);
        bundle.putParcelable(mTetrode.getTableName(), mTetrode);
        return bundle;
    }

    /**
     * OTHER MISC
     */

    public Rat getRat() {
        return mSession.getRat();
    }

    public Session getSession() {
        return mSession;
    }

    public Tetrode getTetrode() {
        return mTetrode;
    }

    @Override
    public synchronized void updateFromDb(SQLiteDatabase db) {
        super.updateFromDb(db);
        mSession.updateFromDb(db);
        mTetrode.updateFromDb(db);
    }

    public boolean hasTagsPre() {
        return getString(TurnEntry.COLUMN_TAG_ID_PRE) != null;
    }

    public boolean hasTagsPost() {
        return getString(TurnEntry.COLUMN_TAG_ID_POST) != null;
    }


    public double getTurnsTotal() {

        int threadDir = getRat().getScrewThreadDir();

        double initialAngle = getTetrode().getDouble(TurnContract.TetrodeEntry.COLUMN_INITIAL_ANGLE);

        if (threadDir == Rat.THREAD_DIR_CCW) {
            return (getDouble(TurnEntry.COLUMN_END_ANGLE) - initialAngle) / 360d;
        } else {
            return (initialAngle - getDouble(TurnEntry.COLUMN_END_ANGLE)) / 360d;
        }

    }

    public double getTurnsThisSession() {

        double startAng = getDouble(TurnEntry.COLUMN_START_ANGLE);
        double endAng = getDouble(TurnEntry.COLUMN_END_ANGLE);

        int threadDir = getRat().getScrewThreadDir();

        if (threadDir == Rat.THREAD_DIR_CCW) {
            return (endAng - startAng) / 360d;
        } else {
            return (startAng - endAng) / 360d;
        }

    }

    public double getDepth() {
        return getTurnsTotal() * getRat().getTurnMicrometers();
    }

    public double getDepthAvancement() {
        return getTurnsThisSession() * getRat().getTurnMicrometers();
    }

    public void setAngles(double startAngle, double endAngle) {

        Long time;

        int sessionTimeMode = mSession.getInt(TurnContract.SessionEntry.COLUMN_TIME_MODE);

        if (sessionTimeMode == TurnDbUtils.TIME_PICKED) {
            time = mSession.getLong(TurnContract.SessionEntry.COLUMN_TIME_START);
        }
        else if (sessionTimeMode == TurnDbUtils.TIME_REAL) {
            time = System.currentTimeMillis();
        }
        else {
            Log.e(LOG_TAG, "Invalid time mode");
            time = null;
        }

        // Compare start and end angle to determine if TT has been turned
        int wasTurned = startAngle != endAngle ? Turn.TURNED_YES : Turn.TURNED_NO;

        set(TurnEntry.COLUMN_START_ANGLE, startAngle);
        set(TurnEntry.COLUMN_END_ANGLE, endAngle);
        set(TurnEntry.COLUMN_WAS_TURNED, wasTurned);
        set(TurnEntry.COLUMN_TIME, time);

        // If turning has occurred, also mark as edited
        if (wasTurned == Turn.TURNED_YES) {
            set(TurnEntry.COLUMN_WAS_EDITED, Turn.EDITED_YES);
        }

    }

    public Turn findLastTurn(SQLiteDatabase db) {

        // Returns the _id of the last registered turn record before the
        // specified record

        mSession.printContents();
        if (mSession.getInt(TurnContract.SessionEntry.COLUMN_IS_FIRST_SESSION) == Session.FIRST_YES) {
            return null;
        }
        else {
            long lastSessionId = mSession.getLong(TurnContract.SessionEntry.COLUMN_SESSION_KEY_LAST);
            return new Session(db, lastSessionId).findTurnByTetrode(db, mTetrode);
        }

    }

    public boolean wasTurnedThisSession() {
        return getTurnsThisSession() != 0;
    }

    public void undoTurning(SQLiteDatabase db) {

        set(TurnEntry.COLUMN_END_ANGLE, getDouble(TurnEntry.COLUMN_START_ANGLE));
        set(TurnEntry.COLUMN_WAS_TURNED, TURNED_NO);
        set(TurnEntry.COLUMN_TAG_ID_POST, new String());

        updateFromDb(db);

        Tetrode tetrode = getTetrode();

        if (tetrode.wasEverTurned(db) == Tetrode.EVER_TURNED_NO) {
            tetrode.set(TurnContract.TetrodeEntry.COLUMN_EVER_TURNED, Tetrode.EVER_TURNED_NO);
            tetrode.writeToDb(db);
        }

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

        // After undoing the turn for the specified record, check if the tetrode was turned before.
        // If so, mark the tetrode record as never turned
//        if (tetrodeWasEverTurned(db, tetrodeId) == TETRODE_NOT_TURNED) {
//            singleEntryUpdate(db, TurnContract.TetrodeEntry.TABLE_NAME, tetrodeId, TurnContract.TetrodeEntry.COLUMN_EVER_TURNED, TETRODE_NOT_TURNED);
//        }
//
//        AppInstance.getBackupHelper().notifyDatabaseChanged();

    }

    public Tag[] findTags(SQLiteDatabase db, int tagType) {

        String tagIdColumn = Tag.getTagListColumn(tagType);

        String tagIdString = getString(tagIdColumn);

        long[] tagIds = Tag.parseIdsFromString(tagIdString);

        Tag[] tags = null;

        if (tagIds != null) {

            int numTags = tagIds.length;
            tags = new Tag[numTags];

            for (int t = 0; t < numTags; t++) {
                tags[t] = new Tag(db, tagIds[t]);
            }

        }

        return tags;

    }

    /**
     * PARCELABLE METHODS
     */

// this is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Turn> CREATOR = new Parcelable.Creator<Turn>() {

        public Turn createFromParcel(Parcel in) {
            return new Turn(in.readBundle(getClass().getClassLoader()));
        }

        public Turn[] newArray(int size) {
            return new Turn[size];
        }

    };

}