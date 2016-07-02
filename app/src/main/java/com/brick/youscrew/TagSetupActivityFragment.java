package com.brick.youscrew;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.brick.youscrew.data.TurnContract;
import com.brick.youscrew.data.TurnContract.TagEntry;
import com.brick.youscrew.data.TurnContract.TagGroupEntry;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class TagSetupActivityFragment extends TagActivityFragment {

    public static final int REQUEST_NEW_TAG_DIALOG = 1;
    public static final int RESULT_OK = 1;
    public static final int RESULT_CANCEL = 2;

    private static final String LOG_TAG = TagActivityFragment.class.getSimpleName();

    private boolean mNewTagGroupPending = false; // true when new tag group is created and the edit dialog is showing

    public TagSetupActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContext = getContext();

        mTurnTime = TurnDbUtils.TURNTIME_GENERIC;

        mRootView = inflater.inflate(R.layout.fragment_tag, container, false);

        // Open the db
        mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();

        refreshLayout();

        return mRootView;

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        TagSetupActivity activity = (TagSetupActivity) getActivity();

        // Prevent simultaneous activation of both context menus
        if (activity.mContextMenuShowing) {
            return;
        }

        else {

            // Calling the super method marks the boolean state of the context menu as OPEN
            super.onCreateContextMenu(menu, v, menuInfo);

            // Determine if clicked view is a tag group panel or a tag
            MenuInflater inflater = getActivity().getMenuInflater();

            int id = v.getId();

            // If the overall tag group layout was clicked, bring up the tag group context menu
            if (id == R.id.tag_layout) {
                inflater.inflate(R.menu.context_menu_tag_setup, menu);
            }

            // Otherwise, if a tag in the listview was clicked, bring up the tag context menu
            else if (id == R.id.tag_panel_listview) {
                inflater.inflate(R.menu.context_menu_tag_setup_tag, menu);
            }

        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        final long id = mTagGroupIds[mTagGroupLastClicked];

        switch (item.getItemId()) {
            case R.id.action_tag_group_new_tag :
                addNewTag(mTagGroupLastClicked);
                break;

            case R.id.action_tag_group_edit :
                // Open the tag group in the custom edit dialog
                editTagGroup(id);
                break;

            case R.id.action_tag_group_delete :

                // Show a warning dialog. If 'OK' clicked, the record is deleted
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TurnDbUtils.tagGroupDeleteEntry(mDb, id);
                        TurnDbUtils.closeDisplayIndexGaps(mDb, TagGroupEntry.TABLE_NAME, null);
                        refreshLayout();
                    } })

                // If 'cancel' clicked, nothing happens
                .setNegativeButton("Cancel", null)
                .setMessage("Are you sure you want to delete this tag group?");

                AlertDialog dialog = builder.create();
                dialog.show();
                break;

            case R.id.action_tag_rename :
                renameTag(mTagLastClicked, mTagGroupLastClicked);
                break;

            case R.id.action_tag_reposition :
                repositionTag(mTagLastClicked, mTagGroupLastClicked);
                break;

            case R.id.action_tag_delete :
                deleteTag(mTagLastClicked, mTagGroupLastClicked);
                break;
        }

        return true;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_NEW_TAG_DIALOG) {

            // If creation of new tag group was cancelled, delete the new record
            if (resultCode == RESULT_CANCEL && mNewTagGroupPending) {
                TurnDbUtils.singleEntryDelete(mDb, TagGroupEntry.TABLE_NAME, data.getLongExtra("tagGroupId", -1));
                mNewTagGroupPending = false;
            }

            refreshLayout();

        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_tag_setup, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.actions_add_new_tag_group :

                mNewTagGroupPending = true;
                long tagGroupId = addNewBlankTagGroup();
                editTagGroup(tagGroupId);

                break;
        }

        return true;

    }

    @Override
    protected void refreshLayout() {

        mShowTagGroupHints = true;

        // (These two lines are both in super method)
        createTags();

        createTagListViews();

        // (This differs from the super)
        addTagClickListeners();

    }

    @Override
    protected void createTagAdapter(int index) {
        // Override the super method so that the TagAdaper class defined here instead of the super's
        // inner class

        mListAdapters[index] = new TagAdapter(
                mContext,
                R.layout.list_item_tag,
                R.id.list_item_tag_textview,
                mTagStrings[index],
                mTagGroupColours[index],
                index);

    }

    private void editTagGroup(long tagGroupId) {

        // Show the dialog
        android.support.v4.app.FragmentManager manager = getActivity().getSupportFragmentManager();
        EditTagGroupDialog dialog = new EditTagGroupDialog();
        Bundle args = new Bundle();
        args.putLong("tagGroupId", tagGroupId);
        dialog.setArguments(args);
        dialog.setTargetFragment(this, REQUEST_NEW_TAG_DIALOG);
        dialog.show(manager, "fragment_edit_tag_group_dialog");

    }

    private void addNewTag(int tagGroupIndex) {
        // Show a dialog to request a new name

        final EditText editText = new EditText(mContext);
        final long groupId = mTagGroupIds[tagGroupIndex];

        editText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setMessage("Enter new tag name")
                .setView(editText)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tagName = editText.getText().toString();

                        // Try to add the tag to the db. If the name is not unqiue,
                        // we may get an exception back.
                        try {
                            TurnDbUtils.tagAddEntry(mDb, groupId, tagName);
                            Toast.makeText(mContext, "New tag created", Toast.LENGTH_SHORT);
                            refreshLayout();
                        } catch (RuntimeException e) {
                            Toast.makeText(mContext, "Tag must have a unique name", Toast.LENGTH_LONG);
                        }


                    }
                });

        builder.create().show();

    }

    private void repositionTag(final int tagIndex, final int groupIndex) {
        // Shows a dialog to request a new position to move the tag to

        final long tagId = mTagIds[groupIndex].get(tagIndex);

        Cursor c = TurnDbUtils.tagGetEntries(mDb, tagId, null);
        final int currentDisplayIndex = c.getInt(c.getColumnIndex(TagEntry.COLUMN_DISPLAY_INDEX));
        c.close();


        ArrayList<String> positionList = new ArrayList<>();
        for (int t = 0; t < mNumTags[groupIndex]; t++) {
            positionList.add(Integer.toString(t+1));
        }

        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                mContext, R.layout.simple_spinner_item, positionList);

        final Spinner spinner = new Spinner(mContext);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        spinner.setLayoutParams(layoutParams);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(currentDisplayIndex);
        spinner.setDropDownWidth(GeneralUtils.dip2Pix(mContext, 80));
        spinner.setGravity(Gravity.LEFT);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setMessage("Select the new tag position")
                .setView(spinner)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int newDisplayIndex = spinner.getSelectedItemPosition();

                        TurnDbUtils.displayIndexEdit(mDb, TagEntry.TABLE_NAME,
                                tagId, TagEntry.COLUMN_TAG_GROUP_KEY,
                                mTagGroupIds[groupIndex], newDisplayIndex);

                        // Notify user with toast
                        Toast.makeText(mContext,
                                "Tag moved from position "
                                        + Integer.toString(currentDisplayIndex + 1) +
                                        " to " + Integer.toString(newDisplayIndex + 1),
                                Toast.LENGTH_SHORT).show();
                        refreshLayout();

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

    private void renameTag(final int tagIndex, final int groupIndex) {

        final EditText editText = new EditText(mContext);
        editText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        editText.setText(mTagStrings[groupIndex].get(tagIndex));

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setMessage("Set a new name")
                .setView(editText)

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = editText.getText().toString();
                        try {
                            TurnDbUtils.singleEntryUpdate(mDb, TagEntry.TABLE_NAME,
                                    mTagIds[groupIndex].get(tagIndex), TagEntry.COLUMN_NAME, newName);
                            Toast.makeText(mContext, "Tag renamed", Toast.LENGTH_SHORT).show();
                            refreshLayout();
                        } catch (RuntimeException e) {
                            Toast.makeText(mContext, "Tag must have a unique name", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        builder.create().show();

    }

    private void deleteTag(int tagIndex, int groupIndex) {

        String tagName = mTagStrings[groupIndex].get(tagIndex);
        final long tagId = mTagIds[groupIndex].get(tagIndex);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setMessage("Are you sure you want to delete tag '" + tagName + "'?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TurnDbUtils.tagDeleteEntry(mDb, tagId, true);
                        Toast.makeText(mContext, "Tag deleted", Toast.LENGTH_LONG).show();
                        refreshLayout();
                    }
                });

        builder.create().show();

    }

    private void addTagClickListeners() {
        // Now the listViews are created, set the item click listeners

        for (int g = 0; g < mNumTagGroups; g++) {

            // (Workaround: since the loop counter can't be accessed by the inner class
            // use a final var that's re-declared each time
            final int gg = g;

            // Standard click toggles the tag's "in use" state
            listViews[gg].setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    int inUseValueCurrent = mTagInUse[gg].get(position);
                    int inUseValueNew;

                    if (inUseValueCurrent == TurnDbUtils.IN_USE_NO) {
                        inUseValueNew = TurnDbUtils.IN_USE_YES;
                    }
                    else {
                        inUseValueNew = TurnDbUtils.IN_USE_NO;
                    }

                    // Toggle the "in use" field value
                    mTagInUse[gg].set(position, inUseValueNew);
                    mListAdapters[gg].notifyDataSetChanged();

                    // Register the tag group and item index
                    mTagGroupLastClicked = gg;
                    mTagLastClicked = position;

                }

            });

            listViews[gg].setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    mTagGroupLastClicked = gg;
                    mTagLastClicked = position;
                    return false;
                }
            });

