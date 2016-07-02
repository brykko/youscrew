package com.brick.youscrew;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.Tetrode;
import com.brick.youscrew.data.TurnContract;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class EditRatDialog extends DialogFragment {

    private final String LOG_TAG = EditRatDialog.class.getSimpleName();

    private View mRootView;
    private EditText mEditName;
    private EditText mEditCode;
    private Spinner mEditNumTts;
    private EditText mEditMicrons;
    private Spinner mEditThreadDir;
    private SQLiteDatabase mDb;
    private CheckBox mEditTetrodesIndependent;

//    private long mRatId;
    private Rat mRat;
    private int mRecordMode;

    private Context mContext;

    public EditRatDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Open the database
        mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();

        mContext = getContext();

        Bundle args = getArguments();

        mRecordMode = args.getInt("recordMode");

        if (mRecordMode == TurnDbUtils.RECORD_EDIT) {
            mRat = args.getParcelable("rat");
        }
        else if (mRecordMode == TurnDbUtils.RECORD_NEW) {
            mRat = new Rat();
        }

        setUpUi(inflater, container);

        setDialogFieldValues();

        setOkClickListener();

        return mRootView;

    }

    private boolean valuesUnset() {

        // Helper to check if any UI fields remain unset

        boolean unsetName = mEditName.getText().toString().isEmpty();
        boolean unsetID = mEditCode.getText().toString().isEmpty();
        boolean unsetMicrons = mEditMicrons.getText().toString().isEmpty();
        boolean unsetNumTTs = mEditNumTts.getSelectedItem().toString().isEmpty();

        return unsetName || unsetID || unsetMicrons || unsetNumTTs;

    }

    private void setUpUi(LayoutInflater inflater, ViewGroup container) {

        // Inflate the layout
        mRootView = inflater.inflate(R.layout.fragment_dialog_edit_rat, container, false);

        // Find all of the view objects containing the user-input data
        mEditName = (EditText) mRootView.findViewById(R.id.new_rat_input_name);
        mEditCode = (EditText) mRootView.findViewById(R.id.new_rat_input_id);
        mEditNumTts = (Spinner) mRootView.findViewById(R.id.new_rat_input_num_tetrodes);
        mEditMicrons = (EditText) mRootView.findViewById(R.id.new_rat_input_um_turn);
        mEditThreadDir = (Spinner) mRootView.findViewById(R.id.new_rat_input_thread_dir);
        mEditTetrodesIndependent = (CheckBox) mRootView.findViewById(R.id.new_rat_checkbox_tt_independent);


        // Make an arrayAdapter for the TT number spinner
        ArrayList<String> ttNumList = new ArrayList<>();
        for(int i=1; i<=32; i++) {
            ttNumList.add(Integer.toString(i));
        }
        ArrayAdapter<String> ttNumListAdapter = new ArrayAdapter<>(
                getContext(), R.layout.simple_spinner_item, ttNumList);

        mEditNumTts.setAdapter(ttNumListAdapter);

        // Make an arrayAdapter for the the screw thread dir spinner
        ArrayList<String> screwDirList = new ArrayList<>();
        screwDirList.add("CW");
        screwDirList.add("CCW");

        ArrayAdapter<String> screwDirListAdapter = new ArrayAdapter<>(
                getContext(), R.layout.simple_spinner_item, screwDirList);

        mEditThreadDir.setAdapter(screwDirListAdapter);
        
    }

    private void setDialogFieldValues() {

        if (mRecordMode == TurnDbUtils.RECORD_NEW) {
            mEditNumTts.setSelection(getResources().getInteger(R.integer.default_num_tetrodes) - 1); // Default to 14TT
            mEditThreadDir.setSelection(getResources().getInteger(R.integer.default_thread_dir)); // Default to CCW
            mEditTetrodesIndependent.setChecked(getResources().getInteger(R.integer.default_tt_independent) == Rat.TETRODES_INDEPENDENT_YES);
        }

        else if (mRecordMode == TurnDbUtils.RECORD_EDIT) {
            
            mEditNumTts.setSelection(mRat.getNumTetrodes() - 1);
            mEditThreadDir.setSelection(mRat.getScrewThreadDir() - 1);
            mEditCode.setText(mRat.getCode());
            mEditName.setText(mRat.getName());
            mEditMicrons.setText( Double.toString(mRat.getTurnMicrometers()) );
            mEditTetrodesIndependent.setChecked(mRat.getInt(TurnContract.RatEntry.COLUMN_TETRODES_INDEPENDENT) == Rat.TETRODES_INDEPENDENT_YES);

            // Prevent the user from modifying certain values
            mEditNumTts.setEnabled(false);
            mEditMicrons.setEnabled(false);
            mEditThreadDir.setEnabled(false);
            mEditTetrodesIndependent.setEnabled(false);

        }

    }

    private void setOkClickListener() {

        // Set the click listener for the OK button
        Button buttonOK = (Button) mRootView.findViewById(R.id.new_rat_button_ok);
        buttonOK.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Check if any UI fields are unset (if so, return)
                if (valuesUnset() == true) {
                    Toast.makeText(mContext, "All fields must be set.", Toast.LENGTH_LONG).show();
                    return;
                }

                updateRatFromUiFields();

                // Create a new record in the rat table
                if (mRecordMode == TurnDbUtils.RECORD_NEW) {

                    // Try to create the new record. This may fail if the user chooses a name or
                    // code that has already been used
                    try {
                        if (!mRat.existsInDb(mDb)) {
                            mRat.writeToDb(mDb);
                        }
                        else {
                            throw new RuntimeException("Dialog is in 'RECORD_NEW' mode but rat already exists in database");
                        }
                    }
                    catch (SQLiteConstraintException e) {
                        e.printStackTrace();
                        Toast.makeText(mContext, "The rat must have a unique name ane code", Toast.LENGTH_LONG).show();
                        return;
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                    int defaultNumTts = Integer.parseInt(prefs.getString(getResources().getString(R.string.pref_key_tetrode_default_number), "0"));

                    Log.v(LOG_TAG, "Default num TTs = " + Integer.toString(defaultNumTts));

                    Set<String> defaultRefs = new HashSet<>();
                    String prefKey = getResources().getString(R.string.pref_key_tetrode_default_references);
                    defaultRefs = prefs.getStringSet(prefKey, defaultRefs );

                    Log.v(LOG_TAG, "Default ref TTs = " + defaultRefs.toString());

                    // Create a record for each TT
                    for (int i = 0; i < mRat.getNumTetrodes(); i++) {

                        String ttName = "TT" + Integer.toString(i + 1);
                        int ttColour = getResources().getColor(R.color.colorTTListItemDefault);

                        // If the number of tetrodes in the rat is the same as the default
                        // in the settings, then apply the default
                        int referenceCode;

                        if (mRat.getNumTetrodes() == defaultNumTts && defaultRefs.contains(Integer.toString(i)) ) {
                            referenceCode = Tetrode.REFERENCE_YES;
                        }
                        else {
                            referenceCode = Tetrode.REFERENCE_NO;
                        }

//                        TurnDbUtils.tetrodeAddEntry(mDb, mRat.getId(), i, ttName, ttColour, referenceCode);

                        // Create a new TT object, set the vital fields
                        Tetrode tetrode = mRat.createTetrode();

                        tetrode.set(TurnContract.TetrodeEntry.COLUMN_INDEX, i);
                        tetrode.set(TurnContract.TetrodeEntry.COLUMN_NAME, ttName);
                        tetrode.set(TurnContract.TetrodeEntry.COLUMN_COLOUR, ttColour);
                        tetrode.set(TurnContract.TetrodeEntry.COLUMN_IS_REF, referenceCode);

                        tetrode.writeToDb(mDb);

                        Log.v(LOG_TAG, "Created new tetrode id = " + tetrode.getId() );

                    }
                }

                // Or update the existing record
                else if (mRecordMode == TurnDbUtils.RECORD_EDIT) {
                    if (mRat.existsInDb(mDb)) {
                        mRat.writeToDb(mDb);
                    }
                    else {
                        throw new RuntimeException("Dialog is in 'RECORD_EDIT' mode but rat does not exist in db");
                    }
                }

                // Close the activity
                getDialog().dismiss();

                // Notify the the ratActivityFragment that the dialog was
                getTargetFragment().onActivityResult(RatActivityFragment.REQUEST_EDIT_RAT_DIALOG,
                        TurnDbUtils.DIALOG_OK, null);

                Toast.makeText(getTargetFragment().getContext(), "Changes saved", Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        // Notify the the ratActivityFragment that the dialog was cancelled
        getTargetFragment().onActivityResult(RatActivityFragment.REQUEST_EDIT_RAT_DIALOG,
                TurnDbUtils.DIALOG_CANCEL, null);

        Toast.makeText(getTargetFragment().getContext(), "No changes were made", Toast.LENGTH_SHORT).show();


    }

    private void updateRatFromUiFields() {

        // Get the UI data
        String valID = mEditCode.getText().toString();
        String valName = mEditName.getText().toString();
        int valNumTTs = Integer.parseInt(mEditNumTts.getSelectedItem().toString());
        double valMicronsPerTurn = Double.parseDouble(mEditMicrons.getText().toString());
        int valThreadDir = mEditThreadDir.getSelectedItemPosition() + 1;
        int valTTsIndy = mEditTetrodesIndependent.isChecked() ? Rat.TETRODES_INDEPENDENT_YES : Rat.TETRODES_INDEPENDENT_NO;

        // Check that the rat name and code don't contain reserved chars
        if (GeneralUtils.containsReservedChars(valName)) {
            Toast.makeText(mContext, "The name cannot contain any of the following characters: " +
                    GeneralUtils.RESERVED_CHARS, Toast.LENGTH_LONG).show();
            return;
        }

        if (GeneralUtils.containsReservedChars(valID)) {
            Toast.makeText(mContext, "The code cannot contain any of the following characters: " +
                    GeneralUtils.RESERVED_CHARS, Toast.LENGTH_LONG).show();
            return;
        }

        mRat.setCode(valID);
        mRat.setName(valName);
        mRat.setNumTetrodes(valNumTTs);
        mRat.setTurnMicrometers(valMicronsPerTurn);
        mRat.setScrewThreadDir(valThreadDir);
        mRat.set(TurnContract.RatEntry.COLUMN_TETRODES_INDEPENDENT, valTTsIndy);

    }


}
