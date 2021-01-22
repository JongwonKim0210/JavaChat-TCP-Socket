package lastproject.nbyn.client;

import java.io.BufferedReader;
import java.io.IOException;

public class ClientReceiver implements Runnable {

    private String name;
    private BufferedReader bufferedReader;

    public ClientReceiver(String name, BufferedReader bufferedReader) {
        this.name = name;
        this.bufferedReader = bufferedReader;
    }

    // 클라이언트가 채팅방 이탈 시 BufferedReader 소켓닫힘 예외발생, 이 예외를 이용해서 이 스레드를 정상종료 시킵니다.
    @Override
    public void run() {
        try {
            processingMessage();
        } catch (IOException e) {
            System.out.println("연결을 종료합니다.");   // 예외 catch, 이 문구 출력(socket Exception...)
        } finally {
            try {
                bufferedReader.close();             // BufferedReader 종료 후 스레드 완전종료(추가예외 없음)
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processingMessage() throws IOException {
        while (true) {
            String receive = bufferedReader.readLine();     // 클라이언트 종료 시 이 줄에서 예외발생
            String[] tokens = receive.split(":");
            if (tokens[0].equals(name + " ") || tokens[0].equals("info")) { continue; }
            System.out.println(receive);
        }
    }
}
