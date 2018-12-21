package com.cqkct.FunKidII.Ui.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.CalendarViewAdapter;
import com.cqkct.FunKidII.Ui.view.CalendarCard;
import com.cqkct.FunKidII.Utils.L;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * @author chendali
 */
public class CalendarActivity extends BaseActivity implements OnClickListener, CalendarCard.OnCellClickListener {  //OnCellClickListener
    private static final String TAG = "CalendarActivity";

    /**
     * 选中的日期，格式：yyyy-MM-DD
     */
    public static final String PARAM_SELECTED_DAY = "selected_day";
    /**
     * 有标记的日期，是一个 ArrayList<String>，元素里的日期为字符串，格式：yyyy-MM-DD
     */
    public static final String PARAM_MARK_DAY_LIST = "mark_days";
    /**
     * 返回值为选中的日期字符串，格式为 yyyy-MM-dd
     */
    public static final String RETURN_SELECTED_DATE = "selected_date";
    public static final String RESULT_ACTION = "com.cqkct.FunKidII.DatePicker.RESULT_ACTION";

    private static final SimpleDateFormat DATE_TITLE_DATE_FORMAT = new SimpleDateFormat("yyyy年MM月", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private ViewPager mViewPager;
    private int mCurrentIndex = 498;
    private CalendarCard[] mShowViews;
    private CalendarViewAdapter<CalendarCard> adapter;
    private SildeDirection mDirection = SildeDirection.NO_SILDE;

    private String paramSelectedDay;

    @Override
    public void clickDate(Date date) {
        if (date != null) {
            Intent intent = new Intent(RESULT_ACTION);
            intent.putExtra(RETURN_SELECTED_DATE, DATE_FORMAT.format(date));
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void changeDate(Date date) {
        monthText.setText(DATE_TITLE_DATE_FORMAT.format(date));
    }

    enum SildeDirection {
        RIGHT, LEFT, NO_SILDE;
    }

    private ImageView preImgBtn, nextImgBtn;
    private TextView monthText;
    Animation animation;
    LinearLayout calen_ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calender);
        setTitleBarTitle(R.string.title_date_picker);

        Intent intent = getIntent();
        paramSelectedDay = intent.getStringExtra(PARAM_SELECTED_DAY);
        List<String> dataList = intent.getStringArrayListExtra(PARAM_MARK_DAY_LIST);
        Calendar selectedDay = null;
        try {
            Date date = DATE_FORMAT.parse(paramSelectedDay);
            selectedDay = Calendar.getInstance();
            selectedDay.setTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        ArrayList<Calendar> dataCalendarList = null;
        try {
            for (String one : dataList) {
                Date date = DATE_FORMAT.parse(one);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                if (dataCalendarList == null)
                    dataCalendarList = new ArrayList<>();
                dataCalendarList.add(cal);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        mViewPager = (ViewPager) this.findViewById(R.id.vp_calendar);
        preImgBtn = (ImageView) this.findViewById(R.id.btnPreMonth);
        nextImgBtn = (ImageView) this.findViewById(R.id.btnNextMonth);
        monthText = (TextView) this.findViewById(R.id.tvCurrentMonth);
        preImgBtn.setOnClickListener(this);
        nextImgBtn.setOnClickListener(this);
        CalendarCard[] views = new CalendarCard[3];
        for (int i = 0; i < 3; i++) {
            views[i] = new CalendarCard(this, this, selectedDay, dataCalendarList);
        }
        adapter = new CalendarViewAdapter<>(views);
        setViewPager();
        calen_ll = (LinearLayout) this.findViewById(R.id.calen_ll);
        animation = AnimationUtils.loadAnimation(CalendarActivity.this, R.anim.in_from_top);

        calen_ll.startAnimation(animation);
    }

    @Override
    public void onTitleBarClick(View v) {
        switch (v.getId()) {
            case R.id.title_bar_left_icon:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setViewPager() {
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(498);
        mViewPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                measureDirection(position);
                updateCalendarView(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnPreMonth:  //上一月
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
                L.e("上一个月-----" + (mViewPager.getCurrentItem() - 1));
                break;
            case R.id.btnNextMonth: // 下一月
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                break;
            default:
                break;
        }
    }

    /**
     * @param arg0
     */
    private void measureDirection(int arg0) {

        if (arg0 > mCurrentIndex) {
            mDirection = SildeDirection.RIGHT;

        } else if (arg0 < mCurrentIndex) {
            mDirection = SildeDirection.LEFT;
        }
        mCurrentIndex = arg0;
    }

    //
    private void updateCalendarView(int arg0) {
        mShowViews = adapter.getAllItems();
        if (mDirection == SildeDirection.RIGHT) {
            mShowViews[arg0 % mShowViews.length].rightSlide();
        } else if (mDirection == SildeDirection.LEFT) {
            mShowViews[arg0 % mShowViews.length].leftSlide();
        }
        mDirection = SildeDirection.NO_SILDE;
    }

    public boolean onTouchEvent(MotionEvent event) {

        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, R.anim.out_to_top);
    }

}
