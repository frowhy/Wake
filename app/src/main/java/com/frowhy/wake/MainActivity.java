package com.frowhy.wake;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.frowhy.wake.model.Schema;
import com.google.gson.Gson;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private SharedPreferences mSp;
    private SharedPreferences.Editor mSpEditor;
    private String mSchema = "mqq://";

    private View mBtnTest;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSp = PreferenceManager.getDefaultSharedPreferences(this);
        mSpEditor = mSp.edit();
        mSpEditor.apply();

        initView();
        initSp();
        initHandle();
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

            Log.d("----------------", jsonStr);
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
        } else {
            mSp.getStringSet(mSchema, new HashSet<String>());
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

    private void initView() {
        mBtnTest = findViewById(R.id.btn_test);
    }

    private void initHandle() {
        packageManager = getPackageManager();
        mBtnTest.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("LongLogTag")
            @Override
            public void onClick(View v) {
                handle(Uri.parse(mSchema));
            }
        });
    }

    private void handle(Uri uri) {
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

                            Process process = Runtime.getRuntime().exec("su");
                            DataOutputStream os = new DataOutputStream(process.getOutputStream());
                            os.writeBytes("pm enable " + packageName + "\n");
                            os.writeBytes("exit\n");
                            os.flush();
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
