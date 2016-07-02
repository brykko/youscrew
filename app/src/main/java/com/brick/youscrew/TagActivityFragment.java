package com.brick.youscrew;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brick.youscrew.data.Tetrode;
import com.brick.youscrew.data.TurnContract.TagEntry;
import com.brick.youscrew.data.TurnContract.TagGroupEntry;
import com.brick.youscrew.data.TurnContract.TetrodeEntry;
import com.brick.youscrew.data.TurnContract.TurnEntry;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;


public class TagActivityFragment extends Fragment {

    private final String LOG_TAG = TagActivityFragment.class.getSimpleName();

    public static final int REQUEST_TAG = 2;

    public static final int RESULT_OK = 1;
    public static final int RESULT_CANCELLED = 2;
    public static final int RESULT_SKIPPED = 3;


    // TAG EDIT MODES
    public static final int MODE_NEW = 1;    // Only persistent tags imported, editable
    public static final int MODE_UPDATE = 2; // ALL tags imported, editable
    public static final int MODE_VIEW = 3;   // ALL tags imported, cannot be edited
    public static final int MODE_BLANK = 4;  // No tags imported, editable

    public static final int PANEL_HEIGHT_NO_HINT = 300;
    public static final int PANEL_HEIGHT_HINT = 300;

    public static final int SELECTION_NORMAL = 0;
    public static final int SELECTION_PERSISTENT = 1;
    public static final boolean USE_RANDOM_TAGS = false;

    protected int mNumTagGroups;
    protected int[] mNumTags;

    protected GridLayout mTagGroupGridLayout;

    protected boolean mGlobalLayoutCreated;

    protected ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

    protected SQLiteDatabase mDb;

    protected TagAdapter[] mListAdapters;
    protected ListView[] listViews;
    protected View[] mTagGroupPanel;
    protected View mRootView;

    protected ArrayList<String>[] mTagStrings;
    protected ArrayList<Long>[] mTagIds;
    protected ArrayList<Boolean>[] mTagSelected;
    protected ArrayList<Boolean>[] mTagPersistent;
    protected ArrayList<Integer>[] mTagInUse;

    protected int mTurnTime;
    protected int mFlag = TurnDbUtils.FLAG_REPLACE; // eventually this will be a dynamically set option
    protected long mTurnId, mTetrodeId;
    protected String mTetrodeName;
    protected int mTagEditMode;             // MODE_VIEW, MODE_UPDATE, MODE_NEW
    protected boolean mShowTagGroupHints;
    protected boolean mShowTagContextMenu;
    protected boolean mViewInitialized = false;

    protected int[] mTagGroupInUse;
    protected long[] mTagGroupIds;
    protected int[] mTagGroupColours;
    protected int[] mTagGroupMandatory;
    protected int[] mTagGroupSingleConstraint;
    protected String[] mTagGroupHint;
    protected String[] mTagGroupNames;

    protected int mTagGroupLastClicked;
    protected int mTagLastClicked;

    protected Context mContext;

    public TagActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mContext = getContext();

        // Determine the tag type sent with the start intent
        Intent startIntent = getActivity().getIntent();

        mTurnTime = startIntent.getIntExtra("turnTime", -1);
        mTurnId = startIntent.getLongExtra("turnId", -1);
        mTagEditMode = startIntent.getIntExtra("editMode", -1);

        if(mTurnTime == -1) { throw new RuntimeException("Intent extra 'turnTime' was not found"); }
        if(mTagEditMode == -1) { throw new RuntimeException("Intent extra 'editMode' was not found"); }
        if(mTurnId == -1) { throw new RuntimeException("Intent extra 'turnId' was not found"); }

        mRootView = inflater.inflate(R.layout.fragment_tag, container, false);

        // Open the database
        mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();

        Cursor c = TurnDbUtils.turnGetEntries(mDb, mTurnId);
        mTetrodeId = c.getLong(c.getColumnIndex(TurnEntry.COLUMN_TETRODE_KEY));
        c.close();

        Log.v(LOG_TAG, "Turn Id = " + Long.toString(mTetrodeId));

        c = TurnDbUtils.singleEntryQuery(mDb, TetrodeEntry.TABLE_NAME, null, mTetrodeId, null);
        mTetrodeName = c.getString(c.getColumnIndex(TetrodeEntry.COLUMN_NAME));

