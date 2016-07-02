package com.brick.youscrew;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.brick.youscrew.data.Rat;
import com.brick.youscrew.data.Session;
import com.brick.youscrew.data.Tetrode;
import com.brick.youscrew.data.Turn;
import com.brick.youscrew.data.TurnContract;
import com.brick.youscrew.data.TurnContract.TetrodeEntry;
import com.brick.youscrew.data.TurnContract.TurnEntry;
import com.brick.youscrew.data.TurnDbHelper;
import com.brick.youscrew.data.TurnDbUtils;
import com.brick.youscrew.utils.AppInstance;
import com.brick.youscrew.utils.GeneralUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO: - Update TT list in parent SessionActivity after turning is done
 *       - Add "end session" button to bottom of TTList
 *       - Add start angle indicator
 *       - Add text view showing turn amount
 */
public class TurnActivityFragment extends Fragment {

    public static final int MODE_TURN = 1;
    public static final int MODE_VIEW = 2;

    private static final long VIBRATOR_PULSE_DURATION = 5;

    private static final double SNAP_ANIMATION_STEP = 0.01;

    private final String LOG_TAG = TurnActivityFragment.class.getSimpleName();

    private final double DIAL_SCALE_FACTOR = 0.75; // fraction of the view covered by the dial image

    static final int RESULT_NEXT_TT = 99;

    private static Bitmap imageOriginal, imageScaled;
    private static Matrix matrix;
    private double mDialImageAngleOffset;

    private ImageView dial;
    private int dialHeight, dialWidth, dialCenX, dialCenY;

    // Double for holding the current angle of the screw, the angle when the activity was started
    // and the angle when the session was started
    private double mAngleCurrent;        // Current angle of the UI dial
    private double mAngleSessionStart;   // TT angle SESSION was started
    private double mAngleInitial;
    private double mTickRadius;

    private boolean mIsFirstSession;                // True during the first session the TT was turned
    private boolean mSuppressTagging = false;       // True if first session and pref value set to suppress

    private Tetrode mTetrode;
    private Session mSession;
    private Turn mTurn;
    private Rat mRat;

    private double mDialSnap;
    private double mAngleCurrentSnapped;

    private Vibrator mVibrator;

    private boolean[] mTicksInitialized = {false, false, false};
    private int mTurnRef;
    private int mActivityMode;

    private boolean mGetTagsPre = false;
    private boolean mGetTagsPost = false;

    private SQLiteDatabase mDb;

    private View mSetInitialAngleButton, mButtonOk, mButtonNextTt;

    private View mTickMarkerStart, mTickMarkerCurrent, mTickMarkerZero;

    private float mDialTranslateX, mDialTranslateY;

    private Intent startingIntent;

    private View mRootView;

    private Context mContext;

    private boolean mAnimating = false;

    private boolean mSingleTetrodeMode;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_turn, container, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Resources res = getResources();

        getDataFromDb();

        // Determine from shared preferences whether to collect tags

        Set<String> defaults = new HashSet<>();

        defaults.add("-1");

        String[] promptValues = res.getStringArray(R.array.pref_tag_prompt_values);
        Set<String> results = prefs.getStringSet(res.getString(R.string.pref_key_tag_prompt), defaults);

        int taggable =  mTetrode.getInt(TetrodeEntry.COLUMN_TAGGABLE);

        if (mActivityMode == MODE_TURN) {

            if (
                    results.contains(promptValues[0]) &&
                            mTurn.getInt(TurnEntry.COLUMN_WAS_TURNED) == Turn.TURNED_NO &&
                            !mSuppressTagging &&
                            taggable == TurnDbUtils.TETRODE_TAGGABLE) {
                mGetTagsPre = true;
            }

            if (results.contains(promptValues[1]) && !mSuppressTagging && taggable == TurnDbUtils.TETRODE_TAGGABLE) {
                mGetTagsPost = true;
            }

        }

        // Get the dial snap pref value
        int index = Integer.parseInt(prefs.getString(res.getString(R.string.pref_key_dial_snap), "-1") );
        String[] dialSnapOptions = res.getStringArray(R.array.pref_dial_snap_names);

        String dialSnapString = dialSnapOptions[index];

        Log.v(LOG_TAG, "Dial snap string = " + dialSnapString);

        if (dialSnapString.contentEquals("none")) {
            mDialSnap = 0;
        }
        else {
            mDialSnap = 360 / Double.parseDouble(dialSnapString.substring(2));
        }

