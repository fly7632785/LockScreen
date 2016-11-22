package com.jafir.lockscreen.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Created by jafir on 16/7/25.
 */
public class WindowUtils {


    private static final String LOG_TAG = "WindowUtils";
    private static Context mContext = null;


    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        return line;
    }


    public static boolean isMIUIRom() {
        String property = getSystemProperty("ro.miui.ui.version.name");
        return !TextUtils.isEmpty(property);
    }

    public static String getProperty() {
        String property = "null";
        if (!"Xiaomi".equals(Build.MANUFACTURER)) {
            return property;
        }
        try {
            Class<?> spClazz = Class.forName("android.os.SystemProperties");
            Method method = spClazz.getDeclaredMethod("get", String.class, String.class);
            property = (String) method.invoke(spClazz, "ro.miui.ui.version.name", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return property;
    }


    /**
     * 经测试V5版本是有区别的
     *
     * @param context
     */
    public static void openMiuiPermissionActivity(Context context) {
        Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");

        if ("V5".equals(getProperty())) {
            PackageInfo pInfo = null;
            try {
                pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("canking", "error");
            }
            intent.setClassName("com.miui.securitycenter", "com.miui.securitycenter.permission.AppPermissionsEditor");
            intent.putExtra("extra_package_uid", pInfo.applicationInfo.uid);
        } else {
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
        }

        if (isIntentAvailable(context, intent)) {
            if (context instanceof Activity) {
                Activity a = (Activity) context;
                a.startActivityForResult(intent, 2);
                PreferenceUtil.write(context, "common", "isOpen", true);
            }
        } else {
            Log.d("debug", "不行");
            PreferenceUtil.write(context, "common", "isOpen", true);
            Toast.makeText(context, "对不起，你的手机是miui系统\n如果你还是想用，你需要到\n设置-->应用-->你的应用-->权限管理\n去设置权限", Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 判断是否有可以接受的Activity
     *
     * @param context
     * @param action
     * @return
     */
    public static boolean isIntentAvailable(Context context, Intent intent) {
        if (intent == null) return false;
        return context.getPackageManager().queryIntentActivities(intent, PackageManager.GET_ACTIVITIES).size() > 0;
    }

    public static boolean getAppOps(Context context) {
        try {
            Object object = context.getSystemService("appops");
            if (object == null) {
                return false;
            }
            Class localClass = object.getClass();
            Class[] arrayOfClass = new Class[3];
            arrayOfClass[0] = Integer.TYPE;
            arrayOfClass[1] = Integer.TYPE;
            arrayOfClass[2] = String.class;
            Method method = localClass.getMethod("checkOp", arrayOfClass);
            if (method == null) {
                return false;
            }
            Object[] arrayOfObject1 = new Object[3];
            arrayOfObject1[0] = Integer.valueOf(24);
            arrayOfObject1[1] = Integer.valueOf(Binder.getCallingUid());
            arrayOfObject1[2] = context.getPackageName();
            int m = ((Integer) method.invoke(object, arrayOfObject1)).intValue();
            return m == AppOpsManager.MODE_ALLOWED;
        } catch (Exception ex) {

        }
        return false;
    }


    /**
     * 判断MIUI的悬浮窗权限
     *
     * @param context
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isMiuiFloatWindowOpAllowed(Context context) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return checkOp(context, 24);  // AppOpsManager.OP_SYSTEM_ALERT_WINDOW
        } else {
            if ((context.getApplicationInfo().flags & 1 << 27) == 1 << 27) {
                return true;
            } else {
                return false;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean checkOp(Context context, int op) {
        final int version = Build.VERSION.SDK_INT;

        if (version >= 19) {
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            try {

                Class<?> spClazz = Class.forName(manager.getClass().getName());
                Method method = manager.getClass().getDeclaredMethod("checkOp", int.class, int.class, String.class);
                int property = (Integer) method.invoke(manager, op,
                        Binder.getCallingUid(), context.getPackageName());
                Log.e("debug", AppOpsManager.MODE_ALLOWED + " invoke " + property);

                if (AppOpsManager.MODE_ALLOWED == property) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                Log.e("debug", e.getMessage());
            }
        } else {
            Log.e("debug", "Below API 19 cannot invoke!");
        }
        return false;
    }

}
