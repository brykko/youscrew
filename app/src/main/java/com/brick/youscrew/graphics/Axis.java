package com.brick.youscrew.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Created by windows on 12.04.2016.
 */
public class Axis extends Drawable {

    public static final int DIRECTION_X = 1;
    public static final int DIRECTION_Y = 2;

    public static final float MARGIN_Y = 150;
    public static final float MARGIN_LEFT = 200;
    public static final float MARGIN_RIGHT = 40;
    public final float TICK_OFFSET_STANDARD = 10;

    private Paint mPaintLine, mPaintTickLabel, mPaintTitle, mPaintGrid;

    private float mRightMargin;
    private float mLeftMargin;
    private float mYMargin;

    private float mTickOffset;

    private float mScaleX = 1;
    private float mScaleY = 1;

    private RectF mBounds;

//    private float mExtentLower, mExtentUpper;

    private String mTitle = null;

    private int mDirection;

    private boolean mShowGrid = false;

    // TICKS
    private int mNumTicks = 0;
    private float[] mTickPos;
    private String[] mTickLabels;

    private Context mContext;


    public Axis(Context context, int direction, RectF dataBounds) {

        super();

        if (direction != DIRECTION_X && direction != DIRECTION_Y) {
            throw new IllegalArgumentException("Illegal value '" + Integer.toString(direction) +
            "' for argument 'direction'");
        }


        mContext = context;

        mBounds = dataBounds;


        mLeftMargin = MARGIN_LEFT;
        mRightMargin = MARGIN_RIGHT;
        mYMargin = MARGIN_Y;

        mTickOffset = TICK_OFFSET_STANDARD;

        mDirection = direction;

        mPaintLine = new Paint();
        mPaintLine.setStyle(Paint.Style.STROKE);
        mPaintLine.setColor(Color.WHITE);
        mPaintLine.setStrokeWidth(5);

        mPaintTickLabel = new Paint();
        mPaintTickLabel.setColor(Color.WHITE);
        mPaintTickLabel.setTextSize(20);

        mPaintGrid = new Paint();
        mPaintGrid.setStyle(Paint.Style.STROKE);
        mPaintGrid.setColor(Color.GRAY);
        mPaintGrid.setStrokeWidth(2);
        mPaintGrid.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));

        if (direction == DIRECTION_X) {
            mPaintTickLabel.setTextAlign(Paint.Align.LEFT);
        }
        else {
            mPaintTickLabel.setTextAlign(Paint.Align.RIGHT);
        }

        mPaintTitle = new Paint(mPaintTickLabel);
        mPaintTitle.setTextSize(30);
        mPaintTitle.setTextAlign(Paint.Align.CENTER);

        makePath();

    }

    public void setTicks(float[] positions, String[] labels) {

        if (positions.length != labels.length) {
            throw new IllegalArgumentException("Arguments tickPos and tickLabels must be 1-D arrays of equal size");
        }

        mTickPos = positions;
        mTickLabels = labels;
        mNumTicks = positions.length;

    }

    public void setMargins(float left, float right, float y) {
        if (left < 0 || right < 0 || y < 0) {
            throw new IllegalArgumentException("Offset value cannot be negative");
        }
        mLeftMargin = left;
        mRightMargin = right;
        mYMargin = y;
    }

    public void setTickOffset(float value) {
        mTickOffset = value;
    }

    public void setScaleX(float value) {
        mScaleX = value;
    }

    public void setScaleY(float value) {
        mScaleY = value;
    }

    public void setShowGrid(boolean value) {
        mShowGrid = value;
    }

    public void setTickLabelTextSize(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Font size must be > 0");
        }
        mPaintTickLabel.setTextSize(value);
    }

    public void setTitleTextSize(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Font size must be > 0");
        }
        mPaintTitle.setTextSize(value);
    }

    public void setTitle(String value) {
        mTitle = value;
    }

    public void setTextSize(float textSize) {
        mPaintTickLabel.setTextSize(textSize);
    }

    public void setStrokeWidth(float strokeWidth) {
        mPaintTickLabel.setStrokeWidth(strokeWidth);
        mPaintLine.setStrokeWidth(strokeWidth);
    }

    public void setAlpha(int alpha) {
        mPaintLine.setAlpha(alpha);
        mPaintTickLabel.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaintLine.setColorFilter(colorFilter);
        mPaintTickLabel.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    private Path makePath() {

        Path path = new Path();

        if (mDirection == DIRECTION_X) {

            path.moveTo(mLeftMargin, mYMargin);

            for (int t=0; t < mNumTicks; t++) {
                float x = mLeftMargin + mTickPos[t]*mScaleX;
                path.lineTo(x, mYMargin);
                path.lineTo(x, mYMargin -mTickOffset);
                path.lineTo(x, mYMargin);
            }

//            path.lineTo(mBounds.right + mRightMargin, mYMargin);

        }

        else {

            path.moveTo(mLeftMargin, mYMargin);

            for (int t=0; t < mNumTicks; t++) {
                float y = mYMargin + mTickPos[t]*mScaleY;
                path.lineTo(mLeftMargin, y);
                path.lineTo(mLeftMargin -mTickOffset, y);
                path.lineTo(mLeftMargin, y);
            }

//            path.lineTo(mLeftMargin,  mYMargin);

        }

        return path;

    }

    @Override
    public void draw(Canvas canvas) {

        // First, draw the axis line
        canvas.drawPath(makePath(), mPaintLine);

        // Now generate the ticks
        for (int t = 0; t < mTickPos.length; t++) {

            float x, y, xText, yText;

            if (mDirection == DIRECTION_X) {
                x = mLeftMargin + mTickPos[t]*mScaleX;
                xText = x + mPaintTickLabel.getTextSize()/2;
                y = mYMargin - mTickOffset;
                yText = y;
            }
            else {
                x = mLeftMargin - mTickOffset;
                xText = x;
                y = mYMargin + mTickPos[t]*mScaleY;
                yText = y + mPaintTickLabel.getTextSize()/2;
            }

            if (mDirection == DIRECTION_X) {
                canvas.save();
                canvas.rotate(-90, x, y);
                canvas.drawText(mTickLabels[t], xText, yText, mPaintTickLabel);
                canvas.restore();

                if (mShowGrid) {
                    Path path = new Path();
                    path.moveTo(x, mYMargin);
                    path.lineTo(x, mYMargin +(mBounds.bottom-mBounds.top)*mScaleY);
                    canvas.drawPath(path, mPaintGrid);
//                    canvas.drawLine(x, mYMargin, x, mYMargin +(mBounds.bottom-mBounds.top)*mScaleY, mPaintGrid);
                }

            }
            else {
                canvas.drawText(mTickLabels[t], xText, yText, mPaintTickLabel);

                if (mShowGrid) {
                    Path path = new Path();
                    path.moveTo(mLeftMargin, y);
                    path.lineTo(mLeftMargin+(mBounds.right-mBounds.left)*mScaleX, y);
                    canvas.drawPath(path, mPaintGrid);
//                    canvas.drawLine(mLeftMargin, y, mLeftMargin+(mBounds.right-mBounds.left)*mScaleX, y, mPaintGrid);
                }

            }

        }

        // Finally, draw the title
        if (mTitle != null) {

            float x, y;

            if (mDirection == DIRECTION_Y) {

                x = mLeftMargin/2 - mPaintTitle.getTextSize()/2;
                y = mYMargin + (mBounds.top + mBounds.bottom) / 2 * mScaleY;

                canvas.save();
                canvas.rotate(-90, x, y);
                canvas.drawText(mTitle, x, y, mPaintTitle);
                canvas.restore();
            }
            else {
                x = mLeftMargin + (mBounds.left + mBounds.right)/2 * mScaleX;
                y = mYMargin /2 - mPaintTitle.getTextSize()/2;
                canvas.drawText(mTitle, x, y, mPaintTitle);
            }
        }

    }
}
