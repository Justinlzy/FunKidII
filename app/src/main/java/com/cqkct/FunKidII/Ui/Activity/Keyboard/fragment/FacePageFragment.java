package com.cqkct.FunKidII.Ui.Activity.Keyboard.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Ui.Activity.Keyboard.Adapter.FaceAdapter;
import com.cqkct.FunKidII.Utils.DensityUtils;
import com.cqkct.FunKidII.Utils.EmotionUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 表情分类中每个分类的具体显示
 *
 * @author kymjs (http://www.kymjs.com/)
 */
public class FacePageFragment extends Fragment {
    private static final int ITEM_COL_COUNT = 5;
    private static final int ITEM_ROW_COUNT = 2;
    private static final int ITEM_PAGE_COUNT = ITEM_COL_COUNT * ITEM_ROW_COUNT;

    private ViewPager mPagerFace;
    private RadioGroup pagePointLayout;

    private GridView[] allPageViews;
    private RadioButton[] pointViews;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chat_frag_face, null);
        initWidget(view);
        return view;
    }

    protected void initWidget(View rootView) {
        mPagerFace = rootView.findViewById(R.id.frag_pager_face);
        pagePointLayout = rootView.findViewById(R.id.frag_point);

        int total = EmotionUtils.EMOTION_CLASSIC_MAP.size();
        int pages = (total + ITEM_PAGE_COUNT - 1) / ITEM_PAGE_COUNT;

        allPageViews = new GridView[pages];
        pointViews = new RadioButton[pages];
        int pointUncheckedPix = (int) DensityUtils.dp2px(getContext(), 7);
        int pointCheckedPix = (int) DensityUtils.dp2px(getContext(), 8);
        int spacingPix = (int) DensityUtils.dp2px(getContext(), 10);
        int paddingTopPix = (int) DensityUtils.dp2px(getContext(), 16);
        int paddingLeftPix = (int) DensityUtils.dp2px(getContext(), 5);

        List<String> faceNames = EmotionUtils.EMOTICON_NAME_LIST;
        for (int x = 0; x < pages; x++) {
            int start = x * ITEM_PAGE_COUNT;
            int end = start + ITEM_PAGE_COUNT > total ? total : start + ITEM_PAGE_COUNT;
            List<String> itemDatas = faceNames.subList(start, end);

            GridView view = new GridView(getContext());
            FaceAdapter faceAdapter = new FaceAdapter(view, itemDatas);
            view.setAdapter(faceAdapter);

            view.setNumColumns(ITEM_COL_COUNT);
            view.setHorizontalSpacing(1);
            view.setVerticalSpacing(1);
            view.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            view.setCacheColorHint(0);
            view.setPadding(paddingLeftPix, paddingTopPix, paddingLeftPix, 0);
            view.setBackgroundResource(android.R.color.transparent);
            view.setSelector(android.R.color.transparent);
            view.setVerticalScrollBarEnabled(false);
            view.setGravity(Gravity.CENTER);
            LayoutParams gridViewLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            gridViewLayoutParams.gravity = Gravity.CENTER;
            view.setLayoutParams(gridViewLayoutParams);
            view.setOnItemClickListener((parent, view1, position, id) -> {
                Object item = parent.getAdapter().getItem(position);
                if (item != null && item instanceof String) {
                    EventBus.getDefault().post(new Event.SendEmoticonChatMessage(System.currentTimeMillis(), (String) item));
                }
            });
            allPageViews[x] = view;

            RadioButton tip = new RadioButton(getContext());
            tip.setClickable(false);
            tip.setBackgroundResource(R.drawable.chat_emoticon_page_indicator_point_selector);
            RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(pointUncheckedPix, pointUncheckedPix);
            layoutParams.leftMargin = pointUncheckedPix;
            tip.setButtonDrawable(new ColorDrawable(Color.TRANSPARENT));
            tip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ViewGroup.LayoutParams params = buttonView.getLayoutParams();
                if (isChecked) {
                    params.height = pointCheckedPix;
                    params.width = pointCheckedPix;
                } else {
                    params.height = pointUncheckedPix;
                    params.width = pointUncheckedPix;
                }
                buttonView.setLayoutParams(params);
            });
            pagePointLayout.addView(tip, layoutParams);
            if (x == 0) {
                tip.setChecked(true);
            }
            pointViews[x] = tip;
        }

        PagerAdapter facePagerAdapter = new FacePagerAdapter(allPageViews);
        mPagerFace.setAdapter(facePagerAdapter);
        mPagerFace.addOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int index) {
                pointViews[index].setChecked(true);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });
    }

    public class FacePagerAdapter extends PagerAdapter {
        private final GridView[] gridViewList;

        public FacePagerAdapter(GridView[] gridViewList) {
            this.gridViewList = gridViewList;
        }

        @Override
        public int getCount() {
            return gridViewList.length;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        @Override
        public int getItemPosition(Object object) {
            return super.getItemPosition(object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(gridViewList[position]);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = gridViewList[position];
            container.addView(view);
            return view;
        }
    }


}