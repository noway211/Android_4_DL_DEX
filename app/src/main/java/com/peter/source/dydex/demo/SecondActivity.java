package com.peter.source.dydex.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

/**
 * Created by Administrator on 2017/7/6.
 */

public class SecondActivity  extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        if (MyApp.DEBGU){
            Log.d("peter","run SecondActivity");
        }

    }
    public void onClick(View view){
        finish();
    }
}
