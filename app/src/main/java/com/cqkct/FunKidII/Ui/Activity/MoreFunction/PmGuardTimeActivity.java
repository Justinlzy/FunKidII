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

public class PmGuardTimeActivity extends BaseActivity {
    private int pmArrivalSchoolHour = 2, pmArrivalSchoolMinute = 30, pmLeaveSchoolHour = 16, pmLeaveSchoolMinute = 30;
    private SchoolGuardianInfo schoolGuardianInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pm_guard_time);
        Intent intent = this.getIntent();
        schoolGuardianInfo = (SchoolGuardianInfo) intent.getSerializableExtra(GuardianActivity.GUARDIAN_PM_TIME);
        initView();
    }

    public void initView() {
        TextView title = findViewById(R.id.dialog_title);
        title.setText(R.string.pm_guardian_time);

        if (schoolGuardianInfo.getPm_arrival_time() != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(schoolGuardianInfo.getPm_arrival_time() * 1000L);
            pmArrivalSchoolHour = calendar.get(Calendar.HOUR_OF_DAY);
            pmArrivalSchoolMinute = calendar.get(Calendar.MINUTE);
        }
        if (schoolGuardianInfo.getPm_leave_time() != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(schoolGuardianInfo.getPm_leave_time() * 1000L);
            pmLeaveSchoolHour = calendar.get(Calendar.HOUR_OF_DAY);
            pmLeaveSchoolMinute = calendar.get(Calendar.MINUTE);
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
                long pm_arrival = DateUtil.getTimeAtHM(pmArrivalSchoolHour, pmArrivalSchoolMinute);
                long pm_leave = DateUtil.getTimeAtHM(pmLeaveSchoolHour, pmLeaveSchoolMinute);
                if (pm_arrival >= pm_leave) {
                    toast(R.string.school_guide_SchoolProNote2);
                    return;
                }
                schoolGuardianInfo.setPm_arrival_time(pm_arrival);
                schoolGuardianInfo.setPm_leave_time(pm_leave);

                Intent intent = new Intent();
                intent.putExtra(GuardianActivity.GUARDIAN_PM_TIME, schoolGuardianInfo);
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
            if (h == pmArrivalSchoolHour) {
                currentStartTimeHour = h;
                editStartTime[0] = h;
            }
        }
        final String[] mDisplayedHour = hourList.toArray(new String[0]);
        setNumberPicker(startHourNp, mDisplayedHour, currentStartTimeHour).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editStartTime[0] = Integer.valueOf(mDisplayedHour[newVal]);
            pmArrivalSchoolHour = editStartTime[0];
        });


        int currentStartTimeMin = 0;
        prefix = "0";
        List<String> minList = new ArrayList<>();
        for (int m = 0; m < 60; m++) {
            if (m >= 10) {
                prefix = "";
            }
            minList.add(prefix + m);
            if (m == pmArrivalSchoolMinute) {
                currentStartTimeMin = m;
                editStartTime[1] = m;
            }
        }
        final String[] mDisplayedMinValues = minList.toArray(new String[0]);
        setNumberPicker(startMinNp, mDisplayedMinValues, currentStartTimeMin).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editStartTime[1] = Integer.valueOf(mDisplayedMinValues[newVal]);
            pmArrivalSchoolMinute = editStartTime[1];
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
            if (pmLeaveSchoolHour == h) {
                currentEndTimeHour = h;
                editEndTime[0] = h;
            }
        }
        final String[] mDisplayHourValues = hours.toArray(new String[0]);
        setNumberPicker(endHourNp, mDisplayHourValues, currentEndTimeHour).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editEndTime[0] = Integer.valueOf(mDisplayHourValues[newVal]);
            pmLeaveSchoolHour = editEndTime[0];
        });

        int currentEndTimeMin = 0;
        prefix = "0";
        List<String> mins = new ArrayList<>();
        for (int m = 0; m < 60; m++) {
            if (m >= 10) {
                prefix = "";
            }
            mins.add(prefix + m);
            if (pmLeaveSchoolMinute == m) {
                currentEndTimeMin = m;
                editEndTime[1] = m;
            }
        }
        final String[] mDisplayedMinValues = mins.toArray(new String[0]);
        setNumberPicker(endMinNp, mDisplayedMinValues, currentEndTimeMin).setOnValueChangedListener((picker, oldVal, newVal) -> {
            editEndTime[1] = Integer.valueOf(mDisplayedMinValues[newVal]);
            pmLeaveSchoolMinute = editEndTime[1];
        });


    }


}

