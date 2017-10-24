package com.mingxxx.nestpro.view.recyclerView;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

/**
 * Created by Jiang on 16/3/8.
 */
public abstract class BaseRecyclerViewAdapter<T> extends RecyclerView.Adapter {

    private boolean onBind;

    private List<T> mDataSet;

    private Context context;

    /**
     * Initialize the dataset of the Adapter.
     */
    public BaseRecyclerViewAdapter(List<T> dataSet) {
        setHasStableIds(true);
        mDataSet = dataSet;


    }


    /**
     * Initialize the dataset of the Adapter.
     */
    public BaseRecyclerViewAdapter(Context context, List<T> dataSet) {
        setHasStableIds(true);
        mDataSet = dataSet;
        this.context = context;
    }

    public List<T> getDataSet() {
        return mDataSet;
    }

    public T getT(int position) {
        return mDataSet.get(position);
    }

    public Context getContext() {
        return context;
    }

    public int getDataSetSize() {
        return mDataSet.size();
    }

    public boolean isOnBind() {
        return onBind;
    }

    /**
     * Provide a reference to the type of views that you are using (custom
     * ViewHolder)
     */
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//
//        private final TextView tv;
//        private final CheckBox cb;
//
//        public ViewHolder(View v) {
//            super(v);
//            tv = (TextView) v;
//            cb = (CheckBox) v;
//        }
//    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(getItemLayoutId(viewType),
                viewGroup, false);
        return onCreateViewHolder(v, viewType, viewGroup);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int i) {
        // Get element from your dataset at this position and replace the
        // contents of the view
        // with that element
        if (mOnItemClickLitener != null) {
            viewHolder.itemView.setOnClickListener(null);
        }
        onBind = true;
        onTheBindViewHolder(viewHolder, i, getItemViewType(i));
        onBind = false;
        if (mOnItemClickLitener != null) {
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (i < mDataSet.size()) {
                        mOnItemClickLitener.onItemClick(viewHolder.itemView, i, mDataSet.get(i));
                    }
                }
            });
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public abstract int getItemLayoutId(int viewType);

    public abstract RecyclerView.ViewHolder onCreateViewHolder(View v, int viewType, ViewGroup
            viewGroup);

    public abstract void onTheBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int
            position, int viewType);

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public OnItemClickLitener mOnItemClickLitener;

    public void setOnItemClickLitener(OnItemClickLitener<T> mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    public void reset() {
        mDataSet.clear();
        notifyDataSetChanged();
    }


    public interface OnItemClickLitener<T> {
        void onItemClick(View view, int position, T item);
    }

    public void addUpdate(T item) {
        mDataSet.add(item);
        notifyItemInserted(mDataSet.indexOf(item));
    }

    public void deleteUpdate(int position) {
        mDataSet.remove(position);
        notifyItemRemoved(position);
    }

    public void addList(List<T> dataSet) {
        this.mDataSet.addAll(dataSet);
        notifyDataSetChanged();
    }

    public void update(List<T> dataSet) {
        this.mDataSet = dataSet;
        notifyDataSetChanged();
    }

    public void update(T t) {
      if(mDataSet!=null) {
        for (int i = 0; i < mDataSet.size(); i++) {
          T dao = mDataSet.get(i);
          if(dao.equals(t)){
            mDataSet.set(i, t);
            notifyItemChanged(i);
            break;
          }
        }
      }
    }

    public void addUpdateAndNotifyAll(T item) {
        mDataSet.add(item);
        notifyDataSetChanged();
    }

    public void clear() {
        mDataSet.clear();
        notifyDataSetChanged();
    }
}
