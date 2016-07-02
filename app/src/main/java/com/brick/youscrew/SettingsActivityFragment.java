package com.brick.youscrew;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.AppInstance;
import com.brick.youscrew.utils.BackupHelper;
import com.brick.youscrew.utils.BackupRestoreDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends PreferenceFragment {

    public static final int DIAL_IMAGE_ALAN = 0;
    public static final int DIAL_IMAGE_HYPERDRIVE = 1;

    public static final int TURN_REF_WORLD = 0;
    public static final int TURN_REF_SCREW = 1;

    private SharedPreferences mPrefs;

    private boolean mDefaultsInitialized = false;

    private static final String LOG_TAG = SettingsActivityFragment.class.getSimpleName();

    public static final int MAX_TETRODES = 40;

    private Context mContext;

    private DropboxAPI<AndroidAuthSession> mDbApi;

    private SwitchPreference mDoBackupPref;

    private SwitchPreference mDoDropboxBackupPref;

    private Preference mDropboxBackupRestorePref;

    private BackupHelper mBackupHelper;

    private MultiSelectListPreference mDefaultReferenceTetrodesPref;

    public SettingsActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mBackupHelper = AppInstance.getInstance(mContext).getBackupHelper();

        // Add the preferences and listeners
        createUi();

    }

    // Need to register the listener every time the fragment resumes
    @Override
    public void onResume() {
        super.onResume();

        Log.v(LOG_TAG, "onResume()");

        // Initialize the summaries once
        if(!mDefaultsInitialized) {

            String[] allPrefKeys = getSharedPrefKeys(getPreferenceScreen().getSharedPreferences());

            // For each preference on the screen, add a listener for changes
            for (final String prefKey : allPrefKeys) {
                addPrefSummaryListener(prefKey);
            }

            setAllPrefSummaries(mPrefs);
            mDefaultsInitialized = true;

        }

        // Check for the case in which we're resuming after having just authenticated dropbox
        if (mDbApi != null && mDbApi.getSession().authenticationSuccessful()) {
            try {
                mDbApi.getSession().finishAuthentication();

                String accessToken = mDbApi.getSession().getOAuth2AccessToken();

                // Save the access token to shared prefs
                mPrefs
                        .edit()
                        .putBoolean(getResources().getString(R.string.pref_key_db_dropbox_linked), true)
                        .putString(getResources().getString(R.string.pref_key_db_dropbox_access_token), accessToken)
                        .commit();

                // Enable the restore button
                mDropboxBackupRestorePref.setEnabled(true);

                // Update the backupHelper instance
                mBackupHelper.setDoDropboxBackups(true);

            }
            catch (IllegalStateException e) {
                // If it failed, switch the pref back off
                Log.i(LOG_TAG, "Error with Dropbox authentication");
                Toast.makeText(mContext, "Error with Dropbox authentication", Toast.LENGTH_LONG).show();
                mDoDropboxBackupPref.setChecked(false);
            }
        }

    }

    public void createUi() {

        // Add the XML prefs to the fragment
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.preferences);

        // Find the "edit tags" preference and set its click listener
        Resources res = getResources();
        Preference tagPref = findPreference(res.getString(R.string.pref_key_edit_tags));
        Preference tagRestorePref = findPreference(res.getString(R.string.pref_key_restore_default_tags));
        Preference backupRestorePref = findPreference(res.getString(R.string.pref_key_db_restore_backup));
        Preference resetAllPref = findPreference(res.getString(R.string.pref_key_reset_all));
        Preference dialImagePref = findPreference(res.getString(R.string.pref_key_dial_image));

        mDoDropboxBackupPref = (SwitchPreference) findPreference(res.getString(R.string.pref_key_db_dropbox_do_backup));
        mDoBackupPref = (SwitchPreference) findPreference(res.getString(R.string.pref_key_db_do_backups));
        mDropboxBackupRestorePref = findPreference(res.getString(R.string.pref_key_db_dropbox_restore));

        // Set the content of the default reference TTs pref (this is not stored in XML)
        mDefaultReferenceTetrodesPref = (MultiSelectListPreference) findPreference(res.getString(R.string.pref_key_tetrode_default_references));

        setDefaultReferenceTetrodeInitialState();

        final EditTextPreference defaultNumTetrodesPreference = (EditTextPreference) findPreference(res.getString(R.string.pref_key_tetrode_default_number));

        tagPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), TagSetupActivity.class);
                startActivity(intent);
                return true;
            }
        });

        tagRestorePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final Context context = getActivity();

                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setMessage("Do you really want to restore the default tag set? " +
                                "Any deleted tags associated with turn records will be lost!")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TurnDbUtils.tagSetDefaults(context);
                                Toast.makeText(context, "Default tag set restored", Toast.LENGTH_LONG);
                            }
                        });

                builder.create().show();

                return true;

            }
        });



        // Add listener to update the contents of the default reference tetrodes pref whenever the
        // default number of tetrodes is changed
        defaultNumTetrodesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                int newNumTetrodes = Integer.parseInt((String) newValue);

                // Check if the new value is allowed
                if (newNumTetrodes <= MAX_TETRODES) {
                    updateDefaultRefererenceTetrodePref(newNumTetrodes);
                    setPrefSummary(defaultNumTetrodesPreference, (String) newValue);
                    return true;
                } else {
                    Toast.makeText(getActivity(), "The maximum number of tetrodes allowed is "
                            + Integer.toString(MAX_TETRODES), Toast.LENGTH_LONG).show();
                    return false;
                }


            }
        });

        backupRestorePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                // Create the backup restore dialog, overriding its onDismiss method
                // such that the preference UI will refresh when it closes
                DialogFragment dialog = new BackupRestoreDialog() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        super.onDismiss(dialog);
                        createUi();
                    }
                };

                Bundle args = new Bundle();
                args.putBoolean(BackupRestoreDialog.ARG_USE_DROPBOX, false);
                dialog.setArguments(args);
                dialog.setTargetFragment(SettingsActivityFragment.this, BackupRestoreDialog.REQUEST_CODE);
                dialog.show(getFragmentManager(), "BackupRestoreDialog");
                return true;
            }
        });

        // If dropbox link isn't set up, disable the option to restore from DB
        boolean isDropboxLinked = mPrefs.getBoolean(res.getString(R.string.pref_key_db_dropbox_linked), false);
        if (!isDropboxLinked) {
            mDropboxBackupRestorePref.setEnabled(false);
        }

        // If backups are switched off, disable the option to back up to dropbox
