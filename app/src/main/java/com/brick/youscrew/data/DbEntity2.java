package com.brick.youscrew.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import com.brick.youscrew.utils.AppInstance;

import java.lang.reflect.Constructor;


/**
 * Created by windows on 03.04.2016.
 */
public abstract class DbEntity2 implements Parcelable {

    public static final String LOG_TAG = DbEntity2.class.getSimpleName();

    protected static final String KEY_ID = BaseColumns._ID;

    // Keep a reference to the database such that it can be used in future to query for relatives
    protected SQLiteDatabase mDb;

    protected long mId;

    protected Bundle mData = new Bundle();

    protected String[] mKeys;
    protected int[] mTypes;     // Type int codes

    /**
     * All DbEntity subclasses have a rowId for their designated table in the SQLite database.
     * This is initialized here at -1 such that any instance can determine whether it has been set
     * or not.
     */

    public static final int TYPE_STRING = 1;
    public static final int TYPE_INT = 2;
    public static final int TYPE_LONG = 3;
    public static final int TYPE_DOUBLE = 4;
    public static final int TYPE_FLOAT = 5;

    public DbEntity2() {
        setKeys();
    }

    public DbEntity2 (Bundle bundle) {

        setKeys();

        mDb = TurnDbHelper.getInstance(null).getWritableDatabase();

        mId = bundle.getLong(KEY_ID);

        for (int k = 0; k < mKeys.length; k++) {

            String key = mKeys[k];

            switch (mTypes[k]) {

                case TYPE_STRING:
//                    Log.v(LOG_TAG, "Type = STRING");
                    mData.putString(key, bundle.getString(key));
                    break;

                case TYPE_INT:
//                    Log.v(LOG_TAG, "Type = INTEGER");
                    mData.putInt(key, bundle.getInt(key));
                    break;

                case TYPE_LONG:
                    mData.putLong(key, bundle.getLong(key));
                    break;

                case TYPE_DOUBLE:
//                    Log.v(LOG_TAG, "Type = DOUBLE");
                    mData.putDouble(key, bundle.getDouble(key));
                    break;

                case TYPE_FLOAT:
//                    Log.v(LOG_TAG, "Type = FLOAT");
                    mData.putFloat(key, bundle.getFloat(key));
                    break;

            }
        }

    }

    protected abstract void setKeys();

    /**
     * The toBundle method is used to package the object into a single bundle, for use by the
     * Parcelable implementation.  This method packages all database fields and the record rowId.
     * Subclasses can override this method to package additional items into the bundle.
     * @return
     */

    protected Bundle toBundle() {
        Bundle bundle = mData;
        bundle.putLong(KEY_ID, mId);
        return bundle;
    };

    public abstract String getTableName();

    public synchronized void updateFromDb(SQLiteDatabase db) {

        // Collect reference to the database and store it
        mDb = db;

        Cursor c = db.query(
                getTableName(),
                mKeys,
                KEY_ID + " = " + Long.toString(getId()),
                null, null, null, null );

        if (c.getCount() == 0) {
            throw new RuntimeException(
                    "Record id " + getId() + " does not exist in database table " + getTableName());
        }

        c.moveToFirst();

        for (int k = 0; k < mKeys.length; k++) {

            String key = mKeys[k];
            int columnIndex = c.getColumnIndex(key);

//            Log.v(LOG_TAG, "Retrieving key " + key + " from col idx " + Integer.toString(columnIndex));

            switch (mTypes[k]) {

                case TYPE_STRING :
//                    Log.v(LOG_TAG, "Type = STRING");
                    mData.putString(key, c.getString(columnIndex));
                    break;

                case TYPE_INT :
//                    Log.v(LOG_TAG, "Type = INTEGER");
                        mData.putInt(key, c.getInt(columnIndex));
                    break;

                case TYPE_LONG :
                    mData.putLong(key, c.getLong(columnIndex));
                    break;

                case TYPE_DOUBLE :
//                    Log.v(LOG_TAG, "Type = DOUBLE");
                    mData.putDouble(key, c.getDouble(columnIndex));
                    break;

                case TYPE_FLOAT :
//                    Log.v(LOG_TAG, "Type = FLOAT");
                    mData.putFloat(key, c.getFloat(columnIndex));
                    break;
            }
        }

        c.close();

    }

    public synchronized void writeToDb(SQLiteDatabase db) {

        ContentValues values = new ContentValues();

        for (int k = 0; k < mKeys.length; k++) {

            String key = mKeys[k];

            switch (mTypes[k]) {

                case TYPE_STRING :
                    values.put(key, mData.getString(key));
                    break;

                case TYPE_INT :
                    values.put(key, mData.getInt(key));
                    break;

                case TYPE_LONG :
                    values.put(key, mData.getLong(key));
                    break;

                case TYPE_DOUBLE :
                    values.put(key, mData.getDouble(key));
                    break;

                case TYPE_FLOAT :
                    values.put(key, mData.getFloat(key));
                    break;

            }

        }

        if (existsInDb(db)) {

            db.update(
                    getTableName(),
                    values,
                    KEY_ID + " = " + Long.toString(getId()),
                    null);
        }
        else {
            long rowId = db.insert(getTableName(), null, values);
            if (rowId == -1) {
                throw new RuntimeException("Failed to insert new db record");
            }
            mId = rowId;
        }

        AppInstance.getInstance(null).getBackupHelper().notifyDatabaseChanged();

    }

