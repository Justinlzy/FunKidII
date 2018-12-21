package com.cqkct.FunKidII.Ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Adapter.CalendarViewAdapter;
import com.cqkct.FunKidII.Ui.view.CalendarCard;
import com.umeng.analytics.MobclickAgent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by Administrator on 2018/3/26.
 */

public class CalendarDialogFragment extends DialogFragment implements View.OnClickListener, CalendarCard.OnCellClickListener {


    enum SildeDirection {
        RIGHT, LEFT, NO_SILDE;
    }

    private static final SimpleDateFormat DATE_TITLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public static final String PARAM_SELECTED_DAY = "selected_day";       // 选中的日期，格式：yyyy-MM-DD

    private ViewPager mViewPager;
    private ImageView preImgBtn, nextImgBtn;
    private TextView monthText;
    private Animation animation;
    private LinearLayout calen_ll;
    private int mCurrentIndex = 498;
    private CalendarCard[] mShowViews;
    private CalendarViewAdapter<CalendarCard> adapter;
    private SildeDirection mDirection = SildeDirection.NO_SILDE;

    private String paramSelectedDay;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.CalendarDialog);
        setStyle(STYLE_NORMAL, R.style.CalendarDialog);
        dialog.setContentView(R.layout.fragment_dialog_calender);
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);

        initView(dialog);

        EventBus.getDefault().register(this);

        return dialog;
    }

    @Override
    public void onAttach(Context context) {
//        calendarDialogFragmentListener = (CalendarDialogFragmentListener) context;
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();

        MobclickAgent.onPageStart(getClass().getName());
    }

    @Override
    public void onPause() {
        super.onPause();

        MobclickAgent.onPageEnd(getClass().getName());
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void initView(Dialog dialog) {

        Bundle bundle = getArguments();
        paramSelectedDay = bundle.getString(PARAM_SELECTED_DAY);
        Calendar selectedDay = null;
        try {
            Date date = simpleDateFormat.parse(paramSelectedDay);
            selectedDay = Calendar.getInstance();
            selectedDay.setTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        List<Calendar> dataCalendarList = Collections.emptyList();

        mViewPager = (ViewPager) dialog.findViewById(R.id.vp_calendar);
        preImgBtn = (ImageView) dialog.findViewById(R.id.btnPreMonth);
        nextImgBtn = (ImageView) dialog.findViewById(R.id.btnNextMonth);
        monthText = (TextView) dialog.findViewById(R.id.tvCurrentMonth);
        calen_ll = (LinearLayout) dialog.findViewById(R.id.calen_ll);
        preImgBtn.setOnClickListener(this);
        nextImgBtn.setOnClickListener(this);
        CalendarCard[] views = new CalendarCard[3];
        for (int i = 0; i < 3; i++) {
            views[i] = new CalendarCard(getActivity(), this, selectedDay, dataCalendarList);
        }
        adapter = new CalendarViewAdapter<>(views);
        setViewPager();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnPreMonth:  //上一月
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
                break;
            case R.id.btnNextMonth: // 下一月
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
                break;
            default:
                break;
        }
    }

    @Override
    public void clickDate(Date date) {
        if (date != null) {
            EventBus.getDefault().post(new Event.CalendarOnClickData(simpleDateFormat.format(date)));
            dismiss();
        }
    }

    @Override
    public void changeDate(Date date) {
        if (date != null) {
            String dateStr = DATE_TITLE_DATE_FORMAT.format(date);
            if (!monthText.getText().equals(dateStr)) {
                monthText.setText(dateStr);
                EventBus.getDefault().post(new Event.CalendarOnMonthChanged(date));
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onDaysLocationOfDevice(Event.DaysLocationOfDevice days) {
        List<Calendar> list = convDayList(days.getDaysList());
        CalendarCard calendarCard = adapter.getItem(mViewPager.getCurrentItem());
        Calendar show = Calendar.getInstance();
        show.setTime(calendarCard.getShowDate());
        Calendar dataCal = (Calendar) show.clone();
        dataCal.setTime(days.getDate());
        if (dataCal.get(Calendar.YEAR) == show.get(Calendar.YEAR) && dataCal.get(Calendar.MONTH) == show.get(Calendar.MONTH)) {
            calendarCard.setMarkDayList(list);
        }
    }

    private List<Calendar> convDayList(List<String> dayList) {
        List<Calendar> list = new ArrayList<>();
        if (dayList != null) {
            try {
                for (String one : dayList) {
                    Date date = simpleDateFormat.parse(one);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);

                    list.add(cal);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private void setViewPager() {
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(498);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                dismiss();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void measureDirection(int arg0) {
        if (arg0 > mCurrentIndex) {
            mDirection = SildeDirection.RIGHT;
        } else if (arg0 < mCurrentIndex) {
            mDirection = SildeDirection.LEFT;
        }
        mCurrentIndex = arg0;
    }

    private void updateCalendarView(int arg0) {
        mShowViews = adapter.getAllItems();
        if (mDirection == SildeDirection.RIGHT) {
            mShowViews[arg0 % mShowViews.length].rightSlide();
        } else if (mDirection == SildeDirection.LEFT) {
            mShowViews[arg0 % mShowViews.length].leftSlide();
        }
        mDirection = SildeDirection.NO_SILDE;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
