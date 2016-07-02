package com.brick.youscrew.utils;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;

/**
 * Created by windows on 23.02.2016.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    public static final int START_TIME_CURRENT = 1;
    public static final int START_TIME_CALENDAR = 2;
    public static final int START_TIME_MILLIS = 3;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker

        Calendar c;
        int startTimeMode;
        Bundle args = getArguments();

        // Determine start time mode; if args are null, use current date
        if (args != null) {
            startTimeMode = args.getInt("startTimeMode");
        }
        else {
            startTimeMode = START_TIME_CURRENT;
        }

        if (startTimeMode == START_TIME_CALENDAR) {
            c = (Calendar) args.get("startValue");
        }
        else if (startTimeMode == START_TIME_MILLIS) {
            Long timeMillis = args.getLong("startValue");
            c = Calendar.getInstance();
            c.setTimeInMillis(timeMillis);
        }
        else {
            c = Calendar.getInstance();
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Do something with the date chosen by the user
    }
}
