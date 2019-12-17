package com.example.autochatapp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

public class MinusAuction extends ApplicationThread {
    public static final String START = "마이너스경매";

    private final String helpString = "큐브는 -3 부터 -35까지 주어집니다.\n" +
            "만약, 당신이 큐브를 매입하고 싶다면, 낙찰하세요!!\n" +
            "그렇지 않다면, 칩을 하나 소비하여 패스 할 수 있습니다.\n" +
            "낙찰 받은 플레이어는 현재 큐브에 걸린 모든 칩을 받습니다.\n" +
            "낙찰 받은 큐브 중, 연속된 수가 있다면, 연속된 값 중 가장 큰 값으로 정해집니다.\n" +
            "예) -27, -13, -12, -26 을 가지고 있다면, 큐브의 총 합은 -38\n" +
            "점수는 큐브의 총합과 남은 칩의 합으로 계산됩니다.\n" +
            "게임 종료시 높은 점수를 가진 사람이 승리합니다\n" +
            "단, 절대 나오지 않는 2개의 히든 큐브가 있습니다! 주의하세요!";

    private HashMap<String, Player> players = new HashMap<>();

    public MinusAuction(SimpleNotification startCall, NotificationReceiver service) {
        super(startCall, service);
    }

    private int inputLimitTimer = 60;
    private int gameTimer;

    @Override
    public void run() {
        service.reply(startCall, "마이너스 경매가 시작되었습니다.\n" +
                NotificationReceiver.getFlag() + "참여 로 참여 할 수 있습니다.\n" +
                "(최소 4 명, 최대 6 명)\n" +
                "시작한 사람이 " + NotificationReceiver.getFlag() + "시작 으로 시작할 수 있습니다.\n" +
                "언제든지, " + NotificationReceiver.getFlag() + "? 로 도움말을 볼 수 있습니다.\n" +
                "강제 종료 하시려면, 시작한 사람이 " + NotificationReceiver.getFlag() + "종료 를 입력하세요.");


        new Thread() {
            @Override
            public void run() {
                while (inputLimitTimer < 300) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    --inputLimitTimer;
                }
            }
        }.start();

        SimpleNotification next;

        while (true) {
            if (inputLimitTimer <= 0) {
                service.reply(startCall, "입력시간이 초과되어 강제 종료되었습니다.");
                allTimerThreadStop();
                return;
            }

            if (buffer.isEmpty()) continue;
            next = buffer.poll();
            if (next == null) continue;

            if ((next.sender.equals(startCall.sender) && next.text.contains("종료"))) {
                allTimerThreadStop();
                service.reply(next, "종료되었습니다.");
                return;
            }

            if (next.text.contains("?")) {
                inputLimitTimer = 60;
                service.reply(next, helpString);
                continue;
            }

            if (next.text.contains("참여")) {
                inputLimitTimer = 60;

                if (players.containsKey(next.sender)) {
                    service.reply(next, "이미 참여했습니다!!");
                    continue;
                }

                players.put(next.sender, new Player());
                service.reply(next, next.sender + "님이 참여했습니다.\n" + returnAllPlayers(players.keySet()));

                if (players.size() == 6) {
                    service.reply(next, "최대 참여자수에 도달했습니다!!");
                    break;
                }

                continue;
            }

            if ((next.sender.equals(startCall.sender) && next.text.contains("시작"))) {
                inputLimitTimer = 60;
                if (players.size() < 4) {
                    service.reply(next, "최소 참여자를 만족하지 않아 시작할 수 없습니다.");
                    continue;
                } else {
                    service.reply(next, "방장의 권한으로 시작합니다!!");
                    break;
                }
            }

            service.reply(next, "알 수 없는 명령입니다.");
            inputLimitTimer = 60;
        }

        Vector<String> playerOrder = new Vector<>(players.keySet());;
        service.reply(startCall, "무작위로 순서를 정합니다!!");
        Collections.shuffle(playerOrder);
        service.reply(startCall, returnAllPlayers(playerOrder));
        service.reply(startCall, "10초 후에 시작합니다!!");

        gameTimer = 10;