        return mRootView;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.v(LOG_TAG, "onCreate()");
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshLayout();

        if (!mViewInitialized) {
            setToolbarSubtitle();
            mViewInitialized = true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_tag, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent data = new Intent();

        switch (item.getItemId()) {

            case R.id.action_settings :
                Intent intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                return true;

            // OK : if viewing only, just finish
            //      if editing, check constraings before saving and finishing
            case R.id.action_ok :
                        data.putExtra("editMode", mTagEditMode)
                        .putExtra("turnTime", mTurnTime);


                if (mTagEditMode == MODE_VIEW) {
                    getActivity().setResult(RESULT_OK, data);
                    getActivity().finish();
                }
                else {
                    boolean mandatoryOk = checkMandatoryConstraints();
                    if (mandatoryOk) {
                        writeTagsToDb();
                        getActivity().setResult(RESULT_OK, data);
                        Toast.makeText(mContext, "Tags saved", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                }
                return true;

            // SKIP: just finish
            case R.id.action_skip :
                data.putExtra("editMode", mTagEditMode)
                        .putExtra("turnTime", mTurnTime);
                getActivity().setResult(RESULT_SKIPPED, data);
                Toast.makeText(mContext, "Tagging skipped", Toast.LENGTH_SHORT).show();
                getActivity().finish();

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    protected void setToolbarSubtitle() {
        // Set the toolbar subtitle
        String subtitle;
        if (mTurnTime == TurnDbUtils.TURNTIME_PRE) {
            subtitle = mTetrodeName + " PRETURN";
        }
        else if (mTurnTime == TurnDbUtils.TURNTIME_POST) {
            subtitle = mTetrodeName + " POSTTURN";
        }
        else {
            subtitle = null;
        }

        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(subtitle);

    }

    protected void attachGlobalLayoutListener() {

        mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
//                onGlobalLayoutChange();
                if (!mGlobalLayoutCreated) {
                    onGlobalLayoutChange();
                }
                mGlobalLayoutCreated = true;
            }
        };

        mTagGroupGridLayout.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

    }

    protected void onGlobalLayoutChange() {

        final int MARGIN = 3;

        int pWidth = mTagGroupGridLayout.getWidth();
        int numCol = mTagGroupGridLayout.getColumnCount();
        int w = pWidth/numCol;

        for (int idx = 0; idx < mNumTagGroups; idx++) {
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) mTagGroupPanel[idx].getLayoutParams();
            params.width = w - 2*MARGIN;

//            if (mShowTagGroupHints) {
//                params.height = GeneralUtils.dip2Pix(mContext, PANEL_HEIGHT_HINT);
//            }
//            else {
//                params.height = GeneralUtils.dip2Pix(mContext, PANEL_HEIGHT_NO_HINT);
//            }

            GeneralUtils.setListViewHeightBasedOnChildren(listViews[idx]);

        }
    }

    protected void refreshLayout() {
        // Call each time a tag group is edited or a tag is added/deleted

        Resources res = getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mShowTagGroupHints = prefs.getBoolean(res.getString(R.string.pref_key_tag_hints), false);

        createTags();

        createTagListViews();

        // If in editable mode, add the tag click listeners
        if (mTagEditMode != MODE_VIEW) {
            setEditableTagClickListeners(mRootView);
        }
    }

    protected void createTags() {

        if (USE_RANDOM_TAGS) {
            createRandomTags();
        }

        else {


            // For a generic tag view, we import all regardless of their
            // "in use" status
            if (mTurnTime==TurnDbUtils.TURNTIME_GENERIC) {
                createTagsFromDb(false, false);
            }

            // Otherwise, only get those in use for the current type of tag
            // session.
            else {
                createTagsFromDb(true, true);
                // If edit mode is 'view' or 'update', get both
                // standard and persistent tags
                if (mTagEditMode == MODE_VIEW || mTagEditMode == MODE_UPDATE) {
                    getSelectedTagsFromDb(true, true);
                }
                // If creating a 'new' tag set, only get the persistent tags
                else if (mTagEditMode == MODE_NEW) {
                    getSelectedTagsFromDb(false, true);
                }

                // (For 'blank' mode, no tags will be selected)
            }

        }
    }

    protected void createRandomTags() {

        // Set the number of columns and initialize the arrays
        mNumTagGroups = 3;
        initializeArrays();

        // Create a set of random string tags for testing
        final int nTags = 5;

        for (int c = 0; c<mNumTagGroups; c++) {

            for (int t = 0; t<nTags; t++) {
                mTagStrings[c].add(Long.toHexString(Double.doubleToLongBits(Math.random())));

                // Add a surrogate tagID (from 0 to nColumns*nTags-1)
                mTagIds[c].add((long) (c*nTags) + t);

                mTagSelected[c].add(false);

            }
        }
    }

    protected void createTagsFromDb(boolean groupInUseOnly, boolean tagInUseOnly) {

        // Get the tag groups from the mDb and place them into the arrayLists.
        // The results will be ordered by their display_index value,
        // so we can put them into the listViews as they arrive

        Cursor cGroup;

        if (groupInUseOnly) {
            cGroup = TurnDbUtils.tagGroupGetEntriesInUse(mDb, mTurnTime);
        }
        else {
            cGroup = TurnDbUtils.tagGroupGetEntries(mDb, null, TurnDbUtils.DELETED_NO);
        }

        cGroup.moveToFirst();
        mNumTagGroups = cGroup.getCount();

        if (mNumTagGroups==0) { Log.e(LOG_TAG, "No tag groups found in mDb!"); }

        // Now we know how many tag groups there are, we can initialize the class arrays
        initializeArrays();

        int colIdxName = cGroup.getColumnIndex(TagGroupEntry.COLUMN_NAME);
        int colIdxColor = cGroup.getColumnIndex(TagGroupEntry.COLUMN_COLOUR);
        int colIdxMandatory = cGroup.getColumnIndex(TagGroupEntry.COLUMN_MANDATORY);
        int colIdxId = cGroup.getColumnIndex(TagGroupEntry._ID);
        int colIdxSingleConstraint = cGroup.getColumnIndex(TagGroupEntry.COLUMN_SINGLE_CONSTRAINT);
        int colIdxHint = cGroup.getColumnIndex(TagGroupEntry.COLUMN_HINT_TEXT);
        int colIdxInUse = cGroup.getColumnIndex(TagGroupEntry.COLUMN_IN_USE);


        cGroup.moveToFirst();

        for (int g=0; g<mNumTagGroups; g++) {

            mTagGroupIds[g] = cGroup.getLong(colIdxId);
            mTagGroupNames[g] = cGroup.getString(colIdxName);
            mTagGroupColours[g] = Color.parseColor(cGroup.getString(colIdxColor));
            mTagGroupMandatory[g] = cGroup.getInt(colIdxMandatory);
            mTagGroupSingleConstraint[g] = cGroup.getInt(colIdxSingleConstraint);
            mTagGroupHint[g] = cGroup.getString(colIdxHint);
            mTagGroupInUse[g] = cGroup.getInt(colIdxInUse);

            Log.v(LOG_TAG, "Setting up tag group '" + mTagGroupNames[g] + "'");

            cGroup.moveToNext();

            // Now query for all tags within the group and add them to the available tags array
            Integer tagInUseCode = tagInUseOnly ? TurnDbUtils.IN_USE_YES : null;

            Cursor cTag = TurnDbUtils.tagGetEntriesByTagGroupId(mDb, mTagGroupIds[g], tagInUseCode);

            cTag.moveToFirst();
            mNumTags[g] = cTag.getCount();

            if (mNumTags[g]==0) {
                Log.e(LOG_TAG, "No tags found in group " + Integer.toString(g) + ", rowId " + Long.toString(mTagGroupIds[g]));
            }

            // For each tag retrieved:
            for (int t=0; t<mNumTags[g]; t++) {

                // Put the current tag name and ID into the arraylist arrays
                mTagStrings[g].add(cTag.getString(cTag.getColumnIndex(TagEntry.COLUMN_NAME)));
                mTagIds[g].add(cTag.getLong(cTag.getColumnIndex(TagEntry._ID)));
                mTagInUse[g].add(cTag.getInt(cTag.getColumnIndex(TagEntry.COLUMN_IN_USE)));
                mTagSelected[g].add(false);
                mTagPersistent[g].add(false);

                Log.v(LOG_TAG, "Added tag '" + mTagStrings[g].get(t));

                cTag.moveToNext();

            }

            cTag.close();

        }

        cGroup.close();

    }

    protected void createTagListViews() {

        mTagGroupGridLayout = (GridLayout) mRootView.findViewById(R.id.tag_layout);

        // Clear any previously existing child views
        mTagGroupGridLayout.removeAllViews();

//        mTagGroupGridLayout.setColumnCount(3);

        int numRows = (int) Math.ceil( (double)mNumTagGroups / (double)mTagGroupGridLayout.getColumnCount() );

        mTagGroupGridLayout.setRowCount(numRows);


        // For each tag group:
        for (int g = 0; g < mNumTagGroups; g++) {

            final int gg = g;

            // Inflate the panel view
            TextView titleText;

            mTagGroupPanel[g] = LayoutInflater.from(getContext()).inflate(R.layout.tag_panel, mTagGroupGridLayout, false);

            // Attach a listener to detect which panel was last touched.
            // This is useful for the context menus
            mTagGroupPanel[g].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTagGroupLastClicked = gg;
                }
            });

