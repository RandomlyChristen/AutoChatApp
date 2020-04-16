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
    private EditText replyString;
    private EditText targetPackageName;
    private EditText flagString;
    private Switch serviceSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Static 값으로 초기화, 초기값 : "/" 이 값은 NotificationReceiver 가 받은 알림이 해당 문자로 시작하는지 확인할 때 쓰임.
        flagString = findViewById(R.id.flagString);
        flagString.setText(NotificationReceiver.getFlag());

        // Static 값으로 초기화, 초기값: "답장" 이 값은 NotificationReceiver 가 받은 알림의 Action 의 Title 이 해당 문자열과 일치하는지 확인 할 때 쓰임.
        replyString = findViewById(R.id.replyString);
        replyString.setText(NotificationReceiver.getReplyString());

        // Static 값으로 초기화, 초기값: "com.kakao.talk" 이 값은 NotificationReceiver 가 받은 알림의 앱 패키지 명이 해당 문자열과 일치하는지 확인 할 때 쓰임.
        targetPackageName = findViewById(R.id.targetPackageName);
        targetPackageName.setText(NotificationReceiver.getTargetPackageName());

        // 서비스를 시작할 때, 혹은 종료할 때 쓰일 스위치.
        serviceSwitch = findViewById(R.id.serviceSwitch);

        //
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
                    NotificationReceiver.setReplyString(replyString.getText().toString());
                    NotificationReceiver.setTargetPackageName(targetPackageName.getText().toString());
                    NotificationReceiver.legalStartCall(getApplicationContext());
                }
            }
        });
    }
}
