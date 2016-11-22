package com.jafir.lockscreen.app;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;

import com.jafir.lockscreen.R;
import com.jafir.lockscreen.util.GlideImageLoader;

import cn.finalteam.galleryfinal.BuildConfig;
import cn.finalteam.galleryfinal.CoreConfig;
import cn.finalteam.galleryfinal.FunctionConfig;
import cn.finalteam.galleryfinal.GalleryFinal;
import cn.finalteam.galleryfinal.ImageLoader;
import cn.finalteam.galleryfinal.ThemeConfig;

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


        ThemeConfig theme = new ThemeConfig.Builder()
                //两种方法都可以
//                .setTitleBarBgColor(getResources().getColor(R.color.colorPrimary))
                .setTitleBarBgColor(Color.rgb(0, 151, 255))
                .setCropControlColor(Color.rgb(255, 64, 129))
                .setFabNornalColor(Color.rgb(0, 151, 255))
                .setFabPressedColor(Color.rgb(0, 15, 213))
                .setCheckSelectedColor(getResources().getColor(R.color.colorPrimary))
                .build();



        //配置功能
        FunctionConfig functionConfig = new FunctionConfig.Builder()
                .setEnableCamera(true)
                .setEnableEdit(true)
                .setEnableCrop(true)
                .setEnableRotate(true)
                .setEnablePreview(true)
                .setForceCrop(true)
                .build();

        //配置imageloader
        ImageLoader imageloader = new GlideImageLoader();
        CoreConfig coreConfig = new CoreConfig.Builder(getApplicationContext(), imageloader, theme)
                .setDebug(BuildConfig.DEBUG)
                .setFunctionConfig(functionConfig)
                .build();
        GalleryFinal.init(coreConfig);

    }


}