            // Set the title of the tag group
            titleText = (TextView) mTagGroupPanel[g].findViewById(R.id.tag_panel_title);
            titleText.setText(mTagGroupNames[g]);

            // Add the labels if necessary
            setTagGroupLabels(g, mTagGroupPanel[g]);

            listViews[g] = (ListView) mTagGroupPanel[g].findViewById(R.id.tag_panel_listview);

//            listViews[g].setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    v.getParent().requestDisallowInterceptTouchEvent(true);
//                    return false;
//                }
//            });

            // If tag type is specifically PRE or POST, mark the mandatory, inUse and singleConstraint
            // props of the tag group
            if (mTurnTime != TurnDbUtils.TURNTIME_GENERIC) {
                boolean isMandatory = GeneralUtils.turnTimeComb2Single(mTagGroupMandatory[g], mTurnTime);
                tagGroupSetMandatory(g, isMandatory);

                boolean isInUse = GeneralUtils.turnTimeComb2Single(mTagGroupInUse[g], mTurnTime);
                tagGroupSetInUse(g, isInUse);
            }

            // Create a new list adapter for the current column
            createTagAdapter(g);

            listViews[g].setAdapter(mListAdapters[g]);

            GridLayout.Spec rowSpan = GridLayout.spec(GridLayout.UNDEFINED, 1);
            GridLayout.Spec colSpan = GridLayout.spec(GridLayout.UNDEFINED, 1);
            GridLayout.LayoutParams gridParam = new GridLayout.LayoutParams(rowSpan, colSpan);

