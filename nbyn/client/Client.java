package lastproject.nbyn.client;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.Vector;

public class Client {

    private final String USER_ID = UUID.randomUUID().toString().replace("-", "");      // 사용자 고유 id
    private final Charset CHARSET = StandardCharsets.UTF_8;
    private final List<String> MY_CHAT_ROOM = new Vector<>();     // 내가 참가중인 채팅방 목록(강제종료 대비)

    private String userName;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            setName(scanner);
            makeConnect();
            selectService(scanner);
        } catch (Exception e) {
            log("채팅서버와 통신 중 연결이 끊어졌습니다.");
            e.printStackTrace();
        }

        sendMessage(makeRemoveList());
        closeAllConnect();
    }

    private void setName(Scanner scanner) {
        while (true) {
            log("사용하실 이름을 입력해주세요");
            System.out.print(" >> ");
            userName = scanner.nextLine();
            log("입력하신 이름이 " + userName + "이(가) 맞다면 1번을, 그렇지 않다면 아무키나 입력해주세요");
            System.out.print(" >> ");
            String order = scanner.nextLine();
            if (order.equals("1")) { break; }
        }
    }

    private void makeConnect() throws IOException {
        socket = new Socket("localhost", 9999);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), CHARSET), true);
    }

    private void selectService(Scanner scanner) throws IOException {
        log("원하시는 서비스를 선택해주세요");
        while (true) {  // 1 : 채팅방 목록, 2 : 채팅방 생성, 3 : 채팅방 입장, 4 : 종료
            int order = getNumber(scanner);
            scanner.nextLine();
            if (order == 4) {
                log("이용해주셔서 감사합니다.");
                break;
            }

            selectOther(scanner, order);
        }
    }

    private void selectOther(Scanner scanner, int order) throws IOException {
        switch (order) {
            case 1 :    // 방 목록요청
                requestRoomList();
                break;
            case 2 :    // 방 개설요청
                makeChatRoom(scanner);
                break;
            case 3 :    // 방 입장요청
                enterChatRoom(scanner);
                break;
            default:
                log("서비스를 정확히 선택해주세요");
        }
    }

    private void requestRoomList() throws IOException {
        sendMessage(makeJson("userId", USER_ID, "userName", userName, "option", Option.REQUEST_ROOM_LIST.toString()));     // 예상되는 반환값 : success / 방 목록
        Map<String, String> message = messageHandler();
        if (message != null && message.get("option").equals(Option.SUCCESS.toString())) {
            log(message.get("message"));
        }
    }

    private void makeChatRoom(Scanner scanner) throws IOException {
        log("원하시는 채팅방의 이름을 입력해주세요 단, ', ' 는 사용할 수 없습니다. / 되돌아가시려면 엔터를 눌러주세요");
        System.out.print(" >> ");
        String roomName = scanner.nextLine().replace(", ", ""); // 방 목록 구분자(, )는 없에주는 처리
        if (roomName.equals("")) { return; }

        sendMessage(makeJson("userId", USER_ID, "userName", userName, "option", Option.CREATE_ROOM.toString(), "message", roomName));      // 예상되는 반환값 : success / 방 목록
        Map<String, String> message = messageHandler();
        if (message != null && message.get("option").equals(Option.SUCCESS.toString())) {
            log(roomName + "이(가) 생성되었습니다.");
            log(message.get("message"));   // 방 목록 전시
            return;
        }

        log("채팅방을 개설하지 못했습니다.");
    }

    private void enterChatRoom(Scanner scanner) throws IOException {
        log("원하시는 채팅방의 이름을 입력해주세요 / 되돌아가시려면 엔터를 눌러주세요");
        System.out.print(" >> ");
        String roomName = scanner.nextLine();
        if (roomName.equals("")) { return; }

        sendMessage(makeJson("userId", USER_ID, "userName", userName, "option", Option.ENTER_ROOM.toString(), "message", roomName));    // 예상되는 반환값 : success, false
        Map<String, String> message = messageHandler();
        if (message != null && message.get("option").equals(Option.SUCCESS.toString())) {
            MY_CHAT_ROOM.add(roomName);
            new ClientChatView(MY_CHAT_ROOM, USER_ID, userName, roomName, reader, writer, scanner).run();
            return;
        }

        log("입력하신 채팅방이 존재하지 않거나 입장되어 있습니다.");
    }

    // 이하, 클라이언트 서비스 메서드 모음
    private Map<String, String> messageHandler() throws IOException {
        String temp;
        try {
            temp = reader.readLine();
        } catch (IOException e) {
            log("통신 중 오류가 발생했습니다.");
            throw new IOException();
        }

        return getOption(temp);
    }

    private Map<String, String> getOption(String message) {
        JsonObject parseObject = (JsonObject) JsonParser.parseString(message);
        JsonArray messageArray = (JsonArray) parseObject.get("client");
        if (messageArray == null) { return null; }

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

    private String makeJson(String ... data) {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < data.length; i += 2) {
            JsonObject jsonInfo = new JsonObject();
            jsonInfo.addProperty(data[i], data[i + 1]);
            jsonArray.add(jsonInfo);
        }

        jsonObject.add("service", jsonArray);

        return jsonObject.toString();
    }

    private void sendMessage(String data) {
        writer.println(data);
    }

    private int getNumber(Scanner scanner) {
        while (true) {
            try {
                log("1 : 채팅방 목록, 2 : 채팅방 생성, 3 : 채팅방 입장, 4 : 종료");
                System.out.print(" >> ");
                return scanner.nextInt();
            } catch (InputMismatchException e) {
                log("숫자만 입력해주세요");
                scanner.nextLine();
            }
        }
    }

    private String makeRemoveList() {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        String[] jsonProperty = {"userId", "userName", "option"};
        String[] jsonValue = {USER_ID, userName, Option.REMOVE.toString()};
        for (int i = 0; i < jsonProperty.length; i++) { // 송신자 기본정보
            JsonObject jsonInfo = new JsonObject();
            jsonInfo.addProperty(jsonProperty[i], jsonValue[i]);
            jsonArray.add(jsonInfo);
        }

        for (int i = 0; i < MY_CHAT_ROOM.size(); i++) { // 송신자가 가입한 채팅방 목록
            JsonObject jsonInfo = new JsonObject();
            jsonInfo.addProperty("roomName" + i, MY_CHAT_ROOM.get(i));
            jsonArray.add(jsonInfo);
        }

        jsonObject.add("service", jsonArray);
        return jsonObject.toString();
    }

    private void log(String log) {
        System.out.println(log);
    }

    private void closeAllConnect() {
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }
}
