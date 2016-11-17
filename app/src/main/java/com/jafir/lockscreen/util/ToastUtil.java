package com.jafir.lockscreen.util;

import android.widget.Toast;

import com.jafir.lockscreen.app.AppContext;


/**
 * Created by jafir on 16/8/18.
 */
public class ToastUtil {

    private static Toast toast ;


    public static void show(String s) {
        Toast.makeText(AppContext.context, s, Toast.LENGTH_SHORT).show();
    }

    public static void showNoDelay(String text) {

        if (toast == null) {
            toast = Toast.makeText(AppContext.context, text, Toast.LENGTH_SHORT);  //正常执行
        } else {
            toast.setText(text);  //用于覆盖前面未消失的提示信息
        }
        toast.show();
    }
}
