package com.brick.youscrew.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.dropbox.client2.exception.DropboxException;
import com.brick.youscrew.R;
import com.brick.youscrew.SettingsActivityFragment;
import com.brick.youscrew.TagActivity;
import com.brick.youscrew.TagActivityFragment;
import com.brick.youscrew.data.LogWriter;
import com.brick.youscrew.data.TurnDbUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static class containing general app utilities
 */
public final class GeneralUtils {

//    public static final String RESERVED_CHARS = Pattern.quote("|\/?*<":>[]',");

    public static final String RESERVED_CHARS = "|\\?*<\":>\\[\\]/',.";

    private static final String LOG_TAG = GeneralUtils.class.getSimpleName();

    public static void getTags(Fragment targetFragment, int turnTime, long turnId, int editMode) {
        // Start the TagActivity

        Intent intent = new Intent(targetFragment.getContext(), TagActivity.class);

        if (turnTime != TurnDbUtils.TURNTIME_PRE && turnTime != TurnDbUtils.TURNTIME_POST &&
                turnTime != TurnDbUtils.TURNTIME_GENERIC) {
            throw new IllegalArgumentException("Argument 'tagType' must correspond to TURN_PRE or " +
                    "TURN_POST");
        }

        if (turnId <= 0) {
            throw new IllegalArgumentException("Argument 'turnId' must be greater than zero");
        }

        if (editMode != TagActivityFragment.MODE_BLANK && editMode != TagActivityFragment.MODE_NEW
                && editMode != TagActivityFragment.MODE_UPDATE && editMode != TagActivityFragment.MODE_VIEW) {
            throw new IllegalArgumentException("Argument 'editMode' must correspond to" +
                    " MODE_BLANK, MODE_NEW, MODE_UPDATE or MODE_VIEW");
        }

        intent.putExtra("turnTime", turnTime);
        intent.putExtra("turnId", turnId);
        intent.putExtra("editMode", editMode);

        targetFragment.startActivityForResult(intent, TagActivityFragment.REQUEST_TAG);

    }

