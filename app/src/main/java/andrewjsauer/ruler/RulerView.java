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

    private Paint rulerLinePaint, measureLinePaint, measureTextPaint, backgroundPaint;

    private int backgroundColor;

    private float rulerLineWidth;
    private float rulerLineTextSize;
    private int rulerLineColor;

    private float measureTextSize;
    private int measureTextColor;

    private float measureLineStokeWidth;
    private int measureLineColor;

    private boolean isMeasureLineMoving;
    private PointF startMeasureLinePoint;

    private float ydpmm;



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
            rulerLineWidth = typedArray.getDimension(R.styleable.RulerView_rulerLineWidth, 8);
            rulerLineTextSize = typedArray.getDimension(R.styleable.RulerView_rulerLineTextSize, 40);
            rulerLineColor = typedArray.getColor(R.styleable.RulerView_rulerLineColor, 0xFF03070A);

            measureLineStokeWidth = typedArray.getDimension(R.styleable.RulerView_measureLineStrokeWidth, 8);
            measureLineColor = typedArray.getColor(R.styleable.RulerView_measureLineColor,
                    ContextCompat.getColor(context, R.color.colorPrimary));

            measureTextSize = typedArray.getDimensionPixelSize(R.styleable.RulerView_measureTextSize, 80);
            measureTextColor = typedArray.getColor(R.styleable.RulerView_measureTextColor, 0xFF03070A);

            backgroundColor = typedArray.getColor(R.styleable.RulerView_backgroundColor,
                    ContextCompat.getColor(context, R.color.colorAccent));

        } finally {
            typedArray.recycle();
        }

        initializeView();
    }

    private void initializeView() {

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        ydpmm = (int) metrics.ydpi;

        rulerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rulerLinePaint.setStrokeWidth(rulerLineWidth);
        rulerLinePaint.setTextSize(rulerLineTextSize);
        rulerLinePaint.setColor(rulerLineColor);

        measureLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        measureLinePaint.setColor(measureLineColor);
        measureLinePaint.setStrokeWidth(measureLineStokeWidth);
        measureLinePaint.setStyle(Paint.Style.STROKE);

        measureTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        measureTextPaint.setTextSize(measureTextSize);
        measureTextPaint.setColor(measureTextColor);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(backgroundColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Drawing background color
        canvas.drawPaint(backgroundPaint);

        int heightPx = getHeight();
        int paddingTopPx = getPaddingTop();
        int width = getWidth();

        int inchLineSize = 90;
        int halfInchLineSize = 45;
        int quarterInchSize = 27;

        // Drawing lines in inches, half inches and quarter inches
        for (int i = 0; ;i++) {

            float startYIn = (ydpmm + paddingTopPx) * i;
            float startXIn = 0;

            float startYHalfIn = startYIn / 2;
            float startXHalfIn = 0;

            float startYQuarterIn = startYHalfIn / 2;
            float startXQuarterIn = 0;

            if (startYQuarterIn > heightPx) {
                break;
            }

            canvas.drawLine(startXIn, startYIn, inchLineSize, startYIn, rulerLinePaint);
            canvas.drawLine(startXHalfIn, startYHalfIn, halfInchLineSize, startYHalfIn, rulerLinePaint);
            canvas.drawLine(startXQuarterIn, startYQuarterIn, quarterInchSize,
                    startYQuarterIn, rulerLinePaint);

            // Setting the inches text
            String textInch = i + "";
            canvas.save();
            canvas.translate(startXIn + inchLineSize + rulerLinePaint.measureText(textInch), startYIn + rulerLinePaint.measureText(textInch) / 2);
            canvas.drawText(textInch, 0, 0, rulerLinePaint);
            canvas.restore();
        }

        // Drawing the measuring line
        if (isMeasureLineMoving) {
            canvas.drawLine(0, startMeasureLinePoint.y, width, startMeasureLinePoint.y, measureLinePaint);
        }

        // Setting placeholder text
        String measureText = "-- --- inches";

        // Setting text in inches from measuring line
        if (isMeasureLineMoving && startMeasureLinePoint.y > 0) {
            float distance = Math.abs(startMeasureLinePoint.y);
            String distanceFormatted  = String.format("%.3f", distance / ydpmm);
            measureText = distanceFormatted + " inches";
        }
        canvas.drawText(measureText, width - measureTextPaint.measureText(measureText), paddingTopPx + measureTextSize, measureTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex = event.getActionIndex();

        // Setting measuring line according to respected onTouchEvents
        switch(action) {
            case (MotionEvent.ACTION_DOWN): {
                startMeasureLinePoint = new PointF(event.getX(pointerIndex), event.getY(pointerIndex));
                isMeasureLineMoving = true;
                break;
            }
            case (MotionEvent.ACTION_MOVE) : {
                if (isMeasureLineMoving) {
                    startMeasureLinePoint.y = event.getY();
                }
                break;
            }
            case (MotionEvent.ACTION_UP) :
            case (MotionEvent.ACTION_CANCEL) : {
                if (isMeasureLineMoving) {
                    startMeasureLinePoint.y = event.getY();
                    isMeasureLineMoving = false;
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
