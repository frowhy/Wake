package com.frowhy.wake;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.frowhy.wake.model.Schema;
import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WakeActivity extends Activity {
    private Gson gson = new Gson();
    private SharedPreferences mSp;
    private SharedPreferences.Editor mSpEditor;
    private PackageManager packageManager;
    private boolean checkRoot = false;

    protected int execRootCmdSilent(String paramString) {
        try {
            Process localProcess = Runtime.getRuntime().exec("su");
            Object localObject = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream(
                    (OutputStream) localObject);
            String str = String.valueOf(paramString);
            localObject = str + "\n";
            localDataOutputStream.writeBytes((String) localObject);
            localDataOutputStream.flush();
            localDataOutputStream.writeBytes("exit\n");
            localDataOutputStream.flush();
            localProcess.waitFor();
            return localProcess.exitValue();
        } catch (Exception ignored) {
            return -1;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSp = PreferenceManager.getDefaultSharedPreferences(this);
        mSpEditor = mSp.edit();
        mSpEditor.apply();

        packageManager = getPackageManager();

        initSp();
        initSchema();
    }

    private void initSchema() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            handle(uri);
        }
    }

    private void initSp() {
        int versionCode = mSp.getInt("version_code", 0);
        int currentVersionCode = getVersionCode();

        if (versionCode == 0 || versionCode < currentVersionCode) {
            String jsonStr = getLocalJson();
            Schema schemas = gson.fromJson(String.valueOf(jsonStr), Schema.class);
            List<Schema.SchemasBean> schemasList = schemas.getSchemas();
            for (Schema.SchemasBean schema : schemasList) {
                Set<String> set = new HashSet<>();
                String schemaName = schema.getSchema();
                List<String> packageNameList;
                packageNameList = schema.getPackage_name();
                set.addAll(packageNameList);
                mSpEditor.putStringSet(schemaName, set);
            }
            mSpEditor.putInt("version_code", currentVersionCode);
            mSpEditor.commit();
        }
    }

    private String getLocalJson() {
        String resultString = "";
        try {
            InputStream inputStream = getResources().getAssets().open("schemas.json");
            byte[] buffer = new byte[inputStream.available()];
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(buffer);
            resultString = new String(buffer, "GB2312");
        } catch (Exception ignored) {
        }
        return resultString;
    }

    private int getVersionCode() {
        int versionCode = 0;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return versionCode;
    }

    private void handle(final Uri uri) {
        String mSchema = uri.getScheme();

        if (mSchema.equals("wake")) {
            final String mPackageName = uri.getHost();
            PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfo(mPackageName, 0);
                if (!packageInfo.applicationInfo.enabled) {
                    checkRoot = 0 == execRootCmdSilent("pm enable " + mPackageName);
                }

                final Intent intent = packageManager.getLaunchIntentForPackage(mPackageName);

                Toast.makeText(this, "已唤醒[" + packageInfo.applicationInfo.loadLabel(packageManager).toString() + "]", Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        startActivity(intent);
                    }
                }, 1000);

                finish();
            } catch (PackageManager.NameNotFoundException ignored) {
                Toast.makeText(this, "唤醒失败", Toast.LENGTH_SHORT).show();
            }
        } else {

        /* 该Schema存在 */
            if (mSp.contains(mSchema)) {

            /* 取包名 */
                Set<String> mPackageNames = mSp.getStringSet(mSchema, new HashSet<String>());

            /* 如果包名不为空 */
                if (!mPackageNames.isEmpty()) {
                    int count = 0;

                /* 循环包名 */
                    for (String packageName : mPackageNames) {

                        PackageInfo packageInfo;
                        try {
                            packageInfo = packageManager.getPackageInfo(packageName, 0);
                            if (!packageInfo.applicationInfo.enabled) {
                                count += 1;
                                checkRoot = 0 == execRootCmdSilent("pm enable " + packageName);

                                String label = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                                    BitmapDrawable icon = (BitmapDrawable) packageInfo.applicationInfo.loadIcon(packageManager);
                                    createShortcut(label, packageName, icon.getBitmap(), new Intent(Intent.ACTION_VIEW, Uri.parse("wake://" + packageName)));
                                }

                                Toast.makeText(this, "已唤醒[" + label + "]", Toast.LENGTH_SHORT).show();
                            }
                        } catch (PackageManager.NameNotFoundException ignored) {
                        }
                    }

                    if (count == 0) {
                        Toast.makeText(this, "所需APP未被冻结,重新打开,请勿选择唤醒", Toast.LENGTH_SHORT).show();

                        finish();
                    } else {
                        if (checkRoot) {
                            Toast.makeText(this, "已唤醒所有APP,正在打开,请等待...", Toast.LENGTH_SHORT).show();

                            new Handler().postDelayed(new Runnable() {
                                public void run() {
                                    startActivityWithUri(uri, false);
                                }
                            }, 1000);
                            finish();
                        } else {
                            Toast.makeText(this, "未获取ROOT权限,重新打开,请唤醒并授权", Toast.LENGTH_SHORT).show();

                            startActivityWithUri(uri, true);
                        }
                    }
                } else {
                    Toast.makeText(this, "未记录该APP,请反馈Schema以及PackageName", Toast.LENGTH_SHORT).show();

                    startActivityWithUri(uri, true);
                }
            } else {
                Toast.makeText(this, "未记录该APP,请反馈Schema以及PackageName", Toast.LENGTH_SHORT).show();

                startActivityWithUri(uri, true);
            }
        }
    }

    private void startActivityWithUri(Uri uri, boolean isFinish) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
        if (isFinish) {
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private void createShortcut(String label, String packageName, Bitmap icon, Intent intent) {
        ShortcutManager shortcutManager;
        shortcutManager = getSystemService(ShortcutManager.class);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, packageName)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(Icon.createWithBitmap(icon))
                .setIntent(intent)
                .setRank(0)
                .build();

        List<ShortcutInfo> shortcutList = shortcutManager.getDynamicShortcuts();
        int shortcutSize = shortcutList.size();

        if (shortcutSize >= 5) {
            for (int i = 0; i < shortcutSize - 4; i++) {
                shortcutManager.removeDynamicShortcuts(Collections.singletonList(shortcutList.get(0).getId()));
            }
        }
        shortcutManager.addDynamicShortcuts(Collections.singletonList(shortcut));
    }
}
