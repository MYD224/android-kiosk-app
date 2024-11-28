package com.gammaconcept.kioskmgmtapp;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerRequest {

//    public void checkForUpdates(Context context, String serverVersionUrl) {
//        // Use an HTTP library like OkHttp to fetch the version info
//        String currentVersion = getAppVersion(context);
//        new Thread(() -> {
//            try {
//                URL url = new URL(serverVersionUrl);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestMethod("GET");
//                connection.connect();
//
//                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                    String latestVersion = reader.readLine();
//                    reader.close();
//
//                    if (!currentVersion.equals(latestVersion)) {
//                        downloadApk(ServerAPKPath ,context);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
}
