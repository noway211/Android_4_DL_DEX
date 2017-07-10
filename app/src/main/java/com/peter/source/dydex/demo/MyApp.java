package com.peter.source.dydex.demo;

import android.app.Application;
import android.util.Log;

/**
 * Created by Administrator on 2017/7/6.
 */

public class MyApp extends Application {

    public static boolean DEBGU =  false;
    @Override
    public void onCreate() {
        super.onCreate();
        DEBGU = true;
        if (DEBGU){
            Log.d("peter","DEMO APP ONCREATE");
        }
    }
}
