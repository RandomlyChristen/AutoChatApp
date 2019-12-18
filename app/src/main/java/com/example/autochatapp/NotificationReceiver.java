package com.example.autochatapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Objects;

public class NotificationReceiver extends NotificationListenerService {
    private ApplicationThread START_APP(SimpleNotification startCall) {
        ApplicationThread app = null;

        if (startCall.text.contains("테스트"))
            app = new ApplicationThread(startCall, this);
        else if (startCall.text.contains(OddEven.START))
            app = new OddEven(startCall, this);
        else if (startCall.text.contains(MinusAuction.START))
            app = new MinusAuction(startCall, this);
        /*
        else if (startCall.text.contains("업다운"))
            app = new UpDown(startCall, this);
         */

        return app;
    }

    public static final int ON_GOING_NOTIFICATION_ID = 1;

    private static boolean running = false;
    public static boolean isRunning() { return running; }

    private static String flag = "/";
    public static void setFlag(String _flag) { flag = _flag; }
    public static String getFlag() { return flag; }

    private static String replyString = "답장";
    public static void setReplyString(String _replyString) { replyString = _replyString; }
    public static String getReplyString() { return replyString; }

    private static String targetPackageName = "com.kakao.talk";
    public static void setTargetPackageName(String _targetPackageName) { targetPackageName = _targetPackageName; }
    public static String getTargetPackageName() { return targetPackageName; }

    private HashMap<String, ApplicationThread> apps;

    public static void legalStartCall(Context context) {
        running = true;
        NotificationReceiver.requestRebind(new ComponentName(context, NotificationReceiver.class));
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        if (!running) {
            requestUnbind(); return;
        }
        // running is TRUE, after
        registerReceiver(new ServiceStopInterrupter(this), new IntentFilter(getString(R.string.stop_service)));

        apps = new HashMap<>();
        Notification.Builder foregroundBuilder;

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                    new NotificationChannel(getString(R.string.channel_id), getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) Objects.requireNonNull(getSystemService(NOTIFICATION_SERVICE))).createNotificationChannel(channel);
            foregroundBuilder = new Notification.Builder(getApplicationContext(), channel.getId());
        } else {
            foregroundBuilder = new Notification.Builder(getApplicationContext());
        }

        startForeground(ON_GOING_NOTIFICATION_ID, foregroundBuilder.build());
        Toast.makeText(this, "SERVICE CONNECTED!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Toast.makeText(this, "SERVICE DISCONNECTED!!", Toast.LENGTH_SHORT).show();
        running = false;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        SimpleNotification verified = verifyNotification(sbn);

        if (verified == null) return;

        if (verified.text.contains("안녕")) {
            reply(verified, "안녕하세요, " + verified.sender + "님"); return;
        }

        ApplicationThread app = apps.get(verified.roomName);
        if (app != null && app.isAlive()) {
            app.addBuffer(verified); return;
        }

        apps.put(verified.roomName, START_APP(verified));
    }

    public boolean reply(SimpleNotification notification, String msg) {
        Intent sendIntent = new Intent();
        Bundle messageBundle = new Bundle();

        messageBundle.putCharSequence(notification.sendAction.getRemoteInputs()[0].getResultKey(), msg);
        RemoteInput.addResultsToIntent(notification.sendAction.getRemoteInputs(), sendIntent, messageBundle);
        try {
            notification.sendAction.actionIntent.send(getApplicationContext(), 0, sendIntent);
            return true;
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            return false;
        }
    }


    private SimpleNotification verifyNotification(StatusBarNotification sbn) {
        if (!sbn.getPackageName().equals(targetPackageName)) return null;

        Notification notification = sbn.getNotification();

        if (!notification.category.equals(Notification.CATEGORY_MESSAGE)) return null;
        if (notification.actions == null) return null;

        SimpleNotification result = new SimpleNotification();
        result.text = notification.extras.getString(Notification.EXTRA_TEXT);
        result.sender = notification.extras.getString(Notification.EXTRA_TITLE);
        result.roomName = notification.extras.getString(Notification.EXTRA_SUB_TEXT);
        if (result.roomName == null)
            result.roomName = result.sender;

        if (result.text == null || result.sender == null || !result.text.startsWith(flag)) return null;

        for (Notification.Action action : notification.actions) {
            if (action.title.equals(replyString))
                result.sendAction = action;
        }

        if (result.sendAction == null) return null;

        return result;
    }

    private final class ServiceStopInterrupter extends BroadcastReceiver {
        NotificationReceiver service;

        ServiceStopInterrupter (NotificationReceiver service) {
            this.service = service;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(this);
            stopForeground(true);
            requestUnbind();
        }
    }
}
