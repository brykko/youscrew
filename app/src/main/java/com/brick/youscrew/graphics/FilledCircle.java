package com.brick.youscrew.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;

import com.brick.youscrew.R;
import com.brick.youscrew.utils.GeneralUtils;

/**
 * Created by windows on 13.03.2016.
 */
public class FilledCircle extends View {

    private int mX = 0;
    private int mY = 0;
    private int mWidth = 50;
    private int mHeight = 50;
    private int mColor = getResources().getColor(R.color.colorTag);

    private ShapeDrawable mDrawable;
    private String mText = null;
    private Paint mTextPaint;
    private Rect textBounds;

    public FilledCircle(Context context) {
        super(context);

        mDrawable = new ShapeDrawable(new OvalShape());

        Paint paint = mDrawable.getPaint();


        paint.setColor(mColor);

        mDrawable.setBounds(mX, mY, mX + mWidth, mY + mHeight);
    }

    public FilledCircle(Context context, AttributeSet attrs) {
        super(context, attrs);

        mDrawable = new ShapeDrawable(new OvalShape());
        mDrawable.getPaint().setColor(mColor);
        mDrawable.setBounds(mX, mY, mX + mWidth, mY + mHeight);
    }

    public Paint getPaint() {
        return mDrawable.getPaint();
    }

    public void setPaint(Paint paint) {
        mDrawable.getPaint().set(paint);
    }

    public void setDiameter(int diPixels) {

        int pixels = GeneralUtils.dip2Pix(getContext(), diPixels);

        // Set the dimensions of the drawable
        mWidth = pixels;
        mHeight = pixels;
        mDrawable.setBounds(mX, mY, mX + mWidth, mY + mHeight);

    }

    public void setColor(int color) {
        mColor = color;
        mDrawable.getPaint().setColor(color);
    }

    public void setText(String text, Paint paint) {
        mText = text;
        mTextPaint = paint;
        textBounds = new Rect();
        mTextPaint.getTextBounds(mText, 0, mText.length(), textBounds);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mDrawable.draw(canvas);
        if (mText != null) {
            canvas.drawText(mText, mX + mWidth/2 - textBounds.exactCenterX(), mY + mHeight/2 - textBounds.exactCenterY(), mTextPaint);
        }
    }

}