            // Add the tag panel to the parent layout
            mTagGroupGridLayout.addView(mTagGroupPanel[g], gridParam);

            // Register each tag listView for the context menu
            registerForContextMenu(mTagGroupGridLayout);
            registerForContextMenu(listViews[g]);

        }

        // Attach a listener to update the GridLayout child dimensions
        // when the global layout is finished.
        attachGlobalLayoutListener();

        // If the global layout is already finished, call the global layout
        // callback
        if (mGlobalLayoutCreated) {
            onGlobalLayoutChange();
        }

    }

    protected void createTagAdapter(int index) {
        mListAdapters[index] = new TagAdapter(
                mContext,
                R.layout.list_item_tag,
                R.id.list_item_tag_textview,
                mTagStrings[index],
                mTagGroupColours[index],
                index);
    }

    protected void setTagGroupLabels(int index, View panel) {

        TextView textHint = (TextView) panel.findViewById(R.id.tag_panel_hint);
        TextView textInUse = (TextView) panel.findViewById(R.id.tag_panel_in_use);
        TextView textMandatory = (TextView) panel.findViewById(R.id.tag_panel_mandatory);
        TextView textSingleConstraint = (TextView) panel.findViewById(R.id.tag_panel_single_constraint);

        if (mShowTagGroupHints) {
            textHint.setText(mTagGroupHint[index]);

            if (mTagGroupInUse[index] == TurnDbUtils.SPECIFY_NONE) {
                textInUse.setText("NOT IN USE");
            }
            else {
                textInUse.setText("IN USE: " + GeneralUtils.turnTimeCombString(mTagGroupInUse[index]));
            }

            if (mTagGroupMandatory[index] == TurnDbUtils.SPECIFY_NONE) {
                textMandatory.setVisibility(View.GONE);
            }
            else {
                textMandatory.setText("MANDATORY: " + GeneralUtils.turnTimeCombString(mTagGroupMandatory[index]));
            }

            if (mTagGroupSingleConstraint[index] == TurnDbUtils.SINGLE_CONSTRAINT_ON) {
                textSingleConstraint.setText("SINGLE CONSTRAINT");
            }
            else {
                textSingleConstraint.setVisibility(View.GONE);
            }
        }
        else {
            // Make them vanish
            textHint.setVisibility(View.GONE);
            textInUse.setVisibility(View.GONE);
            textMandatory.setVisibility(View.GONE);
            textSingleConstraint.setVisibility(View.GONE);
        }

    }

    private void tagGroupSetMandatory(int index, boolean isMandatory) {
        // Set UI features to indicate tag group is mandatory

        TextView tagTitle = (TextView) mTagGroupPanel[index].findViewById(R.id.tag_panel_title);

        if (isMandatory) {
            tagTitle.setTypeface(null, Typeface.BOLD);
            tagTitle.setTextColor(getResources().getColor(R.color.colorMandatoryYes));
//            listView.setBackgroundColor(Color.GRAY);
        }
        else {
            tagTitle.setTypeface(null, Typeface.NORMAL);
//            listView.setBackgroundColor(Color.WHITE);
        }

    }

    private void tagGroupSetInUse(int index, boolean isInUse) {
        // Set UI features to indicate tag group in use
        listViews[index].setEnabled(isInUse);
    }

    private void getSelectedTagsFromDb(boolean getNormal, boolean getPersistent) {
        // When a turn record is created, persistent tagIds are automatically copied from the
        // POST-TURN ids from the tetrodes' last turn record. This method retrieves the current
        // record's PRE-TURN persistent tag Ids and marks the corresponding tags in the UI
        //

            // Open it and read the list of persistent tag IDs
            Cursor c = TurnDbUtils.turnGetEntries(mDb, mTurnId);

            String colName, colNamePersistent;

            // For whichever tag type specified ("preturn" or "postturn")
            // get both standard AND persistent tags from db

            if (mTurnTime == TurnDbUtils.TURNTIME_PRE) {
                colName = TurnEntry.COLUMN_TAG_ID_PRE;
                colNamePersistent = TurnEntry.COLUMN_TAG_ID_PERSISTENT_PRE;
            }
            else if (mTurnTime == TurnDbUtils.TURNTIME_POST) {
                colName = TurnEntry.COLUMN_TAG_ID_POST;
                colNamePersistent = TurnEntry.COLUMN_TAG_ID_PERSISTENT_POST;
            }
            else {
                throw new RuntimeException("Illegal tag type");
            }

            if (getNormal) {
                String tagIdCsv = c.getString(c.getColumnIndex(colName));
                Log.v(LOG_TAG, "Tag ID Csv = " + tagIdCsv);
                processCsvTagString(tagIdCsv, false);
            }

            if (getPersistent) {
                String persistentTagIdCsv = c.getString(c.getColumnIndex(colNamePersistent));
                Log.v(LOG_TAG, "Persistent tag ID Csv = " + persistentTagIdCsv);
                processCsvTagString(persistentTagIdCsv, true);
            }

    }

    private void processCsvTagString(String csv, boolean persistent) {

        if (csv != null && csv.length() > 0) {

            String[] pIdStrings = csv.split(",");
            int numPTags = pIdStrings.length;
            long id;

            // For each persistent tag, mark it
            for (int i = 0; i < numPTags; i++) {

                id = Long.parseLong(pIdStrings[i]);
                markAsActive(id, persistent);

            }

        }
    }

    private void markAsActive(long tagId, boolean persistent) {
        // Helper for getselectedTagsFromDb during setup.
        // Should NOT be called during UI interaction

        // Determine which group the tag is from
        int[] index = new int[2];

        // Try to locate the tag's group and index
        findTagById(tagId, index);

        // If it was found, set the approprite tag's activity state
        if (index != null) {
            int tagGroup = index[0];
            int tagIndex = index[1];

            // By marking the tag as 'persistent', it must be unmarked as 'selected'
            mTagSelected[tagGroup].set(tagIndex, !persistent);
            mTagPersistent[tagGroup].set(tagIndex, persistent);
        }

        // Otherwise do nothing (if the tag CSV string refers to tag groups
        // that are not designated as enabled then they will not be found, so
        // that's OK).

    }

    private void applyNewSelection(int tagGroup, int index, int selectionType) {

        boolean value, newValue;
        boolean isSelected = mTagSelected[tagGroup].get(index);
        boolean isPersistent = mTagPersistent[tagGroup].get(index);

        if (selectionType == SELECTION_NORMAL) {

            value = isSelected;
            newValue = !value;

            // Toggle the selection value
            mTagSelected[tagGroup].set(index, newValue);

            // If tag was persistent before and is now marked for normal selection, its persistence
            // must be erased
            if (newValue == true && isPersistent) {
                mTagPersistent[tagGroup].set(index, false);
            }

        }

        else if (selectionType == SELECTION_PERSISTENT) {

            value = isPersistent;
            newValue = !value;

            // Toggle the persistence value
            mTagPersistent[tagGroup].set(index, newValue);

            // If tag was selected before and is now marked for persistence, the previous selection
            // must be erased
            if (newValue == true && isSelected) {
                mTagSelected[tagGroup].set(index, false);
            }

        }

        if (mTagGroupSingleConstraint[tagGroup] == TurnDbUtils.SINGLE_CONSTRAINT_ON) {
            applySingleConstraint(tagGroup, index, selectionType);
        }

    }

    private void findTagById(long tagId, int[] index) {
        // Determine the tag group and index of tag from its Id

        if (index.length != 2) {
            throw new RuntimeException("Argument index must be of length 2");
        }

        boolean found = false;
        int idxG = 0;
        int idxT = 0;
        int numTagGroups = mTagIds.length;
        int numTagsInGroup;

        long currentId;

        do {

            currentId = mTagIds[idxG].get(idxT);

            if (currentId == tagId) {
                found = true;
                break;
            }

            // Cycle through each tagId arraylist

            numTagsInGroup = mTagIds[idxG].size();

            if (idxT<numTagsInGroup-1) {
                idxT++;
            }
            else {
                idxT = 0;
                idxG++;
            }

        }
        // Keep running until the match is found, or we reach the end of the arrayList
        while (idxG < numTagGroups);

        if (found) {
            index[0] = idxG;
            index[1] = idxT;
        }
        else {
//            throw new RuntimeException("Tag not found");
            index = null;
        }

    }

    protected class TagAdapter extends ArrayAdapter<String> {

        // Custom adapter class that sets a single background colour for all child views

        ArrayList<String> mStrings;
        LayoutInflater mInflater;
        int mResource;
        int mListItemResource;
        int mColour;
        int mViewIndex;

        public TagAdapter(Context context, int resource, int textViewResourceId, ArrayList<String> strings, int colour, int viewIndex) {
            super(context, resource, textViewResourceId, strings);
            mStrings = strings;
            mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mResource = resource;
            mListItemResource = textViewResourceId;
            mColour = colour;
            mViewIndex = viewIndex;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = mInflater.inflate(mResource, null, false);

            TextView text = (TextView) view.findViewById(mListItemResource);
            text.setText(mStrings.get(position));

            GradientDrawable shape = (GradientDrawable)text.getBackground();

            int color = changeColorValue(mColour, 0.8f);

            // If selected, darken background shape and bolden border
            if (mTagSelected[mViewIndex].get(position)) {
                color = changeColorValue(color, 1.3f);
                color = changeColorSat(color, 2f);
                shape.setStroke(GeneralUtils.dip2Pix(mContext, 3), Color.WHITE);
                text.setTextColor(Color.BLACK);
            }
            else if (mTagPersistent[mViewIndex].get(position)) {
                color = changeColorValue(color, 0.2f);
                shape.setStroke(GeneralUtils.dip2Pix(mContext, 3), Color.WHITE);
                text.setTextColor(Color.WHITE);
            }
            // Otherwise, restore normal appearance
            else {
                shape.setStroke(GeneralUtils.dip2Pix(mContext, 1), Color.GRAY);
                text.setTextColor(Color.BLACK);
            }

            shape.setColor(color);

            if (mShowTagContextMenu) {
                registerForContextMenu(view);
            }

            return view;

        }
    }

    private void setEditableTagClickListeners(View rootView) {
        // Now the listViews are created, set the item click listeners

        for (int g=0; g < mNumTagGroups; g++) {

            // (Workaround: since the loop counter can't be accessed by the inner class
            // use a final var that's re-declared each time
            final int gg = g;

            // Set the title of each tag group

            // Set the listener for each AVAILABLE tag column

            listViews[g].setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // Protect from normal clicks if the tag is marked as persistent; it must be
                    // long-pressed to remove persistence before it can be selected normally
                    if (!mTagPersistent[gg].get(position)) {
                        applyNewSelection(gg, position, SELECTION_NORMAL);

                        // Tell the adapter to refresh the view contents
                        mListAdapters[gg].notifyDataSetChanged();
                    }

                }


            });

            listViews[g].setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                    applyNewSelection(gg, position, SELECTION_PERSISTENT);

                    // Tell the adapter to refresh the view contents
                    mListAdapters[gg].notifyDataSetChanged();

                    return true;
                }
            });

        }
    }

    protected int changeColorValue(int color, float valueFactor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= valueFactor;
        return Color.HSVToColor(hsv);
    }

    protected int changeColorSat(int color, float satFactor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] *= satFactor;
        return Color.HSVToColor(hsv);
    }

    public void writeTagsToDb() {


        // Check if tags already exist
        // Make a new int list to contain all selected tag IDs
        ArrayList<Long> newTagIds = new ArrayList<>();
        ArrayList<Long> newPersistentTagIds = new ArrayList<>();

        int nTag;
        long currentId;

        // If not, for each tag category:
        for (int g=0; g<mNumTagGroups; g++) {

            nTag = mListAdapters[g].getCount();

            // Get the selected tags in the listView and add to the list
            for (int t=0; t<nTag; t++) {

                currentId = mTagIds[g].get(t);

                // If the current tag was not selected, skip it
                if (mTagSelected[g].get(t)) {
                    newTagIds.add(currentId);
                }

                if (mTagPersistent[g].get(t)) {
                    newPersistentTagIds.add(currentId);
                }
            }

        }

        // Now write the persistent tags. As with the standard tags, the persistent tags are
        // appropriately written to the pre and post fields according to the current instance type

        // If this is a PRETURN instance, we want to:
        // 1) Update the preturn persistent tags in accordance with any changes made
        // 2) Transfer any changes made forward to the post-turn persistent tags
        if (mTurnTime == TurnDbUtils.TURNTIME_PRE) {

            // First set normal tags
            TurnDbUtils.turnSetTagIds(mDb, mTurnId, newTagIds, TurnDbUtils.TAG_TYPE_PRETURN, mFlag);

            // Set persistent tags for PRE- and POST-turn
            TurnDbUtils.turnSetTagIds(mDb, mTurnId, newPersistentTagIds, TurnDbUtils.TAG_TYPE_PERSISTENT_PRE, mFlag);
            TurnDbUtils.turnSetTagIds(mDb, mTurnId, newPersistentTagIds, TurnDbUtils.TAG_TYPE_PERSISTENT_POST, mFlag);
        }

        // If this is a POSTTURN instance, we just need update the post-turn persistent tag ids.
        // (It wouldn't make sense to carry back the current persistent tags to the pre-turn state)
        else if (mTurnTime == TurnDbUtils.TURNTIME_POST) {

            // First set normal tags
            TurnDbUtils.turnSetTagIds(mDb, mTurnId, newTagIds, TurnDbUtils.TAG_TYPE_POSTTURN, mFlag);

            // Set persistent tags for ONLY POST-turn
            TurnDbUtils.turnSetTagIds(mDb, mTurnId, newPersistentTagIds, TurnDbUtils.TAG_TYPE_PERSISTENT_POST, mFlag);
        }

    }

    protected long addNewBlankTagGroup() {

        final String newGroupBaseName = "new tag group";
        int newGroupNumber = 1;

        boolean isUnique = false;
        boolean matched;

        String newGroupName = newGroupBaseName + " " + Integer.toString(newGroupNumber);

        while (!isUnique) {

            matched = false;

            for (int g = 0; g < mNumTagGroups; g++) {
                if (mTagGroupNames[g].equals(newGroupName)) {
                    matched = true;
                }
            }

            if (matched) {
                newGroupNumber++;
                newGroupName = newGroupBaseName + " " + Integer.toString(newGroupNumber);
            }
            else {
                isUnique = true;
            }

        }

        return TurnDbUtils.tagGroupAddEntry(
                mDb,
                newGroupName,
                TurnDbUtils.SPECIFY_NONE,
                getResources().getString(R.string.tag_group_new_colour),
                null,
                TurnDbUtils.SPECIFY_BOTH,
                TurnDbUtils.SINGLE_CONSTRAINT_OFF,
                "hint text goes here"
        );

    }

    protected void initializeArrays() {
        // Called once the tags have been imported and the number of groups determined

        mListAdapters = new TagAdapter[mNumTagGroups];
        listViews = new ListView[mNumTagGroups];

        // ArrayLists of available/chosen tags
        mTagStrings = new ArrayList[mNumTagGroups];
        mTagIds = new ArrayList[mNumTagGroups];
        mTagSelected = new ArrayList[mNumTagGroups];
        mTagPersistent = new ArrayList[mNumTagGroups];
        mTagInUse = new ArrayList[mNumTagGroups];

        // Arrays of info about the tag groups
        mTagGroupIds = new long[mNumTagGroups];
        mTagGroupColours = new int[mNumTagGroups];
        mTagGroupMandatory = new int[mNumTagGroups];
        mTagGroupNames = new String[mNumTagGroups];
        mTagGroupInUse = new int[mNumTagGroups];
        mTagGroupSingleConstraint = new int[mNumTagGroups];
        mTagGroupHint = new String[mNumTagGroups];
        mTagGroupPanel = new View[mNumTagGroups];
        mNumTags = new int[mNumTagGroups];

        for (int g = 0; g<mNumTagGroups; g++) {
            // Initialize the available and chosen tag array lists
            mTagStrings[g] = new ArrayList<>();
            mTagIds[g] = new ArrayList<>();
            mTagSelected[g] = new ArrayList<>();
            mTagPersistent[g] = new ArrayList<>();
            mTagInUse[g] = new ArrayList<>();
        }

    }

    private void applySingleConstraint(int tagGroupIndex, int selectedTagIndex, int selectionType) {
        // Allows only one tag in a group to be selected, either persistently or normally.

        // Remove all normal and persistent selections from the group
        for (int i=0; i<mTagSelected[tagGroupIndex].size(); i++) {
            mTagSelected[tagGroupIndex].set(i, false);
            mTagPersistent[tagGroupIndex].set(i, false);
        }

        // Apply the single selection to the appropriate array, depending on the type
        if (selectionType == SELECTION_NORMAL) {
            mTagSelected[tagGroupIndex].set(selectedTagIndex, true);
        }
        else if (selectionType == SELECTION_PERSISTENT) {
            mTagPersistent[tagGroupIndex].set(selectedTagIndex, true);
        }

    }

    public boolean checkMandatoryConstraints() {

        int numUnsatisfied = 0;

        boolean anySelected;

        // Iterate through all tag groups
        for(int g=0; g<mNumTagGroups; g++) {

            boolean checkGroup = GeneralUtils.turnTimeComb2Single(mTagGroupMandatory[g], mTurnTime);

            // Skip the current group if the mandatory constraint doesn't apply
            if (!checkGroup) { continue; }

            anySelected = false;

            // Checks if there are any true values in the boolean mTagSelected arraylist of a tag groups
            for (int t=0; t<mTagSelected[g].size(); t++) {

                if (mTagSelected[g].get(t) || mTagPersistent[g].get(t)) {
                    anySelected = true;
                }

            }

            // If no true values found, increment the counter of unsatisfied mandatory constraints
            if (!anySelected) {numUnsatisfied++;}

        }

        if (numUnsatisfied==1) {
            Toast.makeText(getContext(), Integer.toString(numUnsatisfied) +
                    " mandatory tag group has no selected values", Toast.LENGTH_LONG).show();
        }
        else if (numUnsatisfied>1) {
            Toast.makeText(getContext(), Integer.toString(numUnsatisfied) +
                    " mandatory tag groups have no selected values", Toast.LENGTH_LONG).show();
        }

        // Return true if all constraints satisfied
        return numUnsatisfied==0;

    }

    public int getTagEditMode() {
        return mTagEditMode;
    }

    public int getTurnTime() {
        return mTurnTime;
    }



}
