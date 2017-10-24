package com.mingxxx.nestpro.view.ptrListview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Scroller;
import android.widget.TextView;
import com.mingxxx.nestpro.view.SelfAppBarLayout;

/**
 * 嵌套下拉刷新容器 放在
 *
 * @author Jiang
 *         <p>
 *         继承ViewGroup：主要实现下拉刷新功能，可适配各种ViewGroup，把其当作一个子view放进去，使用时需要用其子类，
 *         实现其头部view
 *         </p>
 *         <p>
 *         接口说明：
 *         </p>
 *         <p>
 *         OnUIRefreshListener:下拉刷新动作中，各个状态时，头部view要进行UI改变的接口，在头部view中注册
 *         </p>
 *         <p>
 *         OnPtrRefreshListener：下拉刷新发生时，activity要触发事件的接口，在activity中注册
 *         </p>
 *         <p>
 *         activity中用的重要方法:
 *         <li>startOnRefresh(); 静默刷新，不下拉，后台刷新时调用</li>
 *         <li>stopRefresh(); 调用刷新结束，恢复状态</li>
 *         <li>setPullRefreshEnable(boolean); 下拉刷新开关</li>
 *         <li>setOnRefreshListener(this); 设置activity中开始刷新监听</li>
 *         </p>
 */
