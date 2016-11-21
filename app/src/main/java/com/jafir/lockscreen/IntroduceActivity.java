package com.jafir.lockscreen;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.jafir.lockscreen.app.AppContext;
import com.jafir.lockscreen.util.PreferenceUtil;
import com.jafir.lockscreen.util.StringUtil;
import com.jafir.lockscreen.util.ToastUtil;

import java.util.ArrayList;

/**
 * Created by jafir on 16/11/16.
 */

public class IntroduceActivity extends AppCompatActivity {


    private static final String GITHUB = "github: https://github.com/fly7632785";
    private static final String GITHUB_URL = "https://github.com/fly7632785";
    private Switch mSwitch;
    private TextView mToGithub;
    private TextView mDays;

    private View openLl;
    private View kaifazhe;
    TextView developer;
    private long mLastClickTime;
    private long MIN_CLICK_INTERVAL = 1000;
    private long MIN_CLICK = 6;
    private int mSecretNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduce);
        init();

    }

    private void init() {

        mSwitch = (Switch) findViewById(R.id.switchview);
        mToGithub = (TextView) findViewById(R.id.togithub);
        mDays = (TextView) findViewById(R.id.days);
        int days = PreferenceUtil.readInt(this, "common", "days", 0);
        if (days > 0) {
            mDays.setText("太棒了！你已经坚持了" + days + "天！");
        } else {
            mDays.setText("今天是第1天哦，加油，一定要坚持");
        }

        /**
         * 这里先隐藏了，弄个一个小机关
         * 需要点很多次，才能开启开发者模式 才有关闭的按钮
         * 开启开发者模式：需要多次点击才能开启，是一个不可见的view，见xml
         * 主要这是 为了给我的那些朋友用的，不让他们能够轻易关掉这个功能
         *
         */
        openLl = findViewById(R.id.open_ll);
        openLl.setVisibility(View.GONE);
        PreferenceUtil.write(this, "common", "isOpen", true);

        kaifazhe = findViewById(R.id.kaifazhe);
        developer = (TextView) findViewById(R.id.developer);
        kaifazhe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long currentClickTime = SystemClock.uptimeMillis();
                long elapsedTime = currentClickTime - mLastClickTime;
                mLastClickTime = currentClickTime;
                if (elapsedTime < MIN_CLICK_INTERVAL) {
                    if (mSecretNumber > 0 && mSecretNumber < MIN_CLICK - 1 && developer.getVisibility() == View.GONE) {
                        ToastUtil.showNoDelay("再点击" + (MIN_CLICK - mSecretNumber - 1) + "次打开开发者模式");
                    }
                    ++mSecretNumber;
                    if (MIN_CLICK == mSecretNumber) {
                        developer.setVisibility(View.VISIBLE);
                        AppContext.isDeveloper = true;
                        openLl.setVisibility(View.VISIBLE);
                    }
                } else {
                    mSecretNumber = 0;
                }
            }
        });
        mSwitch.setChecked(PreferenceUtil.readBoolean(this, "common", "isOpen", false));

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    PreferenceUtil.write(IntroduceActivity.this, "common", "isOpen", true);
//                    requestPermission();
                } else {
                    PreferenceUtil.write(IntroduceActivity.this, "common", "isOpen", false);
                }
            }
        });
        //由于使用toast类型window这里不需要请求权限了 4.0一下使用的是phone type,所以xml里面还是要配置
        requestPermission();

        mToGithub.setText(StringUtil.matcherSearchTitle(Color.BLUE, GITHUB, GITHUB_URL));
        mToGithub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri content_url = Uri.parse(GITHUB_URL);
                intent.setData(content_url);
                startActivity(intent);
            }
        });


        startService(new Intent(this, KeepAliveService.class));
    }

    /**
     * 请求权限
     * <p>
     * 由于miui系统很特殊，需要特别对待
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(IntroduceActivity.this)) {
                if (WindowUtils.isMIUIRom()) {
                    Log.w("debug", "6是MIUI");
                    WindowUtils.openMiuiPermissionActivity(IntroduceActivity.this);
                } else {
                    Log.w("debug", "6不是MIUI");
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1);
                }
            } else {
                Log.w("debug", "6有权限");
            }
        } else {
            //如果小于6.0需要先看是不是miui，是miui要特殊处理
            //如果不是miui可直接使用
            if (WindowUtils.isMIUIRom()) {
                if (WindowUtils.isMiuiFloatWindowOpAllowed(IntroduceActivity.this)) {
                    Log.w("debug", "小于6miui有权限");
                } else {
                    WindowUtils.openMiuiPermissionActivity(IntroduceActivity.this);
                }
                Log.w("debug", "小于6是MIUI");
            } else {
                Log.w("debug", "小于6不是MIUI");
            }
        }
    }

    private void permission() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(IntroduceActivity.this, "已加载权限", Toast.LENGTH_SHORT).show();
                init();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(IntroduceActivity.this, "没有权限\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }


        };
        new TedPermission(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("你没有权限，需要手动去设置一下\n\n请跳转去设置")
                .setPermissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
                .check();


    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(IntroduceActivity.this, "权限授予失败，无法开启悬浮窗", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(IntroduceActivity.this, "权限授予成功！", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == 2) {
        }
    }
}
