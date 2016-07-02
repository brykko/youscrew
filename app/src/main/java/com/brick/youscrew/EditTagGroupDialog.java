package com.brick.youscrew;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.brick.youscrew.R;
import com.brick.youscrew.data.TurnContract.TagGroupEntry;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;

/**
 * Created by Brick on 06/03/16.
 *
 * Edits the fields of a single tagGroup record.
 *
 */
public class EditTagGroupDialog extends android.support.v4.app.DialogFragment {

    private final String LOG_TAG = EditTagGroupDialog.class.getSimpleName();

    private SQLiteDatabase mDb;

    private long mTagGroupId;

    private View mRootView;

    private String mName;
    private EditText mDlgName;

    private int mMandatory;
    private Spinner mDlgMandatory;

    private String mColour;

    private int mInUse;
    private Spinner mDlgInUse;

    private int mDisplayIndex;
    private Spinner mDlgDisplayIndex;

    private int mSingleConstraint;
    private CheckBox mDlgSingleConstraint;

    private String mHintText;
    private EditText mDlgHintText;

    private Context mContext;

    private ArrayList<String> mInUseStrings, mMandatoryStrings;
    private ArrayAdapter<String> mInUseAdapter, mMandatoryAdapter;

    private final int[] mInUseCodes = {TurnDbUtils.SPECIFY_NONE, TurnDbUtils.SPECIFY_PRE, TurnDbUtils.SPECIFY_POST, TurnDbUtils.SPECIFY_BOTH};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContext = getActivity();

        // Retrieve the row id of the tag group to be edited
        mTagGroupId = getArguments().getLong("tagGroupId");

        mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();

        mRootView = inflater.inflate(R.layout.fragment_edit_tag_group_dialog, container);

        readDb();

        findUiObjects();

        initializeSpinners();

        setUiContent();

        setButtonListeners();