    public static int dip2Pix(Context context, int dips) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dips * scale + 0.5f);
    }

    public static double roundToDecimalPlace(double value, int dp) {

        Double factor = Math.pow(10, (double) dp);

        value *= factor;
        value = Math.round(value);
        value /= factor;

        return  value;

    }

    public static boolean turnTimeComb2Single(int combCode, int singleCode) {
        // Evaluates whether a combinatorial tag type specification code (e.g. SPECIFY_NONE, SPECIFY_BOTH)
        // applies to a tag type single int code (either TYPE_PRE or TYPE_POST)

        boolean checkGroup = false;

        if (singleCode < 1 || singleCode > 5) {
            throw new RuntimeException("Invalid tag type single code");
        }

        if (combCode < 1 || combCode > 4) {
            throw new RuntimeException("Invalid tag type combination code");
        }

        if (singleCode == TurnDbUtils.TURNTIME_GENERIC) {
            return true;
        }

        switch (combCode) {
            case (TurnDbUtils.SPECIFY_NONE) :
                break;

            case (TurnDbUtils.SPECIFY_PRE) :
                if (singleCode == TurnDbUtils.TURNTIME_PRE) {
                    checkGroup = true;
                }
                break;
            case (TurnDbUtils.SPECIFY_POST) :
                if (singleCode == TurnDbUtils.TURNTIME_POST) {
                    checkGroup = true;
                }
                break;

            case (TurnDbUtils.SPECIFY_BOTH) :
                checkGroup = true;
                break;

        }

        return checkGroup;
    }

    public static String turnTimeCombString(int combCode) {

        switch (combCode) {
            case TurnDbUtils.SPECIFY_NONE :
                return "NEVER";
            case TurnDbUtils.SPECIFY_PRE :
                return "PRE-TURN";
            case TurnDbUtils.SPECIFY_POST :
                return "POST-TURN";
            case TurnDbUtils.SPECIFY_BOTH :
                return "ALWAYS";
            default :
                throw new IllegalArgumentException("Invalid code");
        }
    }

    public static double getScrewImageAngleOffset(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        Resources res = context.getResources();

        String prefKey = res.getString(R.string.pref_key_dial_image);

        double offsetAngle;

        switch ( Integer.parseInt(prefs.getString(prefKey , "") ) ) {

            case SettingsActivityFragment.DIAL_IMAGE_ALAN:
                offsetAngle = 0;
                break;
            case SettingsActivityFragment.DIAL_IMAGE_HYPERDRIVE:
                offsetAngle = 68;
                break;
            default:
                throw new RuntimeException("Invalid screw image preference value");
        }

        return offsetAngle;

    }

    public static void getScrewImageCentreOffset(Context context, int[] coords, int newWidth) {

        if (coords.length != 2 ){
            throw new RuntimeException("Argument 'coords' must be an int array of length 2");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        Resources res = context.getResources();

        String prefKey = res.getString(R.string.pref_key_dial_image);

        int originalWidth;

        switch (prefs.getString(prefKey , "") ) {

            case "0":
                coords[0] = 356;
                coords[1] = 344;
                originalWidth = 684;
                break;
            case "1":
                coords[0] = 492;
                coords[1] = 488;
                originalWidth = 964;
                break;
            default:
                throw new RuntimeException("Invalid screw image preference value");
        }

        double resizeFactor = (double) newWidth / (double) originalWidth;

            coords[0] *= resizeFactor;
            coords[1] *= resizeFactor;

    }

    public static boolean containsReservedChars(String string) {
        Pattern pattern = Pattern.compile(new String("[\\Q" + RESERVED_CHARS + "\\E]"), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string);
        return matcher.find();
    }

    public static void exportTurningLog(final Context context, final long ratId) {

        Toast.makeText(context, "Please wait...", Toast.LENGTH_LONG).show();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Resources res = context.getResources();

        final boolean uploadToDropbox = prefs.getBoolean(res.getString(R.string.pref_key_db_dropbox_linked), false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new LogWriter(context, ratId).write(uploadToDropbox);
                }
                catch (DropboxException e) {
                    e.printStackTrace();
//                  Toast.makeText(context, "Error uploading file to Dropbox", Toast.LENGTH_LONG).show();
                }
                catch (IOException e) {
                    e.printStackTrace();
//                  Toast.makeText(context, "Error writing to csv file", Toast.LENGTH_LONG).show();
                }

            }
        }).start();


    }

    public static float matrixGetRotation(Matrix matrix) {
        float[] values = new float[9];

        matrix.getValues(values);

//// translation is simple
//        float tX = values[Matrix.MTRANS_X];
//        float tY = values[Matrix.MTRANS_Y];

// calculate real scale
        float scaleX = values[Matrix.MSCALE_X];
//        float skewY = values[Matrix.MSKEW_Y];
//        float realScale = (float) Math.sqrt(scaleX * scaleX + skewY * skewY);

// calculate the degree of rotation
        float skewX = values[Matrix.MSKEW_X];
        float realAngle = Math.round(Math.atan2(skewX, scaleX) * (180 / Math.PI));

        return realAngle;

    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {

         ListAdapter adapter = (ListAdapter) listView.getAdapter();

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;

        for (int i = 0; i < adapter.getCount(); i++) {
            view = adapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();

        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() -1 ));
        listView.setLayoutParams(params);

    }

    public static int intValueOf( String str )
    {
        int ival = 0, idx = 0, end;
        boolean sign = false;
        char ch;

        if( str == null || ( end = str.length() ) == 0 ||
                ( ( ch = str.charAt( 0 ) ) < '0' || ch > '9' )
                        && ( !( sign = ch == '-' ) || ++idx == end || ( ( ch = str.charAt( idx ) ) < '0' || ch > '9' ) ) )
            throw new NumberFormatException( str );

        for(;; ival *= 10 )
        {
            ival += '0'- ch;
            if( ++idx == end )
                return sign ? ival : -ival;
            if( ( ch = str.charAt( idx ) ) < '0' || ch > '9' )
                throw new NumberFormatException( str );
        }
    }

    public static long longValueOf( String str )
    {
        long lval = 0;
        int idx = 0, end;
        boolean sign = false;
        char ch;

        if( str == null || ( end = str.length() ) == 0 ||
                ( ( ch = str.charAt( 0 ) ) < '0' || ch > '9' )
                        && ( !( sign = ch == '-' ) || ++idx == end || ( ( ch = str.charAt( idx ) ) < '0' || ch > '9' ) ) )
            throw new NumberFormatException( str );

        for(;; lval *= 10 )
        {
            lval += '0'- ch;
            if( ++idx == end )
                return sign ? lval : -lval;
            if( ( ch = str.charAt( idx ) ) < '0' || ch > '9' )
                throw new NumberFormatException( str );
        }
    }

}
