package com.brick.youscrew.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.brick.youscrew.data.TurnContract.TagGroupEntry;

/**
 * Created by windows on 11.04.2016.
 */
public class TagGroup extends DbEntity2 {

    public TagGroup() {
        super();
    }

    public TagGroup(SQLiteDatabase db, long id) {
        this();
        mId = id;
        updateFromDb(db);
    }

    public TagGroup(Bundle bundle) {
        super(bundle);
    }

    @Override
    protected void setKeys() {

    }

    @Override
    public String getTableName() {
        return TagGroupEntry.TABLE_NAME;
    }

    public Tag[] findTags(SQLiteDatabase db) {

        Cursor c = db.query(
                TurnContract.TagEntry.TABLE_NAME,
                new String[] {TurnContract.TagEntry._ID},
                TurnContract.TagEntry.COLUMN_TAG_GROUP_KEY + " = " + Long.toString(mId),
                null,
                null,
                null,
                null
        );

        int numTags = c.getCount();

        if (numTags == 0) {
            return null;
        }
        else {

            c.moveToFirst();

            Tag[] tags = new Tag[numTags];

            for (int t = 0; t < numTags; t++) {
                tags[t] = new Tag(db, c.getLong(c.getColumnIndex(TurnContract.TagEntry._ID)));
                c.moveToNext();
            }

            c.close();

            return tags;

        }

    }

    // this is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<TagGroup> CREATOR = new Parcelable.Creator<TagGroup>() {

        public TagGroup createFromParcel(Parcel in) {
            return new TagGroup(in.readBundle(getClass().getClassLoader()));
        }

        public TagGroup[] newArray(int size) {
            return new TagGroup[size];
        }

    };

}
