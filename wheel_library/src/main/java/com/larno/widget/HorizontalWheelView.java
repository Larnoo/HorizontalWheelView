package com.larno.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.OverScroller;

import java.util.List;

public class HorizontalWheelView extends View implements GestureDetector.OnGestureListener {
    public static final float DEFAULT_INTERVAL_FACTOR = 1.2f;

    private int mHighlightTextColor, mNormalTextColor;
    private Drawable mHighlightTextBackground;
    private Drawable mNormalTextBackground;
    private float mHighlightTextSize, mNormalTextSize;
    private float mTextPaddingLeftAndRight, mTextPaddingTopAndBottom;

    private List<String> mItems;
    private OnWheelItemSelectedListener mOnWheelItemSelectedListener;
    private float mIntervalFactor = DEFAULT_INTERVAL_FACTOR;
    private int mItemCount;
    private TextPaint mTextPaint;
    private int mCenterIndex;

    private int mViewScopeSize;
    private float mHalfWidth;
    private int mHeight;
    private float mIntervalDistance;
    // scroll control args ---- start
    private OverScroller mScroller;
    private RectF mContentRectF = new RectF();
    private boolean mFling = false;
    private GestureDetectorCompat mGestureDetectorCompat;
    // scroll control args ---- end
    private Rect reuseRect = new Rect();


    public HorizontalWheelView(Context context) {
        this(context, null);
    }

    public HorizontalWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;

