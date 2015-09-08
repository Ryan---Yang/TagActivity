package com.example.chenhaoych.tagactivity.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.Hashtable;


public class AutoWrapLineLayout extends RelativeLayout {
    private static final String TAG = "AutoWrapLineLayout";

    // Disable customized paddings
	/*
	private int mPaddingLeft;		// layout padding
	private int mPaddingRight;
	private int mPaddingTop;
	private int mPaddingBottom;
	*/

    private int mItemSpacing = (int)(6 * getResources().getDisplayMetrics().density);	// 子控件之间水平间距
    private int mLineSpacing = (int)(8 * getResources().getDisplayMetrics().density);	// 子控件行间距
    private int mLineGravity = Gravity.TOP;

    private int mLines;

    private boolean mSimplifiedMode = false;	// 是否以简化模式展示，true为简化模式
    private int mSimplifiedModeLines;			// 在简化模式下展示行数
    // 注：仅当mSimplifiedMode为true时才有意义

    private View mLayoutEndView = null;			// 切换简化模式的控件，用户自定义

    private Hashtable<View, Position> mPosMap	// 存储子控件布局参数的数据结构
            = new Hashtable<View, Position>();

    private static final int NEW_LINE_ID = 99990;	// 换行符id，预设以便识别
    private static final int LAYOUT_END_RES_ID = 99999;	// mLayoutEndView的资源id，预设以便识别
    private static final int DEFAULT_SIMPLIFIED_MODE_LINES = 2;	// 缺省的简化模式展示行数

    public AutoWrapLineLayout(Context context) {
        super(context);
        init();
    }

