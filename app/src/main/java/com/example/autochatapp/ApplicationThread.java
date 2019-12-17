package com.example.autochatapp;

import java.util.LinkedList;
import java.util.Queue;

public class ApplicationThread extends Thread {
    protected final NotificationReceiver service;
    protected final SimpleNotification startCall;
    protected Queue<SimpleNotification> buffer = new LinkedList<>();

    public ApplicationThread(SimpleNotification startCall, NotificationReceiver service) {
        this.startCall = startCall;
        this.service = service;
        start();
    }

    public void addBuffer(SimpleNotification notification) {
        buffer.add(notification);
    }

    @Override
    public void run() {
        super.run();
        service.reply(startCall, startCall.sender + "에 응답한 명령입니다.\n3초 뒤에 종료됩니다.");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        service.reply(startCall, "종료되었습니다.");
    }
}
