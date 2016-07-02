package com.brick.youscrew.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AppKeyPair;
import com.brick.youscrew.R;
import com.brick.youscrew.data.TurnDbHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Brick on 11/03/16.
 */
public final class BackupHelper {


    private static final long HOUR = 60*60;
    private static final long DAY = HOUR*24;
    private static final long MONTH = DAY*30;
    private static final long YEAR = DAY*365;

    public static final long[] BACKUP_INTERVAL = new long[] {60*5, HOUR, DAY};


    public static final long[] BACKUP_LIMIT = new long[] {HOUR, DAY, YEAR};

//    public static final int[] BACKUP_INTERVAL = new int[] {5, 20, 60, 60*5}; // up to the first 10 mins
//
//    public static final int[] BACKUP_LIMIT = new int[] {20, 60, 60*5, 60*60};

    public static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    public static final String PREF_KEY_DB_LINKED = "dropbox_linked";

    private static final String DROPBOX_APP_KEY = "h3szcn1uoqs1s2v";

    private static final String DROPBOX_APP_SECRET = "xkptfp29va8xdm0";

    private static final String LOG_TAG = BackupHelper.class.getSimpleName();

    private static long sLastBackupCheckTime = -1;

    private Context mContext;

    private boolean mDoBackups;

    private boolean mDropboxLinked;

    private boolean mDropboxDoBackups;

    private String mDropboxAccessToken;

    private DropboxAPI<AndroidAuthSession> mDBApi;

