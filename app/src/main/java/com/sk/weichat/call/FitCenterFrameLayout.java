
package com.sk.weichat.call;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.call
 * @作者:王阳
 * @创建时间: 2015年11月17日 下午4:55:50
 * @描述: TODO
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class FitCenterFrameLayout extends ViewGroup {
    public FitCenterFrameLayout(Context context) {
        super(context);
    }

    public FitCenterFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED);

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();

        final int parentLeft = getPaddingLeft();
        final int parentTop = getPaddingTop();
        final int parentRight = r - l - getPaddingRight();
        final int parentBottom = b - t - getPaddingBottom();

        final int parentWidth = parentRight - parentLeft;
        final int parentHeight = parentBottom - parentTop;

        int unpaddedWidth, unpaddedHeight, parentUnpaddedWidth, parentUnpaddedHeight;
        int childPaddingLeft, childPaddingTop, childPaddingRight, childPaddingBottom;

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }


            childPaddingLeft = child.getPaddingLeft();
            childPaddingTop = child.getPaddingTop();
            childPaddingRight = child.getPaddingRight();
            childPaddingBottom = child.getPaddingBottom();

            unpaddedWidth = child.getMeasuredWidth() - childPaddingLeft - childPaddingRight;
            unpaddedHeight = child.getMeasuredHeight() - childPaddingTop - childPaddingBottom;

            parentUnpaddedWidth = parentWidth - childPaddingLeft - childPaddingRight;
            parentUnpaddedHeight = parentHeight - childPaddingTop - childPaddingBottom;

            if (parentUnpaddedWidth * unpaddedHeight > parentUnpaddedHeight * unpaddedWidth) {
                final int scaledChildWidth = unpaddedWidth * parentUnpaddedHeight
                        / unpaddedHeight + childPaddingLeft + childPaddingRight;
                child.layout(
                        parentLeft + (parentWidth - scaledChildWidth) / 2,
                        parentTop,
                        parentRight - (parentWidth - scaledChildWidth) / 2,
                        parentBottom);
            } else {
                final int scaledChildHeight = unpaddedHeight * parentUnpaddedWidth
                        / unpaddedWidth + childPaddingTop + childPaddingBottom;
                child.layout(
                        parentLeft,
                        parentTop + (parentHeight - scaledChildHeight) / 2,
                        parentRight,
                        parentTop + (parentHeight + scaledChildHeight) / 2);
            }
        }
    }
}
