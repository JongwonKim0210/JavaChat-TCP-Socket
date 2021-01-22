package lastproject.nbyn.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

// 주된 역할 : 클라이언트들의 메시지를 방송처리합니다.
public class ChatMessageProcessor implements Runnable {

    private BufferedReader bufferedReader;
    private PrintWriter printWriter;
    private final Socket socket;
    private final List<PrintWriter> writers;
    private final String roomName;

    public ChatMessageProcessor(Socket socket, List<PrintWriter> writers, BufferedReader bufferedReader, PrintWriter printWriter, String name) {
        this.socket = socket;
        this.writers = writers;
        this.bufferedReader = bufferedReader;
        this.printWriter = printWriter;
        roomName = name;
    }

    // FIXME : username 중복허용 해결해야함... 해결되면 귓속말 기능 추가 가능!
    @Override
    public void run() {
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            messageProcessor();
        } catch (SocketException e) {   // 클라이언트 강제종료 시 예외로 캐치, 아래문자 출력 후 정상동작
            System.out.println("클라이언트 중 하나와 비정상적인 방법으로 연결이 해제되었습니다.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) { bufferedReader.close(); }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("통신 중 알수없는 오류가 발생했습니다. 시스템이 종료됩니다.");
            }
        }
    }

    private void messageProcessor() throws IOException {
        while (true) {
            String receive = bufferedReader.readLine();
            System.out.println(roomName + " / " + receive);     // 테스트용, 채팅방 구분목적
            if (receive == null) {                  // 갑작스러운 클라이언트 퇴장 / 연결 끊김 시
                System.out.println("클라이언트 중 하나가 장시간 응답이 없어 연결이 해제되었습니다.");
                break;
            }

            String[] tokens = receive.split(":");
            if (tokens[0].equals("join")) {         // 입장 시 알림
                joinClient(printWriter, tokens[1]);
                continue;
            }

            if (tokens[0].equals("quit")) {         // 퇴장 시 알림
                quitClient(printWriter, tokens[1]);
                break;
            }

            broadcastMessage(receive);              // 수신받은 메시지를 접속중인 클라이언트에게 방송처리
        }
    }

    private void joinClient(PrintWriter writer, String data) {
        synchronized (writers) {
            writers.add(writer);
            broadcastMessage(data + "님이 입장하셨습니다.");
            broadcastMessage("info:퇴장을 원하시는 경우 '나가기' 를 입력해 주시기 바랍니다.");
        }
    }

    private void quitClient(PrintWriter writer, String data) {
        synchronized (writers) {
            writers.remove(writer);
            broadcastMessage(data + "님이 퇴장하셨습니다.");
        }
    }

    private void broadcastMessage(String data) {
        synchronized (writers) {
            for (PrintWriter writer : writers) {
                writer.println(data);
            }
        }
    }
}
