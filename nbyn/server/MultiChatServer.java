package lastproject.nbyn.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 주된 역할 : 채팅방을 생성, 관리합니다.
public class MultiChatServer {

    /*
     * KEY(String) : 방이름 / VALUE(List<PrintWriter>) : 채팅방(의 방송장치)
     * final : 채팅방과 관련된 목록은 단 한개만 존재해야하고, 관리되어야 함
    **/
    private final Map<String, List<PrintWriter>> roomList = new HashMap<>();
    private ServerSocket serverSocket;

    public MultiChatServer() {
        roomList.put("기본", new ArrayList<>());
    }

    // 모든 채팅방 사용자는 이메소드를 거쳐 연결된 뒤 원하는 채팅방을 찾아서 들어감
    public void multiConnectProcess() {
        try {
            serverSocket = new ServerSocket(9999);
            System.out.println("연결 대기중입니다.");
            connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect() throws IOException {
        while (true) {                              // 서버가 계속 살아있어야지 다른 클라이언트들이 접속 가능
            Socket socket = serverSocket.accept();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            selectRoom(socket, bufferedReader, printWriter);
        }
    }

    private void selectRoom(Socket socket, BufferedReader bufferedReader, PrintWriter printWriter) throws IOException {
        while (true) {
            printWriter.println("개설된 방 목록 : " + getRoomList());   // 현재 개설된 채팅방목록 전송
            String roomName = bufferedReader.readLine();
            String[] tokens = roomName.split(":");
            if (tokens[0].equals("방만들기")
                    && checkChatRoomName(tokens[1])) { continue; }    // 채팅방 중복검사 / 별도 안내없음

            if (tokens[0].equals("방만들기")) {                         // 클라이언트에 의한 방 만들기
                makeChatRoom(tokens[1]);
                continue;
            }

            List<PrintWriter> chatRoom;
            if ((chatRoom = findChatRoom(roomName)) != null) {
                printWriter.println("연결완료");                        // 실행되면 연결완료 보내기
                new Thread(new ChatMessageProcessor(socket, chatRoom, bufferedReader, printWriter, roomName))
                        .start();
                break;
            }

            printWriter.println("없음");
        }
    }

    // 이하 아래 메소드는 클라이언트들의 채팅방 입장을 처리하기 위한 각종 메소드 들...
    
    // 해당 방 이름으로 접속하는 경우 동일한 ArrayList 참조하도록...
    private List<PrintWriter> findChatRoom(String roomName) {
        return roomList.get(roomName);                      // 입장해야하는 방 찾아주기
    }

    private void makeChatRoom(String roomName) {            // 방만들기
        roomList.put(roomName, new ArrayList<>());
    }

    private String getRoomList() {                          // 채팅방 목록 보내주기
        StringBuilder list = new StringBuilder();
        for (String name : listChatRoom()) {
            list.append(name).append(" ");
        }

        return new String(list);
    }

    private boolean checkChatRoomName(String roomName) {    // 방 이름 중복검사
        for (String name : listChatRoom()) {
            if (name.equals(roomName)) { return true; }
        }

        return false;
    }

    private Set<String> listChatRoom() {                    // 채팅방 목록 반환하기
        return roomList.keySet();
    }

    public static void main(String[] args) {
        new MultiChatServer().multiConnectProcess();
    }
}
