package main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.google.gson.Gson; // Import Gson
import com.google.gson.JsonSyntaxException; // Import lỗi

public class ClientHandler extends Thread {
    
    private Socket clientSocket;
    private PrintWriter writer; // Đối tượng để GHI tin nhắn ra
    private BufferedReader reader; // Đối tượng để ĐỌC tin nhắn vào
    
    private Gson gson = new Gson(); // Đối tượng Gson của riêng luồng này
    private ConnectionManager manager = ConnectionManager.getInstance(); // Lấy "tổng đài"

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // 1. Khởi tạo Reader và Writer (Rất quan trọng)
            // Lấy luồng output (để GHI)
            this.writer = new PrintWriter(clientSocket.getOutputStream(), true); // 'true' = autoFlush
            // Lấy luồng input (để ĐỌC)
            this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Đang xử lý client: " + clientSocket.getInetAddress());

            // 2. Vòng lặp đọc tin nhắn
            String jsonString;
            while ((jsonString = reader.readLine()) != null) {
                System.out.println("Nhận được JSON: " + jsonString);
                
                // 3. Chuyển JSON (String) sang JsonMessage (Object)
                try {
                    JsonMessage message = gson.fromJson(jsonString, JsonMessage.class);
                    processMessage(message); // Xử lý tin nhắn
                } catch (JsonSyntaxException e) {
                    System.out.println("Lỗi JSON không hợp lệ từ client.");
                }
            }
        } catch (Exception e) {
            // Thường là lỗi client ngắt kết nối đột ngột (SocketException)
            System.out.println("Client " + clientSocket.getInetAddress() + " đã ngắt kết nối.");
        } finally {
            // 4. Dọn dẹp
            manager.removeClient(this); // Báo cho "tổng đài" biết client này đã off
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null) clientSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Hàm phân loại và xử lý tin nhắn
     */
    private void processMessage(JsonMessage message) {
        if (message == null || message.type == null) {
            System.out.println("Tin nhắn không hợp lệ.");
            return;
        }

        // Dùng switch-case để phân loại
        switch (message.type) {
            case "REGISTER_AGENT":
                // Lấy tên máy từ payload
                String machineName = (String) message.payload.get("machineName");
                if (machineName != null) {
                    manager.registerAgent(machineName, this);
                }
                break;
                
            case "REGISTER_ADMIN":
                manager.registerAdmin(this);
                break;
            
            // (Chúng ta sẽ thêm các case khác như "CMD_LOCK_MACHINE" ở đây)
            
            default:
                System.out.println("Nhận được type tin nhắn không xác định: " + message.type);
        }
    }

    // --- Các hàm hỗ trợ ---

    // Các lớp khác cần 2 hàm này để giao tiếp
    public PrintWriter getWriter() {
        return writer;
    }
    
    public Socket getClientSocket() {
        return clientSocket;
    }
}
