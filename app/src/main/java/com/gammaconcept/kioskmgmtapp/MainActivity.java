package com.gammaconcept.kioskmgmtapp;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.text.InputType;
import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.gammaconcept.kioskmgmtapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize DevicePolicyManager
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        // Activate kiosk mode on launch
        if (devicePolicyManager.isAdminActive(adminComponentName)) {
            enableKioskMode();
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

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitKioskMode();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAnchorView(R.id.fab)
//                        .setAction("Action", null).show();
            }
        });
    }
    private void exitKioskMode() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Password");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setText("YourSecurePassword");
        builder.setView(input);

        builder.setPositiveButton("Unlock", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals("YourSecurePassword")) {
                stopLockTask(); // Exits Kiosk Mode
            } else {
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void enableKioskMode() {
//        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
//            // Allow lock task mode
//            String[] packages = {getPackageName()};
//            devicePolicyManager.setLockTaskPackages(adminComponentName, packages);
//
//            // Start lock task mode
//            startLockTask();
//        } else {
//            Toast.makeText(this, "App is not set as Device Owner", Toast.LENGTH_LONG).show();
//        }
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            devicePolicyManager.setLockTaskPackages(adminComponentName, new String[]{getPackageName()});
            startLockTask(); // Automatically starts persistent lock task
        } else {
            requestDeviceAdmin();
        }
    }

    private void requestDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "App needs admin permissions.");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            enableKioskMode();
        } else {
            Toast.makeText(this, "Admin permission is required!", Toast.LENGTH_LONG).show();
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