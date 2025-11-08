package main;

import java.net.Socket;

public class ClientHandler extends Thread {
 
 private Socket clientSocket;

 public ClientHandler(Socket socket) {
     this.clientSocket = socket;
 }

 @Override
 public void run() {
     try {
         System.out.println("Đang xử lý client: " + clientSocket.getInetAddress());

         // (Bước tiếp theo: Chúng ta sẽ thêm BufferedReader/PrintWriter
         //  để đọc/ghi tin nhắn JSON ở đây)

     } catch (Exception e) {
         System.out.println("Client " + clientSocket.getInetAddress() + " đã ngắt kết nối.");
     } finally {
    	 
     }
 }
}
