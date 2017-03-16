package com.frowhy.wake;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

import static com.frowhy.wake.WakeActivity.execRootCmdSilent;
import static com.frowhy.wake.WakeActivity.getVersionCode;

public class MainActivity extends Activity {
    private SharedPreferences.Editor mSpEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences mSp = PreferenceManager.getDefaultSharedPreferences(this);
        mSpEditor = mSp.edit();
        mSpEditor.apply();

        PackageManager packageManager = getPackageManager();

        if (mSp.getInt("version_code", 0) < getVersionCode(packageManager, getPackageName()) || !mSp.getBoolean("hasRoot", false)) {
            Toast.makeText(this, "更新版本可能要重新授权,首次授权可能会慢一些", Toast.LENGTH_SHORT).show();
        }

        new Handler().postDelayed(new Runnable() {
            public void run() {
                boolean checkRoot = 0 == execRootCmdSilent("");
                if (checkRoot) {
                    mSpEditor.putBoolean("hasRoot", true);
                    mSpEditor.commit();
                }
            }
        }, 500);
    }
}
