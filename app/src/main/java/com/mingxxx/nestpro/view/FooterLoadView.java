package com.mingxxx.nestpro.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.mingxxx.nestpro.R;
import com.mingxxx.nestpro.view.recyclerView.OnMoreLoadListener;

/**
 * Created by Jiang on 16/4/11.
 */
public class FooterLoadView extends RelativeLayout implements OnMoreLoadListener {

    public static final int TYPE_LOADING = 1;
    public static final int TYPE_NOMORE = 2;
    public static final int TYPE_ONCLICK = 3;
    public static final int TYPE_GONE = 4;

    private ImageView iv_logo_loading;
    private TextView tv_loading_bg;
    private TextView tv_loading;
    private ObjectAnimator rotationY;
    private ValueAnimator openAnimator;
    private TextView tv_no_data;
    private RelativeLayout rl_loading;
    private RelativeLayout rl_content;

    public FooterLoadView(Context context) {
        super(context);
        init(context);
    }

    public FooterLoadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FooterLoadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.xlistview_header, this);
        rl_content = (RelativeLayout) findViewById(R.id.rl_content);
        rl_loading = (RelativeLayout) findViewById(R.id.rl_loading);
        iv_logo_loading = (ImageView) findViewById(R.id.iv_logo_loading);
        tv_loading_bg = (TextView) findViewById(R.id.tv_loading_bg);
        tv_loading = (TextView) findViewById(R.id.tv_loading);
        tv_no_data = (TextView) findViewById(R.id.tv_no_data);

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

        openAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(1500);
        openAnimator.setRepeatMode(ValueAnimator.RESTART);
        openAnimator.setRepeatCount(ValueAnimator.INFINITE);
        openAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                tv_loading.setWidth(0);
                tv_loading.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                tv_loading.setWidth(tv_loading_bg.getWidth());
            }
        });

        openAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = animation.getAnimatedFraction();
                tv_loading.setWidth((int) (animatedFraction * tv_loading_bg.getWidth()));
            }
        });
        setState(TYPE_GONE);
    }

    @Override
    public void onMoreLoad() {

    }

    public void onUIMoreRefresh_REFRESH() {
        tv_no_data.setVisibility(INVISIBLE);
        rotationY.start();
        openAnimator.start();
        rl_loading.setVisibility(VISIBLE);
        rl_content.setVisibility(VISIBLE);
    }

    public void onUIMoreRefresh_DOWN() {
    }

    public void onUIMoreRefresh_NOMORE() {
        rotationY.end();
        openAnimator.end();
        rl_loading.setVisibility(INVISIBLE);
        tv_no_data.setVisibility(VISIBLE);
        rl_content.setVisibility(VISIBLE);
    }

    private void onUIMoreRefresh_GONE() {
        rotationY.end();
        openAnimator.end();
        rl_content.setVisibility(GONE);
    }

    public void setState(int state) {
        switch (state) {
            case TYPE_LOADING:
                onUIMoreRefresh_REFRESH();
                break;
            case TYPE_NOMORE:
                onUIMoreRefresh_NOMORE();
                break;
            case TYPE_ONCLICK:
                onUIMoreRefresh_DOWN();
                break;
            case TYPE_GONE:
                onUIMoreRefresh_GONE();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimator();
        super.onDetachedFromWindow();
    }

    public void cancelAnimator() {
        iv_logo_loading.animate().cancel();
        tv_loading.animate().cancel();
    }

    public void setContent(String str) {
        tv_no_data.setText(str);
    }
}
