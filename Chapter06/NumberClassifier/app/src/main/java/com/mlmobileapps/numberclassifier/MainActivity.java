

package com.mlmobileapps.numberclassifier;

import android.app.Activity;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;


import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "MainActivity";

    // Chart Initials
    BarChart chart;
    ArrayList<BarEntry> BARENTRY;
    BarDataSet Bardataset;
    BarData BARDATA;


    private FreeHandView paintView;
    private Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the model and labels.
        try {
            classifier = new DigitClassifierModel(this);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize an image classifier.", e);
        }
        //startBackgroundThread();

        setContentView(R.layout.activity_main);
        paintView = (FreeHandView) findViewById(R.id.paintView);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);


        BarChart barChart = (BarChart) findViewById(R.id.barChart);
        barChart.animateY(3000);
        barChart.getXAxis().setEnabled(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0.0f); // start at zero
        barChart.getAxisLeft().setAxisMaximum(1.0f); // the axis maximum is 100
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);

        // the labels that should be drawn on the XAxis
        final String[] barLabels = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

        IAxisValueFormatter formatter = new IAxisValueFormatter() {

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return barLabels[(int) value];
            }
        };

        barChart.getXAxis().setGranularity(0f); // minimum axis-step (interval) is 1
        barChart.getXAxis().setValueFormatter(formatter);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setTextSize(5f);

        BARENTRY = new ArrayList<>();
        initializeBARENTRY();

        Bardataset = new BarDataSet(BARENTRY, "project");

        BARDATA = new BarData(Bardataset);
        barChart.setData(BARDATA);


        paintView.init(metrics, classifier, barChart);


        Button resetButton = (Button) findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                paintView.clear();
            }
        });

    }

    public void initializeBARENTRY() {
        for (int j = 0; j < 10; ++j) {
            BARENTRY.add(new BarEntry(j, 0.1f));
        }
    }

}