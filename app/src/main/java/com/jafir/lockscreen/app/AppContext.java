package com.jafir.lockscreen.app;

import android.app.Application;
import android.content.Context;

/**
 * Created by jafir on 16/8/11.
 */
public class AppContext extends Application {


    public static Context context;
    public static int sceenWidth;
    public static int sceenHeight;
    public static boolean isDeveloper = false;


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();

        sceenWidth = this.getResources().getDisplayMetrics().widthPixels;
        sceenHeight = this.getResources().getDisplayMetrics().heightPixels;

    }


}
