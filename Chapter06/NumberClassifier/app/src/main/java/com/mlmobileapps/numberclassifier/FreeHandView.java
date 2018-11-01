package com.mlmobileapps.numberclassifier;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Random;

public class FreeHandView extends View {

    public static int BRUSH_SIZE = 30;
    public static final int DEFAULT_COLOR = Color.WHITE;
    //public static final int DEFAULT_COLOR = 255;
    public static final int DEFAULT_BG_COLOR = Color.BLACK;
    //public static final int DEFAULT_BG_COLOR = 0;
    public ProgressBar predictionBar;
    private static final float TOUCH_TOLERANCE = 1;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int currentColor;
    private int backgroundColor = DEFAULT_BG_COLOR;
    private int strokeWidth;
    private boolean emboss;
    private boolean blur;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    private BarChart barChart;
    final ArrayList<String> xAxisLabel = new ArrayList<>();

    private Classifier mClassifier;
    private FileOutputStream pngFile;

    public FreeHandView(Context context) {
        this(context, null);
    }

    public FreeHandView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        //mPaint.setAlpha(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);

        mEmboss = new EmbossMaskFilter(new float[] {1, 1, 1}, 0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);
    }

    public void init(DisplayMetrics metrics, Classifier classifier, BarChart barChart) {
        int height = 1000;
        int width = 1000;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;
        mClassifier = classifier;
        this.predictionBar = predictionBar;
        this.barChart = barChart;
        addValuesToBarEntryLabels();
    }

    public void normal() {
        emboss = false;
        blur = false;
    }

    public void emboss() {
        emboss = true;
        blur = false;
    }

    public void blur() {
        emboss = false;
        blur = true;
    }

    public void clear() {
        backgroundColor = DEFAULT_BG_COLOR;
        paths.clear();
        normal();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mCanvas.drawColor(backgroundColor);

        for (FingerPath fp : paths) {
            mPaint.setColor(fp.color);
            mPaint.setStrokeWidth(fp.strokeWidth);
            mPaint.setMaskFilter(null);

            if (fp.emboss)
                mPaint.setMaskFilter(mEmboss);
            else if (fp.blur)
                mPaint.setMaskFilter(mBlur);

            mCanvas.drawPath(fp.path, mPaint);
        }
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }

    public void setStrokeWidth(int brushThickness) {
        strokeWidth = brushThickness;
    }

    public void setClassifier(Classifier classifier) {
        mClassifier = classifier;
    }

    private void touchStart(float x, float y) {
        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, emboss, blur, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    public BarData updateBarEntry() {
        ArrayList<BarEntry> mBarEntry = new ArrayList<>();
        for (int j = 0; j < 10; ++j) {
            mBarEntry.add(new BarEntry(j, mClassifier.getProbability(j)));
        }
        BarDataSet mBarDataSet = new BarDataSet(mBarEntry, "Projects");
        mBarDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        BarData mBardData = new BarData(mBarDataSet);
        return mBardData;
    }

    public void addValuesToBarEntryLabels() {
        for (int j = 0; j < 10; ++j) {
            xAxisLabel.add(Integer.toString(j));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        BarData exampleData;

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN :
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE :
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP :
                touchUp();
                //toGrayscale(mBitmap);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(mBitmap, mClassifier.getImageSizeX(), mClassifier.getImageSizeY(), true);
                Random rng = new Random();

                try {
                    File mFile;
                    mFile = this.getContext().getExternalFilesDir(String.valueOf(rng.nextLong() + ".png"));
                    FileOutputStream pngFile = new FileOutputStream(mFile);
                }
                catch (Exception e){
                }
                //scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, pngFile);
                Float prediction = mClassifier.classifyFrame(scaledBitmap);
                exampleData = updateBarEntry();
                barChart.animateY(1000, Easing.EasingOption.EaseOutQuad);
                XAxis xAxis = barChart.getXAxis();
                xAxis.setValueFormatter(new IAxisValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        return xAxisLabel.get((int) value);
                    }
                });
                barChart.setData(exampleData);
                exampleData.notifyDataChanged(); // let the data know a dataSet changed
                barChart.notifyDataSetChanged(); // let the chart know it's data changed
                break;
        }

        return true;
    }
}