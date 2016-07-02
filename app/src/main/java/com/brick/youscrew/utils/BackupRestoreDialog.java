package com.brick.youscrew.utils;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
//import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brick.youscrew.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Brick on 11/03/16.
 */
public class BackupRestoreDialog extends DialogFragment{

    private static final String LOG_TAG = BackupRestoreDialog.class.getSimpleName();

    public static final String ARG_USE_DROPBOX = "use_dropbox";

    public static final int REQUEST_CODE = 1;

    public static final int RESULT_OK = 1;

    private Context mContext;
    private View mRootView;
    private ListView mListView;
    private ArrayAdapter mListViewAdapter;
    private ArrayList<String> mFileNameList;
    private List<File> mFileList;
//    private File[] mFiles;
    private boolean mUseDropbox;
    private BackupHelper mBackupHelper;
    private int mSelectionIndex;
    private boolean mDialogEnabled;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDialogEnabled = true;

        mContext = getActivity();

        mBackupHelper = AppInstance.getInstance(mContext).getBackupHelper();

        Bundle args = getArguments();
        mUseDropbox = args.getBoolean(ARG_USE_DROPBOX);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_backup_restore_dialog, container);

        mListView = (ListView) mRootView.findViewById(R.id.list_view_backup);

        mFileNameList = new ArrayList<>();

        mListViewAdapter = new ArrayAdapter(mContext, R.layout.list_item_backup, R.id.list_item_textview, mFileNameList) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(R.id.list_item_textview);
                textView.setTextColor(Color.BLACK);

                if (position == mSelectionIndex) {
                    view.setBackgroundColor(Color.GRAY);
                }
                else {
                    view.setBackgroundColor(Color.LTGRAY);
                }

                return view;

            }

        };

        mListView.setAdapter(mListViewAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                mSelectionIndex = position;
                mListViewAdapter.notifyDataSetChanged();
            }
        });

        View buttonOk = mRootView.findViewById(R.id.button_ok);
        View buttonCancel = mRootView.findViewById(R.id.button_cancel);

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setMessage("Are you sure you wish to restore this backup?")
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, int which) {

                                if (mDialogEnabled == true) {

                                    // Some reassuring toast
                                    Toast.makeText(mContext, "Please wait...", Toast.LENGTH_SHORT).show();

                                    // Get the path of the selected file and attempt to restore
                                    final String fileName = mFileList.get(mSelectionIndex).getName();
                                    Log.v(LOG_TAG, "Attempting to restore backup " + fileName);

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                            final boolean succeeded = mBackupHelper.restoreBackup(fileName, mUseDropbox);

                                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                                            Resources res = mContext.getResources();

                                            // Set shared prefs to NOT continue with auto backups
                                            prefs.edit()
                                                    .putBoolean(res.getString(R.string.pref_key_db_do_backups), false)
                                                    .putBoolean(res.getString(R.string.pref_key_db_dropbox_do_backup), false)
                                                    .commit();

                                            final String dialogText;

                                            if (succeeded) {
                                                dialogText = "The backup was successfully restored. Automatic backups have been " +
                                                        "disabled. Check you are sure you want to keep the restored state, since " +
                                                        "any backups made after the restored state will be deleted.";
                                            }
                                            else {
                                                dialogText = "There was an error restoring the backup. Please try again.";
                                            }

                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                                                            .setMessage(dialogText)
                                                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    dialog.dismiss();
                                                                }
                                                            });
                                                    builder.create().show();
                                                }
                                            });

                                            // Notify the target fragment that the restore has completed
                                            getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, null);

                                        }
                                    }).start();

                                    BackupRestoreDialog.this.getDialog().dismiss();

                                }

                                mDialogEnabled = false;

                            }
                        });

                builder.create().show();

            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogEnabled == true) {
                    mDialogEnabled = false;
                    getDialog().dismiss();
                }
            }
        });

        getFileList();

        return mRootView;

    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void getFileList() {
        if (mUseDropbox) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    mFileList = mBackupHelper.findAllBackupFiles(mUseDropbox);
                    Collections.reverse(mFileList);
                    makeListViewStrings();
                }
            };

            new Thread(runnable).start();

        }
        else {
            mFileList = mBackupHelper.findAllBackupFiles(false);
            Collections.reverse(mFileList);
            makeListViewStrings();
        }

    }

    private void makeListViewStrings() {

        mFileNameList.clear();

        for (File file : mFileList) {
            mFileNameList.add(file.getName());
        }

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mListViewAdapter.notifyDataSetChanged();
            }
        });

    }


}
