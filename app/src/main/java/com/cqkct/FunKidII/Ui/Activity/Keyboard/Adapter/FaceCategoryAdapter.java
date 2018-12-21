package com.cqkct.FunKidII.Ui.Activity.Keyboard.Adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.cqkct.FunKidII.Ui.Activity.Keyboard.fragment.EmojiPageFragment;
import com.cqkct.FunKidII.Ui.Activity.Keyboard.fragment.FacePageFragment;

/**
 * 控件分类的viewpager适配器
 *
 * @author kymjs (http://www.kymjs.com/)
 */
public class FaceCategoryAdapter extends FragmentStatePagerAdapter {
    public FaceCategoryAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public Fragment getItem(int position) {
        return new EmojiPageFragment();
    }
}