    public BackupHelper(Context context) {

        mContext = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Resources res = mContext.getResources();

        // Get the current values for the shared preferences determining whether backups should occur
        mDoBackups = prefs.getBoolean(res.getString(R.string.pref_key_db_do_backups), false);
        mDropboxLinked = prefs.getBoolean(res.getString(R.string.pref_key_db_dropbox_linked), false);
        mDropboxDoBackups = prefs.getBoolean(res.getString(R.string.pref_key_db_dropbox_do_backup), false);
        mDropboxAccessToken = prefs.getString(res.getString(R.string.pref_key_db_dropbox_access_token), "");

        AppKeyPair appKeys = new AppKeyPair(DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
        mDBApi = new DropboxAPI<>(new AndroidAuthSession(appKeys));

        // If shared prefs indicate that a dropbox link is already established, use the access token
        // to authenticate it
        if (mDropboxLinked) {
            mDBApi.getSession().setOAuth2AccessToken(mDropboxAccessToken);
        }

    }

    public DropboxAPI getDropboxApi() {
        return mDBApi;
    }

    /**
     * This method is called to notify the helper to start/stop backing up to dropbox.  Since a new
     * instance is only created on app startup, this method must be called for any changes in the
     * settings to have an immediate effect
     * @param value
     */
    public void setDoDropboxBackups(boolean value) {

        mDropboxDoBackups = value;

        if (value == true) {
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    copyLocalBackupsToDropbox();
                }
            };
            new Thread(runnable).start();
        }

    }

    /**
     * This method is called to notify the helper to start/stop backing up.  Must be called from
     * settings when changed
     * @param value
     */

    public void setDoBackups(boolean value) {
        mDoBackups = value;
    }

    public void notifyDatabaseChanged() {
        // Only back up if initialization is finished
        if ( AppInstance.getSetupInitialized() && mDoBackups) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runBackupProcess();
                }
            }).start();
        }
    }

    public boolean runBackupProcess() {
        // On every call, this method checks if a new backup is needed and makes one if so.

        if (isBackupNeeded()) {

            String fileName = makeBackupFileName();
            String filePath = getDatabaseDirectory() + fileName;

            Log.v(LOG_TAG, "Backing up, file = " + fileName);

            saveBackup(fileName, false);

            if (mDropboxDoBackups) {
                Log.v(LOG_TAG, "Backing up to dropbox, file path = " + fileName);
                saveBackup(fileName, true);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            Resources res = mContext.getResources();

            prefs.edit()
                    .putLong(res.getString(R.string.pref_key_db_last_backup_time), System.currentTimeMillis())
                    .putString(res.getString(R.string.pref_key_db_checksum), getMd5Checksum(filePath))
                    .commit();

            // Scan through all backups, deleting unnecessary old ones
            deleteUnnecessaryBackups(false);

            return true;

        }
        else {
//            Log.v(LOG_TAG, "No backup needed");
            return false;
        }
    }

    private boolean isBackupNeeded() {
        // New version, only checks whether minimum time interval has passed since the last backup

        long currentTime = System.currentTimeMillis();

        if (sLastBackupCheckTime == -1) {
            sLastBackupCheckTime = currentTime;
            return true;
        }

        else if ( (currentTime-sLastBackupCheckTime)/1000l > BACKUP_INTERVAL[0] ) {
            sLastBackupCheckTime = currentTime;
            return true;
        }

        else {
            return false;
        }

    }

    public String getDatabaseDirectory() {
        return "/data/data/com.brick.youscrew/databases/";
    }

    private String getDatabaseFilePath() {
        return getDatabaseDirectory() + TurnDbHelper.DATABASE_NAME;
    }

    private String getBackupBaseName() {

        String dbName = TurnDbHelper.DATABASE_NAME;

        return dbName.substring(0, dbName.length() - 3) + "_backup_" ;

    }

    private String makeBackupFileName() {

        Date date = new Date(System.currentTimeMillis());

        String dateString = new SimpleDateFormat(DATE_FORMAT).format(date);

        String fileName = getBackupBaseName() + dateString + ".db";

        return fileName;

    }

    private boolean saveBackup(String targetFileName, boolean toDropbox) {

        String filePathToCopy = getDatabaseFilePath();

        if (toDropbox) {

            try {

                File sourceFile = new File(filePathToCopy);
                Log.v(LOG_TAG, "Transferring file of size " + Long.toString(sourceFile.length()) + " bytes" );

                FileInputStream inputStream = new FileInputStream(sourceFile);

                DropboxAPI.Entry response = mDBApi.putFile(
                        targetFileName,
                        inputStream,
                        sourceFile.length(),
                        null,
                        null);

                inputStream.close();

            }
            catch (DropboxUnlinkedException e) {
                e.printStackTrace();
                unlinkDropbox();
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;

        }

        else {

            String targetFilePath = getDatabaseDirectory() + targetFileName;

            try {
                FileInputStream inputStream = new FileInputStream(filePathToCopy);
                FileOutputStream outputStream = new FileOutputStream(targetFilePath, false);

                while (true) {
                    int i = inputStream.read();
                    if (i != -1) {
                        outputStream.write(i);
                    } else {
                        break;
                    }
                }

                outputStream.flush();

                outputStream.close();
                inputStream.close();

                return true;


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "File not found");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "IO error while backing up");
            }

            return false;

        }

    }

    private boolean deleteFromDropbox(String fileTarget) {
        try {
            mDBApi.delete(fileTarget);
            return true;
        }
        catch (DropboxUnlinkedException e) {
            e.printStackTrace();
            unlinkDropbox();
            return false;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<File> findAllBackupFiles(boolean fromDropbox) {
        // Returns an array of all files matching the backup base filename,
        // sorted by ASCENDING filename (i.e. from earliest to latest)

        final List<File> allFiles = new ArrayList<>();

        final String backupBaseName = getBackupBaseName();

        if (fromDropbox) {
            // Do network task
            try {

                DropboxAPI.Entry entries = mDBApi.metadata("/", 10000, null, true, null);

                List<DropboxAPI.Entry> entryList = entries.contents;

                for (DropboxAPI.Entry ent : entryList) {
                    Log.v(LOG_TAG, "Checking file '" + ent.fileName() + "'");
                    if (ent.fileName().startsWith(backupBaseName)) {
                        allFiles.add(new File(ent.fileName()));
                    }
                }

            }
            catch (DropboxUnlinkedException e) {
                e.printStackTrace();
                unlinkDropbox();
            }
            catch (DropboxException e) {
                e.printStackTrace();
            }
        }
        else {

            File folder = new File(getDatabaseDirectory());

            Log.v(LOG_TAG, "Searching for all files in directory " + folder.getPath());

            File[] files = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.startsWith(backupBaseName);
                }});

            allFiles.addAll(Arrays.asList(files));

        }

        Collections.sort(allFiles);

        Log.v(LOG_TAG, "Files found = " + Integer.toString(allFiles.size()));

        return allFiles;

    }

    private long parseBackupTime(String fileName) {
        // Takes a backup filename and gets the long time in milliseconds

        DateFormat format = new SimpleDateFormat(DATE_FORMAT);

        String[] fileNameParts = fileName.split("_backup_");

        try {
            Date date = format.parse(fileNameParts[1]);
            return date.getTime();
        }
        catch(Exception e) {
            e.printStackTrace();
//            Toast.makeText(mContext, "Error parsing time from backup file name", Toast.LENGTH_LONG).show();
            return -1;
        }

    }

    public boolean deleteAllBackups(boolean useDropbox) {
        // Delete all backup files

        List<File> allFiles = findAllBackupFiles(useDropbox);

        boolean allSucceeded = true;
        boolean succeeded;

        for (int f = 0; f < allFiles.size(); f++) {

            File file = allFiles.get(f);

            if (useDropbox) {
                succeeded = deleteFromDropbox(file.getName());
            }
            else {
                succeeded = allFiles.get(f).delete();
            }

            if (succeeded) {
//                Log.v(LOG_TAG, "Deleted " + allFiles[f].getName());
            }
            else {
                allSucceeded = false;
                Log.e(LOG_TAG, "Failed to delete " + allFiles.get(f).getName());
            }

        }

        return allSucceeded;

    }

    public void copyLocalBackupsToDropbox() {

        List<File> allBackups = findAllBackupFiles(false);

        InputStream inputStream;

        try {
            for (File file : allBackups) {

                inputStream = new FileInputStream(file);
                mDBApi.putFile(file.getName(), inputStream, file.length(), null, null);
                inputStream.close();

            }
        }
        catch (DropboxUnlinkedException e) {
            e.printStackTrace();
            unlinkDropbox();
        }
        catch (Exception e) {
            e.printStackTrace();
//            Toast.makeText(mContext, "Error copying local backup to dropbox", Toast.LENGTH_LONG).show();
        }

    }

    public void updateLastBackupPrefValue() {

        List<File> allFiles = findAllBackupFiles(false);

        long lastBackupTime;

        if (allFiles.size() == 0) {
            lastBackupTime = -1;
        }
        else {
            lastBackupTime = parseBackupTime(allFiles.get(allFiles.size()-1).getName());
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Resources res = mContext.getResources();
        prefs.edit()
                .putLong(res.getString(R.string.pref_key_db_last_backup_time), lastBackupTime)
                .commit();

    }

    public int deleteAllBackupsAfterTime(long time, boolean useDropbox) {
        // To be called after automatic backups are switched back on following
        // a restore. Don't call this in the UI thread!

        List<File> allFiles = findAllBackupFiles(useDropbox);

        long currentBackupTime;

        int numDeleted = 0;

        boolean succeeded;

        for(File file : allFiles) {
            currentBackupTime = parseBackupTime(file.getName());
            if (currentBackupTime > time) {
                if (useDropbox) {
                    succeeded = deleteFromDropbox(file.getName());
                }
                else {
                    succeeded = file.delete();
                }

                if (succeeded) {
                    numDeleted++;
                }
            }
        }

        updateLastBackupPrefValue();

        return numDeleted;

    }

    public boolean restoreBackup(String fileName, boolean fromDropbox) {

        String dbFilePath = getDatabaseFilePath();

        // Close any open databases
        TurnDbHelper dbHelper = TurnDbHelper.getInstance(mContext);
        dbHelper.close();

        long fileSizeSource, fileSizeTarget;

        try {

            // Initialize ouput stream
            OutputStream outputStream = new FileOutputStream(dbFilePath, false);

            if (fromDropbox) {
                DropboxAPI.DropboxFileInfo info = mDBApi.getFile(fileName, null, outputStream, null);
                fileSizeSource = info.getFileSize();
            }

            else {

                String sourceFilePath = getDatabaseDirectory() + fileName;

                fileSizeSource = new File(sourceFilePath).length();

                // Initialize input stream
                InputStream inputStream = new FileInputStream(sourceFilePath);

                // Do the file transfer
                while (true) {
                    int i = inputStream.read();
                    if (i != -1) {
                        outputStream.write(i);
                    } else {
                        break;
                    }
                }

                inputStream.close();

            }

            outputStream.close();

            fileSizeTarget = new File(dbFilePath).length();

            if (fileSizeSource == fileSizeTarget) {

                // Enter the time of the restored backup into shared prefs
                long restoreTime = parseBackupTime(fileName);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                Resources res = mContext.getResources();
                prefs.edit()
                        .putBoolean(res.getString(R.string.pref_key_db_do_backups), false) // turn off auto-backups until the user reactivates
                        .putBoolean(res.getString(R.string.pref_key_db_backups_resumed), false) // indicate that backups have not yet resumed
                        .putLong(res.getString(R.string.pref_key_db_last_restore_time), restoreTime) // save the last restore time
                        .commit();

                // Check that the restored file matches the size of the source backup file
                return true;
            }
            else {
                return false;
            }

        }

        //
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    private String getMd5Checksum(String filePath) {

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    messageDigest.update(buffer, 0, numRead);
                }
            }
            byte[] mD5Bytes = messageDigest.digest();
            return convertHashToString(mD5Bytes);
        }
        catch (Exception e) {
            e.printStackTrace();

//            Toast.makeText(mContext, "", Toast.LENGTH_LONG).show();
            return null;
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (Exception e) {}
            }
        }

    }

    private static String convertHashToString(byte[] mD5Bytes) {

        String returnVal = "";

        for (int i = 0; i < mD5Bytes.length; i++) {
            returnVal += Integer.toString(( mD5Bytes[i] & 0xff ) + 0x100, 16).substring(1);
        }

        return returnVal;

    }

    private int deleteUnnecessaryBackups(boolean fromDropbox) {
        // To be called each time a new backup is made. This method runs through all
        // existing backups, deleting any that falls within the minimum time interval

        List<File> allFiles = findAllBackupFiles(fromDropbox);
        int numFiles = allFiles.size();

        long currentTime = System.currentTimeMillis();
        long currentBackupTime, secondsFromPresent, secondsSinceLatestBackup;

        long minimumTimeInterval;

        int backupPeriodIndex = BACKUP_LIMIT.length-1;

        int numDeleted = 0;

        boolean forceKeep = true; // always keep the first backup

        long lastValidBackupTime = 0;

        // For each file
        for (int f = 0; f < numFiles; f++) {

            // Determine the backup's time relative to the present
            currentBackupTime = parseBackupTime(allFiles.get(f).getName());

            secondsFromPresent = (currentTime - currentBackupTime) / 1000;

            // If the backup's time from present is within the span of the next backup
            // period, we need to locate the appropriate period
            if (backupPeriodIndex > 0 && secondsFromPresent < BACKUP_LIMIT[backupPeriodIndex - 1]) {

                // Scan back through the array until we find the right period
                while (backupPeriodIndex > 0 && BACKUP_LIMIT[backupPeriodIndex - 1] > secondsFromPresent) {
                    backupPeriodIndex--;
                }

                // When moving on to a new backup period, always keep the first backup
                forceKeep = true;

            }

            secondsSinceLatestBackup = (long) Math.ceil((double) (currentBackupTime - lastValidBackupTime) / 1000.0);

            minimumTimeInterval = BACKUP_INTERVAL[backupPeriodIndex];

            // Check if the backup's time occurs within the minimum interval.
            // If it does, delete it.
            if (!forceKeep && secondsSinceLatestBackup < minimumTimeInterval) {

                if (fromDropbox) {
                    deleteFromDropbox(allFiles.get(f).getName());
                } else {
                    allFiles.get(f).delete();
                }
//                Log.v(LOG_TAG, "Backup deleted: " + allFiles[f].getName() + " within " +
//                Long.toString(secondsSinceLatestBackup) + " seconds of previous backup");
                numDeleted++;

            } else {
                lastValidBackupTime = currentBackupTime;
            }

            forceKeep = false;

        }

        return numDeleted;

    }

    public void unlinkDropbox() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Resources res = mContext.getResources();

        prefs.edit().putBoolean(res.getString(R.string.pref_key_db_dropbox_linked), false)
                .putBoolean(res.getString(R.string.pref_key_db_dropbox_do_backup), false)
                .putString(res.getString(R.string.pref_key_db_dropbox_access_token), "")
                .commit();

    }

    public Context getContext() {
        return mContext;
    }

    public static class UploadToDropboxTask extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... strings) {

            String sourceFilePath = strings[0];
            String targetFileName = strings[1];

            try {
                File file = new File(sourceFilePath);
                InputStream inputStream = new FileInputStream(file);

                BackupHelper helper = AppInstance.getInstance(null).getBackupHelper();

                helper.getDropboxApi().putFile(targetFileName, inputStream, file.length(), null, null);

                return true;
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

    }

}
