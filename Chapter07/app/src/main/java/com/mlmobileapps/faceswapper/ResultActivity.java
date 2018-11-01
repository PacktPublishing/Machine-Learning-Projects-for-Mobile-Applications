package com.mlmobileapps.faceswapper;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;



@SuppressWarnings("UnusedParameters")
public class ResultActivity extends AppCompatActivity {
    /* For storing temporary imaging. */
    private Bitmap bitmap;

    /* For showing the result */
    private ImageView imageView;

    private Toast infoToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);


        // Get swapped bitmap
        bitmap = GlobalBitmap.img;
        imageView = (ImageView) findViewById(R.id.imageView);

        if (bitmap != null && imageView != null) {
            imageView.setImageBitmap(bitmap);
        }

    }


    @Override
    protected void onPause() {
        super.onPause();
        super.onStop();
    }


    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {

        super.onStop();
    }


}