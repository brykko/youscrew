package com.brick.youscrew.tags;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import com.brick.youscrew.data.TurnContract.*;
import com.brick.youscrew.data.TurnDbUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brick on 26/03/16.
 */
public class TagGroup {

    private long mId;
    private String mName;
    private int mColor;
    private int mMandatory;
    private int mSingleConstraint;
    private int mInUse;
    private int mDeleted;

    public TagGroup(SQLiteDatabase db, long rowId) {

        Cursor c = TurnDbUtils.tagGroupGetEntries(db, rowId, null);

        mId = rowId;
        mName              = c.getString(c.getColumnIndex(TagGroupEntry.COLUMN_NAME));
        mMandatory         = c.getInt(c.getColumnIndex(TagGroupEntry.COLUMN_MANDATORY));
        mSingleConstraint  = c.getInt(c.getColumnIndex(TagGroupEntry.COLUMN_SINGLE_CONSTRAINT));
        mInUse             = c.getInt(     c.getColumnIndex(TagGroupEntry.COLUMN_IN_USE));
        mDeleted           = c.getInt(     c.getColumnIndex(TagGroupEntry.COLUMN_DELETED));

        mColor          = Color.parseColor(c.getString(c.getColumnIndex(TagGroupEntry.COLUMN_COLOUR)));
        c.close();

    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public int getMandatory() {
        return mMandatory;
    }

    public int getSingleConstraint() {
        return mSingleConstraint;
    }

    public int getInUse() {
        return mInUse;
    }

    public int getDeleted() {
        return mDeleted;
    }

    public List<Tag> findChildTags(SQLiteDatabase db, int inUseCode) {

        List<Tag> tagList = new ArrayList<>();

        Cursor c = TurnDbUtils.tagGetEntriesByTagGroupId(db, mId, inUseCode);

        long tagId;

        for (int t = 0; t < c.getCount(); t++) {
            tagId = c.getLong(c.getColumnIndex(TagEntry._ID));
            tagList.add(new Tag(db, tagId));
        }

        return tagList;

    }

    public static  List<TagGroup> findAll(SQLiteDatabase db, Integer deletedCode) {

        List<TagGroup> tagGroupList = new ArrayList<>();

        TagGroup tagGroup;

        Cursor c = TurnDbUtils.tagGroupGetEntries(db, null, deletedCode);

        int numGroups = c.getCount();

        for (int g = 0; g < numGroups; g++) {

            tagGroup = new TagGroup(db, c.getLong(c.getColumnIndex(TagGroupEntry._ID)));

            tagGroupList.add(tagGroup);

            c.moveToNext();

        }

        return tagGroupList;
    }


}
