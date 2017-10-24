package com.mingxxx.nestpro.view.ptrListview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * 抽象下拉刷新容器，实际使用中用其实现类
 *
 * @author Jiang
 *         <p/>
 *         <p>
 *         继承PtrFrameLayout：默认实现下拉刷新功能功能抽象类，需要子类实现头布局， 实现OnUIRefreshListener
 *         </p>
 *         <p>
 *         方法说明：
 *         <li>initHeaderView(); 初始化头布局抽象方法，子类实现</li>
 *         <li>crossRotateLineFromTopUnderTouch(); 松开刷新</li>
 *         <li>crossRotateLineFromBottomUnderTouch(boolean); 松开刷新变下拉刷新</li>
 *         </p>
 */
public abstract class PtrClassicFrameLayout extends PtrFrameLayout implements PtrFrameLayout
        .OnUIRefreshListener {

    public PtrClassicFrameLayout(Context context) {
        super(context);
        initViews();
    }

    public PtrClassicFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews();
    }

    public PtrClassicFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initViews();
    }

    protected void initViews() {
        mHeaderView = initHeaderView();
        if (mHeaderView == null) {
            return;
        }
        setHeaderView(mHeaderView);
        setOnUIRefreshListener(this);
    }

    /**
     * init the headerview and return it.
     *
     * @return
     */
    public abstract View initHeaderView();

    @Override
    public void onUIPositionChange(PtrFrameLayout frame, boolean isUnderTouch, byte status,
                                   PtrIndicator ptrIndicator) {
        final int mOffsetToRefresh = frame.getOffsetToRefresh();
        final int currentPos = ptrIndicator.getCurrentPosY();
        final int lastPos = ptrIndicator.getLastPosY();

        if (currentPos < mOffsetToRefresh && lastPos >= mOffsetToRefresh) {
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {
                crossRotateLineFromBottomUnderTouch(frame);
            }
        } else if (currentPos > mOffsetToRefresh && lastPos <= mOffsetToRefresh) {
            if (isUnderTouch && status == PtrFrameLayout.PTR_STATUS_PREPARE) {
                crossRotateLineFromTopUnderTouch(frame);
            }
        }
    }

    /**
     * 松开刷新
     *
     * @param frame
     */
    public abstract void crossRotateLineFromTopUnderTouch(PtrFrameLayout frame);

    /**
     * 松开刷新变下拉刷新
     *
     * @param frame
     */
    public abstract void crossRotateLineFromBottomUnderTouch(PtrFrameLayout frame);
}
