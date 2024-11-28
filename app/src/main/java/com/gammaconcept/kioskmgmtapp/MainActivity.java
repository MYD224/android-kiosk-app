package com.gammaconcept.kioskmgmtapp;

import static com.gammaconcept.kioskmgmtapp.SilentInstaller.installApkSilently;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Process;
import android.text.InputType;
import android.util.Log;
import android.view.View;

import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.gammaconcept.kioskmgmtapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponentName;

    private static final String AnyDeskPkgName = "com.anydesk.anydeskandroid";
    private static final String AnyDeskPluginName = "com.anydesk.adcontrol.ad1";

    //server urls
    private static final String ServerUrl = "http://192.168.1.169:8000";
    private static final String AppVersionUrl = ServerUrl+"/api/auth/mobile-app-latest-version";
    private static final String ServerAPKPath = ServerUrl+"/storage/PartnerMobileAPKs/teliyasantepartners.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize DevicePolicyManager
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        // Activate kiosk mode on launch
        if (devicePolicyManager.isAdminActive(adminComponentName)) {
            startKioskMode();
        } else {
            requestDeviceAdmin();
        }
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view -> exitKioskMode());

        // Button to launch AnyDesk
        Button launchAnyDesk = findViewById(R.id.btn_lunch_anydesk);
        launchAnyDesk.setOnClickListener(view -> {
            launchAnyDeskApp();
        });

        // Button to stop AnyDesk
        Button stopAnyDesk = findViewById(R.id.btn_stop_anydesk);
//        stopAnyDesk.setOnClickListener(view -> debugRunningProcesses());
        stopAnyDesk.setOnClickListener(view -> {
                    try {
                        checkForUpdates(this, AppVersionUrl);
                    } catch (PackageManager.NameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
//                stopAnyDesk(MainActivity.this)
        );
    }
    private void exitKioskMode() {
//        try {
//            checkForUpdates(this, AppVersionUrl);
//        } catch (PackageManager.NameNotFoundException e) {
//            throw new RuntimeException(e);
//        }
        String secret = "Tel1y@SecureP@ssw0rd79";
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Password");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setText(secret);
        builder.setView(input);

        builder.setPositiveButton("Unlock", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals(secret)) {
                stopLockTask(); // Exits Kiosk Mode
            } else {
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }



    private void startKioskMode() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);

        // Verify if the app is a device owner
        if (dpm.isDeviceOwnerApp(getPackageName())) {
            // Whitelist your app and AnyDesk(with it's plugin)
            dpm.setLockTaskPackages(adminComponent, new String[]{
                    getPackageName(), // Your app
                    AnyDeskPkgName, // AnyDesk package name
                    AnyDeskPluginName // AnyDesk adcontrol package name
            });

            // Start lock task mode
            startLockTask();
        } else {
            Toast.makeText(this, "App is not set as device owner.", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "App needs admin permissions.");
        startActivityForResult(intent, 1);
    }


    private void launchAnyDeskApp() {
        Intent intent = new Intent();
        intent.setClassName(AnyDeskPkgName,  AnyDeskPkgName+".gui.activity.MainActivity");
        try {
            startActivity(intent);
//            launchAnyDeskPlugin();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch AnyDesk. Please check installation.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stop AnyDesk application whether it's running in the foreground or background.
     * This method attempts to kill the AnyDesk process.
     */
    private void stopAnyDesk(Context context) {
        final String TAG = "AppManager";
        // Check if AnyDesk is running
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        Log.d("test result", String.valueOf(activityManager != null));
        if (activityManager != null) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();

            Log.d("Running Processes :", String.valueOf(runningProcesses));
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.processName.equals(AnyDeskPkgName)) {
                    // Kill the AnyDesk process on Lollipop or higher using Process.killProcess()
                    int pid = processInfo.pid;
                    Process.killProcess(pid);
                    Log.d(TAG, "AnyDesk process killed: " + pid);
                    return;
                }
            }

            Log.d(TAG, "AnyDesk process not killed: ");
        }
    }

    public void checkForUpdates(Context context, String serverVersionUrl) throws PackageManager.NameNotFoundException {
        // Use an HTTP library like OkHttp to fetch the version info
        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        String currentVersion = packageInfo.versionName;
//        String currentVersion = getAppVersion(context);
        new Thread(() -> {
            try {
                URL url = new URL(serverVersionUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                Log.d("Fetching apk version => ", "from online serve");
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String latestVersion = reader.readLine();
                    reader.close();
                    Log.d("latestVersion => ", latestVersion);
                    Log.d("App has new version => ", String.valueOf((!currentVersion.equals(latestVersion))));
                    if (!currentVersion.equals(latestVersion)) {
                        downloadApk(ServerAPKPath ,context);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    public void downloadApk(String apkUrl, Context context) {
        String fileName = "update.apk";
        File apkFile = new File(context.getExternalFilesDir(null), fileName);

        // Download APK
        new Thread(() -> {
            try {
                URL url = new URL(apkUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream input = connection.getInputStream();
                    FileOutputStream output = new FileOutputStream(apkFile);

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }

                    output.close();
                    input.close();
                    Log.d("Ready to install new app", "We are able to install the new app");
                    // Silent install
                    installApk(context, apkFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public void installApk(Context context, File apkFile) {
        Log.d("APP installation in progess", "");
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
        Log.d("APP installation done", "");
    }

    private void launchAnyDeskPlugin() {
        Intent intent = new Intent();
        intent.setClassName(AnyDeskPluginName, AnyDeskPluginName+".MainActivity"); // Replace with actual activity
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch AnyDesk Plugin AD1. Please check installation.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


}