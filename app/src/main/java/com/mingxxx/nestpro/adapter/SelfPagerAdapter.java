package com.mingxxx.nestpro.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.mingxxx.nestpro.fragment.ListFragment;

/**
 * Created by Jiang on 2017/10/20.
 */

public class SelfPagerAdapter extends FragmentPagerAdapter {
  public SelfPagerAdapter(FragmentManager fm) {
    super(fm);
  }

  @Override
  public Fragment getItem(int position) {
    return new ListFragment();
  }

  @Override
  public int getCount() {
    return 3;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return "Tab" + position;
  }

}
