package andrewjsauer.ruler;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class RulerView extends View {

    /**
     * Android Documentation:
     *
     *  https://developer.android.com/intl/ru/training/custom-views/index.html
     *  https://developer.android.com/intl/ru/reference/android/content/res/TypedArray.html
     *  https://developer.android.com/intl/ru/reference/android/view/View.html#onMeasure(int, int)
     *
     *  https://developer.android.com/intl/ru/training/custom-views/create-view.html
     *  https://developer.android.com/intl/ru/reference/android/graphics/Canvas.html
     *  https://developer.android.com/intl/ru/training/custom-views/making-interactive.html
     *
     *  https://developer.android.com/intl/ru/training/gestures/detector.html
     *  https://developer.android.com/intl/ru/training/graphics/opengl/touch.html
     *  https://developer.android.com/intl/ru/reference/android/graphics/PointF.html
     *
     *  https://developer.android.com/intl/ru/reference/android/util/DisplayMetrics.html
     */

    private Paint mRulerLinePaint, mMeasureLinePaint, mMeasureTextPaint, mBackgroundPaint;

    private int mBackgroundColor;

    private float mRulerLineStrokeWidth;
    private float mRulerLineTextSize;
    private int mRulerLineColor;

    private float mMeasureTextSize;
    private int mMeasureTextColor;

    private float mMeasureLineStrokeWidth;
    private int mMeasureLineColor;

    private boolean mIsMeasureLineMoving;
    private PointF mStartMeasureLinePoint;

    private float mPxPerInchY;


    public RulerView(Context context) {
        this(context, null);
    }

    public RulerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.RulerView,
                0, 0);
        try {

            mRulerLineStrokeWidth = typedArray
                    .getDimension(R.styleable.RulerView_rulerLineWidth, 8);
            mRulerLineTextSize = typedArray
                    .getDimension(R.styleable.RulerView_rulerLineTextSize, 40);
            mRulerLineColor = typedArray.getColor(R.styleable.RulerView_rulerLineColor, 0xFF03070A);

            mMeasureLineStrokeWidth = typedArray
                    .getDimension(R.styleable.RulerView_measureLineStrokeWidth, 8);
            mMeasureLineColor = typedArray.getColor(R.styleable.RulerView_measureLineColor,
                    ContextCompat.getColor(context, R.color.colorPrimary));

            mMeasureTextSize = typedArray
                    .getDimensionPixelSize(R.styleable.RulerView_measureTextSize, 80);
            mMeasureTextColor = typedArray
                    .getColor(R.styleable.RulerView_measureTextColor, 0xFF03070A);

            mBackgroundColor = typedArray.getColor(R.styleable.RulerView_backgroundColor,
                    ContextCompat.getColor(context, R.color.colorAccent));

        } finally {
            typedArray.recycle();
        }

        initializeView();
    }

    private void initializeView() {

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mPxPerInchY = (int) metrics.ydpi;

        mRulerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRulerLinePaint.setStrokeWidth(mRulerLineStrokeWidth);
        mRulerLinePaint.setTextSize(mRulerLineTextSize);
        mRulerLinePaint.setColor(mRulerLineColor);

        mMeasureLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMeasureLinePaint.setColor(mMeasureLineColor);
        mMeasureLinePaint.setStrokeWidth(mMeasureLineStrokeWidth);
        mMeasureLinePaint.setStyle(Paint.Style.STROKE);

        mMeasureTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMeasureTextPaint.setTextSize(mMeasureTextSize);
        mMeasureTextPaint.setColor(mMeasureTextColor);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(mBackgroundColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Drawing background color
        canvas.drawPaint(mBackgroundPaint);

        // Getting dimensions of our screen
        int heightPx = getHeight();
        int paddingTopPx = getPaddingTop();
        int widthPx = getWidth();

        // Setting size of our ruler lines
        int inchLineSize = 90;
        int halfInchLineSize = 45;
        int quarterInchSize = 27;

        // Drawing lines in inches, half inches and quarter inches
        for (int i = 0; ;i++) {

            float startYInch = (mPxPerInchY + paddingTopPx) * i;
            float startXInch = 0;

            float startYHalfInch = startYInch / 2;
            float startXHalfInch = 0;

            float startYQuarterInch = startYHalfInch / 2;
            float startXQuarterInch = 0;

            if (startYQuarterInch > heightPx) {
                break;
            }

            canvas.drawLine(startXInch, startYInch, inchLineSize, startYInch, mRulerLinePaint);
            canvas.drawLine(startXHalfInch, startYHalfInch, halfInchLineSize, startYHalfInch,
                    mRulerLinePaint);
            canvas.drawLine(startXQuarterInch, startYQuarterInch, quarterInchSize,
                    startYQuarterInch, mRulerLinePaint);

            // Setting the ruler inches text
            String textInch = i + "";
            canvas.save();
            canvas.translate(startXInch + inchLineSize + mRulerLinePaint.measureText(textInch),
                    startYInch + mRulerLinePaint.measureText(textInch) / 2);
            canvas.drawText(textInch, 0, 0, mRulerLinePaint);
            canvas.restore();
        }

        // Drawing the measuring line
        if (mIsMeasureLineMoving) {
            canvas.drawLine(0, mStartMeasureLinePoint.y, widthPx,
                    mStartMeasureLinePoint.y, mMeasureLinePaint);
        }

        // Setting placeholder text
        String measureText = "--- ---- inches";

        // Setting text in inches from measuring line
        if (mIsMeasureLineMoving && mStartMeasureLinePoint.y > 0) {
            float distance = Math.abs(mStartMeasureLinePoint.y);
            String distanceFormatted = String.format("%.3f", distance / mPxPerInchY);
            measureText = distanceFormatted + " inches";
        }

        int measureTextWidth = widthPx - 20;
        int measureTextHeight = paddingTopPx + 20;

        canvas.drawText(measureText, measureTextWidth - mMeasureTextPaint.measureText(measureText),
                measureTextHeight + mMeasureTextSize, mMeasureTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex = event.getActionIndex();

        // Setting measuring line according to respected onTouchEvents
        switch(action) {
            case (MotionEvent.ACTION_DOWN): {
                mStartMeasureLinePoint = new PointF(event.getX(pointerIndex),
                        event.getY(pointerIndex));
                mIsMeasureLineMoving = true;
                break;
            }
            case (MotionEvent.ACTION_MOVE) : {
                if (mIsMeasureLineMoving) {
                    mStartMeasureLinePoint.y = event.getY();
                }
                break;
            }
            case (MotionEvent.ACTION_UP) :
            case (MotionEvent.ACTION_CANCEL) : {
                if (mIsMeasureLineMoving) {
                    mStartMeasureLinePoint.y = event.getY();
                    mIsMeasureLineMoving = false;
                }
                break;
            }
            default :
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        // Setting layout constraints
        int desiredWidth = 200;
        int desiredHeight = 200;

        int minWidth = getPaddingLeft() + getPaddingRight() + desiredWidth;
        int widthSize = Math.max(minWidth, MeasureSpec.getSize(widthMeasureSpec));

        int minHeight = getPaddingBottom() + getPaddingTop() + desiredHeight;
        int heightSize = Math.max(minHeight, MeasureSpec.getSize(heightMeasureSpec));

        setMeasuredDimension(widthSize, heightSize);
    }
}
