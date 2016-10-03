package com.brick.youscrew.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.brick.youscrew.R;
import com.brick.youscrew.data.Tag;
import com.brick.youscrew.data.TurnContract;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by windows on 12/07/2016.
 */
public class TagChart extends View {

    private static final String LOG_TAG = TagChart.class.getSimpleName();

    private static final boolean SHOW_PERSISTENT_BARS = false;

    private final int CIRCLE_DIAMETER = 12;

    private final int[] TAG_TYPES = {
            TurnDbUtils.TAG_TYPE_PRETURN,
            TurnDbUtils.TAG_TYPE_PERSISTENT_PRE,
            TurnDbUtils.TAG_TYPE_POSTTURN,
            TurnDbUtils.TAG_TYPE_PERSISTENT_POST};

    private final boolean[] isPersistent = {false, true, false, true};

    private List<ShapeDrawable> mCircles;

    private float mScaleX, mScaleY;

    private boolean[] mIsPre, mIsPersistentPre, mIsPost, mIsPersistentPost;

    private Tag mTag;

    public TagChart(Context context) {
        super(context);
    }

    public TagChart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setContent(Tag tag, boolean[] isPre, boolean[] isPersistentPre, boolean[] isPost,
                           boolean[] isPersistentPost) {

        mTag = tag;
        mIsPre = isPre.clone();
        mIsPersistentPre = isPersistentPre.clone();
        mIsPost = isPost.clone();
        mIsPersistentPost = isPersistentPost.clone();


    }


    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        int numTurns = mIsPre.length;

        Rect visibleRect = new Rect();

        getGlobalVisibleRect(visibleRect);

        mScaleX = (visibleRect.right - visibleRect.left - 2*Axis.MARGIN_LEFT) / numTurns;
        mScaleY = (visibleRect.bottom - visibleRect.top - 2*Axis.MARGIN_LEFT) / numTurns;


        mCircles = new ArrayList<>();


        boolean isPersistentRun = false;
        int persistentRunStart = 0;

        for (int t=0; t<numTurns; t++) {

            if (mIsPre[t]) {
                drawOneCircle(canvas, t, 10, R.color.colorTag);
            }

            if (mIsPost[t]) {
                drawOneCircle(canvas, t, 25, R.color.colorTag);
            }

            if (SHOW_PERSISTENT_BARS) {
                // Check if current turn has a persistent mark
                if (mIsPersistentPre[t] || mIsPersistentPost[t]) {
                    if (!isPersistentRun) {
                        isPersistentRun = true;
                        persistentRunStart = t;
                    }
                }
                // If not, check if this is the end of a persistent run
                else if (isPersistentRun) {
                    isPersistentRun = false;
                    drawPersistentRun(canvas, persistentRunStart, t - 1);
                }
            }
            else {
                if (mIsPersistentPre[t]) {
                    drawOneCircle(canvas, t, 10, R.color.black);
                }
                if (mIsPersistentPost[t]) {
                    drawOneCircle(canvas, t, 25, R.color.black);
                }
            }


        }


        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        int fontSize = GeneralUtils.dip2Pix(getContext(), 8);
        paint.setTextSize(fontSize);
        paint.setTextAlign(Paint.Align.LEFT);

        String tagName = mTag.getString(TurnContract.TagEntry.COLUMN_NAME);
        Log.v(LOG_TAG, "Tag text = "  + tagName);
        canvas.drawText(tagName, 25, fontSize+5, paint);


        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        paint.setPathEffect( new DashPathEffect(new float[] {10, 40}, 0) );
        canvas.drawLine(visibleRect.left, 0, visibleRect.right, 0, paint);
//        canvas.drawPath();
    }

    private void drawOneCircle(Canvas canvas, int t, int y, int color) {
        int x = (int) (Axis.MARGIN_LEFT + t*mScaleX);
        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.getPaint().setColor(getResources().getColor(color));
        circle.setBounds(x, y, x + CIRCLE_DIAMETER, y + CIRCLE_DIAMETER);
        circle.draw(canvas);
    }

    private void drawPersistentRun(Canvas canvas, int tStart, int tEnd) {

        int y = 15;
        int circRad = CIRCLE_DIAMETER / 2;

        drawOneCircle(canvas, tStart, y, R.color.black);
        drawOneCircle(canvas, tEnd, y, R.color.black);

        int x1 = (int) (Axis.MARGIN_LEFT + tStart*mScaleX);
        int x2 = (int) (Axis.MARGIN_LEFT + tEnd*mScaleX);

        if (tStart != tEnd) {
            ShapeDrawable bar = new ShapeDrawable(new RectShape());
            bar.getPaint().setColor(getResources().getColor(R.color.black));
            bar.setBounds(x1 + circRad, y, x2 + circRad, y + CIRCLE_DIAMETER);
            bar.draw(canvas);
        }
    }

}
