package com.brick.youscrew.graphics;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.Session;
import com.brick.youscrew.data.Tetrode;
import com.brick.youscrew.data.Turn;
import com.brick.youscrew.data.TurnContract;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by windows on 10.04.2016.
 */
public class TimeDepthGraph extends View {

    private static final String LOG_TAG = TimeDepthGraph.class.getSimpleName();

    private static final int LINE_WIDTH_FOCUS = 8;
    private static final int LINE_WIDTH_NONFOCUS = 3;

    private final float DEPTH_TICK_INTERVAL = 200;

    private final int DATA_DEPTH = 1;
    private final int DATA_SESSION_NUMBER = 2;
    private final int DATA_TIME = 3;

    private Tetrode[] mTetrodes;
    private Rat mRat;
    private Session[] mSessions;

    private Line[] mLayers;
    private Axis[] mAxes;

    private int mLineFocusIndex = 0;

    private LayerDrawable mLayerDrawable;

    private float mMaxTetrodeDepth;

    private float mScaleX, mScaleY;

    private boolean mLayoutInitialized = false;

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public TimeDepthGraph(Context context) {
        super(context);
    }

    public TimeDepthGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setContent(SQLiteDatabase db, Tetrode[] tetrodes) {

        if (tetrodes.length == 0) {
            throw new IllegalArgumentException("Tetrode array length of zero");
        }

        mTetrodes = tetrodes;

        mRat = tetrodes[0].getRat();

        // Get the sessions and sort by ascending time
        mSessions = mRat.findSessions(db, null);
        List<Session> sessionList = Arrays.asList(mSessions);
        Collections.reverse(sessionList);
        mSessions = (Session[]) sessionList.toArray();

        float currentDepth;

        // Determine maximum depth of each TT
        for (Turn turn : mSessions[mSessions.length-1].findTurns(db)) {
            currentDepth = (float) turn.getDepth();
            if (currentDepth > mMaxTetrodeDepth) {
                mMaxTetrodeDepth = currentDepth;
            }
        }

        Rect visibleRect = new Rect();

        getGlobalVisibleRect(visibleRect);
        visibleRect.toString();

        mScaleX = (visibleRect.right - visibleRect.left - Axis.MARGIN_LEFT - Axis.MARGIN_RIGHT) / mSessions.length;
        mScaleY = (visibleRect.bottom - visibleRect.top - 2*Axis.MARGIN_Y) / mMaxTetrodeDepth;

        makeAxes();

        mLayers = makeLayers(db);

        mLayoutInitialized = true;


    }

    public void setLineFocus(int index) {

        if (!mLayoutInitialized) { return; }

        int lastIndex = mLineFocusIndex;
        int newIndex = index;

        Line line = mLayers[lastIndex];
        line.setStrokeWidth(LINE_WIDTH_NONFOCUS);
        line.setDotRadius((int)(LINE_WIDTH_NONFOCUS*1.5));

        line = mLayers[newIndex];
        line.setStrokeWidth(LINE_WIDTH_FOCUS);
        line.setDotRadius((int)(LINE_WIDTH_FOCUS*1.5));

        mLineFocusIndex = newIndex;

        invalidate();

    }

    private void makeAxes() {
        RectF dataBounds = new RectF(0f, 0f, mSessions.length, mMaxTetrodeDepth);
        mAxes = new Axis[2];
        mAxes[0] = makeAxes(Axis.DIRECTION_X, DATA_SESSION_NUMBER, dataBounds);
        mAxes[1] = makeAxes(Axis.DIRECTION_Y, DATA_DEPTH, dataBounds);
    }

