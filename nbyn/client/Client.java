package lastproject.nbyn.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private Socket socket;
    private String name;

    public void clientRun() {
        try (Scanner scanner = new Scanner(System.in)) {
            clientInit(scanner);
            connectChatRoom(scanner);
            chatProcess(scanner);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeAllConnection();
        }
    }

    // 닉네임을 입력
    private void clientInit(Scanner scanner) {
        System.out.print("채팅방에서 사용하실 이름을 입력해주세요 >> ");
        name = "[" + scanner.nextLine() + "]";
    }
    
    /*
     * 채팅서버 접근 시도 / 성공 시 해당 서버와 reader, writer 생성
     * 이후 채팅서버에 채팅방 이름으로 접근 시도
     **/
    private void connectChatRoom(Scanner scanner) {
        try {
            socket = new Socket("localhost", 9999); // 채팅서버 포트는 9999로 고정
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            selectRoom(scanner);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectRoom(Scanner scanner) throws IOException {
        while (true) {
            String receiveMessage = bufferedReader.readLine();
            if (receiveMessage.equals("연결완료")) { break; }

            if (receiveMessage.equals("없음")) {
                System.out.println("입력하신 채팅방이 없습니다.");
                continue;
            }

            System.out.println(receiveMessage);
            System.out.println("방을 만드시려는 경우 '방만들기:(만들고 싶은 채팅방의 이름)'으로 입력해주세요");
            String roomName = scanner.nextLine();
            printWriter.println(roomName);
        }
    }

    private void chatProcess(Scanner scanner) {
        sendMessage("join:" + name);        // 입장 통보
        // 스레드를 통해 서버의 방송메시지를 클라이언트에 전시
        new Thread(new ClientReceiver(name, bufferedReader)).start();
        while (true) {
            String message = scanner.nextLine();
            if (message.equals("나가기")) {        // 퇴장 통보
                sendMessage("quit:" + name);
                break;
            }

            sendMessage(name + " : " + message);
        }
    }
    
    // 보낼메시지를 받아서 발송처리
    private void sendMessage(String message) {
        printWriter.println(message);
        printWriter.flush();
    }

    // 클라이언트 종료 시 최종적으로 I/O 관련 종료 처리
    private void closeAllConnection() {
        try {
            if (socket != null) { socket.close(); }
            if (bufferedReader != null) { bufferedReader.close(); }
            if (printWriter != null) { printWriter.close(); }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Client().clientRun();
    }
}
