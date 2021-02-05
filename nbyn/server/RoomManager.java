package lastproject.nbyn.server;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    private static final Map<String, Map<String, PrintWriter>> ROOMS = new ConcurrentHashMap<>();

    public static Map<String, PrintWriter> getUserList(String roomName) {
        return ROOMS.get(roomName);
    }

    public static Set<String> getRoomList() {
        return ROOMS.keySet();
    }

    public static boolean isRemoveUser(String roomName, String uuid) {
        return getUserList(roomName).remove(uuid) != null;
    }

    public static boolean isSetUser(String roomName, String uuid, PrintWriter printWriter) {
        Map<String, PrintWriter> temp = getUserList(roomName);
        if (temp == null) {
            return false;
        } else {
            return temp.putIfAbsent(uuid, printWriter) == null;
        }
    }

    public static boolean isSetChatRoom(String roomName) {
        return (ROOMS.putIfAbsent(roomName, new ConcurrentHashMap<>()) == null);
    }
}