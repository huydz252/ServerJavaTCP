package main;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    // Cổng mà server sẽ lắng nghe
    public static final int SERVER_PORT = 9999;

    public static void main(String[] args) {
        System.out.println("ServerJavaTCP đang khởi động...");
        
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Đã khởi động. Đang lắng nghe trên cổng " + SERVER_PORT);

            // Vòng lặp vô tận để chấp nhận kết nối mới
            while (true) {
                // Chấp nhận một kết nối (hàm này sẽ "treo" cho đến khi có client kết nối)
                Socket clientSocket = serverSocket.accept();
                System.out.println("Một client mới đã kết nối: " + clientSocket.getInetAddress());

                // ---- QUAN TRỌNG: ĐA LUỒNG (Multi-threading) ----
                // Tạo một luồng (Thread) mới để xử lý riêng client này
                // Nếu không có Thread, server sẽ bị "treo" và không thể chấp nhận client thứ 2
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start(); // Chạy luồng
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
