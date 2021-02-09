package lastproject.nbyn.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BroadcastService implements Runnable {

    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();

    public void enQueue(String message) {
        messageQueue.offer(message);
    }

    public void clear() {
        messageQueue.clear();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String message = messageQueue.poll();
            if (message != null) { sendMessage(findRoomName(message), message); }
        }
    }

    private String findRoomName(String message) {
        String findKey = null;
        JsonObject parseObject = (JsonObject) JsonParser.parseString(message);
        for (String key : parseObject.keySet()) {
            findKey = key;
        }

        return findKey;
    }

    private void sendMessage(String roomName, String data) {
        Map<String, PrintWriter> userList = RoomManager.getUserList(roomName);
        for (PrintWriter writer : userList.values()) {
            writer.println(data);
        }
    }
}
