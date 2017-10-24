package com.mingxxx.nestpro.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.mingxxx.nestpro.R;
import com.mingxxx.nestpro.view.recyclerView.BaseRecyclerViewAdapter;
import java.util.List;

/**
 * Created by Jiang on 2016/8/10.
 */
public class ListAdapter
    extends BaseRecyclerViewAdapter<String> {

  public ListAdapter(Context context, List<String> dataSet) {
    super(context, dataSet);
  }

  @Override
  public int getItemLayoutId(int viewType) {
    return R.layout.adapter_list;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(View v, int viewType, ViewGroup viewGroup) {
    return new ItemViewHolder(v);
  }

  @Override
  public void onTheBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int viewType) {
    String dao = getT(position);
    if (dao != null) {
      ItemViewHolder holder = (ItemViewHolder) viewHolder;
      holder.tvText.setText(dao);
    }
  }

  static class ItemViewHolder extends RecyclerView.ViewHolder {

    @Bind(R.id.tv_text)
    TextView tvText;

    public ItemViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
