package com.brick.youscrew.tags;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.brick.youscrew.data.TurnContract.TagEntry;
import com.brick.youscrew.data.TurnDbUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brick on 26/03/16.
 */
public class Tag {

    private final String LOG_TAG = Tag.class.getSimpleName();

    protected long mId;
    protected String mName;
    protected int mDeleted;
    protected long mGroupId;
//    protected TagGroup mParentGroup;

    public Tag(SQLiteDatabase db, long tagId) {

        Log.v(LOG_TAG, "Trying to retrieve tag Id " + Long.toString(tagId));
        Cursor c = TurnDbUtils.tagGetEntries(db, tagId, null);
        mId = tagId;

        // Get values from the tag record
        mDeleted =  c.getInt(   c.getColumnIndex(TagEntry.COLUMN_DELETED));
        mName =     c.getString(c.getColumnIndex(TagEntry.COLUMN_NAME));
        mGroupId =  c.getLong(c.getColumnIndex(TagEntry.COLUMN_TAG_GROUP_KEY));
        c.close();

//         Construct parent group object
//        mParentGroup = new TagGroup(db, mGroupId);

    }

    public TagGroup findParentGroup(SQLiteDatabase db) {
        return new TagGroup(db, mGroupId);
    }

    public String getName() {
        return mName;
    }

    public long getGroupId() {
        return mGroupId;
    }

    public long getId() {
        return mId;
    }

    public int getDeleted() {
        return mDeleted;
    }

    public static List<Tag> fromTagIdCsvString(SQLiteDatabase db, String csvString) {

        List<Tag> tagList = new ArrayList<>();

        long[] tagIds = TurnDbUtils.commaSepStringToLongs(csvString);

        for (int t = 0; t < tagIds.length; t++) {
            tagList.add(new Tag(db, tagIds[t]));
        }

        return tagList;

    }

}