    public AutoWrapLineLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoWrapLineLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // Disable customized paddings
		/*
		mPaddingLeft = getPaddingLeft();
		mPaddingRight = getPaddingRight();
		mPaddingTop = getPaddingTop();
		mPaddingBottom = getPaddingBottom();
		*/
    }

    // Disable customized paddings
	/*
	@Override
	public void setPadding(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
		mPaddingLeft = paddingLeft;
		mPaddingTop = paddingTop;
		mPaddingRight = paddingRight;
		mPaddingBottom = paddingBottom;
	}
	*/

    public void setItemSpacing(int itemSpacing) {
        mItemSpacing = itemSpacing;
    }

    public void setLineSpacing(int lineSpacing) {
        mLineSpacing = lineSpacing;
    }

    public void setLineGravity(int gravity) {
        mLineGravity = gravity;
    }

    public int getLines() {
        return mLines + 1;
    }

    public int getSimplifiedModeLines() {
        return mSimplifiedModeLines;
    }

    public boolean getSimplifiedMode() {
        return mSimplifiedMode;
    }

    public void setSimplifiedMode(boolean simplifiedMode) {
        mSimplifiedMode = simplifiedMode;
        if (mSimplifiedMode)
            mSimplifiedModeLines = DEFAULT_SIMPLIFIED_MODE_LINES;
    }

    public void setSimplifiedMode(boolean simplifiedMode, int simplifiedModeLines) {
        mSimplifiedMode = simplifiedMode;
        if (mSimplifiedMode)
            mSimplifiedModeLines = simplifiedModeLines;
    }

    public void addView(View child) {
        super.addView(child);

        mPosMap.put(child, new Position());
    }

    public void addView(View child, ViewGroup.LayoutParams params) {
        super.addView(child, params);

        mPosMap.put(child, new Position());
    }

    public void addNewLineView(View child) {
        if (null != child) {
            child.setTag(NEW_LINE_ID);
            addView(child);
        }
    }

    public void addNewLineView(View child, ViewGroup.LayoutParams params) {
        if (null != child) {
            child.setTag(NEW_LINE_ID);
            addView(child, params);
        }
    }

    public void setNewLineView(View child) {
        if (null != child) {
            child.setTag(NEW_LINE_ID);
        }
    }

    public void addLayoutEndView(View child) {
        if (null != child && getChildCount() > 0) {
            child.setId(LAYOUT_END_RES_ID);
            mLayoutEndView = child;
            addView(mLayoutEndView);
        }
    }

    public void addLayoutEndView(View child, ViewGroup.LayoutParams params) {
        if (null != child && getChildCount() > 0) {
            child.setId(LAYOUT_END_RES_ID);
            mLayoutEndView = child;
            addView(mLayoutEndView, params);
        }
    }

    public boolean isChildVisible(View child) {
        if (indexOfChild(child) < 0)
            return false;

        if (mSimplifiedMode && mPosMap.get(child).line >= mSimplifiedModeLines)
            return false;

        return true;
    }

    public void destroy() {
        if (null != mPosMap) {
            mPosMap.clear();
            mPosMap = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        View child;
        Position pos;
        for (int i = 0; i < count; i++) {
            child = getChildAt(i);

            pos = mPosMap.get(child);
            if (null != pos) {
                // 在简化模式下，只展示最多mSimplifiedModeLines行的子控件
                if (mSimplifiedMode && pos.line >= mSimplifiedModeLines) {
                    child.layout(0, 0, 0, 0);
                } else {
                    child.layout(pos.left, pos.top, pos.right, pos.bottom);
                }
            } else {
              //  TaoLog.Logi(TAG, "onLayout() error");
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingRight = getPaddingRight();

        int layoutWidth = MeasureSpec.getSize(widthMeasureSpec);
        int left = 0;
        int right = 0;
		/*
		int top = mPaddingTop;
		*/
        int top = paddingTop;
        int bottom = top;
        int viewBottom = bottom;
        int simplifiedViewBottom = -1;

        int i, j;
        // 在简化模式下，如果有mLayoutEndView，则mLayoutEndView的位置需提前计算，所以在普通的
        // 依次measure流程里不会包括mLayoutEndView
        int count = getChildCount()
                - ((mSimplifiedMode && null != mLayoutEndView) ? 1 : 0);
        View view;
        boolean isNewLineView;
        Position position;
        int viewWidth, viewHeight;
        for (i = 0, j = i, mLines = 0; i < count; i++) {
            view = getChildAt(i);
            try {
                isNewLineView = (null != view.getTag()
                        && NEW_LINE_ID == (Integer)view.getTag());
            } catch (Exception e) {
                isNewLineView = false;
            }
            position = mPosMap.get(view);
            viewWidth = view.getMeasuredWidth();
            viewHeight = view.getMeasuredHeight();

            // 先计算子控件的left、right坐标
            left = getPosition(i - j, i);
            right = left + viewWidth;

            // 简化模式比较复杂，在简化模式下，如果完全展示，那和非简化模式没什么区别，但如果不是完全展示，
            // 则在最后一行（也即第mSimplifiedModeLines - 1行），判断一个子控件是否能显示下时，
            // 需要考虑mLayoutEndView的宽度
            if (mSimplifiedMode && mSimplifiedModeLines - 1 == mLines) {
                // 子控件显示不下，另起新行，注意这里区分是否存在mLayoutEndView的情况
                if (null != mLayoutEndView) {
                    // 如果有mLayoutEndView，必须保证mLayoutEndView能显示在最后一行
					/*
					if (right + mItemSpacing + mLayoutEndView.getMeasuredWidth() > layoutWidth - mPaddingRight
					*/
                    if (right + mItemSpacing + mLayoutEndView.getMeasuredWidth() > layoutWidth - paddingRight
                            || isNewLineView) {
                        Position layoutEndViewPosition = mPosMap.get(mLayoutEndView);
						/*
						layoutEndViewPosition.right = layoutWidth - mPaddingRight;
						*/
                        layoutEndViewPosition.right = layoutWidth - paddingRight;
                        layoutEndViewPosition.left = layoutEndViewPosition.right - mLayoutEndView.getMeasuredWidth();
                        layoutEndViewPosition.top = top;
                        layoutEndViewPosition.bottom = layoutEndViewPosition.top + mLayoutEndView.getMeasuredHeight();
                        layoutEndViewPosition.line = mLines;
                        layoutEndViewPosition.lineStart = j;

                        // 设定简化显示区域bottom坐标
                        simplifiedViewBottom = viewBottom > layoutEndViewPosition.bottom ? viewBottom : layoutEndViewPosition.bottom;

                        mLines++;
                        j = i;
                        left = getPosition(i - j, i);
                        right = left + viewWidth;
                        top = viewBottom + mLineSpacing;
                    }
                } else {
					/*
					if (right > layoutWidth - mPaddingRight || isNewLineView) {
					*/
                    if (right > layoutWidth - paddingRight || isNewLineView) {
                        // 设定简化显示区域bottom坐标
                        simplifiedViewBottom = viewBottom;

                        mLines++;
                        j = i;
                        left = getPosition(i - j, i);
                        right = left + viewWidth;
                        top = viewBottom + mLineSpacing;
                    }
                }
            } else {
                // 子控件显示不下，另起新行
				/*
				if (right > layoutWidth - mPaddingRight || isNewLineView) {
				*/
                if (right > layoutWidth - paddingRight || isNewLineView) {
                    mLines++;
                    j = i;
                    left = getPosition(i - j, i);
                    right = left + viewWidth;
                    top = viewBottom + mLineSpacing;
                }
            }

            // 为简化处理，设定一行所有子控件的top坐标相同
            bottom = top + viewHeight;
            // 取一行子控件中bottom坐标最大者为该行的bottom坐标，计算排版用
            if (bottom > viewBottom)
                viewBottom = bottom;

            // 设定子控件的Position
            position.left = left;
            position.right = right;
            if (Gravity.TOP == mLineGravity) {
                position.top = top;
                position.bottom = bottom;
            } else if (Gravity.CENTER == mLineGravity) {
                position.top = top + (viewBottom - top - viewHeight) / 2;
                position.bottom = position.top + viewHeight;
            } else if (Gravity.BOTTOM == mLineGravity) {
                position.bottom = viewBottom;
                position.top = position.bottom - viewHeight;
            }
            position.line = mLines;
            position.lineStart = j;

            // 在非简化模式下，如果有mLayoutEndView，需保证mLayoutEndView的位置始终紧贴layout的右下角
            if (view == mLayoutEndView) {
				/*
				position.right = layoutWidth - mPaddingRight;
				*/
                position.right = layoutWidth - paddingRight;
                position.left = position.right - viewWidth;
            }
        }

        if (simplifiedViewBottom > 0)
            viewBottom = simplifiedViewBottom;

		/*
		setMeasuredDimension(layoutWidth, viewBottom + mPaddingBottom);
		*/
        setMeasuredDimension(layoutWidth, viewBottom + paddingBottom);
    }

    private int getPosition(int indexInRow, int indexInAll) {
        if (indexInRow > 0) {
            return getPosition(indexInRow - 1, indexInAll - 1)		// 同行前一子控件的left坐标
                    + getChildAt(indexInAll - 1).getMeasuredWidth()	// 同行前一子控件的宽度
                    + mItemSpacing;									// 子控件之间水平间距
        }
		/*
		return mPaddingLeft;	// 每列第一个子控件的left坐标
		*/
        return getPaddingLeft();	// 每列第一个子控件的left坐标
    }

    private class Position {
        int left, top, right, bottom;
        int line, lineStart;
    }
}
