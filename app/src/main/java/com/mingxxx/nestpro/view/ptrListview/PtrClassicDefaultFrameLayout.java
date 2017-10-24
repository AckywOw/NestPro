package com.mingxxx.nestpro.view.ptrListview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.mingxxx.nestpro.R;

/**
 * 默认下拉刷新的容器 带刷新时间布局
 *
 * @author Jiang
 *         <p/>
 *         <p>
 *         继承PtrClassicFrameLayout：默认实现下拉刷新功能，带有默认布局，一般没有特殊布局都可直接使用此view
 *         </p>
 *         <p>
 *         重要方法(其他方法请看父类):
 *         <li>setRefreshTime(); 设置view中刷新时间方法</li>
 *         </p>
 */
public class PtrClassicDefaultFrameLayout extends PtrClassicFrameLayout {
  private static String[] TEXT_LOADING =
      { "上市公司安全保障",
      "厦门银行资金存管系统全面上线",
      "历史出借完美兑现",
      "每日10点、14点发布产品" };
  private static int INDEX = 0;

  public PtrClassicDefaultFrameLayout(Context context) {
    super(context);
  }

  public PtrClassicDefaultFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public PtrClassicDefaultFrameLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  private ImageView iv_logo_loading;
  private TextView tv_loading_bg;
  private TextView tv_loading;

  private ObjectAnimator rotationY;
  private ValueAnimator openAnimator;

  @Override
  public View initHeaderView() {
    View mHeader = LayoutInflater.from(getContext())
                                 .inflate(R.layout.xlistview_header, null);
    iv_logo_loading = (ImageView) mHeader.findViewById(R.id.iv_logo_loading);
    tv_loading_bg = (TextView) mHeader.findViewById(R.id.tv_loading_bg);
    tv_loading = (TextView) mHeader.findViewById(R.id.tv_loading);

    rotationY = ObjectAnimator.ofFloat(iv_logo_loading, "rotationY", 360F)
                              .setDuration(1000);
    rotationY.setRepeatMode(ValueAnimator.RESTART);
    rotationY.setRepeatCount(ValueAnimator.INFINITE);
    rotationY.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        super.onAnimationStart(animation);
        iv_logo_loading.setRotationY(0);
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        iv_logo_loading.setRotationY(0);
      }
    });

    openAnimator = ValueAnimator.ofFloat(0f, 1f)
                                .setDuration(1500);
    openAnimator.setRepeatMode(ValueAnimator.RESTART);
    openAnimator.setRepeatCount(ValueAnimator.INFINITE);
    openAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationStart(Animator animation) {
        super.onAnimationStart(animation);
        tv_loading.setMaxWidth(0);
        tv_loading.setVisibility(VISIBLE);
      }

      @Override
      public void onAnimationEnd(Animator animation) {
        super.onAnimationEnd(animation);
        tv_loading.setMaxWidth(tv_loading_bg.getWidth());
      }
    });

    openAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        float animatedFraction = animation.getAnimatedFraction();
        tv_loading.setMaxWidth((int) (animatedFraction * tv_loading_bg.getWidth()));
      }
    });
    return mHeader;
  }

  @Override
  public void onUIReset(PtrFrameLayout frame) {
    tv_loading.setVisibility(INVISIBLE);
  }

  @Override
  public void onUIRefreshPrepare(PtrFrameLayout frame) {
    tv_loading.setText(TEXT_LOADING[INDEX]);
    tv_loading_bg.setText(TEXT_LOADING[INDEX]);
    tv_loading.setVisibility(INVISIBLE);
  }

  @Override
  public void onUIRefreshBegin(PtrFrameLayout frame) {
    rotationY.start();
    openAnimator.start();
    INDEX++;
    INDEX %= TEXT_LOADING.length;
  }

  @Override
  public void onUIRefreshComplete(PtrFrameLayout frame) {
    rotationY.end();
    openAnimator.end();
  }

  @Override
  public void crossRotateLineFromTopUnderTouch(PtrFrameLayout frame) {
  }

  @Override
  public void crossRotateLineFromBottomUnderTouch(PtrFrameLayout frame) {
  }

  @Override
  protected void onDetachedFromWindow() {
    cancelAnimator();
    super.onDetachedFromWindow();
  }

  public void cancelAnimator() {
    iv_logo_loading.animate()
                   .cancel();
    tv_loading.animate()
              .cancel();
  }
}
