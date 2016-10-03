package com.brick.youscrew;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.Tag;
import com.brick.youscrew.data.TagGroup;
import com.brick.youscrew.data.Tetrode;
import com.brick.youscrew.data.Turn;
import com.brick.youscrew.data.TurnContract.*;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.graphics.TagChart;
import com.brick.youscrew.graphics.TimeDepthGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class RatSummaryActivityFragment extends Fragment {

    public static final String LOG_TAG = RatSummaryActivityFragment.class.getSimpleName();

    private View mRootView;

    private TimeDepthGraph mTimeDepthGraph;

    private Rat mRat;
    private Tetrode[] mTetrodes;
    private List<TagGroup> mTagGroups;
    private Turn[] mTurns;

    private SQLiteDatabase mDb;

    private int mTetrodeSelectedIndex;

    private boolean mGraphInitialized = false;

    public RatSummaryActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mDb = TurnDbHelper.getInstance(null).getWritableDatabase();

        mRootView = inflater.inflate(R.layout.fragment_rat_summary, container, false);

        mTimeDepthGraph = (TimeDepthGraph) mRootView.findViewById(R.id.time_depth_graph);

        mRat = getActivity().getIntent().getParcelableExtra("rat");
        mTetrodes = mRat.findTetrodes(mDb, null);

        mTimeDepthGraph.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!mGraphInitialized) {
                    mTimeDepthGraph.setContent(mDb, mRat.findTetrodes(mDb, null));

                    mGraphInitialized = true;
                }
            }
        });

        mTagGroups = Arrays.asList(TagGroup.findAll(mDb));


        /**
         * MAKE THE ROW OF TETRODE SELECTOR BUTTONS
         */

        LinearLayout ttButtonLayout = (LinearLayout) mRootView.findViewById(R.id.layout_tt_buttons);

        final TextView[] ttButtons = new TextView[mTetrodes.length];

        for (int t = 0; t<mTetrodes.length; t++) {

            final int tt = t;

            ttButtons[t] = (TextView) inflater.inflate(R.layout.button_tt_rat_summary, ttButtonLayout, false);

            int ttNumber = mTetrodes[t].getInt(TetrodeEntry.COLUMN_INDEX) + 1;

            ttButtons[t].setText(Integer.toString(ttNumber));
            ttButtons[t].setBackgroundColor(Color.DKGRAY);

            ttButtonLayout.addView(ttButtons[t]);

            // Assign a listener that highlights the pressed button and executes the ttButtonCallback() method
            ttButtons[t].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    TextView oldButton = ttButtons[mTetrodeSelectedIndex];
                    TextView newButton = ttButtons[tt];

                    oldButton.setBackgroundColor(Color.DKGRAY);
                    oldButton.setTextColor(Color.WHITE);

                    newButton.setBackgroundColor(Color.CYAN);
                    newButton.setTextColor(Color.BLACK);

                    mTetrodeSelectedIndex = tt;
                    ttButtonCallback(tt);

                }
            });
        }

        // Select TT1
        refreshTagCharts(0);

        return mRootView;

    }

    private void ttButtonCallback(int buttonIndex) {
        refreshTagCharts(buttonIndex);
        mTimeDepthGraph.setLineFocus(buttonIndex);
    }

    private void refreshTagCharts(int tetrodeIndex) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        Tetrode tetrode = mTetrodes[tetrodeIndex];

        // Get array of all turn events for the current tetrode
        Turn[] turns = tetrode.findTurns(mDb);
        // Reverse the array; we need it chronologically ascending
        List<Turn> turnList = Arrays.asList(turns);
        Collections.reverse(turnList);
        turns = (Turn[]) turnList.toArray();


        List<long[]> tagIdsPre        = new ArrayList<>();
        List<long[]> tagIdsPerPre     = new ArrayList<>();
        List<long[]> tagIdsPost       = new ArrayList<>();
        List<long[]> tagIdsPerPost    = new ArrayList<>();

        final boolean[] isPre     = new boolean[turns.length];
        final boolean[] isPerPre  = new boolean[turns.length];
        final boolean[] isPost    = new boolean[turns.length];
        final boolean[] isPerPost = new boolean[turns.length];

        // Collate all the tag ids of each tag type, for all turns
        for (Turn turn : turns) {
            tagIdsPre.add(turn.getTagIds(TurnDbUtils.TAG_TYPE_PRETURN));
            tagIdsPerPre.add(turn.getTagIds(TurnDbUtils.TAG_TYPE_PERSISTENT_PRE));
            tagIdsPost.add(turn.getTagIds(TurnDbUtils.TAG_TYPE_POSTTURN));
            tagIdsPerPost.add(turn.getTagIds(TurnDbUtils.TAG_TYPE_PERSISTENT_POST));
        }

        // Get the tag group layout and clear it of all pre-existing views
        LinearLayout tagGroupLayout = (LinearLayout) mRootView.findViewById(R.id.tag_group_linlayout);
        tagGroupLayout.removeAllViews();

        // For each tag group
        for (int g=0; g<mTagGroups.size(); g++) {

            TagGroup group = mTagGroups.get(g);

            // Add a new tag group panel.
            LinearLayout tagPanel = (LinearLayout) inflater.inflate(R.layout.list_item_tag_group, null, false);
            tagGroupLayout.addView(tagPanel);

            String color = group.getString(TagGroupEntry.COLUMN_COLOUR);
//            Log.v(LOG_TAG, "Color string = '" + color + "'");
            tagPanel.setBackgroundColor(Color.parseColor(color));

            TextView title = (TextView)tagPanel.findViewById(R.id.textview_title);
            title.setText(group.getString(TagEntry.COLUMN_NAME));

            LinearLayout tagChartPanel = (LinearLayout) tagPanel.findViewById(R.id.layout_tags);

            // Get all tags in the group
            Tag[] tags = group.findTags(mDb);

            for (final Tag tag : tags) {

                tag.updateFromDb(mDb);

                // Create a new tag chart
                final TagChart tagChart = new TagChart(getActivity());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,45);
                tagChart.setLayoutParams(layoutParams);
                tagChartPanel.addView(tagChart);

                for (int t=0; t<turns.length; t++) {

                    for (int n=0; n<4; n++) {

                        boolean[] hasTagArray = null;
                        List<long[]> tagIdList = null;

                        switch (n) {
                            case 0 :
                                hasTagArray = isPre;
                                tagIdList = tagIdsPre;
                                break;
                            case 1 :
                                hasTagArray = isPerPre;
                                tagIdList = tagIdsPerPre;
                                break;
                            case 2 :
                                hasTagArray = isPost;
                                tagIdList = tagIdsPost;
                                break;
                            case 3 :
                                hasTagArray = isPerPost;
                                tagIdList = tagIdsPerPost;
                                break;
                        }

                        // Get the list of tag Ids
                        long[] tagIds = tagIdList.get(t);

                        if (tagIds == null) {
                            hasTagArray[t] = false;
//                            Log.v(LOG_TAG, "Tag ids = null");
                        }
                        else {

//                            Log.v(LOG_TAG, "Tag ids = " + tagIds.toString());

                            // Check if the current tag exists in the tagId array for the current turn
                            boolean hasTag = false;
                            for (long tagId : tagIds) {
                                if (tagId == tag.getId()) {
                                    hasTag = true;
                                }
                            }
                            hasTagArray[t] = hasTag;
                        }

                    }

                }

                tagChart.setContent(tag, isPre, isPerPre, isPost, isPerPost);

            }

        }
    }


}
