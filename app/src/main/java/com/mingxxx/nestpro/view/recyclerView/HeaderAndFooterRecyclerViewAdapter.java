package com.mingxxx.nestpro.view.recyclerView;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;

/**
 * RecyclerView.Adapter with Header and Footer
 */
public class HeaderAndFooterRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER_VIEW = Integer.MIN_VALUE;
    private static final int TYPE_FOOTER_VIEW = Integer.MIN_VALUE + 1;

    public HeaderAndFooterRecyclerViewAdapter() {
    }

    public HeaderAndFooterRecyclerViewAdapter(RecyclerView.Adapter innerAdapter) {
        setAdapter(innerAdapter);
    }
    /**
     * RecyclerView使用的，真正的Adapter
     */
    private RecyclerView.Adapter<RecyclerView.ViewHolder> mInnerAdapter;

    private View mHeaderView;
    private View mFooterView;

    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {

        @Override
        public void onChanged() {
            super.onChanged();
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            super.onItemRangeChanged(positionStart, itemCount);
            notifyItemRangeChanged(positionStart + getHeaderViewsCount(), itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            notifyItemRangeInserted(positionStart + getHeaderViewsCount(), itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            notifyItemRangeRemoved(positionStart + getHeaderViewsCount(), itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
            int headerViewsCountCount = getHeaderViewsCount();
            notifyItemRangeChanged(fromPosition + headerViewsCountCount, toPosition + headerViewsCountCount + itemCount);
        }
    };

    /**
     * 设置adapter
     *
     * @param adapter
     */
    public void setAdapter(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {

        if (adapter != null) {
            if (!(adapter instanceof RecyclerView.Adapter))
                throw new RuntimeException("your adapter must be a RecyclerView.Adapter");
        }

        if (mInnerAdapter != null) {
            notifyItemRangeRemoved(getHeaderViewsCount(), mInnerAdapter.getItemCount());
            mInnerAdapter.unregisterAdapterDataObserver(mDataObserver);
        }

        this.mInnerAdapter = adapter;
        mInnerAdapter.registerAdapterDataObserver(mDataObserver);
        notifyItemRangeInserted(getHeaderViewsCount(), mInnerAdapter.getItemCount());
    }

    public RecyclerView.Adapter getInnerAdapter() {
        return mInnerAdapter;
    }

    public void setHeaderView(View header) {
        if (header == null) {
            throw new RuntimeException("header is null");
        }
        mHeaderView = header;
        this.notifyDataSetChanged();
    }

    public void setFooterView(View footer) {
        if (footer == null) {
            throw new RuntimeException("footer is null");
        }
        mFooterView = footer;
        this.notifyDataSetChanged();
    }


    /**
     * 返回HeaderView
     *
     * @return
     */
    public View getHeaderView() {
        return mHeaderView;
    }

    /**
     * 返回FooterView
     *
     * @return
     */
    public View getFooterView() {
        return mFooterView;
    }


    public void removeHeaderView(View view) {
        mHeaderView = null;
        this.notifyDataSetChanged();
    }

    public void removeFooterView(View view) {
        mFooterView = null;
        this.notifyDataSetChanged();
    }

    public int getHeaderViewsCount() {
        return mHeaderView == null ? 0 : 1;
    }

    public int getFooterViewsCount() {
        return mFooterView == null ? 0 : 1;
    }

    public boolean isHeader(int position) {
        return getHeaderViewsCount() > 0 && position == 0;
    }

    public boolean isFooter(int position) {
        int lastPosition = getItemCount() - 1;
        return getFooterViewsCount() > 0 && position == lastPosition;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER_VIEW) {
            return new ViewHolder(mHeaderView);
        } else if (viewType == TYPE_FOOTER_VIEW) {
            return new ViewHolder(mFooterView);
        } else {
            return mInnerAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int headerViewsCountCount = getHeaderViewsCount();
        int innerCount = mInnerAdapter.getItemCount();
        if (position >= headerViewsCountCount && position < headerViewsCountCount + innerCount) {
            mInnerAdapter.onBindViewHolder(holder, position - headerViewsCountCount);
        } else {
            ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            if (layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
                ((StaggeredGridLayoutManager.LayoutParams) layoutParams).setFullSpan(true);
            }
        }
    }

    @Override
    public int getItemCount() {
        return getHeaderViewsCount() + getFooterViewsCount() + mInnerAdapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        int innerCount = mInnerAdapter.getItemCount();
        int headerViewsCountCount = getHeaderViewsCount();
        if (position < headerViewsCountCount) {
            return TYPE_HEADER_VIEW;
        } else if (headerViewsCountCount <= position && position < headerViewsCountCount + innerCount) {
            int innerItemViewType = mInnerAdapter.getItemViewType(position - headerViewsCountCount);
            if (innerItemViewType <= Integer.MIN_VALUE + 1) {
                throw new IllegalArgumentException("your adapter's return value of getViewTypeCount() must > Integer.MIN_VALUE + 1");
            }
            return innerItemViewType;
        } else {
            return TYPE_FOOTER_VIEW;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
