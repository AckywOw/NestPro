package com.mingxxx.nestpro.view.ptrListview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

/**
 * 加载更多ListView抽象类,需要实际中用其实现类
 * 
 * @author Jiang
 * 
 *         <p>
 *         继承ListView：主要实现滑动到底部加载更多
 *         </p>
 *         <p>
 *         接口说明：
 *         </p>
 *         <p>
 *         OnMoreRefreshListener:activity中注册，监听开始加载更多和开始滚动
 *         </p>
 *         <p>
 *         activity中用的重要方法:
 *         <li>initFooterView(); 初始化底部view方法</li>
 *         <li>stopLoadMore(); 停止刷新</li>
 *         <li>stopNoMore(); 停止刷新并展示无数据</li>
 *         <li>setPullLoadEnable(boolean); 加载更多开关</li>
 *         </p>
 * 
 */
public abstract class LoadMoreListView extends ListView implements OnScrollListener {
	protected boolean mIsBottom;
	protected boolean hasMore = true;
	protected View mFooter;
	protected OnMoreRefreshListener mOnMoreRefreshListener;

	public LoadMoreListView(Context context) {
		super(context);
		init(context);
	}

	public LoadMoreListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public LoadMoreListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		mFooter = initFooterView();
		if (mFooter != null) {
			addFooterView(mFooter);
		} else {
			hasMore = false;
		}
		setOnScrollListener(this);
	}

	public abstract View initFooterView();

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

		mIsBottom = view.getLastVisiblePosition() == view.getCount() - 1;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && mOnMoreRefreshListener != null) {
			mOnMoreRefreshListener.onListViewScrolled();
		}
		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && mOnMoreRefreshListener != null) {
			mOnMoreRefreshListener.onListViewScrolledFinish();
		}

		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && hasMore && mIsBottom) {
			scrollToBottomState();
		}
	}

	/**
	 * if false, cannot load more
	 * 
	 * @param hasMore
	 */
	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
		if (hasMore) {
			setFooterVisible();
			onMoreRefreshComplete();
		} else {
			setFooterGone();
		}
	}

	public void setFooterVisible() {
		if (mFooter != null) {
//			addFooterView(mFooter);
			mFooter.setVisibility(View.VISIBLE);
		}
	}

	public void setFooterGone() {
		if (mFooter != null) {
//            removeFooterView(mFooter);
			mFooter.setVisibility(View.GONE);
		}
	}

	public void setFooterInvisible() {
		if (mFooter != null) {
			mFooter.setVisibility(View.INVISIBLE);
		}
	}

	public void scrollToBottomState() {
		onMoreRefresh();
	}

	private void onMoreRefresh() {
		onUIMoreRefresh_REFRESH();
		if (mOnMoreRefreshListener != null) {
			mOnMoreRefreshListener.onMoreRefresh();
		}
	}

	public void onMoreRefreshComplete() {
		onUIMoreRefresh_DOWN();
	}

	/**
	 * 停止刷新
	 */
	public void stopLoadMore() {
		onMoreRefreshComplete();
	}

	/**
	 * 停止刷新并展示无数据
	 */
	public void stopNoMore() {
		onUIMoreRefresh_NOMORE();
	}

	public void setPullLoadEnable(boolean flag) {
		setHasMore(flag);
	}

	/**
	 * 开始刷新调用
	 */
	public abstract void onUIMoreRefresh_REFRESH();

	/**
	 * 刷新结束
	 */
	public abstract void onUIMoreRefresh_DOWN();

	/**
	 * 刷新结束并无更多数据
	 */
	public abstract void onUIMoreRefresh_NOMORE();

	/**
	 * 设置加载更多监听回调
	 * 
	 * @param l
	 */
	public void setOnMoreRefreshListener(OnMoreRefreshListener l) {
		mOnMoreRefreshListener = l;
	}

	public interface OnMoreRefreshListener {

		/**
		 * 开始加载更多调用
		 */
		void onMoreRefresh();

		/**
		 * listview开始滚动调用
		 */
		void onListViewScrolled();

		/**
		 * listview 滚动结束
		 */
		void onListViewScrolledFinish();
	}
}