        mHighlightTextColor = 0xFFF74C39;
        mNormalTextColor = 0xFF666666;
        mHighlightTextSize = density * 22;
        mNormalTextSize = density * 18;
        mTextPaddingTopAndBottom = density * 2;
        mTextPaddingLeftAndRight = density * 3;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.HorizontalWheelView);
        mHighlightTextColor = a.getColor(R.styleable.HorizontalWheelView_highlightTextColor, mHighlightTextColor);
        mNormalTextColor = a.getColor(R.styleable.HorizontalWheelView_normalTextColor, mNormalTextColor);
        mHighlightTextSize = a.getDimension(R.styleable.HorizontalWheelView_highlightTextSize, mHighlightTextSize);
        mNormalTextSize = a.getDimension(R.styleable.HorizontalWheelView_normalTextSize, mNormalTextSize);
        mTextPaddingTopAndBottom = a.getDimension(R.styleable.HorizontalWheelView_textPaddingTopAndBottom, mTextPaddingTopAndBottom);
        mTextPaddingLeftAndRight = a.getDimension(R.styleable.HorizontalWheelView_textPaddingLeftAndRight, mTextPaddingLeftAndRight);
        mHighlightTextBackground = a.getDrawable(R.styleable.HorizontalWheelView_highlightTextBackground);
        mNormalTextBackground = a.getDrawable(R.styleable.HorizontalWheelView_normalTextBackground);
        a.recycle();

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(mHighlightTextColor);
        mTextPaint.setTextSize(mHighlightTextSize);
        calcIntervalDistance();

        mScroller = new OverScroller(getContext());
        mGestureDetectorCompat = new GestureDetectorCompat(getContext(), this);

        selectIndex(0);
    }

    /**
     * calculate interval distance between items
     */
    private void calcIntervalDistance() {
        String defaultText = "888888";
        float max = 0;
        if (mItems != null && mItems.size() > 0) {
            for (String i : mItems) {
                mTextPaint.getTextBounds(i, 0, i.length(), reuseRect);
                if (reuseRect.width() > max) {
                    max = reuseRect.width() + mTextPaddingLeftAndRight * 2;
                }
            }
        } else {
            mTextPaint.getTextBounds(defaultText, 0, defaultText.length(), reuseRect);
            max = reuseRect.width() + mTextPaddingLeftAndRight * 2;
        }
        mIntervalDistance = max * mIntervalFactor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int widthMeasureSpec) {
        int measureMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureSize = MeasureSpec.getSize(widthMeasureSpec);
        int result = getSuggestedMinimumWidth();
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = measureSize;
                break;
            default:
                break;
        }
        return result;
    }

    private int measureHeight(int heightMeasure) {
        int measureMode = MeasureSpec.getMode(heightMeasure);
        int measureSize = MeasureSpec.getSize(heightMeasure);
        int result = (int) (mTextPaddingTopAndBottom * 2 + mHighlightTextSize);
        switch (measureMode) {
            case MeasureSpec.EXACTLY:
                result = Math.max(result, measureSize);
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(result, measureSize);
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            mHeight = h;
            mHalfWidth = w / 2.f;
            mContentRectF.set(0, 0, (mItemCount - 1) * mIntervalDistance, h);
            mViewScopeSize = (int) Math.ceil(mHalfWidth / mIntervalDistance);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int start = mCenterIndex - mViewScopeSize;
        int end = mCenterIndex + mViewScopeSize + 1;
        start = Math.max(start, -mViewScopeSize * 2);
        end = Math.min(end, mItemCount + mViewScopeSize * 2);
        // extends both ends
        if (mCenterIndex == mItemCount - 1) {
            end += mViewScopeSize;
        } else if (mCenterIndex == 0) {
            start -= mViewScopeSize;
        }

        float x = start * mIntervalDistance;
        for (int i = start; i < end; i++) {
            if (mItemCount > 0 && i >= 0 && i < mItemCount) {
                String item = mItems.get(i);
                if (mCenterIndex == i) {
                    mTextPaint.setColor(mHighlightTextColor);
                    mTextPaint.setTextSize(mHighlightTextSize);
                    if (mHighlightTextBackground != null) {
                        mTextPaint.getTextBounds(item, 0, item.length(), reuseRect);
                        int halfTextWidth = reuseRect.width() / 2;
                        int left = (int) (x - halfTextWidth - mTextPaddingLeftAndRight);
                        int top = (int) (mHeight - reuseRect.height() - 2 * mTextPaddingTopAndBottom);
                        int right = (int) (x + halfTextWidth + mTextPaddingLeftAndRight);
                        int bottom = mHeight;
                        mHighlightTextBackground.setBounds(left, top, right, bottom);
                        mHighlightTextBackground.draw(canvas);
                    }
                    canvas.drawText(item, 0, item.length(), x, mHeight - mTextPaddingTopAndBottom, mTextPaint);
                } else {
                    mTextPaint.setColor(mNormalTextColor);
                    mTextPaint.setTextSize(mNormalTextSize);
                    if (mNormalTextBackground != null) {
                        mTextPaint.getTextBounds(item, 0, item.length(), reuseRect);
                        int halfTextWidth = reuseRect.width() / 2;
                        int left = (int) (x - halfTextWidth - mTextPaddingLeftAndRight);
                        int top = (int) (mHeight - reuseRect.height() - 2 * mTextPaddingTopAndBottom);
                        int right = (int) (x + halfTextWidth + mTextPaddingLeftAndRight);
                        int bottom = mHeight;
                        mNormalTextBackground.setBounds(left, top, right, bottom);
                        mNormalTextBackground.draw(canvas);
                    }
                    canvas.drawText(item, 0, item.length(), x, mHeight - mTextPaddingTopAndBottom, mTextPaint);
                }
            }
            x += mIntervalDistance;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mItems == null || mItems.size() == 0) {
            return false;
        }
        boolean ret = mGestureDetectorCompat.onTouchEvent(event);
        if (!mFling && MotionEvent.ACTION_UP == event.getAction()) {
            if (getScrollX() < -mHalfWidth) {
                mScroller.startScroll(getScrollX(), 0, (int) -mHalfWidth - getScrollX(), 0);
                invalidate();
                ret = true;
            } else if (getScrollX() > mContentRectF.width() - mHalfWidth) {
                mScroller.startScroll(getScrollX(), 0, (int) (mContentRectF.width() - mHalfWidth) - getScrollX(), 0);
                invalidate();
                ret = true;
            } else {
                autoSettle();
                ret = true;
            }
        }
        return ret || super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            refreshCenter();
            invalidate();
        } else {
            if (mFling) {
                mFling = false;
                autoSettle();
            }
        }
    }

    private void autoSettle() {
        int sx = getScrollX();
        float dx = mCenterIndex * mIntervalDistance - sx - mHalfWidth;
        mScroller.startScroll(sx, 0, (int) dx, 0);
        invalidate();
    }

    private void refreshCenter(int offsetX) {
        int offset = (int) (offsetX + mHalfWidth);
        mCenterIndex = Math.round(offset / mIntervalDistance);
        if (mCenterIndex < 0) {
            mCenterIndex = 0;
        } else if (mCenterIndex > mItemCount - 1) {
            mCenterIndex = mItemCount - 1;
        }
        if (null != mOnWheelItemSelectedListener) {
            mOnWheelItemSelectedListener.onWheelItemSelected(mCenterIndex);
        }
    }

    private void refreshCenter() {
        refreshCenter(getScrollX());
    }

    public void selectIndex(int index) {
        mCenterIndex = index;
        post(new Runnable() {
            @Override
            public void run() {
                scrollTo((int) (mCenterIndex * mIntervalDistance - mHalfWidth), 0);
                invalidate();
                refreshCenter();
            }
        });
    }

    public void smoothSelectIndex(int index) {
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        int deltaIndex = index - mCenterIndex;
        mScroller.startScroll(getScrollX(), 0, (int) (deltaIndex * mIntervalDistance), 0);
        invalidate();
    }

    public List<String> getItems() {
        return mItems;
    }

    public void setItems(List<String> items) {
        mItems = items;
        mItemCount = null == mItems ? 0 : mItems.size();
        mCenterIndex = Math.min(mCenterIndex, mItemCount);
        calcIntervalDistance();
        invalidate();
    }

    public int getSelectedPosition() {
        return mCenterIndex;
    }

    public void setOnWheelItemSelectedListener(OnWheelItemSelectedListener onWheelItemSelectedListener) {
        mOnWheelItemSelectedListener = onWheelItemSelectedListener;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(false);
        }
        mFling = false;
        if (null != getParent()) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        playSoundEffect(SoundEffectConstants.CLICK);
        refreshCenter((int) (getScrollX() + e.getX() - mHalfWidth));
        autoSettle();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float dis = distanceX;
        float scrollX = getScrollX();
        if (scrollX < 2 * -mHalfWidth) {
            dis = 0;
        } else if (scrollX < -mHalfWidth) {
            dis = distanceX / 4.f;
        } else if (scrollX > mContentRectF.width()) {
            dis = 0;
        } else if (scrollX > mContentRectF.width() - mHalfWidth) {
            dis = distanceX / 4.f;
        }
        scrollBy((int) dis, 0);
        refreshCenter();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float scrollX = getScrollX();
        if (scrollX < -mHalfWidth || scrollX > mContentRectF.width() - mHalfWidth) {
            return false;
        } else {
            mFling = true;
            fling((int) -velocityX, 0);
            return true;
        }
    }

    public void fling(int velocityX, int velocityY) {
        mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY, (int) -mHalfWidth, (int) (mContentRectF.width() - mHalfWidth), 0, 0, (int) mHalfWidth, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.index = getSelectedPosition();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        selectIndex(ss.index);
        requestLayout();
    }

    static class SavedState extends BaseSavedState {
        int index;
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            index = (int) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(index);
        }

        @Override
        public String toString() {
            return "WheelView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " index=" + index + "}";
        }
    }

    public interface OnWheelItemSelectedListener {
        void onWheelItemSelected(int position);
    }
}
