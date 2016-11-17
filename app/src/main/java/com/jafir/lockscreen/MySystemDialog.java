package com.jafir.lockscreen;

/**
 * Created by jafir on 16/11/9.
 * <p>
 * <p>
 * 实现一个锁屏界面
 * 需求:
 * 开启之后，需要解锁才能进入手机桌面，其他方式不得进入
 * 并且 需要记1个单词，打字输出英文和输入中文的任何一个意思 才能解锁
 * 有两个逻辑跳转
 * 先是显示单词，然后记忆
 * 然后输出英文单词，正确之后切换界面，然后输入中文，都正确就进入主界面
 */

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;


public class MySystemDialog extends Activity {
    private WindowManager wm;
    private Button bb;
    private ViewGroup viewgroup;
    private Button bb1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_screen);
        viewgroup = (ViewGroup) View.inflate(this, R.layout.lock_screen, null);
        bb = new Button(getApplicationContext());
        bb.setBackground(new ColorDrawable(Color.WHITE));
        bb.setText("hahaha");
        bb.setTextColor(Color.BLUE);
        bb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wm.removeView(viewgroup);
                MySystemDialog.this.finish();
            }
        });
        bb1 = new Button(getApplicationContext());
        bb1.setBackground(new ColorDrawable(Color.WHITE));
        bb1.setText("xixxii");
        bb1.setTextColor(Color.BLUE);
        bb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wm.removeView(viewgroup);
                MySystemDialog.this.finish();
            }
        });

        viewgroup.addView(bb);
        viewgroup.addView(bb1);
        wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();

        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        wmParams.format = PixelFormat.OPAQUE;
        wmParams.flags = WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
        ;
        wmParams.width = getResources().getDisplayMetrics().widthPixels;
        wmParams.height = getResources().getDisplayMetrics().heightPixels;
        wm.addView(viewgroup, wmParams);
    }
}
