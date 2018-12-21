package com.cqkct.FunKidII.Ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.cqkct.FunKidII.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author justin
 */
public class CalendarCard extends View {
    private static final String TAG = CalendarCard.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final int TOTAL_COL = 7; // 7
    private static final int TOTAL_ROW = 6; //

    private Paint mCirclePaint; //
    private Paint mTextPaint; //
    private Paint mStrokePaint; // mark day 空心圆
    private int mViewWidth; //
    private int mViewHeight; //
    private int mCellSpace; //
    private Row rows[] = new Row[TOTAL_ROW]; //
    private static Calendar mShowDate; //
    private OnCellClickListener mCellClickListener; //
    private int touchSlop; //
    private boolean callBackCellSpace;

    private Cell mClickCell;
    private float mDownX;
    private float mDownY;
    private List<Calendar> markDayList;
    private Calendar selectedDay;

    /**
     * @author wuwenjie
     */
    public interface OnCellClickListener {
        void clickDate(Date date);
        void changeDate(Date date);
    }

    public CalendarCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CalendarCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CalendarCard(Context context) {
        super(context);
        init(context);
    }

    public CalendarCard(Context context, OnCellClickListener listener, List<Calendar> markDayList) {
        this(context, listener, null, markDayList);
    }

    public CalendarCard(Context context, OnCellClickListener listener, Calendar selectedDay, List<Calendar> markDayList) {
        super(context);
        this.selectedDay = selectedDay;
        this.markDayList = markDayList;
        this.mCellClickListener = listener;
        init(context);
    }


    private void init(Context context) {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(getResources().getColor(R.color.calendar_select_day)); //

        mStrokePaint = new Paint();
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth((float) 4.0);
        mStrokePaint.setColor(getResources().getColor(R.color.main_color_tone));

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        initDate();
    }

    private void initDate() {
        if (selectedDay != null) {
            mShowDate = (Calendar) selectedDay.clone();
        } else {
            mShowDate = Calendar.getInstance();
        }
        if (markDayList == null)
            markDayList = Collections.emptyList();
        mShowDate.set(Calendar.DAY_OF_MONTH, 1);
        fillDate();
    }

    public void setMarkDayList(List<Calendar> markDayList) {
        this.markDayList = markDayList;
        if (this.markDayList == null) {
            this.markDayList = Collections.emptyList();
        }
        update();
    }

