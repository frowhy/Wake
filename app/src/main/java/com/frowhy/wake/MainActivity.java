package com.frowhy.wake;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.frowhy.wake.model.Schema;
import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private SharedPreferences mSp;
    private SharedPreferences.Editor mSpEditor;

    private PackageManager packageManager;
    private boolean checkRoot = false;
    private int count = 0;

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

        if (versionCode == 0 || versionCode < getVersionCode()) {

            String jsonStr = getLocalJson();

            Gson gson = new Gson();

            Schema schemas = gson.fromJson(jsonStr, Schema.class);

            List<Schema.SchemasBean> schemasList = schemas.getSchemas();

            for (Schema.SchemasBean schema : schemasList) {
                String schemaName = schema.getSchema();
                List<String> packageNameList = schema.getPackage_name();
                Set<String> set = new HashSet<>();
                set.addAll(packageNameList);
                mSpEditor.putStringSet(schemaName, set);
            }

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

    private void initHandle() {
        packageManager = getPackageManager();
    }

    private void handle(final Uri uri) {
        String mSchema = uri.getScheme();

        /* 该Schema存在 */
        if (mSp.contains(mSchema)) {

            /* 取包名 */
            Set<String> mPackageNames = mSp.getStringSet(mSchema, new HashSet<String>());

            /* 如果包名不为空 */
            if (!mPackageNames.isEmpty()) {

                /* 循环包名 */
                for (String packageName : mPackageNames) {

                    PackageInfo packageInfo;
                    try {

                        packageInfo = packageManager.getPackageInfo(packageName, 0);
                        if (!packageInfo.applicationInfo.enabled) {
                            count++;
                            checkRoot = 0 == execRootCmdSilent("pm enable " + packageName);
                            Toast.makeText(this, "已唤醒[" + packageInfo.applicationInfo.loadLabel(packageManager).toString() + "]", Toast.LENGTH_SHORT).show();
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                }

                if (count == 0) {
                    Toast.makeText(this, "所需APP未被冻结,重新打开,请勿选择唤醒", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    finish();
                } else {
                    if (checkRoot) {
                        Toast.makeText(this, "已唤醒所有APP,正在打开,请等待...", Toast.LENGTH_SHORT).show();

                        new Handler().postDelayed(new Runnable() {
                            public void run() {

                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(intent);
                                finish();
                            }
                        }, 1000);
                    } else {
                        Toast.makeText(this, "未获取ROOT权限,重新打开,请唤醒并授权", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        finish();
                    }
                }
            } else {
                Toast.makeText(this, "未记录该APP,请反馈Schema以及PackageName", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                finish();
            }
        } else {
            Toast.makeText(this, "未记录该APP,请反馈Schema以及PackageName", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            finish();
        }
    }
}
