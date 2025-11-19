package main.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList; 

import com.google.gson.Gson;

import main.model.JsonMessage;
import main.model.Quiz; 

public class ConnectionManager {
    private static ConnectionManager instance;
    private ConnectionManager() {}
    private String currentExamClassName = null;

    public String getCurrentExamClassName() {
		return currentExamClassName;
	}

	public void setCurrentExamClassName(String currentExamClassName) {
		this.currentExamClassName = currentExamClassName;
	}
	
    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    private Map<String, ClientHandler> agentMap = new ConcurrentHashMap<>();
    
    private List<ClientHandler> adminList = new CopyOnWriteArrayList<>();

    private Gson gson = new Gson(); 


    /**
     * Một Client Agent mới báo danh.
     */
    public void registerAgent(String machineName, ClientHandler handler) {
        agentMap.put(machineName, handler);
        System.out.println("Đăng ký Agent: " + machineName);
        System.out.println("--------------------------------------");
        broadcastAgentListToAdmins();
    }

    /**
     * Một Admin (Giám thị) mới báo danh.
     */
    public void registerAdmin(ClientHandler handler) {
        adminList.add(handler);
        System.out.println("Đăng ký Admin: " + handler.getClientSocket().getInetAddress());
        sendAgentListToAdmin(handler);
    }

    /**
     * Khi một client (Admin hoặc Agent) ngắt kết nối.
     */
    public void removeClient(ClientHandler handler) {
        if (adminList.remove(handler)) {
            System.out.println("Admin ngắt kết nối: " + handler.getClientSocket().getInetAddress());
            System.out.println("--------------------------------------");
        } else {
            System.out.println("Một client đã ngắt kết nối.");
            System.out.println("--------------------------------------");
        }
        broadcastAgentListToAdmins();
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
    public void sendAgentListToAdmin(ClientHandler admin) {
        
        agentMap.entrySet().removeIf(entry -> {
            ClientHandler handler = entry.getValue();
            if (handler == null || handler.getClientSocket().isClosed() || !handler.isAlive()) {
                System.out.println("Phát hiện Agent 'ma': " + entry.getKey() + ". Đang xóa...");
                return true; 
            }
            return false; 
        });

        List<String> agentNames = List.copyOf(agentMap.keySet());
        
        Map<String, Object> payload = Map.of("agents", agentNames);
        JsonMessage agentListMessage = new JsonMessage("DATA_AGENT_LIST", payload);
        sendMessage(admin, agentListMessage);
    }
    
    /**
     * Gửi lệnh khóa máy đến một Agent cụ thể
     */
    public void sendLockCommand(String machineName) {
        ClientHandler agentHandler = agentMap.get(machineName);
        
        if (agentHandler != null) {
            JsonMessage lockMsg = new JsonMessage("SERVER_CMD_LOCK", null);
            System.out.println("--------------------------------------");
            sendMessage(agentHandler, lockMsg);
            System.out.println("Đã gửi lệnh KHÓA MÁY đến: " + machineName);
        } else {
            System.out.println("Không tìm thấy Agent: " + machineName + " để khóa.");
            System.out.println("--------------------------------------");
        }
    }
    
    /**
     * Gửi lệnh MỞ khóa máy đến một Agent cụ thể
     */
    public void sendUnlockCommand(String machineName) {
        ClientHandler agentHandler = agentMap.get(machineName);
        
        if (agentHandler != null) {
            JsonMessage unlockMsg = new JsonMessage("SERVER_CMD_UNLOCK", null);
            sendMessage(agentHandler, unlockMsg);
            System.out.println("Đã gửi lệnh MỞ KHÓA MÁY đến: " + machineName);
            System.out.println("--------------------------------------");
        } else {
            System.out.println("Không tìm thấy Agent: " + machineName + " để mở khóa.");
            System.out.println("--------------------------------------");
        }
    }
    
    /**
     * Gửi lệnh YÊU CẦU CẤU HÌNH đến một Agent cụ thể
     */
    public void sendGetConfigCommand(String machineName) {
        ClientHandler agentHandler = agentMap.get(machineName);
        
        if (agentHandler != null) {
            JsonMessage configRequestMsg = new JsonMessage("SERVER_CMD_GET_CONFIG", null);
            sendMessage(agentHandler, configRequestMsg);
            System.out.println("--------------------------------------");
            System.out.println("SERVER nhận \"SERVER_CMD_GET_CONFIG\" -> Đã gửi lệnh YÊU CẦU CONFIG đến: " + machineName);
        } else {
        	System.out.println("--------------------------------------");
            System.out.println("SERVER nhận \"SERVER_CMD_GET_CONFIG\" ->Không tìm thấy Agent: " + machineName + " để lấy config.");
        }
    }
    
    /**
     * Gửi lệnh YÊU CẦU TIẾN TRÌNH đến một Agent cụ thể
     */
    public void sendGetProcessesCommand(String machineName) {
        ClientHandler agentHandler = agentMap.get(machineName);
        
        if (agentHandler != null) {
            JsonMessage procRequestMsg = new JsonMessage("SERVER_CMD_GET_PROCESSES", null);
            sendMessage(agentHandler, procRequestMsg);
            System.out.println("Đã gửi lệnh YÊU CẦU PROCESSES đến: " + machineName);
            System.out.println("--------------------------------------");
        } else {
            System.out.println("Không tìm thấy Agent: " + machineName + " để lấy processes.");
            System.out.println("--------------------------------------");
        }
    }


	public void broadcastToAgents(JsonMessage message) {
		String jsonMsg = gson.toJson(message);
		
		for (ClientHandler agent : agentMap.values()) {
			if(agent != null) {
				sendMessage(agent, message);
			}
		}
	}
	
	/**
     * Gửi danh sách bộ đề cho Admin
     */
    public void sendQuizListToAdmin(ClientHandler adminHandler, List<Quiz> quizzes) {
        // Gói danh sách vào payload
        Map<String, Object> payload = Map.of("quizzes", quizzes);
        JsonMessage msg = new JsonMessage("DATA_QUIZ_LIST", payload);
        sendMessage(adminHandler, msg);
    }
}
