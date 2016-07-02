package com.brick.youscrew.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.dropbox.client2.exception.DropboxException;
import com.brick.youscrew.data.TurnContract.RatEntry;
import com.brick.youscrew.data.TurnContract.SessionEntry;
import com.brick.youscrew.data.TurnContract.TetrodeEntry;
import com.brick.youscrew.data.TurnContract.TurnEntry;
import com.brick.youscrew.tags.TagEvent;
import com.brick.youscrew.tags.TagGroup;
import com.brick.youscrew.utils.AppInstance;
import com.brick.youscrew.utils.BackupHelper;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Brick on 25/03/16.
 */
public class LogWriter {

    public static final char SEPARATOR = ',';

    private static final String LOG_TAG = LogWriter.class.getSimpleName();
    public static final String DATE_FORMAT_SESSION = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT_TURN = "HH:mm:ss";

    private SQLiteDatabase mDb;
    private BackupHelper mBackupHelper;

    private String mFileName, mFilePath;

    private SimpleDateFormat mDateFormatSession, mDateFormatTurn;

    private Rat mRat;

    private List<TagGroup> mTagGroups;

    private int mNumColumns;

    private CSVWriter mCsvWriter;

    private String[] mTurnHeadings;

    private String[] mTurnStrings;


    public LogWriter(Context context, long ratId) throws IOException {

        mDb = TurnDbHelper.getInstance(context).getWritableDatabase();
        mBackupHelper = AppInstance.getInstance(context).getBackupHelper();

        mRat = new Rat(mDb, ratId);

        mTagGroups = TagGroup.findAll(mDb, TurnDbUtils.DELETED_NO);

        mTurnHeadings = makeTurnHeadings();
        mNumColumns = mTurnHeadings.length;

        makeFilePath();

        mDateFormatSession = new SimpleDateFormat(DATE_FORMAT_SESSION);
        mDateFormatTurn = new SimpleDateFormat(DATE_FORMAT_TURN);

        mCsvWriter = new CSVWriter( new FileWriter(mFilePath), SEPARATOR);

    }

    public void write(boolean toDropbox) throws IOException, DropboxException {

        // Get all turning sessions for the current rat
        Session[] sessions = mRat.findSessions(mDb, null);

        int numSessions = sessions.length;

        if (numSessions == 0) {
            Toast.makeText(
                    mBackupHelper.getContext(),
                    "No turning sessions exist! Log not created",
                    Toast.LENGTH_LONG).show();
            return;
        }
        else {

//            // Start at the end (the earliest session) and move backward

            for (int s = 0; s < numSessions; s++) {

                // Write the session summary line
                writePaddedLine( makeSessionStrings(sessions[sessions.length-s-1], s+1) );

                // Write the turn column headings
                mCsvWriter.writeNext(mTurnHeadings);

                // Find all turn events for the current session
                Turn[] turns = sessions[s].findTurns(mDb);
                for (Turn turn : turns) {
                    makeTurnStrings(turn);
                    mCsvWriter.writeNext(mTurnStrings);
                }

                writeBlankLine(2);

            }
        }

        // Now we're finished with writing, close the file
        mCsvWriter.close();

        if (toDropbox) {
            uploadToDropbox();
        }
        else {
            Toast.makeText(mBackupHelper.getContext(), "Log successfully created", Toast.LENGTH_LONG).show();
        }

    }

    private void makeFilePath() {

        Date date = new Date(System.currentTimeMillis());

        String dateString = new SimpleDateFormat(BackupHelper.DATE_FORMAT).format(date);

        mFileName = "turn_log_" + mRat.getName() + "_" + dateString + ".csv";

        mFilePath = mBackupHelper.getDatabaseDirectory() + mFileName;

    }

