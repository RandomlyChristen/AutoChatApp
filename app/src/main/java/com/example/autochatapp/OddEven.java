package com.example.autochatapp;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OddEven extends ApplicationThread {
    public static final String START = "홀짝";

    private HashMap<String, Integer> scoreBoard = new HashMap<>();
    private int timer = 30;
    private Random random = new Random();
    private Pattern pattern = Pattern.compile("[^홀짝]*([홀짝])[ ]([0-9]+)");

    public OddEven(SimpleNotification startCall, NotificationReceiver service) {
        super(startCall, service);
    }

    @Override
    public void run() {
        service.reply(startCall, "홀짝 게임이 시작되었습니다.\n" +
                NotificationReceiver.getFlag() + "홀 또는 " + NotificationReceiver.getFlag() + "짝과 함께 배당금을 말해주세요.\n" +
                "예) "+ NotificationReceiver.getFlag() + "홀 5500\n" +
                "첫 자금은 10000 포인트 입니다 (배당, 1.9배).\n"
                + NotificationReceiver.getFlag() + "점수 를 입력하면 점수창을 볼 수 있습니다.\n" +
                "시작한 사람이 " + NotificationReceiver.getFlag() + "종료를 입력하면 종료합니다."
        );
        SimpleNotification next;

        new Thread() {
            @Override
            public void run() {
                while (timer > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    --timer;
                }
            }
        }.start();

        while (true) {
            if (timer < 1) {
                service.reply(startCall, "입력이 30초 이상 지연되어 종료되었습니다.");
                showScoreBoard(startCall);
                return;
            }

            if (buffer.isEmpty()) continue;
            next = buffer.poll();
            if (next == null) continue;

            if ((next.sender.equals(startCall.sender) && next.text.contains("종료"))) {
                showScoreBoard(next);
                service.reply(next, "종료되었습니다.");
                return;
            }

            if (next.text.contains("점수")) {
                showScoreBoard(next);
                timer = 30;
                continue;
            }

            Matcher matcher = pattern.matcher(next.text);

            if (matcher.find(0)) {
                String oddEven = matcher.group(1);
                int money = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));

                tryOnce(next, oddEven, money);
                timer = 30;
                continue;
            }

            service.reply(next, "잘못된 입력입니다.");
            timer = 30;
        }
    }

    private void showScoreBoard(SimpleNotification call) {
        if (scoreBoard.isEmpty()) return;

        StringBuilder msg = new StringBuilder();
        int max = 0;
        String winner = null;

        for (Map.Entry<String, Integer> entry : scoreBoard.entrySet()) {
            msg.append(entry.getKey()).append(" : ").append(entry.getValue()).append("포인트\n");

            if (entry.getValue() > max) {
                max = entry.getValue();
                winner = entry.getKey();
            }
        }

        if (winner != null)
            msg.append("1등, ").append(winner).append("님!!");

        service.reply(call, msg.toString());
    }

    private void tryOnce(SimpleNotification call, String oddEven, int money) {
        if (!scoreBoard.containsKey(call.sender))
            scoreBoard.put(call.sender, 10000);

        int currentScore = Objects.requireNonNull(scoreBoard.get(call.sender));

        if (money <= 0) {
            service.reply(call, "0이하의 포인트는 배팅할 수 없습니다!!"); return;
        }

        if (Objects.requireNonNull((currentScore - money) < 0)) {
            service.reply(call, "포인트가 부족합니다!!"); return;
        }

        currentScore -= money;

        int rand = random.nextInt(10) + 1;
        StringBuilder msg = new StringBuilder();
        msg.append("짤랑짤랑~~~\n").append(rand).append("입니다!!\n");

        if ((oddEven.equals("홀") && (rand % 2 != 0)) || (oddEven.equals("짝") && (rand % 2 == 0))) {
            currentScore += (int) ((double)money * 1.9);

            msg.append(call.sender).append("님 정답!!\n\n현재 포인트 : ").append(currentScore);
        } else {
            msg.append(call.sender).append("님 실패..\n\n현재 포인트 : ").append(currentScore);
        }

        service.reply(call, msg.toString());
        scoreBoard.put(call.sender, currentScore);
    }
}
