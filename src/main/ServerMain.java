package main;

import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static final int SERVER_PORT = 9999;

    public static void main(String[] args) {
        System.out.println("ServerJavaTCP đang khởi động...");
        
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Đã khởi động. Đang lắng nghe trên cổng " + SERVER_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Một client mới đã kết nối: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start(); 
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
