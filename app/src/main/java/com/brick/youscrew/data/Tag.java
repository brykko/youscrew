package com.brick.youscrew.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.brick.youscrew.data.TurnContract.TagEntry;

import java.sql.Blob;

/**
 * Created by windows on 11.04.2016.
 */
public class Tag extends DbEntity2 {

    // Consts indicating the type of tag (pre-turn or post-turn)
    public static final int TAGTYPE_PRETURN = 1;
    public static final int TAGTYPE_POSTTURN = 2;
    public static final int TAGTYPE_PERSISTENT_PRE = 3;
    public static final int TAGTYPE_PERSISTENT_POST = 4;
    public static final int TAGTYPE_GENERIC = 5; // use this for settings mode

    private TagGroup mTagGroup;

    public Tag() {
        super();
    }

    public Tag(Bundle bundle) {
        super(bundle);
        mTagGroup = bundle.getParcelable(TurnContract.TagGroupEntry.TABLE_NAME);
    }

    public Tag(SQLiteDatabase db, long id) {
        this();

        mId = id;

        updateFromDb(db);

    }

    @Override
    public String getTableName() {
        return TagEntry.TABLE_NAME;
    }

    public void setKeys() {

        mKeys = new String[] {
                TagEntry.COLUMN_TAG_GROUP_KEY,
                TagEntry.COLUMN_NAME,
                TagEntry.COLUMN_IN_USE,
                TagEntry.COLUMN_DISPLAY_INDEX,
                TagEntry.COLUMN_DELETED
        };

        mTypes = new int[] {
                TYPE_LONG,
                TYPE_STRING,
                TYPE_INT,
                TYPE_INT,
                TYPE_INT,
        };

    }

    @Override
    protected Bundle toBundle() {
        Bundle bundle = super.toBundle();
        bundle.putParcelable(TurnContract.TagGroupEntry.TABLE_NAME, mTagGroup);
        return bundle;
    }


    public static Tag[] findAll(SQLiteDatabase db) {
        Cursor c = db.query(
                TagEntry.TABLE_NAME,
                null,
                null,
                new String[]{},
                null,
                null,
                null);

        int numRecords = c.getCount();

        if (numRecords == 0) {
            return null;
        }

        c.moveToFirst();

        Tag[] tags = new Tag[numRecords];

        for (int i = 0; i < numRecords; i++) {
            tags[i] = new Tag(db, c.getLong(c.getColumnIndex(TagEntry._ID)));
        }

        c.close();

        return tags;

    }

    public boolean existsInTurn(Turn turn, int tagType) {

        String tagString = turn.getString(getTagListColumn(tagType));

        if (tagString == null) {
            return false;
        }

        else {

            long[] tagIds = parseIdsFromString(tagString);

            boolean matchFound = false;

            for (long id : tagIds) {
                if (id == mId) {
                    matchFound = true;
                    break;
                }
            }

            return matchFound;

        }

    }

    public TagGroup getTagGroup() {
        return mTagGroup;
    }

    public static long[] parseIdsFromString(String csv) {

        if (csv == null) {
            return null;
        }
        else {

            String[] tagIdStrings = csv.split(",");

            int numTags = tagIdStrings.length;

            long[] tagIds = new long[numTags];
            for (int i = 0; i < numTags; i++) {
                tagIds[i] = Long.parseLong(tagIdStrings[i]);
            }

            return tagIds;

        }
    }

    public static String getTagListColumn(int tagType) {

        String columnName;

        switch (tagType) {
            case TAGTYPE_PRETURN:
                columnName = TurnContract.TurnEntry.COLUMN_TAG_ID_PRE;
                break;
            case TAGTYPE_POSTTURN:
                columnName = TurnContract.TurnEntry.COLUMN_TAG_ID_POST;
                break;
            case TAGTYPE_PERSISTENT_PRE:
                columnName = TurnContract.TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE;
                break;
            case TAGTYPE_PERSISTENT_POST:
                columnName = TurnContract.TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST;
                break;
            default:
                throw new IllegalArgumentException("Illegal value '" + Integer.toString(tagType) +
                        "' for argument 'tagType'");
        }

        return columnName;

    }



    // this is used to regenerate the object. All Parcelables must have a CREATOR that implements these two methods
    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {

        public Tag createFromParcel(Parcel in) {
            return new Tag(in.readBundle(getClass().getClassLoader()));
        }

        public Tag[] newArray(int size) {
            return new Tag[size];
        }

    };


}
