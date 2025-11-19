package main.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson; // Import Gson
import com.google.gson.JsonSyntaxException; // Import lỗi
import com.google.gson.reflect.TypeToken;

import main.model.JsonMessage;
import main.model.Question;
import main.model.Quiz;
import main.service.ApiClient;

public class ClientHandler extends Thread {
    
    private Socket clientSocket;
    
    private PrintWriter writer; 
    private BufferedReader reader; 
    
    private Gson gson = new Gson(); 
    private ConnectionManager manager = ConnectionManager.getInstance(); 
    
    private String agentMachineName = null;
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    
    public PrintWriter getWriter() {
        return writer;
    }
    
    public Socket getClientSocket() {
        return clientSocket;
    }


	@Override
    public void run() {
        try {

            this.writer = new PrintWriter(clientSocket.getOutputStream(), true); // 'true' = autoFlush
            this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("Đang xử lý client: " + clientSocket.getInetAddress());
            System.out.println("");

            // 2. Vòng lặp đọc tin nhắn
            String jsonString;
            while ((jsonString = reader.readLine()) != null) {
                System.out.println("Nhận được JSON: " + jsonString);
                
                try {
                    JsonMessage message = gson.fromJson(jsonString, JsonMessage.class);
                    processMessage(message); // Xử lý tin nhắn
                } catch (JsonSyntaxException e) {
                    System.out.println("Lỗi JSON không hợp lệ từ client.");
                }
            }
        } catch (Exception e) {
            System.out.println("Client " + clientSocket.getInetAddress() + " đã ngắt kết nối.");
        } finally {
            manager.removeClient(this); 
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
        if (message == null || message.getType() == null) {
            System.out.println("Tin nhắn không hợp lệ.");
            return;
        }

        switch (message.getType()) {
            case "REGISTER_AGENT":
                String machineName = (String) message.getPayload().get("machineName");
                if (machineName != null) {
                	this.agentMachineName = machineName;
                    manager.registerAgent(machineName, this);
                }
                break;
                
            case "REGISTER_ADMIN":
                manager.registerAdmin(this);
                break;
            case "CMD_GET_AGENT_LIST":
                manager.sendAgentListToAdmin(this);
                break;
            
            case "CMD_LOCK_MACHINE":
                String targetMachine = (String) message.getPayload().get("targetMachine");
                if (targetMachine != null) {
                    manager.sendLockCommand(targetMachine);
                }
                break;
                
            case "CMD_UNLOCK_MACHINE":
                String targetToUnlock = (String) message.getPayload().get("targetMachine");
                if (targetToUnlock != null) {
                    manager.sendUnlockCommand(targetToUnlock);
                }
                break;
                
            case "CMD_GET_CONFIG":
                String targetConfig = (String) message.getPayload().get("targetMachine");
                if (targetConfig != null) {
                    manager.sendGetConfigCommand(targetConfig);
                }
                break;
                
            case "DATA_CONFIG":
                System.out.println("Nhận được DATA_CONFIG. Đang broadcast cho Admins...");
                manager.broadcastToAdmins(message); 
                break;
                
            case "CMD_GET_PROCESSES":
                String targetMachine1 = (String) message.getPayload().get("targetMachine");
                if (targetMachine1 != null) {
                    manager.sendGetProcessesCommand(targetMachine1);
                }
                break;
                
            case "DATA_PROCESS_LIST":
                System.out.println("Nhận được DATA_PROCESS_LIST. Đang broadcast cho Admins...");
                manager.broadcastToAdmins(message);
                break;
                
            case "ALERT_PROCESS_VIOLATION":
                System.out.println("!!! CẢNH BÁO VI PHẠM: " + message.getPayload());
                manager.broadcastToAdmins(message);
                break;
            case "CMD_GET_QUIZ_LIST":
                List<Quiz> quizzes = ApiClient.getInstance().getAllQuizzes();
                manager.sendQuizListToAdmin(this, quizzes);
                break;
            case "CMD_START_QUIZ":
                try {
                	
                    Double quizIdDouble = (Double) message.getPayload().get("quizID");
                    String className = (String) message.getPayload().get("className"); 
                    
                    if (className != null) {
                        manager.setCurrentExamClassName(className);
                        System.out.println("Đã lưu tên lớp vào hệ thống: " + className);
                    } else {
                        System.out.println("Cảnh báo: Admin không gửi tên lớp!");
                    }                    //System.out.println("check className: " + currentExamClassName);
                    int quizId = quizIdDouble.intValue();
                    
                    System.out.println("Nhận lệnh BẮT ĐẦU THI cho bộ đề: " + quizId);
                    
                    Quiz quizData = ApiClient.getInstance().getQuizData(quizId);	//lấy data cho quiz

                    if (quizData != null) {
                    	
                        System.out.println("Lấy dữ liệu thành công. Gửi bài thi cho Agents...");

                        //dịch map -> quiz
                        Map<String, Object> payload = Map.of("quizData", quizData);
                        JsonMessage quizMsg = new JsonMessage("SERVER_CMD_START_QUIZ", payload);

                        manager.broadcastToAgents(quizMsg);

                    } else {
                        System.out.println("Lỗi: Không lấy được dữ liệu bài thi từ ApiClient.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case "SUBMIT_QUIZ":
            	System.out.println("Nhận được SUBMIT_QUIZ từ: " + agentMachineName);
                handleQuizSubmission(message.getPayload(), manager.getCurrentExamClassName());
            	break;
                
            default:
                System.out.println("Nhận được type tin nhắn không xác định: " + message.getType());
        }
    }

	private void handleQuizSubmission(Map<String, Object> payload, String className) {
		try {
			System.out.println("check className "+ className);
            int quizId = ((Double) payload.get("quizId")).intValue();
            List<Double> userAnswersDouble = (List<Double>) payload.get("answers");
            
            Quiz quiz = ApiClient.getInstance().getQuizData(quizId);
            if (className == null) {
                System.out.println("Lỗi chấm điểm: Không có tên lớp (className) được lưu.");
                className = "underfined"; 
            }
            if (quiz == null) {
                System.out.println("Lỗi chấm điểm: Không tìm thấy bộ đề " + quizId);
                return;
            }
            
            List<Question> questions = quiz.getQuestions();
            
            //Chấm điểm
            int correctCount = 0;
            for (int i = 0; i < questions.size(); i++) {
                if (i >= userAnswersDouble.size()) break; 

                int userAnswer = userAnswersDouble.get(i).intValue();
                int correctAnswer = questions.get(i).getCorrectAnswerIndex();
                
                if (userAnswer == correctAnswer) {
                    correctCount++;
                }
            }
            
            double score = (double) correctCount / questions.size() * 10.0;
            
            System.out.println("Máy " + agentMachineName + " đạt " + correctCount + "/" + questions.size() + " - Điểm: " + score);
            
            ApiClient.getInstance().postExamResult(quizId, agentMachineName, score, className);
            
            // có thể gửi 1 tin nhắn về Client báo là đã nộp bài)
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi nghiêm trọng khi chấm điểm cho: " + agentMachineName);
        }
	}

}