public class NestedPtrFrameLayout extends ViewGroup
    implements NestedScrollingParent, NestedScrollingChild, SelfAppBarLayout.SelfAppBarListener {

  // status enum
  public final static byte PTR_STATUS_INIT = 1;
  public final static byte PTR_STATUS_PREPARE = 2;
  public final static byte PTR_STATUS_LOADING = 3;
  public final static byte PTR_STATUS_COMPLETE = 4;

  private static int ID = 1;

  // auto refresh status
  private static byte FLAG_AUTO_REFRESH_AT_ONCE = 0x01;
  private static byte FLAG_AUTO_REFRESH_BUT_LATER = 0x01 << 1;
  private static byte FLAG_ENABLE_NEXT_PTR_AT_ONCE = 0x01 << 2;
  private static byte FLAG_PIN_CONTENT = 0x01 << 3;

  private static byte MASK_AUTO_REFRESH = 0x03;

  protected final String LOG_TAG = "ptr-frame-" + ++ID;
  protected View mContent;
  // optional config for define header and content in xml file
  private int mHeaderId = Integer.MAX_VALUE / 2;
  private int mZoomId = Integer.MAX_VALUE / 2 - 1;
  private int mContainerId = Integer.MAX_VALUE / 2 + 1;
  // config
  private int mDurationToClose = 500;
  private int mDurationToCloseHeader = 700;
  private int mDurationToOpenHeader = 1000;
  private boolean mKeepHeaderWhenRefresh = true;
  private boolean mPullToRefresh = false;
  protected View mHeaderView;
  // working parameters
  private ScrollChecker mScrollChecker;
  // private int mCurrentPos = 0;
  // private int mLastPos = 0;
  private int mPagingTouchSlop;
  private int mHeaderHeight;

  private byte mStatus = PTR_STATUS_INIT;
  private boolean mDisableWhenHorizontalMove = true;
  private int mFlag = 0x00;

  // disable when detect moving horizontally
  private boolean mPreventForHorizontal = false;

  private MotionEvent mDownEvent;
  private MotionEvent mLastMoveEvent;

  private PtrUIHandlerHook mRefreshCompleteHook;

  private int mLoadingMinTime = 500;
  private long mLoadingStartTime = 0;
  private PtrIndicator mPtrIndicator;
  private boolean mHasSendCancelEvent = false;
  private boolean mPullRefreshEnable = true;
  private int matchParent;

  private OnUIRefreshListener onUIRefreshListener;
  private OnPtrRefreshListener onPtrRefreshListener;

  private NestedScrollingParentHelper mNestedScrollingParentHelper;
  private NestedScrollingChildHelper mNestedScrollingChildHelper;
  // If nested scrolling is enabled, the total amount that needed to be
  // consumed by this as the nested scrolling parent is used in place of the
  // overscroll determined by MOVE events in the onTouch handler
  private float mTotalUnconsumed;
  private boolean mNestedScrollInProgress;
  private final int[] mParentScrollConsumed = new int[2];
  private final int[] mParentOffsetInWindow = new int[2];

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (!isEnabled()
        || canChildScrollUp()
        || mStatus != PTR_STATUS_LOADING
        || mNestedScrollInProgress) {
      // Fail fast if we're not in a state where a swipe is possible
      return false;
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!isEnabled() || canChildScrollUp() || mNestedScrollInProgress) {
      // Fail fast if we're not in a state where a swipe is possible
      return false;
    }
    return super.onTouchEvent(event);
  }
  // NestedScrollingParent

  @Override
  public int getNestedScrollAxes() {
    return getNestedScrollingParentHelper().getNestedScrollAxes();
  }

  @Override
  public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
    return isEnabled()
        && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0
        && mStatus != PTR_STATUS_LOADING
        && mStatus != PTR_STATUS_COMPLETE
        && isNestedScrollingEnabled();
  }

  @Override
  public void onNestedScrollAccepted(View child, View target, int axes) {
    // Reset the counter of how much leftover scroll needs to be consumed.
    getNestedScrollingParentHelper().onNestedScrollAccepted(child, target, axes);
    // Dispatch up to the nested parent
    startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
    mTotalUnconsumed = 0;
    mPtrIndicator.onPressDown(0, 0);
    mNestedScrollInProgress = true;
    mScrollChecker.abortIfWorking();
  }

  @Override
  public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
    // before allowing the list to scroll
    if (dy > 0 && mPtrIndicator.hasLeftStartPosition()) {
      if (dy > mTotalUnconsumed) {
        consumed[1] = dy - (int) mTotalUnconsumed;
        mTotalUnconsumed = 0;
      } else {
        mTotalUnconsumed -= dy;
        consumed[1] = dy;
      }
      mPtrIndicator.onMove(dx, mTotalUnconsumed);
      movePos(mPtrIndicator.getOffsetY());
    }

    // Now let our nested parent consume the leftovers
    final int[] parentConsumed = mParentScrollConsumed;
    if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
      consumed[0] += parentConsumed[0];
      consumed[1] += parentConsumed[1];
    }
  }

  @Override
  public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
      final int dxUnconsumed, final int dyUnconsumed) {
    // Dispatch up to the nested parent first
    dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);

    // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
    // sometimes between two nested scrolling views, we need a way to be able to know when any
    // nested scrolling parent has stopped handling events. We do that by using the
    // 'offset in window 'functionality to see if we have been moved from the event.
    // This is a decent indication of whether we should take over the event stream or not.
    final int dy = dyUnconsumed + mParentOffsetInWindow[1];
    if ((canScrollDownCallback == null || canScrollDownCallback.canScrollDown())
        && dy < 0
        && !canChildScrollUp()) {
      mTotalUnconsumed += Math.abs(dy);
      mPtrIndicator.onMove(dxUnconsumed, mTotalUnconsumed);
      movePos(mPtrIndicator.getOffsetY());
    }
  }

  @Override
  public void onStopNestedScroll(View target) {
    getNestedScrollingParentHelper().onStopNestedScroll(target);
    mNestedScrollInProgress = false;

    mPtrIndicator.onRelease();
    if (mPtrIndicator.hasLeftStartPosition()) {
      onRelease(false);
    }
    mTotalUnconsumed = 0;

    // Dispatch up our nested parent
    stopNestedScroll();
  }

  @Override
  public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
    return dispatchNestedPreFling(velocityX, velocityY);
  }

  @Override
  public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
    return dispatchNestedFling(velocityX, velocityY, consumed);
  }

  // NestedScrollingChild

  @Override
  public void setNestedScrollingEnabled(boolean enabled) {
    getNestedScrollingChildHelper().setNestedScrollingEnabled(enabled);
  }

  @Override
  public boolean isNestedScrollingEnabled() {
    return getNestedScrollingChildHelper().isNestedScrollingEnabled();
  }

  @Override
  public boolean startNestedScroll(int axes) {
    return getNestedScrollingChildHelper().startNestedScroll(axes);
  }

  @Override
  public void stopNestedScroll() {
    getNestedScrollingChildHelper().stopNestedScroll();
  }

  @Override
  public boolean hasNestedScrollingParent() {
    return getNestedScrollingChildHelper().hasNestedScrollingParent();
  }

  @Override
  public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
      int dyUnconsumed, int[] offsetInWindow) {
    return getNestedScrollingChildHelper().dispatchNestedScroll(dxConsumed, dyConsumed,
        dxUnconsumed, dyUnconsumed, offsetInWindow);
  }

  @Override
  public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
    return getNestedScrollingChildHelper().dispatchNestedPreScroll(dx, dy, consumed,
        offsetInWindow);
  }

  @Override
  public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
    return getNestedScrollingChildHelper().dispatchNestedFling(velocityX, velocityY, consumed);
  }

  @Override
  public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
    return getNestedScrollingChildHelper().dispatchNestedPreFling(velocityX, velocityY);
  }

  public NestedPtrFrameLayout(Context context) {
    this(context, null);
  }

  public NestedPtrFrameLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public NestedPtrFrameLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mPtrIndicator = new PtrIndicator();
    mScrollChecker = new ScrollChecker();

    final ViewConfiguration conf = ViewConfiguration.get(getContext());
    mPagingTouchSlop = conf.getScaledTouchSlop();

    setNestedScrollingEnabled(true);
  }

  public NestedScrollingParentHelper getNestedScrollingParentHelper() {
    if (mNestedScrollingParentHelper == null) {
      mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
    }
    return mNestedScrollingParentHelper;
  }

  public NestedScrollingChildHelper getNestedScrollingChildHelper() {
    if (mNestedScrollingChildHelper == null) {
      mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
    }
    return mNestedScrollingChildHelper;
  }

  @Override
  protected void onFinishInflate() {
    final int childCount = getChildCount();
    if (childCount > 3) {
      throw new IllegalStateException("PtrFrameLayout only can host most 3 elements");
    } else if (childCount >= 2) {
      if (mHeaderId != 0 && mHeaderView == null) {
        mHeaderView = findViewById(mHeaderId);
      }
      if (mContainerId != 0 && mContent == null) {
        mContent = findViewById(mContainerId);
      }
      if (mContent == null) {
        View view = getChildAt(getChildCount() - 1);
        mContent = view;
      }

      // not specify header or content
      if (mContent == null || mHeaderView == null) {

        View child1 = getChildAt(0);
        View child2 = getChildAt(1);
        if (child1 instanceof OnUIRefreshListener) {
          mHeaderView = child1;
          mContent = child2;
        } else if (child2 instanceof OnUIRefreshListener) {
          mHeaderView = child2;
          mContent = child1;
        } else {
          // both are not specified
          if (mContent == null && mHeaderView == null) {
            mHeaderView = child1;
            mContent = child2;
          }
          // only one is specified
          else {
            if (mHeaderView == null) {
              mHeaderView = mContent == child1 ? child2 : child1;
            } else {
              mContent = mHeaderView == child1 ? child2 : child1;
            }
          }
        }
      }
    } else if (childCount == 1) {
      mContent = getChildAt(0);
    } else {
      TextView errorView = new TextView(getContext());
      errorView.setClickable(true);
      errorView.setTextColor(0xffff6600);
      errorView.setGravity(Gravity.CENTER);
      errorView.setTextSize(20);
      errorView.setText("The content view in PtrFrameLayout is empty. Do you forget to "
          + "specify its id in xml layout file?");
      mContent = errorView;
      addView(mContent);
    }
    if (mHeaderView != null) {
      mHeaderView.bringToFront();
    }
    super.onFinishInflate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (mHeaderView != null) {
      measureChildWithMargins(mHeaderView, widthMeasureSpec, 0, heightMeasureSpec, 0);
      MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
      mHeaderHeight = mHeaderView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
      mPtrIndicator.setHeaderHeight(mHeaderHeight);
    }

    if (mZoomView != null) {
      measureChildWithMargins(mZoomView, widthMeasureSpec, 0, heightMeasureSpec, 0);
    }

    if (mContent != null) {
      measureContentView(mContent, widthMeasureSpec, heightMeasureSpec);
    }
  }

  private void measureContentView(View child, int parentWidthMeasureSpec,
      int parentHeightMeasureSpec) {
    final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

    final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
        getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin, lp.width);
    final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
        getPaddingTop() + getPaddingBottom() + lp.topMargin, lp.height);

    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
  }

  @Override
  protected void onLayout(boolean flag, int i, int j, int k, int l) {
    layoutChildren();
  }

  private void layoutChildren() {
    int offsetX = mPtrIndicator.getCurrentPosY();
    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();

    if (mHeaderView != null) {
      MarginLayoutParams lp = (MarginLayoutParams) mHeaderView.getLayoutParams();
      final int left = paddingLeft + lp.leftMargin;
      final int top = paddingTop + lp.topMargin + offsetX - mHeaderHeight;
      final int right = left + mHeaderView.getMeasuredWidth();
      final int bottom = top + mHeaderView.getMeasuredHeight();
      mHeaderView.layout(left, top, right, bottom);
    }

    if (mZoomView != null) {
      MarginLayoutParams lp = (MarginLayoutParams) mZoomView.getLayoutParams();
      final int left = paddingLeft + lp.leftMargin;
      final int top = paddingTop + lp.topMargin + offsetX - mZoomView.getMeasuredHeight();
      final int right = left + mZoomView.getMeasuredWidth();
      final int bottom = top + mZoomView.getMeasuredHeight();
      mZoomView.layout(left, top, right, bottom);
    }

    if (mContent != null) {
      if (isPinContent()) {
        offsetX = 0;
      }
      MarginLayoutParams lp = (MarginLayoutParams) mContent.getLayoutParams();
      final int left = paddingLeft + lp.leftMargin;
      final int top = paddingTop + lp.topMargin + offsetX;
      final int right = left + mContent.getMeasuredWidth();
      final int bottom = top + mContent.getMeasuredHeight();
      mContent.layout(left, top, right, bottom);
    }
  }

  private boolean dispatchTouchEventSupper(MotionEvent e) {
    return super.dispatchTouchEvent(e);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent e) {
    if (!isEnabled()
        || mContent == null
        || mHeaderView == null
        || !mPullRefreshEnable
        || isNestedScrollingEnabled()) {
      return dispatchTouchEventSupper(e);
    }
    int action = e.getAction();
    switch (action) {
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        mPtrIndicator.onRelease();
        if (mPtrIndicator.hasLeftStartPosition()) {
          onRelease(false);
          if (mPtrIndicator.hasMovedAfterPressedDown()) {
            sendCancelEvent();
            return true;
          }
          return dispatchTouchEventSupper(e);
        } else {
          return dispatchTouchEventSupper(e);
        }

      case MotionEvent.ACTION_DOWN:
        mHasSendCancelEvent = false;
        mDownEvent = e;
        mPtrIndicator.onPressDown(e.getX(), e.getY());

        mScrollChecker.abortIfWorking();

        mPreventForHorizontal = false;
        if (mPtrIndicator.hasLeftStartPosition()) {
          // do nothing, intercept child event
        } else {
          dispatchTouchEventSupper(e);
        }
        return true;

      case MotionEvent.ACTION_MOVE:
        mLastMoveEvent = e;
        mPtrIndicator.onMove(e.getX(), e.getY());
        float offsetX = mPtrIndicator.getOffsetX();
        float offsetY = mPtrIndicator.getOffsetY();

        if (mDisableWhenHorizontalMove && !mPreventForHorizontal && (Math.abs(offsetX)
            > mPagingTouchSlop && Math.abs(offsetX) > Math.abs(offsetY))) {
          if (mPtrIndicator.isInStartPosition()) {
            mPreventForHorizontal = true;
          }
        }
        if (mPreventForHorizontal) {
          return dispatchTouchEventSupper(e);
        }

        boolean moveDown = offsetY > 0;
        boolean moveUp = !moveDown;
        boolean canMoveUp = mPtrIndicator.hasLeftStartPosition();

        // disable move when header not reach top
        if (moveDown && onPtrRefreshListener != null && !checkContentCanBePulledDown(this, mContent,
            mHeaderView)) {
          Log.e("ptrframelayout", "disable move when header not reach top");
          return dispatchTouchEventSupper(e);
        }
        if ((moveUp && canMoveUp) || moveDown) {
          movePos(offsetY);
          return true;
        }
    }
    return dispatchTouchEventSupper(e);
  }

  /**
   * if deltaY > 0, move the content down
   *
   * @param deltaY
   */
  private void movePos(float deltaY) {
    // has reached the top
    if ((deltaY < 0 && mPtrIndicator.isInStartPosition())) {
      return;
    }

    int to = mPtrIndicator.getCurrentPosY() + (int) deltaY;

    // over top
    if (mPtrIndicator.willOverTop(to)) {
      to = PtrIndicator.POS_START;
    }

    mPtrIndicator.setCurrentPos(to);
    int change = to - mPtrIndicator.getLastPosY();
    updatePos(change);
  }

  private void updatePos(int change) {
    if (change == 0) {
      return;
    }

    boolean isUnderTouch = mPtrIndicator.isUnderTouch();

    // once moved, cancel event will be sent to child
    if (isUnderTouch && !mHasSendCancelEvent && mPtrIndicator.hasMovedAfterPressedDown()) {
      mHasSendCancelEvent = true;
      //            sendCancelEvent();
    }

    // leave initiated position or just refresh complete
    if ((mPtrIndicator.hasJustLeftStartPosition() && mStatus == PTR_STATUS_INIT)
        || (mPtrIndicator.goDownCrossFinishPosition()
        && mStatus == PTR_STATUS_COMPLETE
        && isEnabledNextPtrAtOnce())) {

      mStatus = PTR_STATUS_PREPARE;
      if (onUIRefreshListener != null) {
        onUIRefreshListener.onUIRefreshPrepare(this);
      }
    }

    // back to initiated position
    if (mPtrIndicator.hasJustBackToStartPosition()) {
      tryToNotifyReset();

      // recover event to children
      if (isUnderTouch) {
        //                sendDownEvent();
      }
    }

    // Pull to Refresh
    if (mStatus == PTR_STATUS_PREPARE) {
      // reach fresh height while moving from top to bottom
      if (isUnderTouch
          && !isAutoRefresh()
          && mPullToRefresh
          && mPtrIndicator.crossRefreshLineFromTopToBottom()) {
        tryToPerformRefresh();
      }
      // reach header height while auto refresh
      if (performAutoRefreshButLater()
          && mPtrIndicator.hasJustReachedHeaderHeightFromTopToBottom()) {
        tryToPerformRefresh();
      }
    }

    mHeaderView.offsetTopAndBottom(change);
    if (!isPinContent()) {
      mContent.offsetTopAndBottom(change);
    }
    invalidate();

    if (onUIRefreshListener != null) {
      onUIRefreshListener.onUIPositionChange(this, isUnderTouch, mStatus, mPtrIndicator);
    }
    onPositionChange(isUnderTouch, mStatus, mPtrIndicator);
  }

  protected void onPositionChange(boolean isInTouching, byte status, PtrIndicator mPtrIndicator) {
  }

  @SuppressWarnings("unused")
  public int getHeaderHeight() {
    return mHeaderHeight;
  }

  private void onRelease(boolean stayForLoading) {

    tryToPerformRefresh();

    if (mStatus == PTR_STATUS_LOADING) {
      // keep header for fresh
      if (mKeepHeaderWhenRefresh) {
        // scroll header back
        if (mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && !stayForLoading) {
          mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToKeepHeaderWhileLoading(),
              mDurationToClose);
        } else {
          // do nothing
        }
      } else {
        tryScrollBackToTopWhileLoading();
      }
    } else {
      if (mStatus == PTR_STATUS_COMPLETE) {
        notifyUIRefreshComplete(false);
      } else {
        tryScrollBackToTopAbortRefresh();
      }
    }
  }

  /**
   * please DO REMEMBER resume the hook
   *
   * @param hook
   */

  public void setRefreshCompleteHook(PtrUIHandlerHook hook) {
    mRefreshCompleteHook = hook;
    hook.setResumeAction(new Runnable() {
      @Override
      public void run() {
        notifyUIRefreshComplete(true);
      }
    });
  }

  /**
   * Scroll back to to if is not under touch
   */
  private void tryScrollBackToTop() {
    if (!mPtrIndicator.isUnderTouch()) {
      mScrollChecker.tryToScrollTo(PtrIndicator.POS_START, mDurationToCloseHeader);
    }
  }

  /**
   * just make easier to understand
   */
  private void tryScrollBackToTopWhileLoading() {
    tryScrollBackToTop();
  }

  /**
   * just make easier to understand
   */
  private void tryScrollBackToTopAfterComplete() {
    tryScrollBackToTop();
  }

  /**
   * just make easier to understand
   */
  private void tryScrollBackToTopAbortRefresh() {
    tryScrollBackToTop();
  }

  private boolean tryToPerformRefresh() {
    if (mStatus != PTR_STATUS_PREPARE) {
      return false;
    }

    //
    if ((mPtrIndicator.isOverOffsetToKeepHeaderWhileLoading() && isAutoRefresh())
        || mPtrIndicator.isOverOffsetToRefresh()) {
      mStatus = PTR_STATUS_LOADING;
      performRefresh();
    }
    return false;
  }

  private void performRefresh() {
    mLoadingStartTime = System.currentTimeMillis();
    if (onUIRefreshListener != null) {
      onUIRefreshListener.onUIRefreshBegin(this);
    }
    if (onPtrRefreshListener != null) {
      onPtrRefreshListener.onRefreshBegin(this);
    }
  }

  /**
   * 后台静默刷新
   */
  public void startOnRefresh() {
    if (onUIRefreshListener != null) {
      onUIRefreshListener.onUIRefreshBegin(this);
    }
    if (onPtrRefreshListener != null) {
      onPtrRefreshListener.onRefreshBegin(this);
    }
  }

  /**
   * If at the top and not in loading, reset
   */
  private boolean tryToNotifyReset() {
    if ((mStatus == PTR_STATUS_COMPLETE || mStatus == PTR_STATUS_PREPARE)
        && mPtrIndicator.isInStartPosition()) {
      if (onUIRefreshListener != null) {
        onUIRefreshListener.onUIReset(this);
      }
      mStatus = PTR_STATUS_INIT;
      clearFlag();
      return true;
    }
    return false;
  }

  protected void onPtrScrollAbort() {
    if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
      onRelease(true);
    }
  }

  protected void onPtrScrollFinish() {
    if (mPtrIndicator.hasLeftStartPosition() && isAutoRefresh()) {
      onRelease(true);
    }
  }

  /**
   * if false, cannot pull
   *
   * @param flag
   */
  public void setPullRefreshEnable(boolean flag) {
    this.mPullRefreshEnable = flag;
  }

  /**
   * Call this when data is loaded. The UI will perform complete at once or
   * after a delay, depends on the time elapsed is greater then
   * {@link #mLoadingMinTime} or not.
   */
  final public void refreshComplete() {
    if (mRefreshCompleteHook != null) {
      mRefreshCompleteHook.reset();
    }

    int delay = (int) (mLoadingMinTime - (System.currentTimeMillis() - mLoadingStartTime));
    if (delay <= 0) {
      performRefreshComplete();
    } else {
      postDelayed(new Runnable() {
        @Override
        public void run() {
          performRefreshComplete();
        }
      }, delay);
    }
  }

  /**
   * 刷新结束
   */
  public void stopRefresh() {
    refreshComplete();
  }

  /**
   * Do refresh complete work when time elapsed is greater than
   * {@link #mLoadingMinTime}
   */
  private void performRefreshComplete() {
    mStatus = PTR_STATUS_COMPLETE;

    // if is auto refresh do nothing, wait scroller stop
    if (mScrollChecker.mIsRunning && isAutoRefresh()) {
      // do nothing
      return;
    }

    notifyUIRefreshComplete(false);
  }

  /**
   * Do real refresh work. If there is a hook, execute the hook first.
   *
   * @param ignoreHook
   */
  private void notifyUIRefreshComplete(boolean ignoreHook) {
    /**
     * After hook operation is done, {@link #notifyUIRefreshComplete} will
     * be call in resume action to ignore hook.
     */
    if (mPtrIndicator.hasLeftStartPosition() && !ignoreHook && mRefreshCompleteHook != null) {
      mRefreshCompleteHook.takeOver();
      return;
    }
    if (onUIRefreshListener != null) {
      onUIRefreshListener.onUIRefreshComplete(this);
    }
    mPtrIndicator.onUIRefreshComplete();
    tryScrollBackToTopAfterComplete();
    tryToNotifyReset();
  }

  public void autoRefresh() {
    autoRefresh(false, mDurationToOpenHeader);
  }

  public void autoRefresh(boolean atOnce) {
    autoRefresh(atOnce, mDurationToOpenHeader);
  }

  private void clearFlag() {
    // remove auto fresh flag
    mFlag = mFlag & ~MASK_AUTO_REFRESH;
  }

  public void autoRefresh(boolean atOnce, int duration) {
    if (!mPullRefreshEnable) {
      return;
    }
    if (mStatus != PTR_STATUS_INIT) {
      reset();
    }

    mFlag |= atOnce ? FLAG_AUTO_REFRESH_AT_ONCE : FLAG_AUTO_REFRESH_BUT_LATER;

    mStatus = PTR_STATUS_PREPARE;
    if (onUIRefreshListener != null) {
      onUIRefreshListener.onUIRefreshPrepare(this);
    }
    mScrollChecker.tryToScrollTo(mPtrIndicator.getOffsetToRefresh(), duration);
    if (atOnce) {
      mStatus = PTR_STATUS_LOADING;
      performRefresh();
    }
  }

  /**
   * 恢复到初始状态
   */
  private void reset() {
    mScrollChecker.reset();
    if (mPtrIndicator.getLastPosY() > PtrIndicator.POS_START) {
      int change = PtrIndicator.POS_START - mPtrIndicator.getLastPosY();
      mPtrIndicator.setCurrentPos(PtrIndicator.POS_START);
      mHeaderView.offsetTopAndBottom(change);
      if (!isPinContent()) {
        mContent.offsetTopAndBottom(change);
      }
      invalidate();
    }
    tryToNotifyReset();
  }

  public boolean isAutoRefresh() {
    return (mFlag & MASK_AUTO_REFRESH) > 0;
  }

  private boolean performAutoRefreshButLater() {
    return (mFlag & MASK_AUTO_REFRESH) == FLAG_AUTO_REFRESH_BUT_LATER;
  }

  /**
   * If @param enable has been set to true. The user can perform next PTR at
   * once.
   *
   * @param enable
   */
  public void setEnabledNextPtrAtOnce(boolean enable) {
    if (enable) {
      mFlag = mFlag | FLAG_ENABLE_NEXT_PTR_AT_ONCE;
    } else {
      mFlag = mFlag & ~FLAG_ENABLE_NEXT_PTR_AT_ONCE;
    }
  }

  public boolean isEnabledNextPtrAtOnce() {
    return (mFlag & FLAG_ENABLE_NEXT_PTR_AT_ONCE) > 0;
  }

  /**
   * The content view will now move when
   *
   * @param pinContent set to true.
   * @param pinContent
   */
  public void setPinContent(boolean pinContent) {
    if (pinContent) {
      mFlag = mFlag | FLAG_PIN_CONTENT;
    } else {
      mFlag = mFlag & ~FLAG_PIN_CONTENT;
    }
  }

  public boolean isPinContent() {
    return (mFlag & FLAG_PIN_CONTENT) > 0;
  }

  /**
   * It's useful when working with viewpager.
   *
   * @param disable
   */
  public void disableWhenHorizontalMove(boolean disable) {
    mDisableWhenHorizontalMove = disable;
  }

  /**
   * loading will last at least for so long
   *
   * @param time
   */
  public void setLoadingMinTime(int time) {
    mLoadingMinTime = time;
  }

  /**
   * Not necessary any longer. Once moved, cancel event will be sent to child.
   *
   * @param yes
   */
  @Deprecated
  public void setInterceptEventWhileWorking(boolean yes) {
  }

  @SuppressWarnings({ "unused" })
  public View getContentView() {
    return mContent;
  }

  public void setPtrIndicator(PtrIndicator slider) {
    if (mPtrIndicator != null && mPtrIndicator != slider) {
      slider.convertFrom(mPtrIndicator);
    }
    mPtrIndicator = slider;
  }

  @SuppressWarnings({ "unused" })
  public float getResistance() {
    return mPtrIndicator.getResistance();
  }

  public void setResistance(float resistance) {
    mPtrIndicator.setResistance(resistance);
  }

  @SuppressWarnings({ "unused" })
  public float getDurationToClose() {
    return mDurationToClose;
  }

  public void setDurationToClose(int duration) {
    mDurationToClose = duration;
  }

  @SuppressWarnings({ "unused" })
  public long getDurationToCloseHeader() {
    return mDurationToCloseHeader;
  }

  public void setDurationToCloseHeader(int duration) {
    mDurationToCloseHeader = duration;
  }

  public void setRatioOfHeaderHeightToRefresh(float ratio) {
    mPtrIndicator.setRatioOfHeaderHeightToRefresh(ratio);
  }

  public int getOffsetToRefresh() {
    return mPtrIndicator.getOffsetToRefresh();
  }

  @SuppressWarnings({ "unused" })
  public void setOffsetToRefresh(int offset) {
    mPtrIndicator.setOffsetToRefresh(offset);
  }

  @SuppressWarnings({ "unused" })
  public float getRatioOfHeaderToHeightRefresh() {
    return mPtrIndicator.getRatioOfHeaderToHeightRefresh();
  }

  @SuppressWarnings({ "unused" })
  public void setOffsetToKeepHeaderWhileLoading(int offset) {
    mPtrIndicator.setOffsetToKeepHeaderWhileLoading(offset);
  }

  @SuppressWarnings({ "unused" })
  public int getOffsetToKeepHeaderWhileLoading() {
    return mPtrIndicator.getOffsetToKeepHeaderWhileLoading();
  }

  @SuppressWarnings({ "unused" })
  public boolean isKeepHeaderWhenRefresh() {
    return mKeepHeaderWhenRefresh;
  }

  public void setKeepHeaderWhenRefresh(boolean keepOrNot) {
    mKeepHeaderWhenRefresh = keepOrNot;
  }

  public boolean isPullToRefresh() {
    return mPullToRefresh;
  }

  public void setPullToRefresh(boolean pullToRefresh) {
    mPullToRefresh = pullToRefresh;
  }

  @SuppressWarnings({ "unused" })
  public View getHeaderView() {
    return mHeaderView;
  }

  public void setHeaderView(View header) {
    if (mHeaderView != null && header != null && mHeaderView != header) {
      removeView(mHeaderView);
    }
    ViewGroup.LayoutParams lp = header.getLayoutParams();
    if (lp == null) {
      lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      header.setLayoutParams(lp);
    }
    mHeaderView = header;
    mHeaderView.setId(mHeaderId);
    addView(header);
  }

  protected View mZoomView;

  public void setZoomView(View zoom) {
    if (mZoomView != null && zoom != null && mZoomView != zoom) {
      removeView(mZoomView);
    }
    ViewGroup.LayoutParams lp = zoom.getLayoutParams();
    if (lp == null) {
      lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      zoom.setLayoutParams(lp);
    }
    mZoomView = zoom;
    mZoomView.setId(mZoomId);
    addView(zoom);
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  @Override
  public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  private void sendCancelEvent() {
    MotionEvent last = mDownEvent;
    last = mLastMoveEvent;
    MotionEvent e = MotionEvent.obtain(last.getDownTime(),
        last.getEventTime() + ViewConfiguration.getLongPressTimeout(), MotionEvent.ACTION_CANCEL,
        last.getX(), last.getY(), last.getMetaState());
    dispatchTouchEventSupper(e);
  }

  private void sendDownEvent() {
    final MotionEvent last = mLastMoveEvent;
    MotionEvent e =
        MotionEvent.obtain(last.getDownTime(), last.getEventTime(), MotionEvent.ACTION_DOWN,
            last.getX(), last.getY(), last.getMetaState());
    dispatchTouchEventSupper(e);
  }

  private SelfAppBarLayout.CanScrollDownCallback canScrollDownCallback;

  @Override
  public void setCanScrollDownCallback(SelfAppBarLayout.CanScrollDownCallback callback) {
    canScrollDownCallback = callback;
  }

  public static class LayoutParams extends MarginLayoutParams {

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);
    }

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    @SuppressWarnings({ "unused" })
    public LayoutParams(MarginLayoutParams source) {
      super(source);
    }

    public LayoutParams(ViewGroup.LayoutParams source) {
      super(source);
    }
  }

  class ScrollChecker implements Runnable {

    private int mLastFlingY;
    private Scroller mScroller;
    private boolean mIsRunning = false;
    private int mStart;
    private int mTo;

    public ScrollChecker() {
      mScroller = new Scroller(getContext());
    }

    public void run() {
      boolean finish = !mScroller.computeScrollOffset() || mScroller.isFinished();
      int curY = mScroller.getCurrY();
      int deltaY = curY - mLastFlingY;
      if (!finish) {
        mLastFlingY = curY;
        movePos(deltaY);
        post(this);
      } else {
        finish();
      }
    }

    private void finish() {
      reset();
      onPtrScrollFinish();
    }

    private void reset() {
      mIsRunning = false;
      mLastFlingY = 0;
      removeCallbacks(this);
    }

    public void abortIfWorking() {
      if (mIsRunning) {
        if (!mScroller.isFinished()) {
          mScroller.forceFinished(true);
        }
        onPtrScrollAbort();
        reset();
      }
    }

    public void tryToScrollTo(int to, int duration) {
      if (mPtrIndicator.isAlreadyHere(to)) {
        return;
      }
      mStart = mPtrIndicator.getCurrentPosY();
      mTo = to;
      int distance = to - mStart;
      removeCallbacks(this);

      mLastFlingY = 0;

      // fix #47: Scroller should be reused,
      // https://github.com/liaohuqiu/android-Ultra-Pull-To-Refresh/issues/47
      if (!mScroller.isFinished()) {
        mScroller.forceFinished(true);
      }
      mScroller.startScroll(0, 0, 0, distance, duration);
      post(this);
      mIsRunning = true;
    }
  }

  public void setOnUIRefreshListener(OnUIRefreshListener onRefreshListener) {
    this.onUIRefreshListener = onRefreshListener;
  }

  /**
   * pull UI state's Listener
   *
   * @author HEcom
   */
  public interface OnUIRefreshListener {

    /**
     * 重置头布局
     *
     * @param frame
     */
    void onUIReset(NestedPtrFrameLayout frame);

    /**
     * 下拉刷新准备
     *
     * @param frame
     */
    void onUIRefreshPrepare(NestedPtrFrameLayout frame);

    /**
     * 开始刷新
     *
     * @param frame
     */
    void onUIRefreshBegin(NestedPtrFrameLayout frame);

    /**
     * 刷新结束恢复
     *
     * @param frame
     */
    void onUIRefreshComplete(NestedPtrFrameLayout frame);

    /**
     * 下拉过程是根据位置调用
     *
     * @param frame
     */
    void onUIPositionChange(NestedPtrFrameLayout frame, boolean isUnderTouch, byte status,
        PtrIndicator ptrIndicator);
  }

  private boolean checkContentCanBePulledDown(final NestedPtrFrameLayout frame, final View content,
      final View header) {
    return !canChildScrollUp(content);
  }

  public boolean canChildScrollUp() {
    if (android.os.Build.VERSION.SDK_INT < 14) {
      if (mContent instanceof AbsListView) {
        final AbsListView absListView = (AbsListView) mContent;
        return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0
            || absListView.getChildAt(0)
                          .getTop() < absListView.getPaddingTop());
      } else {
        return ViewCompat.canScrollVertically(mContent, -1) || mContent.getScrollY() > 0;
      }
    } else {
      return ViewCompat.canScrollVertically(mContent, -1);
    }
  }

  @SuppressLint("NewApi")
  public static boolean canChildScrollUp(View view) {
    if (android.os.Build.VERSION.SDK_INT < 14) {
      if (view instanceof AbsListView) {
        final AbsListView absListView = (AbsListView) view;
        return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0
            || absListView.getChildAt(0)
                          .getTop() < absListView.getPaddingTop());
      } else {
        return view.getScrollY() > 0;
      }
    } else {
      return view.canScrollVertically(-1);
    }
  }

  /**
   * 设置下拉监听回调
   *
   * @param onRefreshListener
   */
  public void setOnRefreshListener(OnPtrRefreshListener onRefreshListener) {
    this.onPtrRefreshListener = onRefreshListener;
  }

  public interface OnPtrRefreshListener {
    /**
     * 开始刷新调用
     *
     * @param frame
     */
    void onRefreshBegin(final NestedPtrFrameLayout frame);
  }
}
