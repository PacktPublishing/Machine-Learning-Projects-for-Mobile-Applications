package com.mlmobileapps.arfilter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ModelSelectionActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model_selection);

        Button genderbtn = (Button)findViewById(R.id.genderbtn);
        Button emotionbtn = (Button)findViewById(R.id.emotionbtn);

        genderbtn.setOnClickListener(this);
        emotionbtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();

        if(id==R.id.genderbtn){
            Intent intent = new Intent(this, ARFilterActivity.class);
            intent.putExtra(ARFilterActivity.MODEL_TYPE,"gender");
            startActivity(intent);
        }
        else if(id==R.id.emotionbtn){
            Intent intent = new Intent(this,ARFilterActivity.class);
            intent.putExtra(ARFilterActivity.MODEL_TYPE,"emotion");
            startActivity(intent);
        }
    }

}
