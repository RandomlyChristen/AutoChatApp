package com.example.autochatapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {
    private boolean isAvailable;
    private final Intent settingStartIntent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
    private EditText flagString;
    private Switch serviceSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        flagString = findViewById(R.id.flagString);
        serviceSwitch = findViewById(R.id.serviceSwitch);

        isAvailable = NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext()).contains(getPackageName());

        if (!isAvailable)
            startActivity(settingStartIntent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        isAvailable = NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext()).contains(getPackageName());

        if (!isAvailable) {
            finishAffinity(); System.runFinalization(); System.exit(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        isAvailable = NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext()).contains(getPackageName());

        if (!isAvailable)
            return;

        serviceSwitch.setChecked(NotificationReceiver.isRunning());
        serviceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!((Switch)v).isChecked()) {
                    sendBroadcast(new Intent(getApplicationContext().getString(R.string.stop_service)));
                } else {
                    NotificationReceiver.setFlag(flagString.getText().toString());
                    NotificationReceiver.legalStartCall(getApplicationContext());
                }
            }
        });
    }
}
