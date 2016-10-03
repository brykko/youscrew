package com.brick.youscrew.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.brick.youscrew.data.TurnContract.*;
import com.brick.youscrew.utils.AppInstance;

/**
 * Created by windows on 07.02.2016.
 */
public class TurnDbHelper extends SQLiteOpenHelper {

    private static TurnDbHelper sInstance;

    private static final String LOG_TAG = TurnDbHelper.class.getSimpleName();

    // If you change the database schema, you must increment the database version

    private static final int DATABASE_VERSION = 2;
    // VERSION 2: added new column "tetrodes_independent" in rat table

    public static final String DATABASE_NAME = "turn.db";

    public static synchronized TurnDbHelper getInstance(Context context) {
        // Implements a singleton pattern for this class. The class constructor
        // is private, so can only be accessed via this method, which enforces a
        // single instance.

        if (context == null) {
            context = AppInstance.getAppContext();
        }

        if (sInstance == null) {
            sInstance = new TurnDbHelper(context.getApplicationContext());
        }

        return sInstance;

    }

    public static void deleteDatabase(Context context) {
        // Always delete the database with this method rather than with
        // context.deleteDatabase(name), since it nullifies the single instance
        // forcing the Db to be created again on the next call to getInstance().
        context.getApplicationContext().deleteDatabase(DATABASE_NAME);
        sInstance = null;
    }

    private TurnDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        // RAT TABLE
        final String SQL_CREATE_RAT_TABLE = "CREATE TABLE " + RatEntry.TABLE_NAME + " (" +
                RatEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                RatEntry.COLUMN_CODE + " TEXT NOT NULL UNIQUE," +
                RatEntry.COLUMN_NAME + " TEXT NOT NULL UNIQUE," +
                RatEntry.COLUMN_NUM_TETRODES + " INTEGER NOT NULL," +
                RatEntry.COLUMN_TETRODES_INDEPENDENT + " INTEGER NOT NULL," +
                RatEntry.COLUMN_DISPLAY_INDEX  + " INTEGER NOT NULL," +
                RatEntry.COLUMN_SCREW_THREAD_DIR + " INTEGER NOT NULL," +
                RatEntry.COLUMN_TURN_MICROMETERS + " REAL NOT NULL );";


        sqLiteDatabase.execSQL(SQL_CREATE_RAT_TABLE);

        // TETRODE TABLE
        final String SQL_CREATE_TETRODE_TABLE = "CREATE TABLE " + TetrodeEntry.TABLE_NAME + " (" +
                TetrodeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TetrodeEntry.COLUMN_RAT_KEY + " INTEGER NOT NULL," +
                TetrodeEntry.COLUMN_INDEX + " INTEGER NOT NULL," +
                TetrodeEntry.COLUMN_NAME + " TEXT NOT NULL," +
                TetrodeEntry.COLUMN_INITIAL_ANGLE + " REAL NOT NULL, " +
                TetrodeEntry.COLUMN_IS_REF + " INTEGER NOT NULL, " +
                TetrodeEntry.COLUMN_IN_USE + " INTEGER NOT NULL, " +
                TetrodeEntry.COLUMN_COLOUR + " INTEGER NOT NULL, " +
                TetrodeEntry.COLUMN_INITIAL_ANGLE_SET + " INTEGER NOT NULL, " +
                TetrodeEntry.COLUMN_EVER_TURNED + " INTEGER NOT NULL, " +
                TetrodeEntry.COLUMN_TAGGABLE + " INTEGER NOT NULL," +
                TetrodeEntry.COLUMN_COMMENT + " TEXT, " +

                " FOREIGN KEY (" + TetrodeEntry.COLUMN_RAT_KEY + ") REFERENCES " +
                RatEntry.TABLE_NAME + " (" + RatEntry._ID + ") );";

        sqLiteDatabase.execSQL(SQL_CREATE_TETRODE_TABLE);

        // SESSION TABLE
        final String SQL_CREATE_SESSION_TABLE = "CREATE TABLE " + SessionEntry.TABLE_NAME + " (" +
                SessionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SessionEntry.COLUMN_RAT_KEY + " INTEGER NOT NULL," +
                SessionEntry.COLUMN_SESSION_KEY_LAST + " INTEGER," + // Null allowed (first session has no precedent)
                SessionEntry.COLUMN_TIME_START + " INTEGER NOT NULL," +
                SessionEntry.COLUMN_TIME_END + " INTEGER," +
                SessionEntry.COLUMN_NUM_TTS_TURNED + " INTEGER NOT NULL," +
                SessionEntry.COLUMN_IS_OPEN + " INTEGER NOT NULL," +
                SessionEntry.COLUMN_TIME_MODE + " INTEGER NOT NULL," +
                SessionEntry.COLUMN_IS_FIRST_SESSION + " INTEGER NOT NULL," +
                SessionEntry.COLUMN_COMMENT + " TEXT," +
                SessionEntry.COLUMN_TAGS + " TEXT NOT NULL );";


