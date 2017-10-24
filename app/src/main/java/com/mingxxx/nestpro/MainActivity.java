package com.mingxxx.nestpro;

import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.mingxxx.nestpro.adapter.SelfPagerAdapter;
import com.mingxxx.nestpro.msg.UpPin;
import com.mingxxx.nestpro.view.SelfAppBarLayout;
import org.greenrobot.eventbus.EventBus;

public class MainActivity extends FragmentActivity {

  public static final String TAG = "MainActivity";

  @Bind(R.id.tl_tabs)
  TabLayout tlTabs;
  @Bind(R.id.appbar)
  SelfAppBarLayout appbar;
  @Bind(R.id.vp_fragments)
  ViewPager vpFragments;
  @Bind(R.id.coordinator)
  CoordinatorLayout coordinator;
  @Bind(R.id.tv_up)
  TextView tvUp;
  @Bind(R.id.tv_top)
  TextView tvTop;

  private SelfPagerAdapter pagerAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    pagerAdapter = new SelfPagerAdapter(getSupportFragmentManager());
    vpFragments.setAdapter(pagerAdapter);
    vpFragments.setOffscreenPageLimit(3);
    tlTabs.setupWithViewPager(vpFragments);
    vpFragments.setCurrentItem(0);
  }

  @OnClick({ R.id.tv_up })
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.tv_up:
        // TODO: 2017/10/20
        up();
        break;
    }
  }

  private void up() {
    appbar.setExpanded(true, true);
    EventBus.getDefault()
            .post(new UpPin());
  }
}
