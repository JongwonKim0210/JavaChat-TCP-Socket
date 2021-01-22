package lastproject.nbyn.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class ChatRoom {

    private List<PrintWriter> writers = new ArrayList<>();

    // test / String : 방 이름 / List : 해당 방의 방송처리를 위한 송신기목록
    private Map<String, List<PrintWriter>> chatRoomList = new HashMap<>();

    public final String chatRoomName;

    public ChatRoom(ServerSocket serverSocket, String chatRoomName) {
        this.chatRoomName = chatRoomName;
        multiConnectProcess(serverSocket);
    }

    private void multiConnectProcess(ServerSocket serverSocket) {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                // socket.getPort() 는 접속한 클라이언트의 포트번호를 반환(int 타입)
                System.out.println(chatRoomName + "에 접속한 인원이 있습니다.");
//                new Thread(new ChatMessageProcessor(socket, writers)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