    private void uploadToDropbox() {
        BackupHelper.UploadToDropboxTask task = new BackupHelper.UploadToDropboxTask() {

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);

                if (aBoolean == false) {
                    Toast.makeText(mBackupHelper.getContext(), "Failed to upload log to Dropbox", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(mBackupHelper.getContext(), "Log successfully uploaded to Dropbox", Toast.LENGTH_SHORT).show();
                }
            }
        };
        task.execute(mFilePath, mFileName);
    }

    private String[] makeSessionStrings(Session session, int sessionNumber) {

        String[] strings = new String[5];

        strings[0] = "Session #" + Integer.toString(sessionNumber);

        strings[1] = "Started " + mDateFormatSession.format(
                new Date(session.getLong(SessionEntry.COLUMN_TIME_START)) );

        strings[2] = "Finished " + mDateFormatSession.format(
                new Date(session.getLong(SessionEntry.COLUMN_TIME_END)) );

        int timeMode = session.getInt(SessionEntry.COLUMN_TIME_MODE);

        if (timeMode == TurnDbUtils.TIME_REAL) {
            strings[3] = "real time";
        }
        else {
            strings[3] = "picked time";
        }

        strings[4] = "\"" + session.getString(SessionEntry.COLUMN_COMMENT) + "\"";

        return strings;

    }


    private String[] makeTurnHeadings() {

        List<String> headings = new ArrayList<>();
        headings.add("Tetrode name");
        headings.add("Total turns before");
        headings.add("Turns during session");
        headings.add("Total turns after");
        headings.add("Depth before");
        headings.add("Depth advancement");
        headings.add("Depth after");
        headings.add("Comment");

        String tagGroupHeading;
        TagGroup tagGroup;

        List<String> tagGroupHeadings = new ArrayList<>();
        for (int g = 0; g < mTagGroups.size(); g++) {
            tagGroup = mTagGroups.get(g);
            tagGroupHeading = "(PRE) " + tagGroup.getName();
            headings.add(tagGroupHeading);
        }

        // 8 ) TAGS POST
        for (int g = 0; g < mTagGroups.size(); g++) {
            tagGroup = mTagGroups.get(g);
            tagGroupHeading = "(POST) " + tagGroup.getName();
            headings.add(tagGroupHeading);
        }

        return headings.toArray(new String[0]);

    }

    private void makeTurnStrings(Turn turn) {

        double anglePost = turn.getDouble(TurnEntry.COLUMN_END_ANGLE);
        double anglePre = turn.getDouble(TurnEntry.COLUMN_START_ANGLE);
        String comment = turn.getString(TurnEntry.COLUMN_COMMENT);

        String tetrodeName = turn.getTetrode().getString(TetrodeEntry.COLUMN_NAME);
        double initialAngle = turn.getTetrode().getDouble(TetrodeEntry.COLUMN_INITIAL_ANGLE);

        double turnsPre, turnsPost;

        if (mRat.getScrewThreadDir() == Rat.THREAD_DIR_CCW) {
            turnsPre = (anglePre - initialAngle) / 360d;
            turnsPost = (anglePost - initialAngle) / 360d;
        }
        else {
            turnsPre = (initialAngle - anglePre) / 360d;
            turnsPost = (initialAngle - anglePost) / 360d;
        }

        double depthPre = turnsPre * mRat.getTurnMicrometers();
        double depthPost = turnsPost * mRat.getTurnMicrometers();

//        List<String> strings = new ArrayList<>();
        if (mTurnStrings == null) {
            mTurnStrings = new String[8 + mTagGroups.size()*2];
        }

//        String[] strings = new String[8 + mTagGroups.size()*2];

        // 0 ) TETRODE NAME
//        strings.add(tetrodeName);
        mTurnStrings[0] = tetrodeName;

        // 1 ) TOTAL TURNS PRE
//        strings.add(Double.toString(turnsPre));
        mTurnStrings[1] = Double.toString(turnsPre);

        // 2 ) TURNS
//        strings.add(Double.toString(turnsPost - turnsPre));
        mTurnStrings[2] = Double.toString(turnsPost - turnsPre);

        // 3 ) TOTAL TURNS POST
//        strings.add(Double.toString(turnsPost));
        mTurnStrings[3] = Double.toString(turnsPost);

        // 4 ) DEPTH PRE
//        strings.add(Double.toString(depthPre));
        mTurnStrings[4] = Double.toString(depthPre);

        // 5 ) DEPTH ADVANCEMENT
//        strings.add(Double.toString(depthPost - depthPre));
        mTurnStrings[5] = Double.toString(depthPost - depthPre);

        // 6 ) DEPTH POST
//        strings.add(Double.toString(depthPost));
        mTurnStrings[6] = Double.toString(depthPost);

        // 7 ) COMMENT
//        strings.add("\"" + comment + "\"");
        mTurnStrings[7] = "\"" + comment + "\"";

        // 7 ) TAGS POST
        for (int g = 0; g < mTagGroups.size(); g++) {
//            strings.add(makeTagGroupString(mTagGroups.get(g).getId(), turn.getId(), TurnDbUtils.TURNTIME_PRE));
            mTurnStrings[8+g] = makeTagGroupString(mTagGroups.get(g).getId(), turn.getId(), TurnDbUtils.TURNTIME_PRE);
        }

        // 8 ) TAGS POST
        for (int g = 0; g < mTagGroups.size(); g++) {
//            strings.add(makeTagGroupString(mTagGroups.get(g).getId(), turn.getId(), TurnDbUtils.TURNTIME_POST));
            mTurnStrings[8+ mTagGroups.size() + g] = makeTagGroupString(mTagGroups.get(g).getId(), turn.getId(), TurnDbUtils.TURNTIME_POST);
        }

//        return strings.toArray(new String[0]);

    }

    private String makeTagGroupString(long tagGroupId, long turnId, int turnTime) {

        if (turnTime != TurnDbUtils.TURNTIME_PRE && turnTime != TurnDbUtils.TURNTIME_POST) {
            throw new IllegalArgumentException("Argument 'turnTime' must correspond to " +
                    "TURNTIME_PRE or TURNTIME_POST");
        }

        List<TagEvent> allTags = new ArrayList<>();

        int typeNormal;
        int typePersistent;

        if (turnTime == TurnDbUtils.TURNTIME_PRE) {
            typeNormal = TurnDbUtils.TAG_TYPE_PRETURN;
            typePersistent = TurnDbUtils.TAG_TYPE_PERSISTENT_PRE;

        }
        else if (turnTime == TurnDbUtils.TURNTIME_POST) {
            typeNormal = TurnDbUtils.TAG_TYPE_POSTTURN;
            typePersistent = TurnDbUtils.TAG_TYPE_PERSISTENT_POST;
        }
        else {
            throw new IllegalArgumentException("Argument 'prePostCode' must correspond to either " +
            "TURN_PRE or TURN_POST");
        }

        allTags.addAll(TagEvent.fromTurnId(mDb, turnId, typeNormal, tagGroupId));
        allTags.addAll(TagEvent.fromTurnId(mDb, turnId, typePersistent, tagGroupId));

        String tagString = new String();

        for (int t = 0; t < allTags.size(); t++) {

            TagEvent tag = allTags.get(t);

            // Append a persistence marker if necessary
            if (t < allTags.size()-1) {
                if (tag.getType() == typePersistent) {
                    tagString += ("(p) " + tag.getName() + "; ");
                } else {
                    tagString += (tag.getName() + "; ");
                }
            }
            else {
                if (tag.getType() == typePersistent) {
                    tagString += ("(p) " + tag.getName());
                } else {
                    tagString += (tag.getName());
                }
            }
        }

        return tagString;

    }

    private void writePaddedLine(String[] strings) {
        // Writes a single CSV line, padding the number of columns to fill the total column count

        int numStrings = strings.length;

        String[] stringsPadded = new String[mNumColumns];

        if (numStrings > mNumColumns) {
            throw new RuntimeException("Number of strings exceeded the tota number of columns");
        }

        for (int i = 0; i < mNumColumns; i++) {
            if (i < numStrings) {
                stringsPadded[i] = strings[i];
            }
        }

        mCsvWriter.writeNext(stringsPadded);

    }

    private void writeBlankLine(int numLines) {
        for (int n = 0; n < numLines; n++) {
            mCsvWriter.writeNext(new String[mNumColumns]);
        }
    }

}
