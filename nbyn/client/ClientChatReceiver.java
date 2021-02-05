package lastproject.nbyn.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lastproject.nbyn.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClientChatReceiver implements Runnable {

    private BufferedReader bufferedReader;
    private String userId;
    private String roomName;

    public ClientChatReceiver(BufferedReader bufferedReader, String userId, String roomName) {
        this.bufferedReader = bufferedReader;
        this.userId = userId;
        this.roomName = roomName;
    }

    @Override
    public void run() {
        try {
            chatMessageHandler();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void chatMessageHandler() throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            String jsonMessage = bufferedReader.readLine();
            if (jsonMessage == null) { break; }

            showMessage(getMessage(jsonMessage));
        }
    }

    private Map<String, String> getMessage(String message) {
        JsonObject parseObject = (JsonObject) JsonParser.parseString(message);
        JsonArray messageArray = (JsonArray) parseObject.get(roomName);
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

    private void showMessage(Map<String, String> data) {
        if (data.get("userId").equals(userId)) { return; }    // 내가 보낸 메시지인지?

        if (!data.get("option").equals(Option.MESSAGE.toString())) {
            checkOption(Option.valueOf(data.get("option")), data.get("userName"));
        } else {
            log(data.get("userName") + " : " + data.get("message"));
            System.out.print(" >> ");
        }
    }

    private void checkOption(Option option, String name) {
        switch (option) {
            case JOIN :
                log(name + "님이 입장하셨습니다.");
                System.out.print(" >> ");
                break;
            case QUIT :
                log(name + "님이 퇴장하셨습니다.");
                System.out.print(" >> ");
        }
    }

    private void log(String log) {
        System.out.println(log);
    }
}
