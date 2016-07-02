/*
package com.brick.youscrew.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;

import com.brick.youscrew.data.TurnContract.TurnEntry;

*/
/**
 * Created by windows on 03.04.2016.
 *//*

public class TurnOld extends DbEntity {

    public static final int TURNED_NO = 1;
    public static final int TURNED_YES = 2;

    public static final int EDITED_NO = 1;
    public static final int EDITED_YES = 2;

    private Session mSession;
    private Tetrode mTetrode;

    private long mTime;
    private double mStartAngle;
    private double mEndAngle;
    private String mTagIdPre;
    private String mTagIdPost;
    private String mTagIdPersistentPre;
    private String mTagIdPersistentPost;
    private String mComment;
    private int mWasTurned;
    private int mWasEdited;


    public TurnOld(Session parentSession, Tetrode parentTetrode) {

        mTableName = TurnEntry.TABLE_NAME;

        mSession = parentSession;
        mTetrode = parentTetrode;

    }

    public TurnOld(Session parentSession, Tetrode parentTetrode, SQLiteDatabase db, long id) {

        this(parentSession, parentTetrode);

        mId = id;

        updateFromDb(db);

//        Cursor c = TurnDbUtils.turnGetEntries(db, id);
//        fillFromCursor(c);
//        c.close();

    }

    public TurnOld(Bundle bundle) {

        this((Session) bundle.getParcelable("session"), (Tetrode) bundle.getParcelable("tetrode"));

        mId = bundle.getLong("id");

        mTime = bundle.getLong("time");
        mStartAngle = bundle.getDouble("startAngle");
        mEndAngle = bundle.getDouble("endAngle");
        mTagIdPre = bundle.getString("tagIdPre");
        mTagIdPost = bundle.getString("tagIdPost");
        mTagIdPersistentPre = bundle.getString("tagIdPersistentPre");
        mTagIdPersistentPost = bundle.getString("tagIdPersistentPost");
        mWasTurned = bundle.getInt("wasTurned");
        mWasEdited = bundle.getInt("wasEdited");
        mComment = bundle.getString("comment");

    }

    */
/**
     * OTHER MISC METHODS
     *//*


    public ContentValues createContentValues() {

        ContentValues values = new ContentValues();

        values.put(TurnEntry.COLUMN_SESSION_KEY, mSession.getId());
        values.put(TurnEntry.COLUMN_TETRODE_KEY, mTetrode.getId());
        values.put(TurnEntry.COLUMN_TIME, mTime);
        values.put(TurnEntry.COLUMN_START_ANGLE, mStartAngle);
        values.put(TurnEntry.COLUMN_END_ANGLE, mEndAngle);
        values.put(TurnEntry.COLUMN_TAG_ID_PRE, mTagIdPre);
        values.put(TurnEntry.COLUMN_TAG_ID_POST, mTagIdPost);
        values.put(TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE, mTagIdPersistentPre);
        values.put(TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST, mTagIdPersistentPost);
        values.put(TurnEntry.COLUMN_WAS_TURNED, mWasTurned);
        values.put(TurnEntry.COLUMN_WAS_TURNED, mWasTurned);
        values.put(TurnEntry.COLUMN_WAS_EDITED, mWasEdited);
        values.put(TurnEntry.COLUMN_COMMENT, mComment);

        return values;

    }

    public Bundle toBundle(){
        Bundle bundle = new Bundle();
        bundle.putParcelable("session", mSession);
        bundle.putParcelable("tetrode", mTetrode);
        bundle.putLong("id", mId);
        bundle.putLong("time", mTime);
        bundle.putDouble("startAngle", mStartAngle);
        bundle.putDouble("endAngle", mEndAngle);
        bundle.putString("tagIdPre", mTagIdPre);
        bundle.putString("tagIdPost", mTagIdPost);
        bundle.putString("tagIdPersistentPre", mTagIdPersistentPre);
        bundle.putString("tagIdPersistentPost", mTagIdPersistentPost);
        bundle.putInt("wasTurned", mWasTurned);
        bundle.putInt("wasEdited", mWasEdited);
        bundle.putString("comment", mComment);

        return bundle;
    }

    protected void fillFromCursor(Cursor c) {
        mTime = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_TIME));
        mStartAngle = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_START_ANGLE));
        mEndAngle = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_END_ANGLE));
        mTagIdPre = c.getString(c.getColumnIndex(TurnEntry.COLUMN_TAG_ID_PRE));
        mTagIdPost = c.getString(c.getColumnIndex(TurnEntry.COLUMN_TAG_ID_POST));
        mTagIdPersistentPre = c.getString(c.getColumnIndex(TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE));
        mTagIdPersistentPost = c.getString(c.getColumnIndex(TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST));
        mWasTurned = c.getInt(c.getColumnIndex(TurnEntry.COLUMN_WAS_TURNED));
        mWasEdited = c.getInt(c.getColumnIndex(TurnEntry.COLUMN_WAS_EDITED));
        mComment = c.getString(c.getColumnIndex(TurnEntry.COLUMN_COMMENT));
    }


    */
