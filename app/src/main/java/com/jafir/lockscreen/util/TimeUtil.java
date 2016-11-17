package com.jafir.lockscreen.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jafir on 16/11/16.
 */

public class TimeUtil {


    public static String getTodayDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = formatter.format(curDate);
        return str;
    }
}
