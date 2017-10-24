package com.mingxxx.nestpro.view.recyclerView;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by Jiang on 16/3/9.
 */
public class HorizontalDividerItemDecoration extends RecyclerView.ItemDecoration {

    private static final int DEFAULT_SIZE = 1;

    protected VisibilityProvider mVisibilityProvider;
    protected ColorProvider mColorProvider;
    protected SizeProvider mSizeProvider;
    protected MarginsProvider mMarginsProvider;
    protected boolean mShowLastDivider;
    protected boolean mPositionInsideItem;
    private Paint mPaint;

    public HorizontalDividerItemDecoration(Builder builder) {
        mColorProvider = builder.mColorProvider;
        mPaint = new Paint();
        setSizeProvider(builder);

        mVisibilityProvider = builder.mVisibilityProvider;
        mShowLastDivider = builder.mShowLastDivider;
        mPositionInsideItem = builder.mPositionInsideItem;
        mMarginsProvider = builder.mMarginsProvider;
    }

    private void setSizeProvider(Builder builder) {
        mSizeProvider = builder.mSizeProvider;
        if (mSizeProvider == null) {
            mSizeProvider = new SizeProvider() {
                @Override
                public int dividerSize(int position, RecyclerView parent) {
                    return DEFAULT_SIZE;
                }
            };
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        RecyclerView.Adapter adapter = parent.getAdapter();
        if (adapter == null) {
            return;
        }

        Resources resources = parent.getContext().getResources();

        int itemCount = adapter.getItemCount();
        int lastDividerOffset = getLastDividerOffset(parent);
        int validChildCount = parent.getChildCount();
        int lastChildPosition = -1;
        for (int i = 0; i < validChildCount; i++) {
            View child = parent.getChildAt(i);
            int childPosition = parent.getChildAdapterPosition(child);

            if (childPosition < lastChildPosition) {
                // Avoid remaining divider when animation starts
                continue;
            }
            lastChildPosition = childPosition;

            if (!mShowLastDivider && childPosition >= itemCount - lastDividerOffset) {
                // Don't draw divider for last line if mShowLastDivider = false
                continue;
            }

            if (wasDividerAlreadyDrawn(childPosition, parent)) {
                // No need to draw divider again as it was drawn already by previous column
                continue;
            }

            int groupIndex = getGroupIndex(childPosition, parent);
            if (mVisibilityProvider.shouldHideDivider(groupIndex, parent)) {
                continue;
            }

            Rect bounds = getDividerBound(groupIndex, parent, child);

            if (mMarginsProvider != null) {
                DecorationPart leftMarginPart = mMarginsProvider.dividerLeftMargin(groupIndex, parent);
                if (leftMarginPart != null) {
                    mPaint.setColor(resources.getColor(leftMarginPart.getColorId()));
                    mPaint.setStrokeWidth(mSizeProvider.dividerSize(groupIndex, parent));
                    int partWidth = resources.getDimensionPixelOffset(leftMarginPart.getWidthId());
                    c.drawLine(bounds.left, bounds.top, bounds.left + partWidth, bounds.bottom, mPaint);
                    bounds.left += partWidth;
                }

                DecorationPart rightMarginPart = mMarginsProvider.dividerRightMargin(groupIndex, parent);
                if (rightMarginPart != null) {
                    mPaint.setColor(resources.getColor(rightMarginPart.getColorId()));
                    mPaint.setStrokeWidth(mSizeProvider.dividerSize(groupIndex, parent));
                    int partWidth = resources.getDimensionPixelOffset(rightMarginPart.getWidthId());
                    c.drawLine(bounds.right - partWidth, bounds.top, bounds.right, bounds.bottom, mPaint);
                    bounds.right -= partWidth;
                }

            }
            mPaint.setColor(mColorProvider.dividerColor(groupIndex, parent));
            mPaint.setStrokeWidth(mSizeProvider.dividerSize(groupIndex, parent));
            c.drawLine(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);
        }
    }

    protected Rect getDividerBound(int position, RecyclerView parent, View child) {
        Rect bounds = new Rect(0, 0, 0, 0);
        int transitionX = (int) ViewCompat.getTranslationX(child);
        int transitionY = (int) ViewCompat.getTranslationY(child);
        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        bounds.left = parent.getPaddingLeft() + transitionX;
        bounds.right = parent.getWidth() - parent.getPaddingRight() + transitionX;

        int dividerSize = getDividerSize(position, parent);

        // set center point of divider
        if (mPositionInsideItem) {
            bounds.top = child.getBottom() + params.topMargin - dividerSize / 2 + transitionY;
        } else {
            bounds.top = child.getBottom() + params.topMargin + dividerSize / 2 + transitionY;
        }
        bounds.bottom = bounds.top;

        return bounds;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        int itemCount = parent.getAdapter().getItemCount();
        int lastDividerOffset = getLastDividerOffset(parent);
        if (!mShowLastDivider && position >= itemCount - lastDividerOffset) {
            // Don't set item offset for last line if mShowLastDivider = false
            return;
        }
        setItemOffsets(outRect, position, parent);
    }

    protected void setItemOffsets(Rect outRect, int position, RecyclerView parent) {
        if (mPositionInsideItem) {
            outRect.set(0, 0, 0, 0);
        } else {
            outRect.set(0, 0, 0, getDividerSize(position, parent));
        }
    }

    private int getDividerSize(int position, RecyclerView parent) {
        if (mSizeProvider != null) {
            return mSizeProvider.dividerSize(position, parent);
        }
        throw new RuntimeException("failed to get size");
    }

    /**
     * In the case mShowLastDivider = false,
     * Returns offset for how many views we don't have to draw a divider for,
     * for LinearLayoutManager it is as simple as not drawing the last child divider,
     * but for a GridLayoutManager it needs to take the span count for the last items into account
     * until we use the span count configured for the grid.
     *
     * @param parent RecyclerView
     * @return offset for how many views we don't have to draw a divider or 1 if its a
     * LinearLayoutManager
     */
    private int getLastDividerOffset(RecyclerView parent) {
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
            GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();
            int spanCount = layoutManager.getSpanCount();
            int itemCount = parent.getAdapter().getItemCount();
            for (int i = itemCount - 1; i >= 0; i--) {
                if (spanSizeLookup.getSpanIndex(i, spanCount) == 0) {
                    return itemCount - i;
                }
            }
        }
        return 1;
    }