        new Thread() {
            @Override
            public void run() {
                while (gameTimer < 300) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    --gameTimer;
                }
            }
        }.start();

        while (gameTimer <= 0) {
            if (buffer.isEmpty()) continue;
            next = buffer.poll();
            if (next == null) continue;

            if ((next.sender.equals(startCall.sender) && next.text.contains("종료"))) {
                allTimerThreadStop();
                service.reply(next, "종료되었습니다.");
                return;
            }

            if (next.text.contains("?")) {
                inputLimitTimer = 60;
                service.reply(next, helpString);
                continue;
            }

            service.reply(next, "알 수 없는 명령입니다.");
        }

        Random random = new Random();
        int hiddenA = random.nextInt(33) + 3;
        int hiddenB;
        do {
            hiddenB = random.nextInt(33) + 3;
        } while (hiddenA == hiddenB);

        Stack<Integer> remainCube = new Stack<>();
        for (int i = 3; i <= 35; ++i) {
            if (i != hiddenA && i != hiddenB)
                remainCube.add(i);
        }

        Collections.shuffle(remainCube);
        remainCube.add(0, 0);

        service.reply(startCall, "시작되었습니다!!\n히든 큐브가 결정되었습니다!!");
        inputLimitTimer = 60;

        int playerIndex = 0;

        int currentCube = remainCube.pop();
        int currentChips = 0;

        while (currentCube != 0) {
            String currentName = playerOrder.elementAt(playerIndex);
            Player currentPlayer = players.get(currentName);
            int remainChips = Objects.requireNonNull(currentPlayer).chip;

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append(currentName).append("의 차례입니다!\n")
                    .append("숫자 큐브는 (-").append(currentCube).append(") 입니다.\n")
                    .append("배팅된 칩은 ").append(currentChips).append("개 입니다\n")
                    .append("남은 칩 : ").append(remainChips).append("\n")
                    .append("현재 점수 : ").append(currentPlayer.score()).append("\n")
                    .append("가진 큐브 : ");

            for (int c : currentPlayer.cubes) {
                stringBuilder.append(-c).append(" ");
            }

            stringBuilder.append("\n").append(NotificationReceiver.getFlag()).append("낙찰\n")
                    .append(NotificationReceiver.getFlag()).append("패스\n")
                    .append(NotificationReceiver.getFlag()).append("게임판\n")
                    .append("\n20초 안에 선택하세요!!");

            service.reply(startCall, stringBuilder.toString());

            gameTimer = 20;

            boolean alert1 = false;
            boolean alert2 = false;

            while (true) {
                if (inputLimitTimer <= 0) {
                    service.reply(startCall, "입력시간이 초과되어 강제 종료되었습니다.");
                    allTimerThreadStop();
                    return;
                }

                if (gameTimer <= 10 && !alert1) {
                    service.reply(startCall, "제한시간 10초 남았습니다!");
                    alert1 = true;
                    continue;
                }

                if (gameTimer <= 5 && !alert2) {
                    service.reply(startCall, "제한시간 5초 남았습니다!");
                    alert2 = true;
                    continue;
                }

                if (gameTimer <= 0) {
                    currentPlayer.cubes.add(currentCube);
                    currentPlayer.chip += currentChips;
                    service.reply(startCall, "제한시간이 지나서 자동으로 낙찰되었습니다!!\n" +
                            "현재 점수 : " + currentPlayer.score() +
                            "\n배팅된 칩 : " + currentChips);

                    currentChips = 0;
                    currentCube = remainCube.pop();
                    break;
                }

                if (buffer.isEmpty()) continue;
                next = buffer.poll();
                if (next == null) continue;

                if ((next.sender.equals(startCall.sender) && next.text.contains("종료"))) {
                    service.reply(next, "종료되었습니다.");
                    allTimerThreadStop();
                    return;
                }

                if (next.text.contains("게임판")) {
                    inputLimitTimer = 60;

                    service.reply(next, playersStatus(playerOrder));
                    continue;
                }

                if (!next.sender.equals(currentName)) {
                    inputLimitTimer = 60;
                    service.reply(next, "아직 " + currentName + "님의 차례입니다!");
                    continue;
                }

                if (next.text.contains("낙찰")) {
                    inputLimitTimer = 60;
                    currentPlayer.cubes.add(currentCube);
                    currentPlayer.chip += currentChips;
                    service.reply(next, "(-" + currentCube + ")을(를) 낙찰받았습니다!!\n" +
                            "현재 점수 : " + currentPlayer.score() +
                            "\n배팅된 칩 : " + currentChips);

                    currentChips = 0;
                    currentCube = remainCube.pop();
                    break;
                }

                if (next.text.contains("패스")) {
                    inputLimitTimer = 60;

                    if (remainChips < 1) {
                        service.reply(next, "남은 칩이 없어서 패스 할 수 없습니다!!");
                        continue;
                    } else {
                        Objects.requireNonNull(currentPlayer).chip -= 1;
                        service.reply(next, "칩을 사용해 패스했습니다!!");
                        ++currentChips;
                        ++playerIndex;

                        if (playerIndex >= playerOrder.size())
                            playerIndex = 0;
                        break;
                    }
                }

                inputLimitTimer = 60;
                service.reply(next, "알 수 없는 명령입니다.");
            }
        }

        int max = -987654321;
        String winner = "";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("게임이 종료되었습니다!!\n")
                .append("히든 큐브 : ")
                .append(-hiddenA)
                .append(", ")
                .append(-hiddenB)
                .append("게임 결과 :\n");

        for (String name : playerOrder) {
            int currentScore = Objects.requireNonNull(players.get(name)).score();
            if (currentScore > max) {
                max = currentScore;
                winner = name;
            }

            stringBuilder.append(name)
                    .append(" : ")
                    .append(currentScore)
                    .append("점\n");
        }

        stringBuilder.append("승자는 ")
                .append(winner)
                .append("님 입니다!!!");

        service.reply(startCall, stringBuilder.toString());
        allTimerThreadStop();
    }

    private String playersStatus(Collection<String> order) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("현재 상황:");

        for (String playerName : order) {
            Player player = Objects.requireNonNull(players.get(playerName));

            stringBuilder.append('\n')
                    .append(playerName)
                    .append("\n")
                    .append("가진 큐브 : ");

            for (int c : player.cubes)
                stringBuilder.append(-c).append(" ");

            stringBuilder.append("\n가진 칩 : ")
                    .append(player.chip)
                    .append('\n');
        }

        return stringBuilder.toString();
    }

    private String returnAllPlayers(Collection<String> order) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("현재 참여자:\n");

        for (String player : order) {
            stringBuilder.append(player).append("님\n");
        }

        return stringBuilder.toString();
    }

    private void allTimerThreadStop() {
        inputLimitTimer = 10000;
        gameTimer = 10000;
    }

    final class Player {
        int chip = 9;
        Vector<Integer> cubes = new Vector<>();

        int score() {
            if (cubes.size() <= 0)
                return chip;

            int result = -cubes.get(0);
            Collections.sort(cubes);

            for (int i = 1; i < cubes.size(); ++i) {
                if ((cubes.get(i) - cubes.get(i - 1)) == 1)
                    continue;

                result -= cubes.get(i);
            }

            result += chip;

            return result;
        }
    }
}
