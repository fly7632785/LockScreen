package com.jafir.lockscreen;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by jafir on 16/11/18.
 * 为了实现一个前台进程来保活
 * 通常情况下 如果熄灭屏幕太久 会自动被杀掉
 * 这样的话基本可以保活成功
 */

public class KeepAliveService extends Service {
    ScreenOnReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        receiver = new ScreenOnReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT); //注册IntentFilter
        filter.setPriority(Integer.MAX_VALUE); //设置级别
        registerReceiver(receiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder notification = new Notification.Builder(getApplicationContext());
        notification.setContentTitle("单词锁屏")
                .setTicker("单词锁屏")
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.logo))
                .setSmallIcon(R.mipmap.logo);
        startForeground(1, notification.build());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
