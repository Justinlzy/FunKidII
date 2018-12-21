package com.cqkct.FunKidII.Ui.Activity.MoreFunction;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.BaseActivity;
import com.cqkct.FunKidII.Utils.L;

import protocol.Message;

import static com.cqkct.FunKidII.Ui.Activity.MoreFunction.ClassDisableItemActivity.WEEK_FLAG;

/**
 * Created by justin on 2017/11/30.
 */

public class SetClassDisableRepeatActivity extends BaseActivity {

    public static final String TAG = SetClassDisableRepeatActivity.class.getSimpleName();
    public static final long WEEK_SUNDAY = 1L << 0;
    public static final long WEEK_MONDAY = 1L << 1;
    public static final long WEEK_TUESDAY = 1L << 2;
    public static final long WEEK_WEDNESDAY = 1L << 3;
    public static final long WEEK_THURSDAY = 1L << 4;
    public static final long WEEK_FRIDAY = 1L << 5;
    public static final long WEEK_SATURDAY = 1L << 6;
    public static final long WEEK_ALL = 0x7F;
    public static int week_days = 0;

    private ImageView iv_monday;
    private ImageView iv_tuesday;
    private ImageView iv_wednesday;
    private ImageView iv_thursday;
    private ImageView iv_friday;
    private ImageView iv_saturday;
    private ImageView iv_sunday;

    private boolean one = false;
    private boolean two = false;
    private boolean three = false;
    private boolean four = false;
    private boolean five = false;
    private boolean six = false;
    private boolean seven = false;

    private int flag = 0;
    private ImageView back;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_class_disable_repeat);
        week_days = getIntent().getIntExtra(WEEK_FLAG, 0);
        init();
        initWeekState(week_days);
    }


    private void init() {
        TextView title = findViewById(R.id.dialog_title);
        title.setText(R.string.repeat);

        back = findViewById(R.id.title_bar_left_icon);

        iv_monday = findViewById(R.id.check_monday);
        iv_tuesday = findViewById(R.id.check_tuesday);
        iv_wednesday = findViewById(R.id.check_wednesday);
        iv_thursday = findViewById(R.id.check_thursday);
        iv_friday = findViewById(R.id.check_friday);
        iv_saturday = findViewById(R.id.check_saturday);
        iv_sunday = findViewById(R.id.check_sunday);
        findViewById(R.id.ok).setOnClickListener(v -> {
            L.e("");
            Intent intent = new Intent();
            intent.putExtra(WEEK_FLAG, getFlag());
            L.e(TAG, "flag: " + getFlag());
            setResult(RESULT_OK, intent);
            finish();
        });
        findViewById(R.id.cancel).setOnClickListener(v -> {
            finish();
        });
    }

    private void initWeekState(int flag) {
        if ((flag & Message.TimePoint.RepeatFlag.MON_VALUE) != 0) {
            one = true;
            iv_monday.setVisibility(View.VISIBLE);
        }
        if ((flag & Message.TimePoint.RepeatFlag.TUE_VALUE) != 0) {
            two = true;
            iv_tuesday.setVisibility(View.VISIBLE);
        }
        if ((flag & Message.TimePoint.RepeatFlag.WED_VALUE) != 0) {
            three = true;
            iv_wednesday.setVisibility(View.VISIBLE);
        }
        if ((flag & Message.TimePoint.RepeatFlag.THU_VALUE) != 0) {
            four = true;
            iv_thursday.setVisibility(View.VISIBLE);
        }
        if ((flag & Message.TimePoint.RepeatFlag.FRI_VALUE) != 0) {
            five = true;
            iv_friday.setVisibility(View.VISIBLE);
        }
        if ((flag & Message.TimePoint.RepeatFlag.SAT_VALUE) != 0) {
            six = true;
            iv_saturday.setVisibility(View.VISIBLE);
        }
        if ((flag & Message.TimePoint.RepeatFlag.SUN_VALUE) != 0) {
            seven = true;
            iv_sunday.setVisibility(View.VISIBLE);
        }
    }


    private int getFlag() {
        int a = 0;

        if (one) a |= Message.TimePoint.RepeatFlag.MON_VALUE;

        if (two) a |= Message.TimePoint.RepeatFlag.TUE_VALUE;

        if (three) a |= Message.TimePoint.RepeatFlag.WED_VALUE;

        if (four) a |= Message.TimePoint.RepeatFlag.THU_VALUE;

        if (five) a |= Message.TimePoint.RepeatFlag.FRI_VALUE;

        if (six) a |= Message.TimePoint.RepeatFlag.SAT_VALUE;

        if (seven) a |= Message.TimePoint.RepeatFlag.SUN_VALUE;

        return a;
    }

    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.rl_monday:
                one = !one;
                if (one) {
                    flag |= WEEK_MONDAY;
                } else {
                    flag &= ~WEEK_MONDAY;
                }
                iv_monday.setVisibility(one ? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.rl_tuesday:
                two = !two;
                if (two) {
                    flag |= WEEK_TUESDAY;
                } else {
                    flag &= ~WEEK_TUESDAY;
                }
                iv_tuesday.setVisibility(two ? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.rl_wednesday:
                three = !three;
                if (three) {
                    flag |= WEEK_WEDNESDAY;
                } else {
                    flag &= ~WEEK_WEDNESDAY;
                }
                iv_wednesday.setVisibility(three ? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.rl_thursday:
                four = !four;
                if (four) {
                    flag |= WEEK_THURSDAY;
                } else {
                    flag &= ~WEEK_THURSDAY;
                }
                iv_thursday.setVisibility(four ? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.rl_friday:
                five = !five;
                if (five) {
                    flag |= WEEK_FRIDAY;
                } else {
                    flag &= ~WEEK_FRIDAY;
                }
                iv_friday.setVisibility(five ? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.rl_saturday:
                six = !six;
                if (six) {
                    flag |= WEEK_SATURDAY;
                } else {
                    flag &= ~WEEK_SATURDAY;
                }
                iv_saturday.setVisibility(six ? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.rl_sunday:
                seven = !seven;
                if (seven) {
                    flag |= WEEK_SUNDAY;
                } else {
                    flag &= ~WEEK_SUNDAY;
                }
                iv_sunday.setVisibility(seven ? View.VISIBLE : View.INVISIBLE);
                break;
        }
    }
}
