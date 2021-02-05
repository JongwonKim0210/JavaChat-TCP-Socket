package lastproject.nbyn.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lastproject.nbyn.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ServiceProvider implements Runnable {

    private final Charset CHARSET = StandardCharsets.UTF_8;
    private final Socket socket;

    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private Thread messageSender;
    private BroadcastService broadcast;

    public ServiceProvider(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            makeSocketStream();
            makeBroadcast();
            messageHandler();
            System.out.println("클라이언트와 연결 종료 / Port : " + socket.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }

        closeAllConnect();
    }

    private void makeSocketStream() throws IOException {
        System.out.println("클라이언트와 연결 됨 / Port : " + socket.getPort());
        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET));
        printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET), true);
        socket.setKeepAlive(true);
    }

    private void makeBroadcast() {
        broadcast = new BroadcastService();
        messageSender = new Thread(broadcast);
        messageSender.start();
    }

    private void messageHandler() throws IOException {
        while (socket.getKeepAlive()) {
            String jsonMessage = bufferedReader.readLine();
            if (jsonMessage == null) {
                throw new IOException("클라이언트와 연결이 해제되었습니다.");
            }

            System.out.println(jsonMessage);   // 서버단 로그(메시지 유통 확인)
            checkMessage(jsonMessage);
        }
    }

    private void checkMessage(String message) {  // 메시지 분류
        if (isMessage(message)) {
            broadcast.enQueue(message);
        } else {
            checkOption(getOption(message));
        }
    }

    private boolean isMessage(String message) {
        return parseMessage(message).get("service") == null;
    }

    private void checkOption(Map<String, String> message) {
        switch (Option.valueOf(message.get("option"))) {
            case REMOVE:    // 클라이언트 종료
                removeUser(message);
                killSocket();
                break;
            case REQUEST_ROOM_LIST:  // 방 목록요청
                sendMessage(makeJson("client", "option", Option.SUCCESS.toString(), "message", getRoomList()));
                break;
            case CREATE_ROOM: // 방 개설 요청
                createChatRoom(message);
                break;
            case ENTER_ROOM:  // 방 입장 요청
                enterChatRoom(message);
                break;
            case QUIT:
                quitChatRoom(message);
        }
    }

    private void quitChatRoom(Map<String, String> message) {
        String roomName = message.get("message");
        String[] keys = message.keySet().toArray(new String[0]);
        String[] keyValues = new String[keys.length * 2];

        for (int i = 0, j = 0; i < keys.length; i++, j += 2) {
            keyValues[j] = keys[i];
            keyValues[j + 1] = message.get(keys[i]);
        }

        if (RoomManager.isRemoveUser(roomName, message.get("userId"))) {
            broadcast.enQueue(makeJson(roomName, keyValues));
            sendMessage(makeJson("client", "option", Option.SUCCESS.toString()));
        }
    }

    private void removeUser(Map<String, String> message) {  // 한번만 실행되는 메서드
        broadcast.clear();           // 메시지 큐 비우기 -> 인터럽트로 에코서비스 종료
        messageSender.interrupt();
        for (int i = 0; i < (message.size() - 3); i++) {    // 전달받은 채팅방 목록 순회
            Map<String, PrintWriter> tempUserList = RoomManager.getUserList(message.get("roomName" + i));
            tempUserList.remove(message.get("userId"));    // 채팅방에서 날 삭제
            for (PrintWriter writer : tempUserList.values()) {  // 접속 유저들 대상 퇴장메시지 송신
                writer.println(makeJson(message.get("roomName" + i),
                                              "userName", message.get("userName"), "option", Option.QUIT.toString()));
            }
        }
    }

    private void enterChatRoom(Map<String, String> message) {
        if (RoomManager.isSetUser(message.get("message"), message.get("userId"), printWriter)) {
            sendMessage(makeJson("client", "option", Option.SUCCESS.toString()));
        } else {
            sendMessage(makeJson("client", "option", Option.FALSE.toString()));
        }
    }

    private void createChatRoom(Map<String, String> message) {
        if (RoomManager.isSetChatRoom(message.get("message"))) {  // put 성공 시 true, 실패 시 false
            sendMessage(makeJson("client", "option", Option.SUCCESS.toString(), "message", getRoomList()));
            System.out.println("새 채팅방 생성 : " + message.get("message")); // 서버단 로그
        } else {
            sendMessage(makeJson("client", "option", "false"));
        }
    }

    private void killSocket() {
        try {
            socket.setKeepAlive(false);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> getOption(String message) {
        JsonArray messageArray = (JsonArray) parseMessage(message).get("service");
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < messageArray.size(); i++) {
            JsonObject jsonData = (JsonObject) messageArray.get(i);
            for (String key : jsonData.keySet()) {
                result.put(key, jsonData.get(key).toString()
                                        .replace("\"", "")
                                        .replace("\\", "\""));
            }
        }

        return result;
    }

    private JsonObject parseMessage(String message) {
        return  (JsonObject) JsonParser.parseString(message);
    }

    private String getRoomList() {
        return RoomManager.getRoomList().toString();
    }

    private String makeJson(String destination, String ... data) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < data.length; i += 2) {
            JsonObject jsonInfo = new JsonObject();
            jsonInfo.addProperty(data[i], data[i + 1]);
            jsonArray.add(jsonInfo);
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add(destination, jsonArray);
        return jsonObject.toString();
    }

    private void sendMessage(String data) {
        printWriter.println(data);
    }

    private void closeAllConnect() {
        try {
            if (printWriter != null) { printWriter.close(); }
            if (bufferedReader != null) { bufferedReader.close(); }
            if (socket != null) { socket.close(); }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