        Log.v(LOG_TAG, "Dial snap value = " + Double.toString(mDialSnap));


        // Only fetch the preturn tags if the TT has not already been turned this session
        if (mGetTagsPre) {
            GeneralUtils.getTags(this, TurnDbUtils.TURNTIME_PRE, mTurn.getId(), TagActivityFragment.MODE_UPDATE);
        }

        /*
         %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
         */

        initializeViews();

        return mRootView;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mContext = getContext();

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_turn, menu);

        // If TT has already been turned in a prior session, disable the initial angle reset button
        if (mTetrode.wasEverTurned(mDb) == Tetrode.EVER_TURNED_YES) {
            MenuItem item = menu.findItem(R.id.action_turn_resetintitialangle);
            item.setEnabled(false);
        }

        if (mActivityMode == MODE_VIEW) {
            menu.findItem(R.id.action_turn_resetintitialangle).setEnabled(false);
            menu.findItem(R.id.action_turn_undo).setEnabled(false);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;

        switch (item.getItemId()) {

            case R.id.action_turn_resetintitialangle :
                unsetInitialAngle();
                return true;

            case R.id.action_turn_edit_pre_tags :
                intent = new Intent(getContext(), TagActivity.class);
                intent.putExtra("turnTime", TurnDbUtils.TURNTIME_PRE);
                intent.putExtra("editMode", TagActivityFragment.MODE_UPDATE);
                intent.putExtra("turnId", mTurn.getId());
                startActivity(intent);
                return true;

            case R.id.action_turn_undo :
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setMessage("Are you sure you want to undo turning for this tetrode?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                undoTurning();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                builder.create().show();

            default :
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // When tagActivityFragment finishes, its result is set to "OK" or "cancelled".
        //
        // If the user cancels during PRE turning, we finish the turnActivity too, to return to the
        // session screen

        int turnTime = data.getIntExtra("turnTime", -1);

        if (requestCode == TagActivityFragment.REQUEST_TAG) {

            // Case : user cancels preturn tagging. Return to session
            if (resultCode == TagActivityFragment.RESULT_CANCELLED && turnTime == TurnDbUtils.TURNTIME_PRE) {
                getActivity().finish();
            }

            // Case : user has applied post-turn tags and saved them OR skipped them. Return to session
            else if ( (resultCode == TagActivityFragment.RESULT_OK || resultCode == TagActivityFragment.RESULT_SKIPPED)
                    && turnTime == TurnDbUtils.TURNTIME_POST) {
                getActivity().finish();
            }

            // Whenever a tag set is approved, mark the turn record as 'edited'
            if (resultCode == TagActivityFragment.RESULT_OK) {
                mTurn.updateFromDb(mDb);
                mTurn.set(TurnEntry.COLUMN_WAS_EDITED, Turn.EDITED_YES);
                mTurn.writeToDb(mDb);
            }

        }

    }

    private void initializeViews() {

        // Set the top textView with the time last turned
        updateTimeLastTurned();

        TextView textTtNumber = (TextView) mRootView.findViewById(R.id.turn_textview_tt_number);

        if (mSingleTetrodeMode) {
            textTtNumber.setText(Integer.toString(mTetrode.getInt(TetrodeEntry.COLUMN_INDEX) + 1));
        }
        else {
            textTtNumber.setText("");
        }

        String comment = mTurn.getString(TurnEntry.COLUMN_COMMENT);
        TextView textComment = (TextView) mRootView.findViewById(R.id.turn_textview_comment);
        if (comment == null) {
            textComment.setVisibility(View.GONE);
        }
        else {
            textComment.setText(mTurn.getString(TurnEntry.COLUMN_COMMENT));
        }

        // Find the initial angle set button and attach listener
        mSetInitialAngleButton = mRootView.findViewById(R.id.turn_button_initial);
        mSetInitialAngleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTetrodeInitialAngle();
            }
        });

        mButtonOk = mRootView.findViewById(R.id.turn_button_ttmenu);
        mButtonNextTt = mRootView.findViewById(R.id.turn_button_next);

        // If the initial angle is not yet set, hide the button now
        if (mActivityMode == MODE_TURN && mTetrode.getInt(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET) == TurnDbUtils.INITIAL_ANGLE_UNSET) {
                mSetInitialAngleButton.setVisibility(View.VISIBLE);
                mButtonOk.setVisibility(View.INVISIBLE);
                mButtonNextTt.setVisibility(View.INVISIBLE);
        }

        if (mActivityMode == MODE_TURN && mTetrode.wasEverTurned(mDb) == TurnDbUtils.TETRODE_NOT_TURNED) {
            mRootView.findViewById(R.id.turn_button_plus1).setVisibility(View.VISIBLE);
        }

        // For view mode, only show the general OK button
        if (mActivityMode == MODE_VIEW) {
            mButtonOk.setVisibility(View.INVISIBLE);
            mButtonNextTt.setVisibility(View.INVISIBLE);
        }


        // Set the listener for the OK button:
        // Return to SessionActivity, posting 'OK' result

        mButtonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updateTurnRecord();

                getActivity().setResult(Activity.RESULT_OK);

                if (mGetTagsPost && !mSuppressTagging) {
                    GeneralUtils.getTags(TurnActivityFragment.this, TurnDbUtils.TURNTIME_POST, mTurn.getId(), TagActivityFragment.MODE_UPDATE);
                }
                else {
                    getActivity().finish();
                }

            }
        });


        // Return to SessionActivity and start the next TT

        mButtonNextTt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updateTurnRecord();

                getActivity().setResult(RESULT_NEXT_TT);

                if (mGetTagsPost && !mSuppressTagging) {
                    GeneralUtils.getTags(TurnActivityFragment.this, TurnDbUtils.TURNTIME_POST, mTurn.getId(), TagActivityFragment.MODE_UPDATE);
                }
                else {
                    getActivity().finish();
                }

            }
        });

        // load the image (will only be loaded once by the appInstance)
        imageOriginal = AppInstance.getScrewImage();
        mDialImageAngleOffset = GeneralUtils.getScrewImageAngleOffset(mContext);

        matrix = new Matrix();

        dial = (ImageView) mRootView.findViewById(R.id.imageView_turn);

        // Attach dial touch listener when in turning mode
        if (mActivityMode == MODE_TURN) {
            dial.setOnTouchListener(new MyOnTouchListener());
        }

        dial.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {

                Log.v(LOG_TAG, "ONGLOBALLAYOUT()");
                // method called more than once, but the values only need to be initialized one time
                if (dialHeight == 0 || dialWidth == 0) {

                    dialHeight = dial.getHeight();
                    dialWidth = dial.getWidth();
                    dialCenX = (int) dial.getX() + dialWidth / 2;
                    dialCenY = (int) dial.getY() + dialHeight / 2;

                    mTickRadius = dialHeight / 2 * DIAL_SCALE_FACTOR;

                    // resize
                    Matrix resize = new Matrix();

                    resize.postScale(
                            (float) Math.min(dialWidth, dialHeight) * (float) DIAL_SCALE_FACTOR / (float) imageOriginal.getWidth(),
                            (float) Math.min(dialWidth, dialHeight) * (float) DIAL_SCALE_FACTOR / (float) imageOriginal.getHeight());

                    imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), resize, false);

                    double resizeFactor = (double) imageScaled.getWidth() / (double) imageOriginal.getWidth();

                    int[] centreCoords = new int[2];
                    GeneralUtils.getScrewImageCentreOffset(mContext, centreCoords, imageScaled.getWidth());

                    Log.v(LOG_TAG, "Resize factor = " + Double.toString(resizeFactor) + ", centre coords = " +
                    Integer.toString(centreCoords[0]) + ", " + Integer.toString(centreCoords[1]));

                    // translate to the image view's center
                    mDialTranslateX = dialWidth/2f - centreCoords[0];
                    mDialTranslateY = dialHeight/2f - centreCoords[1];
                    matrix.postTranslate(mDialTranslateX, mDialTranslateY);

                    Log.v(LOG_TAG, "Matrix translated by X " + Float.toString(mDialTranslateX) + ", Y " +
                            Float.toString(mDialTranslateY));

                    dial.setImageBitmap(imageScaled);
                    dial.setImageMatrix(matrix);
                    dial.setScaleType(ImageView.ScaleType.MATRIX);

                    createTicks();

                    getStartingAngle();

                }
            }
        });

        mRootView.findViewById(R.id.turn_button_plus1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                double increment;

                if (mRat.getScrewThreadDir() == Rat.THREAD_DIR_CW) {
                    increment = -360;
                } else {
                    increment = 360;
                }

                // Animate the dial movement
                animateDial(mAngleCurrentSnapped, mAngleCurrentSnapped + increment);

                mAngleCurrent += increment;
                mAngleCurrentSnapped = snapToAngle(mAngleCurrent);

            }
        });

    }

    private void getDataFromDb() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        Resources res = getResources();

        mTurnRef = Integer.parseInt(prefs.getString(res.getString(R.string.pref_key_turn_ref_point), ""));

        // Open the database
        mDb = TurnDbHelper.getInstance(mContext).getWritableDatabase();

        /*
         GET SOME USEFUL STUFF FROM THE STARTING INTENT
         */

        startingIntent = getActivity().getIntent();

        mTurn = startingIntent.getParcelableExtra("turn");
        mSession = mTurn.getSession();
        mTetrode = mTurn.getTetrode();
        mRat = mTurn.getRat();

        mSingleTetrodeMode = mRat.getInt(TurnContract.RatEntry.COLUMN_TETRODES_INDEPENDENT) == Rat.TETRODES_INDEPENDENT_YES;

        Log.v(LOG_TAG, "Single tetrode mode = " + Boolean.toString(mSingleTetrodeMode));

        // Get the ID of the tetrode and check it
        long turnId = startingIntent.getLongExtra("turnId", -1);

        Cursor c = TurnDbUtils.singleEntryQuery(mDb, TurnEntry.TABLE_NAME, null, turnId, null);

        // Determine whether the activity is to set up for turning or viewing
        mActivityMode = startingIntent.getIntExtra("activityMode", -1);
        if (mActivityMode != MODE_TURN && mActivityMode != MODE_VIEW) {
            throw new RuntimeException("Illegal value for activity mode " + Integer.toString(mActivityMode));
        }

        if (mTurn.getInt(TurnEntry.COLUMN_WAS_TURNED) == TurnDbUtils.TURN_TURNED) {
            Log.v(LOG_TAG, "Turn record marked as TURNED THIS SESSION");
        }
        else {
            Log.v(LOG_TAG, "Turn record marked as NOT TURNED THIS SESSION");
        }

        if (mTetrode.getInt(TetrodeEntry.COLUMN_EVER_TURNED) == TurnDbUtils.TETRODE_TURNED) {
            Log.v(LOG_TAG, "Tetrode PREVIOUSLY TURNED");
        }
        else {
            Log.v(LOG_TAG, "Tetrode NEVER TURNED");
        }

        Turn[] allTurns = mTetrode.findTurns(mDb);

        int numTurnRecords = allTurns.length;

        if (numTurnRecords == 1) {
            mIsFirstSession = true;
            // Check whether the user has chosen to suppress tagging in first session
            mSuppressTagging = prefs.getBoolean(res.getString(R.string.pref_key_tag_suppress_first_session), false);
        }
        else {
            mIsFirstSession = false;
        }

        c.close();

    }

    private class MyOnTouchListener implements View.OnTouchListener {

        private double angle, angle0, dAngle;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    // Get the angle of the touch event
                    angle0 = getAngle(event.getX(), event.getY());
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Get the new angle AFTER the move
                    angle = getAngle(event.getX(), event.getY());

                    // Update the cumulative dial angle
                    dAngle = angle-angle0;
                    if (dAngle > 180) {
                        dAngle -= 360;
                    }
                    else if (dAngle < -180) {
                        dAngle += 360;
                    }

                    mAngleCurrent += dAngle;

                    // Only look for changes in the snap value when an animation's not in progress
                    if (! mAnimating) {
                        // Snap the current touch angle, and if it's changed since the last value,
                        // make a short vibration pulse
                        double newSnappedAngle = snapToAngle(mAngleCurrent);

                        // If there's no dial snap set, just rotate the dial with the touch movement
                        if (mDialSnap == 0) {
                            // Rotate the visual dial
                            mAngleCurrentSnapped = mAngleCurrent;
                            setDialAngle();
                        }
                        // Otherwise, check if the snapped value has changed, and animate the dial if so
                        else {
                            if (Math.abs(newSnappedAngle - mAngleCurrentSnapped) > mDialSnap / 2) {
                                animateDial(mAngleCurrentSnapped, newSnappedAngle);
                                mAngleCurrentSnapped = newSnappedAngle;
                            }
                        }
                    }

                    angle0 = angle;

                    break;

                case MotionEvent.ACTION_UP:

                    break;
            }

            return true;
        }

    }

    private void updateTextViews() {
        // Updates the content of the textViews above the dial

        TextView textTurnsThisSession = (TextView) getActivity().findViewById(R.id.turn_textview_turned_this_session);
        TextView textTotalTurns = (TextView) getActivity().findViewById(R.id.turn_textview_total_turns);
        TextView textDepth = (TextView) getActivity().findViewById(R.id.turn_textview_depth);

        double turnsThisSession = (mAngleCurrentSnapped - mAngleSessionStart) / 360;
        double turnsTotal = (mAngleCurrentSnapped - mAngleInitial) / 360;

        if (mRat.getScrewThreadDir() == Rat.THREAD_DIR_CW) {
            turnsThisSession *= -1;
            turnsTotal *= -1;
        }

        double depthMm = mRat.getTurnMicrometers() * turnsTotal / 1000;

        textTurnsThisSession.setText(String.format("%.2f", turnsThisSession));
        textTotalTurns.setText(String.format("%.2f", turnsTotal));
        textDepth.setText(String.format("%.2f mm", depthMm));

    }

    private double getAngle(double xTouch, double yTouch) {
        double x = xTouch - (dialWidth / 2d);
        double y = dialHeight - yTouch - (dialHeight / 2d);

        return Math.atan2(y, x) * 180 / Math.PI;

    }

    private void animateDial(double startAngle, double endAngle) {

        double angleDifference = (endAngle - startAngle);

        Log.v(LOG_TAG, "animateDial requested from " + Double.toString(startAngle) + " to " + Double.toString(endAngle));

        mAnimating = true;

        final float animEndAngle;

        int numSteps = (int) Math.round( Math.abs(angleDifference) / mDialSnap );

        long singleSnapDuration = (long) mDialSnap * 5 / (long) Math.sqrt(numSteps);

        if (angleDifference > 0) {
            animEndAngle = (float) -mDialSnap * numSteps;
        } else {
            animEndAngle = (float) mDialSnap * numSteps;
        }

        Log.v(LOG_TAG, " Creating animation from " + Double.toString(0) + " to " + Double.toString(animEndAngle) + " deg.");

        RotateAnimation animation =
                new RotateAnimation(
                        0,
                        animEndAngle,
                        dialWidth/2,
                        dialHeight/2) {

                    float mCurrentAngle;

                    Float mLastAngleVib = null;

                    double tolerance = 2;

                    @Override
                    public void initialize(int width, int height, int parentWidth, int parentHeight) {
                        super.initialize(width, height, parentWidth, parentHeight);
                    }

                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        super.applyTransformation(interpolatedTime, t);

                        mCurrentAngle = GeneralUtils.matrixGetRotation(t.getMatrix());

                        Log.v(LOG_TAG, "Current angle = " + Float.toString(mCurrentAngle));

                        if (mLastAngleVib == null) {
                            mVibrator.vibrate(5);
                            mLastAngleVib = mCurrentAngle;
                        }

                        if (Math.abs(mCurrentAngle - mLastAngleVib) > mDialSnap+tolerance) {

                            Log.v(LOG_TAG, "Current angle = " + Double.toString(mCurrentAngle) +
                            ", last vib angle = " + mLastAngleVib.toString() +
                            ", step = " + Double.toString(mDialSnap) );

                            boolean hasTurnedCCW = mCurrentAngle > mLastAngleVib;

                            mVibrator.vibrate(5);

                            if (hasTurnedCCW) {
                                mLastAngleVib += (float)mDialSnap;
                            }
                            else {
                                mLastAngleVib -= (float)mDialSnap;
                            }

                        }

                    }

                };

        // Scale the animation duration to how far the dial needs to move
        animation.setDuration(singleSnapDuration * numSteps);
        animation.setInterpolator(new AccelerateDecelerateInterpolator());

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.v(LOG_TAG, "Animation starting");
                // Disable response to touch events during animation
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.v(LOG_TAG, "Animation finished");
                // Set the dial to the new angle when animation is over
                dial.clearAnimation();
                setDialAngle();
                // Re-enable touch events
                mAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        dial.startAnimation(animation);

    }

    private void setDialAngle() {
        // Matrix rotation here is defined in CLOCKWISE degrees from 12 o'clock.
        // We're working in CCW angles, so must convert to negative degrees
        matrix.setTranslate(mDialTranslateX, mDialTranslateY);
        matrix.postRotate((float) -(mAngleCurrentSnapped - mDialImageAngleOffset),
                dialWidth / 2, dialHeight / 2);
        dial.setImageMatrix(matrix);

        // Reposition the marker arrows.
        // For screw-centric ticks, move the 'current angle' tick to align with the
        // the screw's zero angle
        if (mTurnRef == SettingsActivityFragment.TURN_REF_SCREW) {
            positionViewOnDial(mTickMarkerCurrent, mTickRadius, mAngleCurrentSnapped);
        }
        // For world-centric ticks, move the 'session start angle' from the 12 o'clock
        // position according to the turn value
        else if (mTurnRef == SettingsActivityFragment.TURN_REF_WORLD) {
            positionViewOnDial(mTickMarkerStart, mTickRadius, mAngleCurrentSnapped-mAngleSessionStart);
        }

        // Update the textViews with new turn info
        if (mTetrode.getInt(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET) == Tetrode.INITIAL_ANGLE_SET) {
            updateTextViews();
        }

    }

    private double snapToAngle(double degrees) {

        if (mDialSnap == 0) {
            return degrees;
        }
        else {
            double mod = ( (degrees + mDialSnap/2) % mDialSnap ) - mDialSnap/2;
            return degrees - mod;
        }

    }

    private void createTicks() {

        Resources res = getResources();

        FrameLayout dialFrame = (FrameLayout) mRootView.findViewById(R.id.turn_layout_dial);

        mTickMarkerStart = new View(mContext);
        mTickMarkerCurrent = new View(mContext);
        mTickMarkerZero = new View(mContext);

        int pixels = GeneralUtils.dip2Pix(mContext, 20);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(pixels, pixels);

        Paint.Style[]   styles      = new Paint.Style[] {Paint.Style.FILL,     Paint.Style.FILL,      Paint.Style.STROKE};
        View[]          views       = new View[]        {mTickMarkerStart,     mTickMarkerCurrent,    mTickMarkerZero};
        int[]           colorIds    = new int[]         {R.color.colorPreTurn, R.color.colorPostTurn, R.color.black};
        int[]           alphas      = new int[]         {100,                  255,                   255};

        for (int v = 0; v < 3; v++) {

            views[v].setLayoutParams(layoutParams);

            views[v].setBackgroundResource(R.drawable.dial_triangle_marker);

            // Must mutate the background drawables to make them independent from other concurrent uses
            views[v].getBackground().mutate();

            setTickColor(views[v], res.getColor(colorIds[v]), styles[v], alphas[v]);

            dialFrame.addView(views[v]);

        }

        if (mTurnRef == SettingsActivityFragment.TURN_REF_WORLD) {
            mTickMarkerZero.setVisibility(View.GONE);
        }

    }

    private void addTickGlobalLayoutListener(final View view, final int index, final double startAngle) {

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!mTicksInitialized[index]) {

                    positionViewOnDial(view, mTickRadius, startAngle);
                    mTicksInitialized[index] = true;

                }
            }
        });

    }

    private void getStartingAngle() {

//        Cursor c = TurnDbUtils.turnGetEntries(mDb, mTurn.getId());
//        mAngleSessionStart = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_START_ANGLE));

        mAngleSessionStart = snapToAngle(mTurn.getDouble(TurnEntry.COLUMN_START_ANGLE));

        mAngleCurrent = snapToAngle(mTurn.getDouble(TurnEntry.COLUMN_END_ANGLE));
        mAngleCurrentSnapped = mAngleCurrent;

        mAngleInitial = mTetrode.getDouble(TetrodeEntry.COLUMN_INITIAL_ANGLE);

        Log.v(LOG_TAG, "Tetrode initial angle = " + Double.toString(mAngleInitial));

//        mAngleCurrent = c.getDouble(c.getColumnIndex(TurnEntry.COLUMN_END_ANGLE));
//        mAngleCurrentSnapped = snapToAngle(mAngleCurrent);
//
//        c.close();

        Log.v(LOG_TAG, "Start angle " + Double.toString(mAngleCurrentSnapped));

        // Update the dial rotation
        setDialAngle();

        // Refresh the textViews
        updateTextViews();

        if (mTurnRef == SettingsActivityFragment.TURN_REF_SCREW) {
            addTickGlobalLayoutListener(mTickMarkerStart,   0, mAngleSessionStart);
            addTickGlobalLayoutListener(mTickMarkerCurrent, 1, mAngleCurrentSnapped);
            addTickGlobalLayoutListener(mTickMarkerZero, 2, 0);
        }
        else if (mTurnRef == SettingsActivityFragment.TURN_REF_WORLD) {
            addTickGlobalLayoutListener(mTickMarkerStart,   0, mAngleCurrentSnapped-mAngleSessionStart);
            addTickGlobalLayoutListener(mTickMarkerCurrent, 1, 0);
            addTickGlobalLayoutListener(mTickMarkerZero,    2, 0);
        }

    }

    private void setTetrodeInitialAngle() {
        // Callback method for the 'set initial angle' button.
        // This is called the very first time the TT is turned, after which the button is hidden

        Log.v(LOG_TAG, "Setting TT initial angle to " + Double.toString(mAngleCurrentSnapped));

        // Define the session as starting at the new TT initial angle
        mAngleInitial = mAngleCurrentSnapped;
        mAngleSessionStart = mAngleCurrentSnapped;

        // Write the angle to the mDb
        mTetrode.setInitialAngle(mAngleInitial);

        if (mSingleTetrodeMode) {
            mTetrode.writeToDb(mDb);
        }
        else {
            for (Tetrode tetrode : mRat.findTetrodes(mDb, null)) {
                tetrode.setInitialAngle(mAngleInitial);
                tetrode.writeToDb(mDb);
            }
        }

        // Also set the turn record initial angle. In case anything interrupts the activity, this
        // ensures it will reload correctly
        mTurn.setAngles(mAngleSessionStart, mAngleSessionStart);

        if (mSingleTetrodeMode) {
            mTurn.writeToDb(mDb);
        }
        else {
            for (Turn turn : mSession.findTurns(mDb)) {
                turn.setAngles(mAngleSessionStart, mAngleSessionStart);
                turn.writeToDb(mDb);
            }
        }

        // Set the tick positions:
        // For screw-centric mode, align the 'session start' tick with the screw's current angle
        if (mTurnRef == SettingsActivityFragment.TURN_REF_SCREW) {
            positionViewOnDial(mTickMarkerStart, mTickRadius, mAngleCurrentSnapped);
        }
        // For world-centric mode, move the 'session start' tick back to 12 o'clock
        else if (mTurnRef == SettingsActivityFragment.TURN_REF_WORLD) {
            positionViewOnDial(mTickMarkerStart, mTickRadius, 0);
        }

        // Make sure the turn info textViews show the updated data
        updateTextViews();

        mSetInitialAngleButton.setVisibility(View.INVISIBLE); // hide button once pressed
        mButtonOk.setVisibility(View.VISIBLE);
        mButtonNextTt.setVisibility(View.VISIBLE);

    }

    private void unsetInitialAngle() {

        mTetrode.setInitialAngle(null);
        
        // If in single-tetrode mode, just set the angle of the current TT
        if (mSingleTetrodeMode) {
            // Call the method will null angle - this unsets the initial angle in the db
            mTetrode.writeToDb(mDb);
        }

        // Otherwise, do it for ALL tetrode children of the current rat
        else {
            for (Tetrode tetrode : mRat.findTetrodes(mDb, null)) {
                tetrode.setInitialAngle(null);
                tetrode.writeToDb(mDb);
            }
        }

        mAngleInitial = 0;
        mAngleSessionStart = 0;
        mAngleCurrent = 0;
        mAngleCurrentSnapped = 0;

        // Also erase any turning from the current turn record
        mTurn.undoTurning(mDb);
        mTetrode.updateFromDb(mDb);

        if (mTurnRef == SettingsActivityFragment.TURN_REF_WORLD) {
            positionViewOnDial(mTickMarkerStart, mTickRadius, mAngleCurrentSnapped);
            positionViewOnDial(mTickMarkerCurrent, mTickRadius, mAngleCurrentSnapped);
        }
        else {
            positionViewOnDial(mTickMarkerStart, mTickRadius, 0);
        }

        setDialAngle();

        mSetInitialAngleButton.setVisibility(View.VISIBLE); // hide button once pressed
        mButtonOk.setVisibility(View.INVISIBLE);
        mButtonNextTt.setVisibility(View.INVISIBLE);

        updateTextViews();

    }

    private void undoTurning() {

        // Update the angles and dial position
        mAngleCurrent = mAngleSessionStart;
        mAngleCurrentSnapped = snapToAngle(mAngleCurrent);
        setDialAngle();

        // Undo turning in the db
        mTurn.undoTurning(mDb);
        mTetrode.updateFromDb(mDb);

        // Update the time last turned text appropriately
        updateTimeLastTurned();

        updateTextViews();

    }

    private void updateTurnRecord() {

        if (mAngleCurrentSnapped != mAngleSessionStart) {

            mTurn.setAngles(mAngleSessionStart, mAngleCurrentSnapped);

            if (mSingleTetrodeMode) {
                mTurn.writeToDb(mDb);
            }
            else {
                for (Turn turn : mSession.findTurns(mDb)) {
                    turn.setAngles(mAngleSessionStart, mAngleCurrentSnapped);
                    turn.writeToDb(mDb);
                }
            }

            // If this is the TTs first turn, update the TT record
            if (mTetrode.getInt(TetrodeEntry.COLUMN_EVER_TURNED) == TurnDbUtils.TETRODE_NOT_TURNED) {

                mTetrode.set(TetrodeEntry.COLUMN_EVER_TURNED, Tetrode.EVER_TURNED_YES);

                if (mSingleTetrodeMode) {
                    mTetrode.writeToDb(mDb);
                }
                else {
                    for (Tetrode tetrode : mRat.findTetrodes(mDb, null)) {
                        tetrode.set(TetrodeEntry.COLUMN_EVER_TURNED, Tetrode.EVER_TURNED_YES);
                        tetrode.writeToDb(mDb);
                    }
                }
            }

        }

    }

    private void positionViewOnDial(View view, double radius, double degrees) {
        // Place a view in polar coordinates with respect to the centre of the dial

        // Get the new center position for the view on the dial
        float viewCenterX = (float) (Math.cos(deg12ToRad3(-degrees))*radius + dialCenX);
        float viewCenterY = (float) (Math.sin(deg12ToRad3(-degrees)) * radius + dialCenY);

        view.setX(viewCenterX - view.getWidth()/2);
        view.setY(viewCenterY - view.getHeight() / 2);

        // Rotate the view to the new angle
        view.setRotation((float) -degrees);

    }

    private static double deg12ToRad3(double degrees) {
        // Convert degrees with zero at 12 o'clock to radians with zero at 3 o'clock
        return Math.toRadians(degrees-90);
    }

    private void setTickColor(View tick, int color, Paint.Style style, int alpha) {
        LayerDrawable layer = (LayerDrawable) tick.getBackground();
        RotateDrawable rotate = (RotateDrawable) layer.getDrawable(0);

        ShapeDrawable shape = new ShapeDrawable(new RectShape());

        Paint paint = shape.getPaint();

        paint.setColor(color);
        paint.setStyle(style);
        paint.setStrokeWidth(4);
        paint.setAlpha(alpha);

        rotate.setDrawable(shape);

    }

    private void updateTimeLastTurned() {
        // Determine when the TT was last turned

        TextView textLastTurn = (TextView) mRootView.findViewById(R.id.turn_textview_lastturn);

        boolean wasTurnedThisSession = mTurn.wasTurnedThisSession();
        long timeLastTurned;

        // If the tetrode has ever been turned:
        if (mTetrode.getInt(TetrodeEntry.COLUMN_EVER_TURNED) == TurnDbUtils.TETRODE_TURNED) {

            // If it was already turned this session, get the record and the time
            if (wasTurnedThisSession) {
                timeLastTurned = mTurn.getLong(TurnEntry.COLUMN_TIME);
            }

            // Otherwise, get the previous turn record and its time
            else {
                timeLastTurned = mTurn.findLastTurn(mDb).getLong(TurnEntry.COLUMN_TIME);
            }

            int flags = 0;

            CharSequence relativeDateTimeString = DateUtils.getRelativeDateTimeString(
                    getContext(),
                    timeLastTurned,
                    DateUtils.SECOND_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    flags);

            if (wasTurnedThisSession) {
                textLastTurn.setText("Turned " + relativeDateTimeString);
            }
            else {
                textLastTurn.setText("Last turned " + relativeDateTimeString);
            }


        }

        else {
            textLastTurn.setText("Tetrode has never been turned");
        }

    }

    public int getEverTurned() {
        return mTetrode.wasEverTurned(mDb);
    }

    public int getInitialAngleSet() {
        return mTetrode.getInt(TetrodeEntry.COLUMN_INITIAL_ANGLE_SET);
    }

    public int getActivityMode() {
        return mActivityMode;
    }

}