        sqLiteDatabase.execSQL(SQL_CREATE_SESSION_TABLE);

        // TURN TABLE
        final String SQL_CREATE_TURN_TABLE = "CREATE TABLE " + TurnEntry.TABLE_NAME + " (" +
                TurnEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TurnEntry.COLUMN_SESSION_KEY + " INTEGER NOT NULL," +
                TurnEntry.COLUMN_TETRODE_KEY + " INTEGER NOT NULL," +
                TurnEntry.COLUMN_TIME + " REAL," +
                TurnEntry.COLUMN_START_ANGLE + " REAL," +
                TurnEntry.COLUMN_END_ANGLE + " REAL," +
                TurnEntry.COLUMN_TAG_ID_PRE + " TEXT," +
                TurnEntry.COLUMN_TAG_ID_POST + " TEXT," +
                TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE + " TEXT," +
                TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST + " TEXT," +
                TurnEntry.COLUMN_COMMENT + " TEXT," +
                TurnEntry.COLUMN_WAS_TURNED + " INTEGER NOT NULL," +
                TurnEntry.COLUMN_WAS_EDITED + " INTEGER NOT NULL," +

                " FOREIGN KEY (" + TurnEntry.COLUMN_TAG_ID_PRE + ") REFERENCES " +
                TagEntry.TABLE_NAME + " (" + TagEntry._ID + ")," +

                " FOREIGN KEY (" + TurnEntry.COLUMN_TAG_ID_POST + ") REFERENCES " +
                TagEntry.TABLE_NAME + " (" + TagEntry._ID + ")," +

                " FOREIGN KEY (" + TurnEntry.COLUMN_SESSION_KEY + ") REFERENCES " +
                SessionEntry.TABLE_NAME + " (" + SessionEntry._ID + ")," +

                " FOREIGN KEY (" + TurnEntry.COLUMN_TETRODE_KEY + ") REFERENCES " +
                TetrodeEntry.TABLE_NAME + " (" + TetrodeEntry._ID + ") );";

        sqLiteDatabase.execSQL(SQL_CREATE_TURN_TABLE);


        // TAG GROUP TABLE
        final String SQL_CREATE_TAG_GROUP_TABLE = "CREATE TABLE " + TagGroupEntry.TABLE_NAME + " (" +
                TagGroupEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TagGroupEntry.COLUMN_NAME + " TEXT NOT NULL UNIQUE," +
                TagGroupEntry.COLUMN_COLOUR + " TEXT NOT NULL," +
                TagGroupEntry.COLUMN_IN_USE + " INTEGER NOT NULL," +
                TagGroupEntry.COLUMN_DISPLAY_INDEX + " INTEGER NOT NULL," +
                TagGroupEntry.COLUMN_DELETED + " INTEGER NOT NULL," +
                TagGroupEntry.COLUMN_SINGLE_CONSTRAINT + " INTEGER NOT NULL," +
                TagGroupEntry.COLUMN_HINT_TEXT + " TEXT," +
                TagGroupEntry.COLUMN_MANDATORY + " INTEGER NOT NULL );";

        sqLiteDatabase.execSQL(SQL_CREATE_TAG_GROUP_TABLE);


        // TAG TABLE
        final String SQL_CREATE_TAG_TABLE = "CREATE TABLE " + TagEntry.TABLE_NAME + " (" +
                TagGroupEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TagEntry.COLUMN_TAG_GROUP_KEY + " INTEGER NOT NULL," +
                TagEntry.COLUMN_IN_USE + " INTEGER NOT NULL," +
                TagEntry.COLUMN_DISPLAY_INDEX + " INTEGER NOT NULL," +
                TagEntry.COLUMN_DELETED + " INTEGER NOT NULL," +
                TagEntry.COLUMN_NAME + " TEXT NOT NULL," +

                " FOREIGN KEY (" + TagEntry.COLUMN_TAG_GROUP_KEY+ ") REFERENCES " +
                TagGroupEntry.TABLE_NAME + " (" + TagGroupEntry._ID + ") );";

        sqLiteDatabase.execSQL(SQL_CREATE_TAG_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        switch (oldVersion) {

            case 1 :

                db.execSQL(
                        "ALTER TABLE " + RatEntry.TABLE_NAME
                                + " ADD COLUMN " + RatEntry.COLUMN_TETRODES_INDEPENDENT +
                                " INTEGER NOT NULL DEFAULT " + Integer.toString(Rat.TETRODES_INDEPENDENT_YES));

        }

    }

}
