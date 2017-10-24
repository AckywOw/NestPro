package com.mingxxx.nestpro.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Jiang on 2017/10/23.
 */
@CoordinatorLayout.DefaultBehavior(SelfAppBarLayout.UnDownBehavior.class)
public class SelfAppBarLayout extends AppBarLayout {
  public SelfAppBarLayout(Context context) {
    super(context);
  }

  public SelfAppBarLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void setExpanded(boolean expanded, boolean animate) {
    super.setExpanded(expanded, animate);
    actAppBar(true);
  }

  protected void actAppBar(boolean enable) {
    CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
    AppBarLayout.Behavior behavior = (AppBarLayout.Behavior) params.getBehavior();
    if (enable) {
      behavior.setDragCallback(null);
    } else {
      behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
        @Override
        public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
          return false;
        }
      });
    }
  }

  public static class UnDownBehavior extends AppBarLayout.Behavior {

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child,
        View target, int dx, int dy, int[] consumed) {
      int beforeConsumedY = consumed[1];
      super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
      if (target instanceof SelfAppBarListener && dy < 0 && beforeConsumedY == consumed[1]) {
        //判断是否开启下拉刷新
        if (-getTopAndBottomOffset() >= child.getTotalScrollRange()) {
          ((SelfAppBarListener) target).setCanScrollDownCallback(new CanScrollDownCallback() {
            @Override
            public boolean canScrollDown() {
              return true;
            }
          });
        } else {
          ((SelfAppBarListener) target).setCanScrollDownCallback(new CanScrollDownCallback() {
            @Override
            public boolean canScrollDown() {
              return false;
            }
          });
        }
      }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target,
        int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
      if (child instanceof SelfAppBarLayout
          && dyUnconsumed < 0
          && -getTopAndBottomOffset() >= child.getTotalScrollRange()) {
        //下滑的时候，锁定Collapsed的AppBar
        ((SelfAppBarLayout) child).actAppBar(false);
        return;
      }
      super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed,
          dyUnconsumed);
    }
  }

  /**
   * Created by Jiang on 2017/10/24.
   */
  public interface SelfAppBarListener {

    void setCanScrollDownCallback(CanScrollDownCallback callback);
  }

  public interface CanScrollDownCallback {

    boolean canScrollDown();
  }
}
