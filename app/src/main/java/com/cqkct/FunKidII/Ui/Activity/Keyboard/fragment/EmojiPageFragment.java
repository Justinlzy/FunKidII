/*
 * Copyright (c) 2015, 张涛.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cqkct.FunKidII.Ui.Activity.Keyboard.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.cqkct.FunKidII.EventBus.Event;
import com.cqkct.FunKidII.Ui.Activity.Keyboard.Adapter.EmoticonAdapter;
import com.cqkct.FunKidII.R;
import com.cqkct.FunKidII.Utils.EmotionUtils;
import com.cqkct.FunKidII.Utils.ScreenUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;


/**
 * Emoji表情分类的显示
 *
 * @author kymjs (http://www.kymjs.com/) on 6/8/15.
 */
public class EmojiPageFragment extends Fragment {

    private static final int ITEM_COL_COUNT = 5;
    private static final int ITEM_ROW_COUNT = 2;
    private static final int ITEM_PAGE_COUNT = ITEM_COL_COUNT * ITEM_ROW_COUNT;

    private ViewPager mPagerFace;
    private LinearLayout pagePointLayout;

    private GridView[] allPageViews;
    private RadioButton[] pointViews;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chat_frag_face, null);
        initWidget(rootView);
        return rootView;
    }

    protected void initWidget(View rootView) {
        mPagerFace = rootView.findViewById(R.id.frag_pager_face);
        pagePointLayout = rootView.findViewById(R.id.frag_point);

        int total = EmotionUtils.EMOTION_CLASSIC_MAP.size();
        int pages = (total + ITEM_PAGE_COUNT - 1) / ITEM_PAGE_COUNT;

        allPageViews = new GridView[pages];
        pointViews = new RadioButton[pages];

        // 获取屏幕宽度
        int screenWidth = ScreenUtils.getScreenWidthPixels(getActivity());
        // item 的间距
        int spacing = ScreenUtils.dp2px(getActivity(), 8);
        // 动态计算 item 的宽度和高度
        int itemWidth = (screenWidth - spacing * (ITEM_COL_COUNT + 1)) / ITEM_COL_COUNT;

        //名字集合
        List<String> emotionNames = EmotionUtils.EMOTICON_NAME_LIST;

        int indicatorUncheckedPix = ScreenUtils.dp2px(getContext(), 7);
        int indicatorCheckedPix = ScreenUtils.dp2px(getContext(), 8);

        for (int x = 0; x < pages; x++) {
            int start = x * ITEM_PAGE_COUNT;
            int end = (start + ITEM_PAGE_COUNT) > total ? total : (start + ITEM_PAGE_COUNT);

            final List<String> itemDatas = emotionNames.subList(start, end);

            GridView view = new GridView(getContext());

            view.setNumColumns(ITEM_COL_COUNT);
            view.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            view.setCacheColorHint(0);
            view.setPadding(spacing, spacing * 2 / 3, spacing, 0);
            view.setBackgroundResource(android.R.color.transparent);
            view.setSelector(android.R.color.transparent);
            view.setVerticalScrollBarEnabled(false);
            view.setGravity(Gravity.CENTER);
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            view.setAdapter(new EmoticonAdapter(view, itemDatas, itemWidth));

            view.setOnItemClickListener((parent, view1, position, id) -> {
                        Object item = parent.getAdapter().getItem(position);
                        if (item != null && item instanceof String) {
                            EventBus.getDefault().post(new Event.SendEmoticonChatMessage(System.currentTimeMillis(), (String) item));
                        }
                    }
            );

            allPageViews[x] = view;

            RadioButton tip = new RadioButton(getContext());
            tip.setBackgroundResource(R.drawable.chat_emoticon_page_indicator_point_selector);
            RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(indicatorUncheckedPix, indicatorUncheckedPix);
            layoutParams.leftMargin = indicatorUncheckedPix;
            tip.setButtonDrawable(android.R.color.transparent);
            tip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ViewGroup.LayoutParams params = buttonView.getLayoutParams();
                if (isChecked) {
                    params.height = indicatorCheckedPix;
                    params.width = indicatorCheckedPix;
                } else {
                    params.height = indicatorUncheckedPix;
                    params.width = indicatorUncheckedPix;
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
        mPagerFace.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

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
            View v = gridViewList[position];
            container.addView(v);
            return v;
        }
    }
}