        return mRootView;
    }

    @Override
    public void onResume() {
        // Set dimensions to match parent
        super.onResume();
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void findUiObjects() {
        mDlgName = (EditText) mRootView.findViewById(R.id.new_tag_group_name);
        mDlgInUse = (Spinner) mRootView.findViewById(R.id.new_tag_group_in_use);
        mDlgDisplayIndex = (Spinner) mRootView.findViewById(R.id.new_tag_group_display_index);
        mDlgMandatory = (Spinner) mRootView.findViewById(R.id.new_tag_group_mandatory);
        mDlgSingleConstraint = (CheckBox) mRootView.findViewById(R.id.new_tag_group_single_constraint);
        mDlgHintText = (EditText) mRootView.findViewById(R.id.new_tag_group_hint_text);
    }

    private void initializeSpinners() {
        // Initialize all spinners in the UI

        // Set up the in-use and mandatory spinners
        mInUseStrings = new ArrayList<>();

        for (int n = 0; n < mInUseCodes.length; n++) {
            mInUseStrings.add(GeneralUtils.turnTimeCombString(mInUseCodes[n]));
        }

        // Initialize the mandatory options with the same text
        mMandatoryStrings = (ArrayList) mInUseStrings.clone();

        mInUseAdapter = new ArrayAdapter<>(mContext, R.layout.simple_spinner_item, mInUseStrings);

        mMandatoryAdapter = new ArrayAdapter<String>(mContext, R.layout.simple_spinner_item, mMandatoryStrings) {

            @Override
            public boolean isEnabled(int position) {

                // Get the current specifier code for the in_use spinner
                int inUseSelectedCode = mInUseCodes[mDlgInUse.getSelectedItemPosition()];

                // Get the the  current item in the mandatory spinner
                int mandatoryCode = mInUseCodes[position];

                switch (inUseSelectedCode) {
                    case TurnDbUtils.SPECIFY_NONE :
                        if (mandatoryCode == TurnDbUtils.SPECIFY_NONE) {
                            return true;
                        }
                        else {
                            return false;
                        }

                    case TurnDbUtils.SPECIFY_PRE :
                        if (mandatoryCode == TurnDbUtils.SPECIFY_NONE || mandatoryCode == TurnDbUtils.SPECIFY_PRE) {
                            return true;
                        }
                        else {
                            return false;
                        }

                    case TurnDbUtils.SPECIFY_POST :
                        if (mandatoryCode == TurnDbUtils.SPECIFY_NONE || mandatoryCode == TurnDbUtils.SPECIFY_POST) {
                            return true;
                        }
                        else {
                            return false;
                        }

                    case TurnDbUtils.SPECIFY_BOTH :
                        return true;

                    default :
                        return false;
                }


            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                // When disabled, an item will be shown in grey

                View item = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) item;

                // N.B. important that these colours agree with the app theme!
                if (isEnabled(position)) {
                    textView.setTextColor(Color.LTGRAY);
                }
                else {
                    textView.setTextColor(Color.GRAY);
                };

                return item;
            }

        };

        mDlgInUse.setAdapter(mInUseAdapter);
        mDlgMandatory.setAdapter(mMandatoryAdapter);


        // Whenever the user changes the 'in use' box, reset the
        mDlgInUse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMandatoryAdapter.notifyDataSetChanged();

                // Check if the current mandatory spinner item is valid (enabled); if not, set it
                // to the same value as the new inUse selection.

                boolean mandatoryIsValid = mMandatoryAdapter.isEnabled(mDlgMandatory.getSelectedItemPosition());

                if (!mandatoryIsValid) {
                    mDlgMandatory.setSelection(position);
                    mMandatoryAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Set up the display index spinner by finding the max display index
        Cursor c = TurnDbUtils.tagGroupGetEntries(mDb, null, TurnDbUtils.DELETED_NO);
        int currentIndex;
        int maxIndex = 0;
        for (int g = 0; g < c.getCount(); g++ ) {
            currentIndex = c.getInt(c.getColumnIndex(TagGroupEntry.COLUMN_DISPLAY_INDEX));
            if (currentIndex > maxIndex) {
                maxIndex = currentIndex;
            }
            c.moveToNext();
        }
        c.close();

        // Now create a list of strings going from 1 up to the maximum display index
        ArrayList<String> displayIndexStrings = new ArrayList<>();
        for (int g = 0; g <= maxIndex; g++) {
            displayIndexStrings.add(Integer.toString(g+1));
        }

        ArrayAdapter<String> displayIndexAdapter = new ArrayAdapter<>(mContext, R.layout.simple_spinner_item, displayIndexStrings);
        mDlgDisplayIndex.setAdapter(displayIndexAdapter);

    }

    private void readDb() {
        // Set the member fields from values in the db

        Cursor c = TurnDbUtils.tagGroupGetEntries(mDb, mTagGroupId, null);

        // First set the instance fields
        mName               = c.getString(c.getColumnIndex(TagGroupEntry.COLUMN_NAME));
        mColour             = c.getString(c.getColumnIndex(TagGroupEntry.COLUMN_COLOUR));
        mHintText           = c.getString(c.getColumnIndex(TagGroupEntry.COLUMN_HINT_TEXT));
        mDisplayIndex       = c.getInt(   c.getColumnIndex(TagGroupEntry.COLUMN_DISPLAY_INDEX));
        mInUse              = c.getInt(   c.getColumnIndex(TagGroupEntry.COLUMN_IN_USE));
        mMandatory          = c.getInt(   c.getColumnIndex(TagGroupEntry.COLUMN_MANDATORY));
        mSingleConstraint   = c.getInt(   c.getColumnIndex(TagGroupEntry.COLUMN_SINGLE_CONSTRAINT));

        c.close();

    }

    private void setUiContent() {
        // Update the UI content with the current field values
        mDlgName.setText(mName);
        mDlgHintText.setText(mHintText);
        mDlgSingleConstraint.setChecked(mSingleConstraint == TurnDbUtils.SINGLE_CONSTRAINT_ON ? true : false);
        mDlgDisplayIndex.setSelection(mDisplayIndex);
        mDlgInUse.setSelection(mInUse - 1);
        mDlgMandatory.setSelection(mMandatory - 1);
    }

    public void getUiContent() {
        // Set the class fields to the values currently in the UI
        mName               = mDlgName              .getText().toString();
        mHintText           = mDlgHintText          .getText().toString();
        mSingleConstraint   = mDlgSingleConstraint  .isChecked() ? TurnDbUtils.SINGLE_CONSTRAINT_ON : TurnDbUtils.SINGLE_CONSTRAINT_OFF;
        mDisplayIndex       = mDlgDisplayIndex      .getSelectedItemPosition();
        mInUse              = mInUseCodes[mDlgInUse .getSelectedItemPosition()];
        mMandatory          = mInUseCodes[mDlgMandatory.getSelectedItemPosition()];
    }

    private void setButtonListeners() {
        // Attach listeners to the cancel and ok buttons.

        Button buttonCancel = (Button) mRootView.findViewById(R.id.button_cancel);
        Button buttonOk = (Button) mRootView.findViewById(R.id.button_ok);

        final Intent data = new Intent();
        data.putExtra("tagGroupId", mTagGroupId);

        // OK gets the current dialog contents, updates the database record, then closes the dialog
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUiContent();
                writeContentToDb();
                getTargetFragment().onActivityResult(TagSetupActivityFragment.REQUEST_NEW_TAG_DIALOG, TagSetupActivityFragment.RESULT_OK, data);
                getDialog().dismiss();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTargetFragment().onActivityResult(TagSetupActivityFragment.REQUEST_NEW_TAG_DIALOG, TagSetupActivityFragment.RESULT_CANCEL, data);
                getDialog().dismiss();
            }
        });

    }

    public void writeContentToDb() {
        // Write the class field values to the tagGroup db record

        ContentValues values = new ContentValues();
        values.put(TagGroupEntry.COLUMN_NAME, mName);
        values.put(TagGroupEntry.COLUMN_HINT_TEXT, mHintText);
        values.put(TagGroupEntry.COLUMN_SINGLE_CONSTRAINT, mSingleConstraint);
        values.put(TagGroupEntry.COLUMN_IN_USE, mInUse);
        values.put(TagGroupEntry.COLUMN_MANDATORY, mMandatory);

        int numRecords = mDb.update(
                TagGroupEntry.TABLE_NAME,
                values,
                TagGroupEntry._ID + " = " + Long.toString(mTagGroupId),
                null
        );

        // Update the display index, shifting other groups' values aside accordingly
        TurnDbUtils.displayIndexEdit(mDb, TagGroupEntry.TABLE_NAME, mTagGroupId, null, null, mDisplayIndex);

        if (numRecords != 1) {
            throw new RuntimeException("Error updating tagGroup record");
        }


    }



}

