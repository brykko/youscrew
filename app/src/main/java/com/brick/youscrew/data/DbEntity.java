package com.brick.youscrew.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

/**
 * Created by windows on 03.04.2016.
 */
public abstract class DbEntity implements Parcelable {

    protected String mTableName;

    /**
     * All DbEntity subclasses have a rowId for their designated table in the SQLite database.
     * This is initialized here at -1 such that any instance can determine whether it has been set
     * or not.
     */
    protected long mId = - 1;

    public abstract Bundle toBundle();

    protected abstract ContentValues createContentValues();

    protected abstract void fillFromCursor(Cursor cursor);

    public void updateFromDb(SQLiteDatabase db) {

        Cursor c = TurnDbUtils.singleEntryQuery(db, mTableName, null, mId, null);

        if (c.getCount() == 0) {
            throw new RuntimeException(
                    "Record id " + mId + " does not exist in database table " + mTableName);
        }

        fillFromCursor(c);

    }

    public synchronized void writeToDb(SQLiteDatabase db) {

        ContentValues values = createContentValues();

        if (existsInDb(db)) {

            db.update(
                    mTableName,
                    values,
                    BaseColumns._ID + " = " + Long.toString(mId),
                    null);
        }
        else {
            mId = db.insert(mTableName, null, values);
        }

    }

    public synchronized void deleteFromDb(SQLiteDatabase db) {
        if (existsInDb(db)) {
            TurnDbUtils.singleEntryDelete(db, mTableName, mId);
        }
        else {
            throw new RuntimeException("row id " + Long.toString(mId) + " not found in table " + mTableName);
        }
    }

    public boolean existsInDb(SQLiteDatabase db) {

        // If id is not yet set, record cannot exist in db
        if (mId == -1) {
            return false;
        }

        // If the session has an id, check that it exists in the database
        else {
            Cursor c = db.query(
                    mTableName,
                    new String[]{},
                    BaseColumns._ID + " = " + Long.toString(mId),
                    null,
                    null,
                    null,
                    null);

            int numRecords = c.getCount();

            if (numRecords == 1) {
                return true;
            } else {
                return false;
            }
        }
    }

    public long getId() {
        return mId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // write the object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBundle(toBundle());
    }

}
