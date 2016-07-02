package com.brick.youscrew.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Created by windows on 12.04.2016.
 */
public class Axis extends Drawable {

    public static final int DIRECTION_X = 1;
    public static final int DIRECTION_Y = 2;

    public static final float OFFSET_STANDARD = 150;
    public final float TICK_OFFSET_STANDARD = 10;

    private Paint mPaintLine, mPaintTickLabel, mPaintTitle, mPaintGrid;

    private float mOffset;
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

//        if (lowerValue > upperValue) {
//            throw new IllegalArgumentException("Argument 'upperValue' must has a greater value " +
//                    "than argument 'lowerValue'");
//        }

        mContext = context;

        mBounds = dataBounds;

//        mExtentLower = lowerValue;
//        mExtentUpper = upperValue;

        mOffset = OFFSET_STANDARD;
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
        mPaintGrid.setColor(Color.LTGRAY);
        mPaintGrid.setStrokeWidth(2);
        mPaintGrid.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));

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

    public void setOffset(float value) {
        if (value < 0) {
            throw new IllegalArgumentException("Offset value cannot be negative");
        }
        mOffset = value;
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

            path.moveTo(mBounds.left + mOffset, mOffset);

            for (int t=0; t < mNumTicks; t++) {
                float x = mOffset + mTickPos[t]*mScaleX;
                path.lineTo(x, mOffset);
                path.lineTo(x, mOffset-mTickOffset);
                path.lineTo(x, mOffset);
            }

            path.lineTo(mBounds.right + mOffset, mOffset);

        }

        else {

            path.moveTo(mOffset, mBounds.top + mOffset);

            for (int t=0; t < mNumTicks; t++) {
                float y = mOffset + mTickPos[t]*mScaleY;
                path.lineTo(mOffset, y);
                path.lineTo(mOffset-mTickOffset, y);
                path.lineTo(mOffset, y);
            }

            path.lineTo(mOffset, mBounds.bottom + mOffset);

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
                x = mOffset + mTickPos[t]*mScaleX;
                xText = x + mPaintTickLabel.getTextSize()/2;
                y = mOffset - mTickOffset;
                yText = y;
            }
            else {
                x = mOffset - mTickOffset;
                xText = x;
                y = mOffset + mTickPos[t]*mScaleY;
                yText = y + mPaintTickLabel.getTextSize()/2;
            }

            if (mDirection == DIRECTION_X) {
                canvas.save();
                canvas.rotate(-90, x, y);
                canvas.drawText(mTickLabels[t], xText, yText, mPaintTickLabel);
                canvas.restore();

                if (mShowGrid) {
                    canvas.drawLine(x, mOffset, x, mOffset+(mBounds.bottom-mBounds.top)*mScaleY, mPaintGrid);
                }

            }
            else {
                canvas.drawText(mTickLabels[t], xText, yText, mPaintTickLabel);

                if (mShowGrid) {
                    canvas.drawLine(mOffset, y, mOffset+(mBounds.right-mBounds.left)*mScaleX, y, mPaintGrid);
                }

            }

        }

        // Finally, draw the title
        if (mTitle != null) {

            float x, y;

            if (mDirection == DIRECTION_Y) {

                x = mOffset/2 - mPaintTitle.getTextSize()/2;
                y = mOffset + (mBounds.top + mBounds.bottom) / 2 * mScaleY;

                canvas.save();
                canvas.rotate(-90, x, y);
                canvas.drawText(mTitle, x, y, mPaintTitle);
                canvas.restore();
            }
            else {
                x = mOffset + (mBounds.left + mBounds.right)/2 * mScaleX;
                y = mOffset/2 - mPaintTitle.getTextSize()/2;
                canvas.drawText(mTitle, x, y, mPaintTitle);
            }
        }

    }
}
