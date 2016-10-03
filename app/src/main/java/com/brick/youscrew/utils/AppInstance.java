package com.brick.youscrew.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import com.brick.youscrew.R;
import com.brick.youscrew.SettingsActivityFragment;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;

/**
 * Created by Brick on 16/03/16.
 */
public final class AppInstance {

    private static final String LOG_TAG = AppInstance.class.getSimpleName();

    private static final boolean FORCE_RESET = false;

    private static AppInstance sInstance;

    private static Context sApplicationContext;
    private static boolean sSetupInitialized = false;
    private static BackupHelper sBackupHelper;
    private static Bitmap sScrewImage;

    private AppInstance(Context appContext) {

        sInstance = this;

        sApplicationContext = appContext;

        sBackupHelper = new BackupHelper(sApplicationContext);

        sScrewImage = null; // clear; don't load until requested with getScrewImage()

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sApplicationContext);

        if (prefs.getBoolean("firstrun", true) || FORCE_RESET) {
            sSetupInitialized = false;
            Log.v(LOG_TAG, "First run detected; running setup tasks");
            runSetupTasks();
            prefs.edit().putBoolean("firstrun", false).commit();
        }

        runStartupChecks();

        sSetupInitialized = true;

    }

    public static AppInstance getInstance(Context appContext) {

        if (sInstance == null) {
            new AppInstance(appContext);
        }

        return sInstance;

    }

    public static BackupHelper getBackupHelper() {
        return sBackupHelper;
    }

    public static boolean getSetupInitialized() {
        return sSetupInitialized;
    }

    public static Context getAppContext() { return sApplicationContext; }

    public static void clear() {
        // Nullify the instance
        sInstance = null;
    }

    public static void runSetupTasks() {
        // Here are all tasks that must be run when the app runs for the first time

        // Delete the database and set up default tags

        TurnDbHelper.getInstance(sApplicationContext).close();

        TurnDbHelper.deleteDatabase(sApplicationContext);

        SQLiteDatabase db = TurnDbHelper.getInstance(sApplicationContext).getWritableDatabase();

        TurnDbUtils.tagSetDefaults(sApplicationContext);

        Cursor cursorGroup = TurnDbUtils.tagGroupGetEntries(db, null, null);
        cursorGroup.moveToFirst();

        Cursor cursorTag = TurnDbUtils.tagGetEntries(db, null, null);
        cursorTag.moveToFirst();

        Log.v(LOG_TAG, Integer.toString(cursorGroup.getCount()) + " tag groups in DB");
        Log.v(LOG_TAG, Integer.toString(cursorTag.getCount()) + " tags in DB");

        cursorGroup.close();
        cursorTag.close();

        restoreDefaultPrefs(sApplicationContext);

//        // Set up automatic backup scheduling
//        scheduleBackups(mApplicationContext);

    }

    public static void runStartupChecks() {
        // Runs every time the app starts

        // Check if Dropbox link is still valid
        if (!sBackupHelper.getDropboxApi().getSession().isLinked()) {
            sBackupHelper.unlinkDropbox();
        }


    }

    public static void restoreDefaultPrefs(Context context) {

        // Create a fresh shared prefs file
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();

        // Initialize the prefs
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);

        // Set some other non prefs not used in 'settings'
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();
        prefs.edit().putBoolean(res.getString(R.string.pref_key_db_dropbox_linked), false).commit();

    }

    public static Bitmap getScrewImage() {

        if (sScrewImage == null) {

            Log.v(LOG_TAG, "Decoding screw image...");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(sApplicationContext);

            Resources res = sApplicationContext.getResources();

            String prefKey = res.getString(R.string.pref_key_dial_image);

            int id;

            switch (Integer.parseInt(prefs.getString(prefKey, ""))) {

                case SettingsActivityFragment.DIAL_IMAGE_ALAN:
                    id = R.drawable.alan;
                    break;
                case SettingsActivityFragment.DIAL_IMAGE_HYPERDRIVE:
                    id = R.drawable.hyperdrive_screw2;
                    break;
                default:
                    throw new RuntimeException("Invalid screw image preference value");
            }

            sScrewImage = BitmapFactory.decodeResource(res, id);


        }

        return sScrewImage;

    }

    public static void invalidateScrewImage() {
        sScrewImage = null;
    }

}