    private void fillDate() {
        Calendar calendar = ((Calendar) mShowDate.clone());

        calendar.set(Calendar.DATE, 1);
        calendar.roll(Calendar.DATE, false);
        int currentMonthDays = calendar.get(Calendar.DATE);

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int dayOfWeekOfFirstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 我们需要的数据是 0 开始

        Calendar todayCalendar = Calendar.getInstance();
        boolean isTodayMonth = todayCalendar.get(Calendar.YEAR) == mShowDate.get(Calendar.YEAR)
                && todayCalendar.get(Calendar.MONTH) == mShowDate.get(Calendar.MONTH);
        boolean isSelectedMonth = selectedDay != null && (selectedDay.get(Calendar.YEAR) == mShowDate.get(Calendar.YEAR)
                && (selectedDay.get(Calendar.MONTH) == mShowDate.get(Calendar.MONTH)));

        int day = 0;

        for (int rowIdx = 0; rowIdx < TOTAL_ROW; ++rowIdx) {
            rows[rowIdx] = new Row(rowIdx);
            for (int colIdx = 0; colIdx < TOTAL_COL; ++colIdx) {
                int dayIdx = TOTAL_COL * rowIdx + colIdx;

                if (dayIdx < dayOfWeekOfFirstDayOfMonth) {
                    // last month

                    calendar.set(Calendar.YEAR, mShowDate.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, mShowDate.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.add(Calendar.DAY_OF_MONTH, dayIdx - dayOfWeekOfFirstDayOfMonth);

                    rows[rowIdx].cells[colIdx] = new Cell(calendar.getTime(), State.PAST_MONTH_DAY, colIdx, rowIdx);
                } else if (dayIdx < dayOfWeekOfFirstDayOfMonth + currentMonthDays) {
                    // current mouth
                    ++day;

                    calendar.set(Calendar.YEAR, mShowDate.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, mShowDate.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, day);

                    Date date = calendar.getTime();
                    rows[rowIdx].cells[colIdx] = new Cell(date, State.CURRENT_MONTH_DAY, colIdx, rowIdx);

                    if (isTodayMonth) {
                        if (day == todayCalendar.get(Calendar.DAY_OF_MONTH)) {
                            rows[rowIdx].cells[colIdx] = new Cell(date, State.TODAY, colIdx, rowIdx);
                        } else if (day > todayCalendar.get(Calendar.DAY_OF_MONTH)) {
                            rows[rowIdx].cells[colIdx] = new Cell(date, State.UNREACH_DAY, colIdx, rowIdx);
                        }
                    }

                    // 标记
                    for (Calendar markCalendar : markDayList) {
                        if (calendar.get(Calendar.YEAR) == markCalendar.get(Calendar.YEAR) &&
                                calendar.get(Calendar.DAY_OF_YEAR) == markCalendar.get(Calendar.DAY_OF_YEAR)) {
                            rows[rowIdx].cells[colIdx].state = State.MARK_DAY;
                        }
                    }

                    if (isSelectedMonth && day == selectedDay.get(Calendar.DAY_OF_MONTH)) {
                        if (rows[rowIdx].cells[colIdx].state == State.TODAY) {
                            rows[rowIdx].cells[colIdx].state = State.TODAY_AND_SELECTED_DAY;
                        } else {
                            rows[rowIdx].cells[colIdx].state = State.SELECTED_DAY;
                        }
                    }
                } else {
                    // next month

                    calendar.set(Calendar.YEAR, mShowDate.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, mShowDate.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.add(Calendar.MONTH, 1);
                    calendar.add(Calendar.DAY_OF_MONTH, dayIdx - (dayOfWeekOfFirstDayOfMonth + currentMonthDays));

                    rows[rowIdx].cells[colIdx] = new Cell(calendar.getTime(), State.NEXT_MONTH_DAY, colIdx, rowIdx);
                }
            }
        }

        mCellClickListener.changeDate(mShowDate.getTime());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < TOTAL_ROW; i++) {
            if (rows[i] != null) {
                rows[i].drawCells(canvas);
            }
        }
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        L.e("onMeasure:  " + widthMeasureSpec + "   heightMeasureSpec:  " + heightMeasureSpec);
//        setMeasuredDimension(widthMeasureSpec, (int) (widthMeasureSpec * 1.2));
//    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
//      mCellSpace = Math.min(mViewHeight / TOTAL_ROW, mViewWidth / TOTAL_COL);
        mCellSpace = mViewWidth / TOTAL_COL;
        if (!callBackCellSpace) {
            callBackCellSpace = true;
        }
        mTextPaint.setTextSize(mCellSpace / 3);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float disX = event.getX() - mDownX;
                float disY = event.getY() - mDownY;
                if (Math.abs(disX) < touchSlop && Math.abs(disY) < touchSlop) {
                    int col = (int) (mDownX / mCellSpace);
                    int row = (int) (mDownY / mCellSpace);
                    measureClickCell(col, row);
                }
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * @param col
     * @param row
     */
    private void measureClickCell(int col, int row) {
        if (col >= TOTAL_COL || row >= TOTAL_ROW)
            return;
        if (mClickCell != null) {
            rows[mClickCell.rowIdx].cells[mClickCell.colIdx] = mClickCell;
        }
        if (rows[row] != null) {
            mClickCell = new Cell(rows[row].cells[col].date, rows[row].cells[col].state, rows[row].cells[col].colIdx,
                    rows[row].cells[col].rowIdx);

            Date date = rows[row].cells[col].date;
            mCellClickListener.clickDate(date);
            //
            update();
        }
    }

    /**
     * @author wuwenjie
     */
    class Row {
        public int rowIdx;

        Row(int rowIdx) {
            this.rowIdx = rowIdx;
        }

        public Cell[] cells = new Cell[TOTAL_COL];

        public void drawCells(Canvas canvas) {
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] != null) {
                    cells[i].drawSelf(canvas);
                }
            }
        }

    }

    /**
     * @author wuwenjie
     */
    class Cell {
        final Date date;
        State state;
        final int colIdx;
        final int rowIdx;
        final String dayOfMonthStr;

        public Cell(Date date, State state, int colIdx, int rowIdx) {
            super();
            this.date = date;
            this.state = state;
            this.colIdx = colIdx;
            this.rowIdx = rowIdx;
            this.dayOfMonthStr = String.valueOf(date.getDate());
        }

        public void drawSelf(Canvas canvas) {
            switch (state) {
                case TODAY:
                    mTextPaint.setColor(getResources().getColor(R.color.calendar_today));
                    break;
                case CURRENT_MONTH_DAY:
                    mTextPaint.setColor(Color.parseColor("#292C30"));
                    break;
                case PAST_MONTH_DAY:
                case NEXT_MONTH_DAY:
//                    mTextPaint.setColor(Color.parseColor("#fffffe"));
                    mTextPaint.setColor(Color.LTGRAY);
                    break;
                case UNREACH_DAY:
//                    mTextPaint.setColor(Color.GRAY);
                    mTextPaint.setColor(Color.parseColor("#292C30"));
                    break;
                case MARK_DAY:
                    mTextPaint.setColor(Color.parseColor("#E23D46"));
//                    canvas.drawCircle((float) (mCellSpace * (colIdx + 0.5)), (float) ((rowIdx + 0.5) * mCellSpace), mCellSpace / 3, mStrokePaint);
                    break;
                case SELECTED_DAY:
                    mTextPaint.setColor(Color.WHITE);
                    canvas.drawCircle((float) (mCellSpace * (colIdx + 0.5)), (float) ((rowIdx + 0.5) * mCellSpace), mCellSpace / 3, mCirclePaint);
                    break;
                case TODAY_AND_SELECTED_DAY: {
                    mTextPaint.setColor(Color.WHITE);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(getResources().getColor(R.color.calendar_select_day));
                    canvas.drawCircle((float) (mCellSpace * (colIdx + 0.5)), (float) ((rowIdx + 0.5) * mCellSpace), mCellSpace / 3, paint);
                }
                break;
                default:
                    break;
            }

            canvas.drawText(dayOfMonthStr, (float) ((colIdx + 0.5) * mCellSpace - mTextPaint.measureText(dayOfMonthStr) / 2),
                    (float) ((rowIdx + 0.7) * mCellSpace - mTextPaint.measureText(dayOfMonthStr, 0, 1) / 2), mTextPaint);
        }
    }

    /**
     * @author wuwenjie
     */
    enum State {
        TODAY, CURRENT_MONTH_DAY, PAST_MONTH_DAY, NEXT_MONTH_DAY, UNREACH_DAY, MARK_DAY, SELECTED_DAY, TODAY_AND_SELECTED_DAY
    }

    /**
     *
     */
    public void leftSlide() {
        mShowDate.add(Calendar.MONTH, -1);
        update();
    }

    /**
     *
     */
    public void rightSlide() {
        mShowDate.add(Calendar.MONTH, 1);
        update();
    }

    public void update() {
        fillDate();
        invalidate();
    }

    public Date getShowDate() {
        return mShowDate.getTime();
    }
}