//        boolean areBackupsOn = mPrefs.getBoolean(res.getString(R.string.pref_key_db_do_backups), false);
        boolean areBackupsOn = mDoBackupPref.isChecked();
        if (!areBackupsOn) {
            mDoDropboxBackupPref.setEnabled(false);
            mDoDropboxBackupPref.setChecked(false);
        }

        mDropboxBackupRestorePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Create the backup restore dialog, overriding its onDismiss method
                // such that the preference UI will refresh when it closes
                DialogFragment dialog = new BackupRestoreDialog() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        super.onDismiss(dialog);
                        createUi();
                    }
                };

                Bundle args = new Bundle();
                args.putBoolean(BackupRestoreDialog.ARG_USE_DROPBOX, true);
                dialog.setArguments(args);
                dialog.setTargetFragment(SettingsActivityFragment.this, BackupRestoreDialog.REQUEST_CODE);
                dialog.show(getFragmentManager(), "BackupRestoreDialog");
                return true;
            }
        });

        mDoBackupPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                final Resources res = getResources();

                // Get the time of the last restore (if there has been one)
                final long timeLastRestore = mPrefs.getLong(res.getString(R.string.pref_key_db_last_restore_time), -1);

                // Determine if backups already been resumed since the last restore
                final boolean backupsHaveBeenResumed = mPrefs.getBoolean(res.getString(R.string.pref_key_db_backups_resumed), false);

                final String timeLastRestoreString = new SimpleDateFormat(BackupHelper.DATE_FORMAT).format(new Date(timeLastRestore));


                // The new pref value will be approved UNLESS it's being switched on for the first
                // time following a restore
                boolean changeValue = true;

                Log.v(LOG_TAG, "Backup pref changed: new value = " + newValue.toString() +
                        ", last restore time = " + Long.toString(timeLastRestore));

                // If a restore has been performed, warn about deletion of future backup states
                if ((boolean) newValue == true && !backupsHaveBeenResumed && timeLastRestore != -1) {

                    changeValue = false;

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext)

                            .setMessage("Turning backups on again will delete any backups with timestamps" +
                                    " after the last restore point (" + timeLastRestoreString +
                                    ") . Press 'OK' to proceed and delete.")

                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    // Delete all backups after the restore point
                                    // (on a separate thread)
                                    new Handler().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mBackupHelper.deleteAllBackupsAfterTime(timeLastRestore, false);
                                        }
                                    });

                                    dialog.dismiss();

                                    // Update the saved pref values to indate that backups have been
                                    // resumed.
                                    mPrefs.edit()
                                            .putBoolean(res.getString(R.string.pref_key_db_do_backups), true)
                                            .putBoolean(res.getString(R.string.pref_key_db_backups_resumed), true)
                                            .commit();

                                    // Refresh the UI; this will update the preference with the
                                    // appropriate value according to the user choice in the dialog
                                    createUi();

                                }
                            })

                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                    builder.create().show();

                }

                if (changeValue) {
                    // If being switched on, enable the dropbox backup pref
                    if ((boolean) newValue == true) {
                        mDoDropboxBackupPref.setEnabled(true);

                        // Tell the existing backup helper to start doing dropbox backups
                        mBackupHelper.setDoBackups(true);
                    }
                    // If being switched off, swich off dropbox backups and disable the switch
                    else {
                        mDoDropboxBackupPref.setEnabled(false);
                        mDoDropboxBackupPref.setChecked(false);

                        // Tell the existing backup helper to stop doing dropbox backups
                        mBackupHelper.setDoBackups(false);
                    }
                }

                return changeValue;

            }
        });

        mDoDropboxBackupPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                // If dropbox backups have been switched ON, check if the account has been authenticated
                if ((Boolean) newValue == true) {

                    Resources res = getResources();
                    boolean isLinked = mPrefs.getBoolean(res.getString(R.string.pref_key_db_dropbox_linked), false);

                    // If not, perform authentication.
                    if (!isLinked) {
                        mDbApi = mBackupHelper.getDropboxApi();
                        mDbApi.getSession().startOAuth2Authentication(getActivity());
                    }
                    else {
                        mBackupHelper.setDoDropboxBackups(true);
                    }

                }
                else {

                    mBackupHelper.setDoDropboxBackups(false);

                }


                return true;
            }
        });

        resetAllPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setMessage("Are you sure you want to reset the app? All database contents " +
                                "and settings will be lost")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AppInstance.runSetupTasks();
                            }
                        });

                builder.create().show();

                return true;

            }
        });

        dialImagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                // Invalidate the AppInstance image
                AppInstance.invalidateScrewImage();

                // Update the summary to the new value
                setPrefSummary(preference, newValue.toString());
                ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();

                return true;

            }
        });

    }

    // ... and unregister it every time it pauses
    @Override
    public void onPause() {
        super.onPause();
        Log.v(LOG_TAG, "onPause()");
    }

    private void setPrefSummary(Preference pref, String newValue) {
        // Set a preference's summary according to its current content, OR
        // with the supplied text

        CharSequence[] entries;
        int index;

        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;

            if (newValue == null) {
                pref.setSummary(listPref.getEntry());
            }
            else {
                entries = ((ListPreference) pref).getEntries();
                index = Integer.parseInt(newValue);
                pref.setSummary(entries[index].toString());
            }
        }

        else if (pref instanceof  EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) pref;

            if (newValue == null) {
                pref.setSummary(editTextPref.getText());
            }
            else {
                pref.setSummary(newValue);
            }
        }

    }

    private String[] getSharedPrefKeys(SharedPreferences prefs) {

        Map<String,?> allPrefs = prefs.getAll();
        String[] prefKeys = new String[allPrefs.size()];

        int p = 0;

        for(Map.Entry<String,?> entry : allPrefs.entrySet()){
            prefKeys[p++] = entry.getKey();
        }

        return prefKeys;

    }

    private void setAllPrefSummaries(SharedPreferences prefs) {

        // Get all shared prefs
        Map<String,?> allPrefs = prefs.getAll();

        // Iterate through, setting the summaries of all list / edittext prefs
        for(Map.Entry<String,?> entry : allPrefs.entrySet()){
            Preference pref = findPreference(entry.getKey());
            setPrefSummary(pref, null);
        }

    }

    private void setDefaultReferenceTetrodeInitialState() {
        // The default ref tetrode pref entries and entryvalues are not stored in XML, so they must
        // be set here.
        Resources res = getResources();

        int numTts = Integer.parseInt(mPrefs.getString(res.getString(R.string.pref_key_tetrode_default_number), "0"));

        String[] entryValues = new String[numTts];
        String[] entries = new String[numTts];
        for (int t = 0; t < numTts; t++) {
            entryValues[t] = Integer.toString(t);
            entries[t] = Integer.toString(t+1);
        }

        mDefaultReferenceTetrodesPref.setEntries(entries);
        mDefaultReferenceTetrodesPref.setEntryValues(entryValues);

    }

    public void updateDefaultRefererenceTetrodePref(int numTetrodes) {

        Log.v(LOG_TAG, "Num tetrodes pref change listener triggered");
        // At initialization, or

        Log.v(LOG_TAG, Integer.toString(numTetrodes) + "tetrodes...");

        String[] entryValues = new String[numTetrodes];
        String[] entries = new String[numTetrodes];

        for (int t = 0; t < numTetrodes; t++) {
            entryValues[t] = Integer.toString(t);
            entries[t] = Integer.toString(t + 1);
        }

        HashSet<String> defaultValues = new HashSet<>();

        if (numTetrodes > 2) {
            defaultValues.add(Integer.toString(numTetrodes-1));
            defaultValues.add(Integer.toString(numTetrodes-2));
        }

        // Set the values of the default reference tetrodes multisect pref
        mDefaultReferenceTetrodesPref.setEntryValues(entryValues);
        mDefaultReferenceTetrodesPref.setEntries(entries);
        mDefaultReferenceTetrodesPref.setValues(defaultValues);

    }

    private void updateAllPrefs(PreferenceGroup group) {
        if (group instanceof PreferenceScreen) {
            BaseAdapter adapter = (BaseAdapter) ((PreferenceScreen) group).getRootAdapter();
            adapter.notifyDataSetChanged();
        }
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                updateAllPrefs((PreferenceGroup) pref);
            }
        }

    }

    private void addPrefSummaryListener(final String prefKey) {

        Preference pref = findPreference(prefKey);

        // First check that pref was found
        if (pref == null || pref.getOnPreferenceChangeListener() != null) { return; }

        // If the pref is of editText or list type, add a listener
        if (pref instanceof EditTextPreference || pref instanceof ListPreference) {


            Log.v(LOG_TAG, "Adding pref change listener for '" + prefKey + "'");

            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.v(LOG_TAG, "Pref change listener triggered for '" + prefKey + "'");
                    setPrefSummary(preference, newValue.toString());
                    ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
                    return true;
                }
            });

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Popups may change the sharedprefs values, so we redraw the UI every time
        // we're notified of a result
        if (requestCode == BackupRestoreDialog.REQUEST_CODE) {
            createUi();
        }
    }

}