    private Line[] makeLayers(SQLiteDatabase db) {

        Line[] layers = new Line[mTetrodes.length];

        for (int t = 0; t < mTetrodes.length; t++) {
            Random rnd = new Random();
            int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256) );
            layers[t] = makeTetrodePlot(db, mTetrodes[t], DATA_SESSION_NUMBER, DATA_DEPTH, color);
        }

        return layers;

    }

    private Axis makeAxes(int direction, int data, RectF dataBounds)        {

        float[] tickPositions;
        String[] tickStrings;

        String title;

        int numTicks;

        switch (data) {

            case DATA_DEPTH :

                numTicks = (int) Math.ceil( mMaxTetrodeDepth / DEPTH_TICK_INTERVAL);

                tickPositions = new float[numTicks];
                tickStrings = new String[numTicks];
                title = "Depth / microns";

                for (int t = 0; t < numTicks; t++) {
                    tickPositions[t] = DEPTH_TICK_INTERVAL * t;
                    tickStrings[t] = Float.toString(tickPositions[t]);
                }

                break;

            case DATA_SESSION_NUMBER :

                numTicks = mSessions.length;

                tickPositions = new float[numTicks];
                tickStrings = new String[numTicks];
                title = "Session number";

                for (int t = 0; t < numTicks; t++) {
                    tickPositions[t] = t;
                    tickStrings[t] = Integer.toString(t);
                }
                break;

            case DATA_TIME :

                numTicks = mSessions.length;

                tickPositions = new float[numTicks];
                tickStrings = new String[numTicks];
                title = "Session time";

                Date date = new Date();

                for (int t = 0; t < numTicks; t++) {
                    date.setTime(mSessions[t].getLong(TurnContract.SessionEntry.COLUMN_TIME_START));
                    tickPositions[t] = t;
                    tickStrings[t] = mDateFormat.format(date);
                }
                break;

            default :

                throw new IllegalArgumentException("Illegal value for argument 'data'");

        }

        Axis axis = new Axis(getContext(), direction, dataBounds);

        axis.setStrokeWidth(8f);
        axis.setScaleX(mScaleX);
        axis.setScaleY(mScaleY);
        axis.setTicks(tickPositions, tickStrings);
        axis.setTitle(title);
        axis.setShowGrid(true);

        return axis;

    }

    private Line makeTetrodePlot(SQLiteDatabase db, Tetrode tetrode, int dataX, int dataY, int color) {


        float[] x = new float[mSessions.length];
        float[] y = new float[mSessions.length];

        for (int t = 0; t < mSessions.length; t++) {

            Session session = mSessions[t];

            Turn turn = session.findTurnByTetrode(db, tetrode);

            switch (dataX) {
                case DATA_TIME:
                    x[t] = (float) session.getLong(TurnContract.SessionEntry.COLUMN_TIME_START);
                    break;

                case DATA_SESSION_NUMBER:
                    x[t] = (float) t;
                    break;

                case DATA_DEPTH:
                    x[t] = (float) turn.getDepth();
                    break;

                default:
                    throw new IllegalArgumentException("Illegal data type value for X");

            }

            switch (dataY) {
                case DATA_TIME:
                    y[t] = (float) session.getLong(TurnContract.SessionEntry.COLUMN_TIME_START);
                    break;

                case DATA_SESSION_NUMBER:
                    y[t] = (float) t;
                    break;

                case DATA_DEPTH:
                    y[t] = (float) turn.getDepth();
//                    Log.v(LOG_TAG, "Depth = " + Float.toString(y[t]));
                    break;

                default:
                    throw new IllegalArgumentException("Illegal data type value for Y");
            }

            x[t] = x[t]*mScaleX + Axis.MARGIN_LEFT;
            y[t] = y[t]*mScaleY + Axis.MARGIN_LEFT;

//            Log.v(LOG_TAG, "x = " + Float.toString(x[t]) + ", y = " + Float.toString(y[t]));

        }

        Line line = new Line(getContext(), x, y);
        line.setColor(color);
        line.setStrokeWidth(5);
        line.setDotRadius(7);

        return line;

    }

    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (mLayoutInitialized) {

            for (Axis axis : mAxes) {
                axis.draw(canvas);
            }

            for (Line layer : mLayers) {
                layer.draw(canvas);
            }
        }

    }

}
