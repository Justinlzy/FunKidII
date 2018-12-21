package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.cqkct.FunKidII.Bean.SchoolGuardianInfo;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Ui.view.NumberPickerBlue;
import com.cqkct.FunKidII.Ui.view.NumberPickerRed;
import com.cqkct.FunKidII.Utils.DateUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AmGuardTimeActivity extends BaseActivity {
    private int am_ArrivalSchoolHour = 8, am_ArrivalSchoolMinute = 0, am_LeaveSchoolHour = 12, am_LeaveSchoolMinute = 0;
    private SchoolGuardianInfo schoolGuardianInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_am_guard_time);
        Intent intent = this.getIntent();
        schoolGuardianInfo = (SchoolGuardianInfo) intent.getSerializableExtra(GuardianActivity.GUARDIAN_AM_TIME);
        initView();
    }

    public void initView() {
        TextView title = findViewById(R.id.dialog_title);
        title.setText(R.string.am_guardian_time);

        if (schoolGuardianInfo.getAm_arrival_time() != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(schoolGuardianInfo.getAm_arrival_time() * 1000L);
            am_ArrivalSchoolHour = calendar.get(Calendar.HOUR_OF_DAY);
            am_ArrivalSchoolMinute = calendar.get(Calendar.MINUTE);
        }
        if (schoolGuardianInfo.getAm_leave_time() != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(schoolGuardianInfo.getAm_leave_time() * 1000L);
            am_LeaveSchoolHour = calendar.get(Calendar.HOUR_OF_DAY);
            am_LeaveSchoolMinute = calendar.get(Calendar.MINUTE);
        }
        NumberPickerBlue startHourNP = findViewById(R.id.start_time_hour);
        NumberPickerBlue startMinNP = findViewById(R.id.start_time_min);
        initStartTimeNp(startHourNP, startMinNP);

        NumberPickerRed endHourNP = findViewById(R.id.end_time_hour);
        NumberPickerRed endMinNP = findViewById(R.id.end_time_min);
        initEndTimeNp(endHourNP, endMinNP);
    }

    @Override
    public void onDebouncedClick(View view, int viewId) {
        switch (viewId) {
            case R.id.ok: {
                long am_arrival = DateUtil.getTimeAtHM(am_ArrivalSchoolHour, am_ArrivalSchoolMinute);
                long am_leave = DateUtil.getTimeAtHM(am_LeaveSchoolHour, am_LeaveSchoolMinute);
                if (am_arrival >= am_leave) {
                    toast(R.string.school_guide_SchoolProNote2);
                    return;
                }
                schoolGuardianInfo.setAm_arrival_time(am_arrival);
                schoolGuardianInfo.setAm_leave_time(am_leave);

                Intent intent = new Intent();
                intent.putExtra(GuardianActivity.GUARDIAN_AM_TIME, schoolGuardianInfo);
                setResult(RESULT_OK, intent);
                this.finish();
            }
            break;
            case R.id.cancel:
                this.finish();
                break;
        }
    }

    //开始时间
    private void initStartTimeNp(NumberPicker startHourNp, NumberPicker startMinNp) {
        final int[] editStartTime = {5, 57};
        int currentStartTimeHour = 0;
        List<String> hourList = new ArrayList<>();
        String prefix = "0";
        for (int h = 0; h < 24; ++h) {
            if (h >= 10) {
                prefix = "";
            }
            hourList.add(prefix + h);
            if (h == am_ArrivalSchoolHour) {
                currentStartTimeHour = h;
                editStartTime[0] = h;
            }
        }
        final String[] mDisplayedHour = hourList.toArray(new String[0]);
        setNumberPicker(startHourNp, mDisplayedHour, currentStartTimeHour).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editStartTime[0] = Integer.valueOf(mDisplayedHour[newVal]);
            am_ArrivalSchoolHour = editStartTime[0];
        });
        int currentStartTimeMin = 0;
        prefix = "0";
        List<String> minList = new ArrayList<>();
        for (int m = 0; m < 60; m++) {
            if (m >= 10) {
                prefix = "";
            }
            minList.add(prefix + m);
            if (m == am_ArrivalSchoolMinute) {
                currentStartTimeMin = m;
                editStartTime[1] = m;
            }
        }
        final String[] mDisplayedMinValues = minList.toArray(new String[0]);
        setNumberPicker(startMinNp, mDisplayedMinValues, currentStartTimeMin).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editStartTime[1] = Integer.valueOf(mDisplayedMinValues[newVal]);
            am_ArrivalSchoolMinute = editStartTime[1];
        });
    }

    //结束时间
    private void initEndTimeNp(NumberPicker endHourNp, NumberPicker endMinNp) {
        final int[] editEndTime = {23, 5};
        int currentEndTimeHour = 0;
        List<String> hours = new ArrayList<>();
        String prefix = "0";
        for (int h = 0; h < 24; h++) {
            if (h >= 10) {
                prefix = "";
            }
            hours.add(prefix + h);
            if (am_LeaveSchoolHour == h) {
                currentEndTimeHour = h;
                editEndTime[0] = h;
            }
        }
        final String[] mDisplayHourValues = hours.toArray(new String[0]);
        setNumberPicker(endHourNp, mDisplayHourValues, currentEndTimeHour).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editEndTime[0] = Integer.valueOf(mDisplayHourValues[newVal]);
            am_LeaveSchoolHour = editEndTime[0];
        });
        int currentEndTimeMin = 0;
        prefix = "0";
        List<String> mins = new ArrayList<>();
        for (int m = 0; m < 60; m++) {
            if (m >= 10) {
                prefix = "";
            }
            mins.add(prefix + m);
            if (am_LeaveSchoolMinute == m) {
                currentEndTimeMin = m;
                editEndTime[1] = m;
            }
        }
        final String[] mDisplayedMinValues = mins.toArray(new String[0]);
        setNumberPicker(endMinNp, mDisplayedMinValues, currentEndTimeMin).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editEndTime[1] = Integer.valueOf(mDisplayedMinValues[newVal]);
            am_LeaveSchoolMinute = editEndTime[1];
        });

    }
}