//            registerForContextMenu(mTagGroupPanel[g]);

            mTagGroupPanel[g].setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mTagGroupLastClicked = gg;
                    v.showContextMenu();
                    return true;
                }
            });

        }

    }

    private class TagAdapter extends TagActivityFragment.TagAdapter {

        public TagAdapter(Context context, int resource, int textViewResourceId, ArrayList<String> strings, int colour, int viewIndex) {
            super(context, resource, textViewResourceId, strings, colour, viewIndex);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = mInflater.inflate(mResource, null, false);

            TextView text = (TextView) view.findViewById(mListItemResource);
            text.setText(mStrings.get(position));

            GradientDrawable shape = (GradientDrawable)text.getBackground();
            shape.setColor(mColour);

            // If in use, darken background shape and bolden border
            if (mTagInUse[mViewIndex].get(position) == TurnDbUtils.IN_USE_YES) {
                shape.setColor(mColour);
                shape.setStroke(GeneralUtils.dip2Pix(mContext, 3), Color.BLACK);
                text.setTextColor(Color.BLACK);
            }
            // Otherwise, restore normal appearance
            else {
                shape.setColor(changeColorSat(mColour,(float) 0.5));
                shape.setStroke(GeneralUtils.dip2Pix(mContext, 1), Color.GRAY);
                text.setTextColor(Color.GRAY);
            }

            return view;
        }
    }

    protected void writeTagUseStateToDb() {
        // Write each tag's "in use" field value to that currently set in the UI

        int nTag;
        int inUseCode;
        long tagId;

        // If not, for each tag category:
        for (int g=0; g<mNumTagGroups; g++) {

            nTag = mListAdapters[g].getCount();

            // Get the selected tags in the listView and add to the list
            for (int t=0; t<nTag; t++) {

                tagId = mTagIds[g].get(t);

                inUseCode = mTagInUse[g].get(t);

                TurnDbUtils.singleEntryUpdate(
                        mDb,
                        TurnContract.TagEntry.TABLE_NAME,
                        tagId,
                        TurnContract.TagEntry.COLUMN_IN_USE,
                        inUseCode);

            }
        }
    }

    protected void writeTagGroupFieldsToDb() {

        ContentValues values = new ContentValues();

        for (int g=0; g < mNumTagGroups; g++) {

            values.clear();
            values.put(TagGroupEntry.COLUMN_COLOUR, TurnDbUtils.colorToHexString(mDb, mTagGroupColours[g]));
            values.put(TagGroupEntry.COLUMN_HINT_TEXT, mTagGroupHint[g]);
            values.put(TagGroupEntry.COLUMN_IN_USE, mTagGroupInUse[g]);
            values.put(TagGroupEntry.COLUMN_MANDATORY, mTagGroupMandatory[g]);
            values.put(TagGroupEntry.COLUMN_NAME, mTagGroupNames[g]);
            values.put(TagGroupEntry.COLUMN_SINGLE_CONSTRAINT, mTagGroupSingleConstraint[g]);

            int numRecords = mDb.update(
                    TagGroupEntry.TABLE_NAME,
                    values,
                    TagGroupEntry._ID + " = " + mTagGroupIds[g],
                    null);

            if (numRecords != 1) {
                throw new RuntimeException("Error updating database");
            }

        }


    }

}