    /**
     * Determines whether divider was already drawn for the row the item is in,
     * effectively only makes sense for a grid
     *
     * @param position current view position to draw divider
     * @param parent   RecyclerView
     * @return true if the divider can be skipped as it is in the same row as the previous one.
     */
    private boolean wasDividerAlreadyDrawn(int position, RecyclerView parent) {
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
            GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();
            int spanCount = layoutManager.getSpanCount();
            return spanSizeLookup.getSpanIndex(position, spanCount) > 0;
        }

        return false;
    }

    /**
     * Returns a group index for GridLayoutManager.
     * for LinearLayoutManager, always returns position.
     *
     * @param position current view position to draw divider
     * @param parent   RecyclerView
     * @return group index of items
     */
    private int getGroupIndex(int position, RecyclerView parent) {
        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
            GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();
            int spanCount = layoutManager.getSpanCount();
            return spanSizeLookup.getSpanGroupIndex(position, spanCount);
        }

        return position;
    }

    /**
     * Interface for controlling divider visibility
     */
    public interface VisibilityProvider {

        /**
         * Returns true if divider should be hidden.
         *
         * @param position Divider position (or group index for GridLayoutManager)
         * @param parent   RecyclerView
         * @return True if the divider at position should be hidden
         */
        boolean shouldHideDivider(int position, RecyclerView parent);
    }

    /**
     * Interface for controlling divider color
     */
    public interface ColorProvider {

        /**
         * Returns {@link android.graphics.Color} value of divider
         *
         * @param position Divider position (or group index for GridLayoutManager)
         * @param parent   RecyclerView
         * @return Color value
         */
        int dividerColor(int position, RecyclerView parent);
    }

    /**
     * Interface for controlling divider size
     */
    public interface SizeProvider {

        /**
         * Returns size value of divider.
         * Height for horizontal divider, width for vertical divider
         *
         * @param position Divider position (or group index for GridLayoutManager)
         * @param parent   RecyclerView
         * @return Size of divider
         */
        int dividerSize(int position, RecyclerView parent);
    }

    /**
     * Interface for controlling divider margin
     */
    public interface MarginsProvider {

        /**
         * Returns left margin of divider.
         *
         * @param position Divider position (or group index for GridLayoutManager)
         * @param parent   RecyclerView
         * @return left margin
         */
        DecorationPart dividerLeftMargin(int position, RecyclerView parent);

        /**
         * Returns right margin of divider.
         *
         * @param position Divider position (or group index for GridLayoutManager)
         * @param parent   RecyclerView
         * @return right margin
         */
        DecorationPart dividerRightMargin(int position, RecyclerView parent);
    }

    public static class DecorationPart {
        @ColorRes
        private int colorId;

        @DimenRes
        private int widthId;

        public DecorationPart() {
        }

        public DecorationPart(@ColorRes int colorId, @DimenRes int widthId) {
            this.colorId = colorId;
            this.widthId = widthId;
        }

        public int getColorId() {
            return colorId;
        }

        public void setColorId(int colorId) {
            this.colorId = colorId;
        }

        public int getWidthId() {
            return widthId;
        }

        public void setWidthId(int widthId) {
            this.widthId = widthId;
        }
    }


    public static class Builder {

        private Context mContext;
        protected Resources mResources;
        private ColorProvider mColorProvider;
        private SizeProvider mSizeProvider;
        private MarginsProvider mMarginsProvider;
        private boolean mShowLastDivider = false;
        private boolean mPositionInsideItem = false;

        private VisibilityProvider mVisibilityProvider = new VisibilityProvider() {
            @Override
            public boolean shouldHideDivider(int position, RecyclerView parent) {
                return false;
            }
        };

        public Builder(Context context) {
            mContext = context;
            mResources = context.getResources();
        }

        public Builder color(final int color) {
            return colorProvider(new ColorProvider() {
                @Override
                public int dividerColor(int position, RecyclerView parent) {
                    return color;
                }
            });
        }

        public Builder colorResId(@ColorRes int colorId) {
            return color(ContextCompat.getColor(mContext, colorId));
        }

        public Builder colorProvider(ColorProvider provider) {
            mColorProvider = provider;
            return this;
        }

        public Builder size(final int size) {
            return sizeProvider(new SizeProvider() {
                @Override
                public int dividerSize(int position, RecyclerView parent) {
                    return size;
                }
            });
        }

        public Builder sizeResId(@DimenRes int sizeId) {
            return size(mResources.getDimensionPixelSize(sizeId));
        }

        public Builder sizeProvider(SizeProvider provider) {
            mSizeProvider = provider;
            return this;
        }

        public Builder marginsProvider(MarginsProvider provider) {
            mMarginsProvider = provider;
            return this;
        }

        public Builder visibilityProvider(VisibilityProvider provider) {
            mVisibilityProvider = provider;
            return this;
        }

        public Builder showLastDivider() {
            mShowLastDivider = true;
            return this;
        }

        public Builder positionInsideItem(boolean positionInsideItem) {
            mPositionInsideItem = positionInsideItem;
            return this;
        }

        public HorizontalDividerItemDecoration build() {
            checkBuilderParams();
            return new HorizontalDividerItemDecoration(this);
        }

        private void checkBuilderParams() {
            if (mColorProvider == null) {
                throw new IllegalArgumentException("mColorProvider == null");
            }
        }
    }
}
