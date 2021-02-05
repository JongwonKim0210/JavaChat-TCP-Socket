package lastproject.nbyn.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket;

    public void run() throws IOException {
            makeServerSocket();
            listen();
    }

    private void makeServerSocket() throws IOException {
        serverSocket = new ServerSocket(9999);
        RoomManager.isSetChatRoom("test"); // 테스트용 채팅방 생성
    }

    private void listen() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(new ServiceProvider(socket)).start();
            } catch (IOException e) {
                System.out.println("클라이언트와 연결 중 오류 발생");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server().run();
    }
}
