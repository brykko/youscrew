package com.brick.youscrew.utils;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by windows on 23.02.2016.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Calendar c;
        int startTimeMode;
        Bundle args = getArguments();

        // Determine start time mode; if args are null, use current date
        if (args != null) {
            startTimeMode = args.getInt("startTimeMode");
        }
        else {
            startTimeMode = DatePickerFragment.START_TIME_CURRENT;
        }


        if (startTimeMode == DatePickerFragment.START_TIME_CALENDAR) {
            c = (Calendar) args.get("startValue");
        }
        else if (startTimeMode == DatePickerFragment.START_TIME_MILLIS) {
            Long timeMillis = args.getLong("startValue");
            c = Calendar.getInstance();
            c.setTimeInMillis(timeMillis);
        }
        else {
            // Get current time
            c = Calendar.getInstance();
        }

        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));

    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Do something with the time chosen by the user
    }



}
