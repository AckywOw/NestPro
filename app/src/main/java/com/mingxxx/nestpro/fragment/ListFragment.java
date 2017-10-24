package com.mingxxx.nestpro.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.mingxxx.nestpro.R;
import com.mingxxx.nestpro.adapter.ListAdapter;
import com.mingxxx.nestpro.msg.UpPin;
import com.mingxxx.nestpro.view.FooterLoadView;
import com.mingxxx.nestpro.view.ptrListview.NestedPtrClassicDefaultFrameLayout;
import com.mingxxx.nestpro.view.ptrListview.NestedPtrFrameLayout;
import com.mingxxx.nestpro.view.recyclerView.BaseRecyclerViewAdapter;
import com.mingxxx.nestpro.view.recyclerView.HeaderAndFooterRecyclerViewAdapter;
import com.mingxxx.nestpro.view.recyclerView.HorizontalDividerItemDecoration;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Jiang on 2017/10/20.
 */

public class ListFragment extends Fragment {
  @Bind(R.id.ptr_layout)
  NestedPtrClassicDefaultFrameLayout ptrLayout;
  @Bind(R.id.rv_list)
  RecyclerView rvList;
  @Bind(R.id.tv_error)
  TextView tvError;
  @Bind(R.id.blank_view)
  NestedScrollView blankView;
  private Activity activity;

  private HeaderAndFooterRecyclerViewAdapter mHeaderAndFooterRecyclerViewAdapter;
  private FooterLoadView footerView;
  private BaseRecyclerViewAdapter listAdapter;

  private List<String> dataList = new ArrayList<String>();

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    activity = (Activity) context;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    EventBus.getDefault().register(this);
    View rootView = View.inflate(activity, R.layout.fragment_list, null);
    ButterKnife.bind(this, rootView);

    rvList.setLayoutManager(new LinearLayoutManager(getContext()));
    rvList.setItemAnimator(new DefaultItemAnimator());

    listAdapter = new ListAdapter(activity, dataList);
    mHeaderAndFooterRecyclerViewAdapter = new HeaderAndFooterRecyclerViewAdapter(listAdapter);
    //        View header = new View(getContext());
    //        header.setMinimumHeight(1);
    //        mHeaderAndFooterRecyclerViewAdapter.setHeaderView(header);
    footerView = new FooterLoadView(getContext());
    footerView.setState(FooterLoadView.TYPE_LOADING);
    mHeaderAndFooterRecyclerViewAdapter.setFooterView(footerView);
    rvList.addItemDecoration(
        new HorizontalDividerItemDecoration.Builder(activity).colorResId(R.color.spilt_line_color)
                                                             .sizeResId(R.dimen.divider_line)
                                                             .positionInsideItem(false)
                                                             .build());
    rvList.setAdapter(mHeaderAndFooterRecyclerViewAdapter);
    registListeners();
    return rootView;
  }

  private void registListeners() {
    ptrLayout.setOnRefreshListener(new NestedPtrFrameLayout.OnPtrRefreshListener() {
      @Override
      public void onRefreshBegin(NestedPtrFrameLayout frame) {
        new Thread(new Runnable() {
          @Override
          public void run() {
            SystemClock.sleep(2000);
            activity.runOnUiThread(new Runnable() {
              @Override
              public void run() {
                ptrLayout.refreshComplete();
              }
            });
          }
        }).start();
      }
    });
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    for (int i = 0; i < 15; i++) {
      dataList.add("Item" + i);
    }
    listAdapter.notifyDataSetChanged();
  }

  @Override
  public void onDestroyView() {
    EventBus.getDefault().unregister(this);
    ButterKnife.unbind(this);
    super.onDestroyView();
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void updataList(UpPin enable) {
    rvList.smoothScrollToPosition(0);
  }
}
