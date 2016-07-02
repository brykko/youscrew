package com.brick.youscrew.tags;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.brick.youscrew.data.TurnContract.TurnEntry;
import com.brick.youscrew.data.TurnDbUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brick on 26/03/16.
 *
 * Class for representing discrete tagging events during turn sessions. Each instance of this class
 * represents a tag given to one tetrode during a turn session
 *
 */
public class TagEvent extends Tag {

    private int mType;

    private long mTurnId;
    private long mTetrodeId;
    private long mSessionId;

//    private long mRatId;

    public TagEvent(SQLiteDatabase db, long tagId, long turnId, int tagType) {

        // Call super constructor to set the non-event-specific components of the tag
        super(db, tagId);

        checkTagType(tagType);

        mTurnId = turnId;
        mType = tagType;

        Cursor c = TurnDbUtils.turnGetEntries(db, mTurnId);
        mTetrodeId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY));
        mSessionId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_SESSION_KEY));
        c.close();

    }

    public int getType() {
        return mType;
    }

    private void checkTagType(int tagType) {
        // Make sure that the tag type refers to pre/post/persistent-pre/persistent-post

        if ( tagType != TurnDbUtils.TAG_TYPE_PRETURN &&
                        tagType != TurnDbUtils.TAG_TYPE_POSTTURN &&
                        tagType != TurnDbUtils.TAG_TYPE_PERSISTENT_PRE &&
                        tagType != TurnDbUtils.TAG_TYPE_PERSISTENT_POST ) {
            throw new IllegalArgumentException("Argument 'tagType' must be preturn, postturn, " +
                    "persistent-pre or persistent-post");
        }

    }

    public static List<TagEvent> fromTurnId(SQLiteDatabase db, long turnId, Integer tagType, Long tagGroupId) {

        Cursor c = TurnDbUtils.turnGetEntries(db, turnId);

        List<TagEvent> tagList = new ArrayList<>();

        List<String> columns = new ArrayList<>();

        // Use a single defined tag type
        if (tagType != null) {

            switch (tagType) {
                case TurnDbUtils.TAG_TYPE_PRETURN:
                    columns.add(TurnEntry.COLUMN_TAG_ID_PRE);
                    break;
                case TurnDbUtils.TAG_TYPE_POSTTURN:
                    columns.add(TurnEntry.COLUMN_TAG_ID_POST);
                    break;
                case TurnDbUtils.TAG_TYPE_PERSISTENT_PRE:
                    columns.add(TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE);
                    break;
                case TurnDbUtils.TAG_TYPE_PERSISTENT_POST:
                    columns.add(TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid tag type");
            }
        }

        // Or get all of them
        else {
            columns.add(TurnEntry.COLUMN_TAG_ID_PRE);
            columns.add(TurnEntry.COLUMN_TAG_ID_POST);
            columns.add(TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE);
            columns.add(TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST);
        }


        for (int i = 0; i < columns.size(); i++) {

            String csvString = c.getString(c.getColumnIndex(columns.get(i)));

            if (csvString != null) {
                long[] tagIds = TurnDbUtils.commaSepStringToLongs(csvString);

                TagEvent currentTag;

                for (int t = 0; t < tagIds.length; t++) {

                    currentTag = new TagEvent(db, tagIds[t], turnId, tagType);

                    // Add the tag to the list if it matches the requested tag group id
                    // (or if tag group id is null)
                    if (tagGroupId == null || currentTag.mGroupId == tagGroupId) {
                        tagList.add(currentTag);
                    }
                }

            }

        }

        c.close();

        return tagList;
    }

//    public static List<TagEvent> fromTurnIdAllTypes(SQLiteDatabase db, long turnId) {
//        //Get a list of all tag events of a single turn record
//
//        List<TagEvent> tagList = new ArrayList<>();
//
//        tagList.addAll(fromTurnId(db, turnId, TurnDbUtils.TAG_TYPE_PRETURN));
//        tagList.addAll(fromTurnId(db, turnId, TurnDbUtils.TAG_TYPE_POSTTURN));
//        tagList.addAll(fromTurnId(db, turnId, TurnDbUtils.TAG_TYPE_PERSISTENT_PRE));
//        tagList.addAll(fromTurnId(db, turnId, TurnDbUtils.TAG_TYPE_PERSISTENT_POST));
//
//        return tagList;
//
//    }

}
