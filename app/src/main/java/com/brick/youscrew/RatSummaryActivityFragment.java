package com.brick.youscrew;

import android.app.Fragment;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.Tetrode;
import com.brick.youscrew.data.TurnContract;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.graphics.TimeDepthGraph;

/**
 * A placeholder fragment containing a simple view.
 */
public class RatSummaryActivityFragment extends Fragment {

    private View mRootView;

    private TimeDepthGraph mTimeDepthGraph;

    private Rat mRat;
    private Tetrode[] mTetrodes;

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


        /**
         * MAKE THE ROW OF TETRODE SELECTOR BUTTONS
         */

        LinearLayout ttButtonLayout = (LinearLayout) mRootView.findViewById(R.id.layout_tt_buttons);

        final TextView[] ttButtons = new TextView[mTetrodes.length];

        for (int t = 0; t<mTetrodes.length; t++) {

            final int tt = t;

            ttButtons[t] = (TextView) inflater.inflate(R.layout.button_tt_rat_summary, ttButtonLayout, false);

            int ttNumber = mTetrodes[t].getInt(TurnContract.TetrodeEntry.COLUMN_INDEX) + 1;

            ttButtons[t].setText(Integer.toString(ttNumber));
            ttButtons[t].setBackgroundColor(Color.DKGRAY);

            ttButtonLayout.addView(ttButtons[t]);

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
        ttButtons[0].callOnClick();

        return mRootView;

    }

    private void ttButtonCallback(int buttonIndex) {
        Toast.makeText(
                getActivity(),
                mTetrodes[buttonIndex].getString(TurnContract.TetrodeEntry.COLUMN_NAME) + " selected",
                Toast.LENGTH_SHORT).show();
    }

}
