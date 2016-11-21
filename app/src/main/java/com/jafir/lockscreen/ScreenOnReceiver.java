package com.jafir.lockscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jafir.lockscreen.util.PreferenceUtil;

/**
 * Created by jafir on 16/11/14.
 */

public class ScreenOnReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)
//                这个只有动态注册才能有效
//                        || intent.getAction().equals(Intent.ACTION_SCREEN_ON)
                ) {
            Log.e("debug","收到广播！！！！！！！！！！" +
                    "");

            if (PreferenceUtil.readBoolean(context, "common", "isOpen", false)) {
                Intent toLockScreenIntent = new Intent(context, MainActivity.class);
                toLockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(toLockScreenIntent);
            }
        }
    }
}
