package com.gammaconcept.kioskmgmtapp;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class SilentInstaller {

    public static void installApkSilently(Context context, File apkFile) {
        if (!apkFile.exists()) {
            Log.e("SilentInstaller", "APK file not found.");
            return;
        }

        try {
            // Run shell command for silent installation
            String command = "pm install -r " + apkFile.getAbsolutePath();
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            process.waitFor();

            int exitValue = process.exitValue();
            if (exitValue == 0) {
                Log.d("SilentInstaller", "App installed successfully.");
            } else {
                Log.e("SilentInstaller", "Installation failed with exit code: " + exitValue);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