    public synchronized void deleteFromDb(SQLiteDatabase db) {
        if (existsInDb(db)) {
            TurnDbUtils.singleEntryDelete(db, getTableName(), getId());
        }
        else {
            throw new RuntimeException("row id " + Long.toString(getId()) + " not found in table " + getTableName());
        }
    }

    public boolean existsInDb(SQLiteDatabase db) {

        // If id is not yet set, record cannot exist in db
        if (getId() == -1) {
            return false;
        }

        // If the session has an id, check that it exists in the database
        else {
            Cursor c = db.query(
                    getTableName(),
                    new String[]{},
                    KEY_ID + " = " + Long.toString(getId()),
                    null,
                    null,
                    null,
                    null);

            int numRecords = c.getCount();

            c.close();

            if (numRecords == 1) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 'GET' METHODS TO RETRIEVE A TABLE FIELD VALUE.
     *
     * First the key is checked to determine whether it is valid
     *
     * Then the bundle is checked to determine whether the specified field exists
     *
     * If both checks are OK, the field value is retrieved
     *
     * @param key
     * @return
     */

    public String getString(String key) throws InvalidFieldException, FieldNotFoundException {
        checkKeyIsValid(key);
        checkBundleContainsKey(key);
        return mData.getString(key);
    }

    public double getDouble(String key) throws InvalidFieldException, FieldNotFoundException {
        checkKeyIsValid(key);
        checkBundleContainsKey(key);
        return mData.getDouble(key);
    }

    public long getLong(String key) throws InvalidFieldException, FieldNotFoundException {
        checkKeyIsValid(key);
        checkBundleContainsKey(key);
        return mData.getLong(key);
    }

    public int getInt(String key) throws InvalidFieldException, FieldNotFoundException {
        checkKeyIsValid(key);
        checkBundleContainsKey(key);
        return mData.getInt(key);
    }

    public void set(String key, String value) throws InvalidFieldException, FieldNotFoundException {
        int idx = checkKeyIsValid(key);
        mData.putString(key, value);
    }

    public void set(String key, int value) throws InvalidFieldException, FieldNotFoundException {
        int idx = checkKeyIsValid(key);
        mData.putInt(key, value);
    }

    public void set(String key, long value) throws InvalidFieldException, FieldNotFoundException {
        int idx = checkKeyIsValid(key);
        mData.putLong(key, value);
    }

    public void set(String key, double value) throws InvalidFieldException, FieldNotFoundException {
        int idx = checkKeyIsValid(key);
        mData.putDouble(key, value);
    }

    public void set(String key, float value) throws InvalidFieldException, FieldNotFoundException {
        int idx = checkKeyIsValid(key);
        mData.putFloat(key, value);
    }

    /*
    HELPERS
     */

    private int checkKeyIsValid(String key) throws InvalidFieldException {

        boolean isValid = false;
        int keyIndex = 0;

        for (int i = 0; i < mKeys.length; i++) {
            if (mKeys[i].contentEquals(key)) {
                isValid = true;
                keyIndex = i;
            }
        }

        if (!isValid) {
            throw new InvalidFieldException(key + " is not a valid key in table " + getTableName());
        }

        return keyIndex;

    }

    private void checkBundleContainsKey(String key) {
        if (!mData.containsKey(key)) {
            throw new FieldNotFoundException("Key does not exist in data bundle");
        }
    }

    public void printContents() {

        Log.v(LOG_TAG, "Printing contents:");

        for (int i = 0; i < mKeys.length; i++) {

            String key = mKeys[i];
            String fieldValue = new String();

            switch (mTypes[i]) {
                case TYPE_STRING :
                    fieldValue = getString(key);
                    break;

                case TYPE_INT :
                    fieldValue = Integer.toString(getInt(key));
                    break;

                case TYPE_DOUBLE :
                    fieldValue = Double.toString(getDouble(key));
                    break;

                case TYPE_LONG :
                    fieldValue = Long.toString(getLong(key));
                    break;

            }

            Log.v(LOG_TAG, "Key '" + key + "' = " + fieldValue);
        }
    }

    public long getId() {
        return mId;
    }

    public boolean isSameAs(DbEntity2 other) {
        return mId == other.getId();
    }

    /**
     * METHODS FOR IMPLEMENTING PARCELABLE INTERFACE
     */

    @Override
    public int describeContents() {
        return 0;
    }

    // write the object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeBundle(toBundle());
    }

    private class InvalidFieldException extends RuntimeException {
        public InvalidFieldException(String detailMessage) {
            super(detailMessage);
        }
    }

    private class FieldNotFoundException extends RuntimeException {
        public FieldNotFoundException(String detailMessage) { super(detailMessage); }
    }

    protected static DbEntity2[] findAllInternal(SQLiteDatabase db, String className, String tableName)  {

        Cursor cursor = db.query(tableName, new String[]{BaseColumns._ID}, null, null, null, null, null);
        cursor.moveToFirst();

        try {

            Class<?> clazz = Class.forName(className);

            Constructor<?> constructor = clazz.getConstructor(SQLiteDatabase.class, long.class);

            DbEntity2[] objs = new DbEntity2[cursor.getCount()];

            for (int c=0; c<cursor.getCount(); c++) {
                long rowId = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                objs[c] = (DbEntity2) constructor.newInstance(db, rowId);
                cursor.moveToNext();
            }

            cursor.close();

            return objs;

        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