/**
     * PARCELABLE METHODS
     *//*



    // this is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Creator<TurnOld> CREATOR = new Creator<TurnOld>() {

        public TurnOld createFromParcel(Parcel in) {
            return new TurnOld(in.readBundle(getClass().getClassLoader()));
        }

        public TurnOld[] newArray(int size) {
            return new TurnOld[size];
        }

    };

    */
/**
     * GET/SET METHODS
     *//*


    public Session getSession() {
        return mSession;
    }

    public Tetrode getTetrode() {
        return mTetrode;
    }

    public Rat getRat() {
        return mSession.getRat();
    }

    public void setTime(long value) {
        mTime = value;
    }

    public long getTime() {
        return mTime;
    }

    public void setStartAngle(double value) {
        mStartAngle = value;
    }

    public double getStartAngle() {
        return mStartAngle;
    }

    public void setEndAngle(double value) {
        mEndAngle = value;
    }

    public double getEndAngle() {
        return mEndAngle;
    }

    public void setTagIdPre(String value) {
        mTagIdPre = value;
    }

    public String getTagIdPre() {
        return mTagIdPre;
    }

    public void setTagIdPost(String value) {
        mTagIdPost = value;
    }

    public String getTagIdPost() {
        return mTagIdPost;
    }

    public void setTagIdPersistentPre(String value) {
        mTagIdPersistentPre = value;
    }

    public String getTagIdPersistentPre() {
        return mTagIdPersistentPre;
    }

    public void setTagIdPersistentPost(String value) {
        mTagIdPersistentPost = value;
    }

    public String getTagIdPersistentPost() {
        return mTagIdPersistentPost;
    }

    public void setWasTurned(int value) {
        if (value != TURNED_YES && value != TURNED_NO) {
            throw new IllegalArgumentException("Illegal value for wasTurned");
        }
        mWasTurned = value;
    }

    public int getWasTurned() {
        return mWasTurned;
    }

    public void setWasEdited(int value) {
        if (value != EDITED_NO && value != EDITED_YES) {
            throw new IllegalArgumentException("Illegal value for wasEdited");
        }
        mWasEdited = value;
    }

    public int getWasEdited() {
        return mWasEdited;
    }

    public void setComment(String value) {
        mComment = value;
    }

    public String getComment() {
        return mComment;
    }

    */
/**
     * OTHER MISC
     *//*


    public boolean hasTagsPre() {
        return getTagIdPre() != null;
    }

    public boolean hasTagsPost() {
        return getTagIdPost() != null;
    }

    public double getTurnsTotal() {

        int threadDir = getRat().getScrewThreadDir();

        double initialAngle = getTetrode().getDouble(TurnContract.TetrodeEntry.COLUMN_INITIAL_ANGLE);

        if (threadDir == Rat.THREAD_DIR_CCW) {
            return (getEndAngle() - initialAngle) / 360d;
        }
        else {
            return  (initialAngle - getEndAngle()) / 360d;
        }

    }

    public double getTurnsThisSession() {

        int threadDir = getRat().getScrewThreadDir();

        if (threadDir == Rat.THREAD_DIR_CCW) {
            return ( getEndAngle() - getStartAngle() ) / 360d;
        }
        else {
            return ( getStartAngle() - getEndAngle() ) / 360d;
        }

    }

    public double getDepth() {
        return getTurnsTotal() * getRat().getTurnMicrometers();
    }

    public double getDepthAvancement() {
        return getTurnsThisSession() * getRat().getTurnMicrometers();
    }

}
*/
