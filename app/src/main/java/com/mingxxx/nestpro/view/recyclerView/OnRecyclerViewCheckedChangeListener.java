package com.mingxxx.nestpro.view.recyclerView;

import android.widget.CompoundButton;

/**
 * Created by Jiang on 16/3/8.
 */
public abstract class OnRecyclerViewCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {

    private BaseRecyclerViewAdapter adapter;

    public OnRecyclerViewCheckedChangeListener(BaseRecyclerViewAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (adapter.isOnBind()) return;
        onTheCheckChanged(buttonView, isChecked);
    }

    public abstract void onTheCheckChanged(CompoundButton buttonView, boolean isChecked);
}