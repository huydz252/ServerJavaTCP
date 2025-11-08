package main;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList; 

import com.google.gson.Gson; 

public class ConnectionManager {
    private static ConnectionManager instance;
    private ConnectionManager() {}

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    private Map<String, ClientHandler> agentMap = new ConcurrentHashMap<>();
    
    private List<ClientHandler> adminList = new CopyOnWriteArrayList<>();

    private Gson gson = new Gson(); // Đối tượng Gson để gửi tin nhắn


    /**
     * Một Client Agent mới báo danh.
     */
    public void registerAgent(String machineName, ClientHandler handler) {
        agentMap.put(machineName, handler);
        System.out.println("Đăng ký Agent: " + machineName);
        // Ngay lập tức, thông báo cho tất cả Admin biết có Agent mới
        broadcastAgentListToAdmins();
    }

    /**
     * Một Admin (Giám thị) mới báo danh.
     */
    public void registerAdmin(ClientHandler handler) {
        adminList.add(handler);
        System.out.println("Đăng ký Admin: " + handler.getClientSocket().getInetAddress());
        // Gửi cho Admin này danh sách Agent hiện tại
        sendAgentListToAdmin(handler);
    }

    /**
     * Khi một client (Admin hoặc Agent) ngắt kết nối.
     */
    public void removeClient(ClientHandler handler) {
        // Kiểm tra xem nó là Admin hay Agent
        if (adminList.remove(handler)) {
            System.out.println("Admin ngắt kết nối: " + handler.getClientSocket().getInetAddress());
        } else {
         
            System.out.println("Một client đã ngắt kết nối.");
        }
        broadcastAgentListToAdmins(); // Cập nhật lại danh sách cho Admin
    }

    /**
     * Gửi một tin nhắn JSON đến một ClientHandler cụ thể
     */
    public void sendMessage(ClientHandler handler, JsonMessage message) {
        try {
            String jsonMsg = gson.toJson(message);
            handler.getWriter().println(jsonMsg);
        } catch (Exception e) {
            System.out.println("Lỗi khi gửi tin nhắn cho: " + handler.getClientSocket().getInetAddress());
        }
    }

    /**
     * Gửi (broadcast) một tin nhắn cho TẤT CẢ Admin đang online.
     * Dùng cho việc CẢNH BÁO.
     */
    public void broadcastToAdmins(JsonMessage message) {
        String jsonMsg = gson.toJson(message);
        for (ClientHandler admin : adminList) {
            try {
                admin.getWriter().println(jsonMsg);
            } catch (Exception e) {
                System.out.println("Lỗi khi broadcast cho Admin: " + admin.getClientSocket().getInetAddress());
            }
        }
    }


    /**
     * Gửi danh sách Agent hiện tại cho TẤT CẢ Admin
     */
    private void broadcastAgentListToAdmins() {
        List<String> agentNames = List.copyOf(agentMap.keySet());

        Map<String, Object> payload = Map.of("agents", agentNames);
        JsonMessage agentListMessage = new JsonMessage("DATA_AGENT_LIST", payload);
        
        broadcastToAdmins(agentListMessage);
    }

    /**
     * Gửi danh sách Agent cho CHỈ MỘT Admin (khi họ mới kết nối)
     */
    private void sendAgentListToAdmin(ClientHandler admin) {
        List<String> agentNames = List.copyOf(agentMap.keySet());
        Map<String, Object> payload = Map.of("agents", agentNames);
        JsonMessage agentListMessage = new JsonMessage("DATA_AGENT_LIST", payload);
        sendMessage(admin, agentListMessage);
    }
}
