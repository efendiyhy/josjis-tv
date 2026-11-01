package com.josjis.tv;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

public final class TvHorizontalScrollView extends HorizontalScrollView {
    public TvHorizontalScrollView(Context context) {
        super(context);
        init();
    }

    public TvHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TvHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSmoothScrollingEnabled(true);
        setFocusable(false);
        setFocusableInTouchMode(false);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        centerOn(focused != null ? focused : child);
    }

    private void centerOn(View target) {
        if (target == null) return;
        Rect rect = new Rect();
        target.getDrawingRect(rect);
        offsetDescendantRectToMyCoords(target, rect);
        int margin = Design.dp(getContext(), 32);
        int currentLeft = getScrollX();
        int currentRight = currentLeft + getWidth();
        int desired = currentLeft;

        if (rect.right + margin > currentRight) {
            desired = rect.right - getWidth() + margin;
        } else if (rect.left - margin < currentLeft) {
            desired = Math.max(0, rect.left - margin);
        }
        smoothScrollTo(desired, 0);
    }
}
