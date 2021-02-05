package lastproject.nbyn.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lastproject.nbyn.Option;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

public class ClientChatView implements Runnable {

    private List<String> enterChatRooms;
    private String userId;
    private String userName;
    private String roomName;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread receiver;
    private Scanner scanner;

    public ClientChatView(List<String> enterChatRooms, String userId, String userName, String roomName, BufferedReader reader, PrintWriter writer, Scanner scanner) {
        this.enterChatRooms = enterChatRooms;
        this.userId = userId;
        this.userName = userName;
        this.roomName = roomName;
        this.reader = reader;
        this.writer = writer;
        this.scanner = scanner; // 안받아도 되는 객체
    }

    @Override
    public void run() {
        try {
            System.out.println(roomName + "에 입장하셨습니다.");
            System.out.println("나가시려면 '나가기' 를(을) 입력해주세요");
            sender(makeJson(roomName, userId, userName, Option.JOIN.toString(), null));
            makeChatReceiver(reader, userId, roomName);
            chatting(scanner);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 이 채팅방으로 들어오는 메시지수신기 초기화 / 이 스레드가 종료되면 채팅방도 자동종료(메시지 전시 안되는 시점에서 오류)
    private void makeChatReceiver(BufferedReader reader, String userId, String roomName) {
        receiver = new Thread(new ClientChatReceiver(reader, userId, roomName), "receiver");
        receiver.start();
    }

    private void chatting(Scanner scanner) {
        String message;
        while (true) {
            System.out.print(" >> ");
            message = scanner.nextLine();
            if (message.equals("나가기") || !receiver.isAlive()) {    // readLine() 정상동작 불가 시 receiver interrupt 발생
                quit();
                break;
            }

            sender(makeJson(roomName, userId, userName, Option.MESSAGE.toString(), message));
        }
    }

    private void quit() {
        if (receiver.isAlive()) { receiver.interrupt(); }

        sender(makeJson("service", userId, userName, Option.QUIT.toString(), roomName));
        enterChatRooms.remove(roomName);
    }

    private void sender(String data) {
        writer.println(data);
    }

    private String makeJson(String roomName, String ... data) {
        String[] jsonProperty = {"userId", "userName", "option", "message"};
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < jsonProperty.length; i++) {
            JsonObject jsonInfo = new JsonObject();
            jsonInfo.addProperty(jsonProperty[i], data[i]);
            jsonArray.add(jsonInfo);
        }

        jsonObject.add(roomName, jsonArray);

        return jsonObject.toString();
    }
}
