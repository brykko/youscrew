package com.brick.youscrew.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.brick.youscrew.utils.GeneralUtils;

/**
 * Created by Brick on 10/04/16.
 */
public class Line extends Drawable {

    private static final String LOG_TAG = Line.class.getSimpleName();

    private Context mContext;

    private Path mPath;
    private Paint mPaintLine;
    private Paint mPaintCircle;

    private int mDotRadius;
    private boolean mDotFilled;

    private int mNumPoints;

    private float[] mX, mY;

    public Line(Context context, float[] x, float[] y) {

        mPath = new Path();

        mPaintLine = new Paint();
        mPaintLine.setStyle(Paint.Style.STROKE);

        mPaintCircle = new Paint();

        mContext = context;

        mDotRadius = GeneralUtils.dip2Pix(context, 4);

        setCoords(x, y);

    }

    public void setCoords(float[] x, float[] y) {

//        Log.v(LOG_TAG, "x.length = " + Integer.toString(x.length) + ", y.length = " + Integer.toString(y.length));

        mX = x;
        mY = y;

        // Check inputs are same size
        if (x.length != y.length) {
            throw new IllegalArgumentException("Arguments x and y must be 1-D arrays of equal length");
        }

        mNumPoints = x.length;

        // If no points supplied, do nothing
        if (mNumPoints == 0) return;

        // Clear any preexisting path and set the start position
        mPath.reset();
        mPath.moveTo(x[0], y[0]);

        // Join all the points with straight lines
        for (int n = 1; n < mNumPoints; n++) {
            mPath.lineTo(x[n], y[n]);
        }

        setBounds((int)arrayMin(x), (int)arrayMin(y), (int)arrayMax(x), (int)arrayMax(y));

    }

    public void setColor(int color) {
        mPaintLine.setColor(color);
        mPaintCircle.setColor(color);
    }

    public void setStrokeWidth(float width) {
        mPaintLine.setStrokeWidth(width);
        mPaintCircle.setStrokeWidth(width);
    }

    public void setDotRadius(int radius) {
        mDotRadius = radius;
    }

    public void setDotFilled(boolean isFilled) {
        if (isFilled == true) {
            mPaintCircle.setStyle(Paint.Style.FILL_AND_STROKE);
        }
        else {
            mPaintCircle.setStyle(Paint.Style.STROKE);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mPaintLine.setAlpha(alpha);
        mPaintCircle.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaintLine.setColorFilter(colorFilter);
        mPaintCircle.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    @Override
    public void draw(Canvas canvas) {

        canvas.drawPath(mPath, mPaintLine);

        for (int n = 0; n < mNumPoints; n++) {
            canvas.drawCircle(mX[n], mY[n], mDotRadius, mPaintCircle);
        }

    }

    private static float arrayMax(float[] array) {

        float maxVal = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxVal) maxVal = array[i];
        }

        return maxVal;
    }

    private static float arrayMin(float[] array) {

        float minVal = array[0];

        for (int i = 1; i < array.length; i++) {
            if (array[i] < minVal) minVal = array[i];
        }

        return minVal;
    }


}
