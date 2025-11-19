package main.service; 

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import main.model.Quiz; 

/**
 * Lớp Singleton để gọi API từ ServerDBNodeJs 
 */
public class ApiClient {

    private static ApiClient instance;
    private HttpClient httpClient;
    private Gson gson;
    
    private final String BASE_URL = "http://localhost:3000/api";

    private ApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * Lấy toàn bộ dữ liệu (câu hỏi, đáp án) của một bộ đề
     */
    public Quiz getQuizData(int quizId) {
        try {
            System.out.println("[ApiClient] Đang gọi API để lấy Quiz ID: " + quizId);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/quizzes/" + quizId))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonBody = response.body();
                System.out.println("[ApiClient] Nhận được JSON: " + jsonBody.substring(0, Math.min(jsonBody.length(), 100)) + "..."); // In 100 ký tự đầu
                
                Quiz quiz = gson.fromJson(jsonBody, Quiz.class);
                return quiz;
            } else {
                System.out.println("[ApiClient] Lỗi khi gọi API: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Gửi kết quả (điểm) lên ServerDBNodeJs (Project 1)
     */
    public boolean postExamResult(int quizId, String machineName, double score, String className) {
        try {
            System.out.println("[ApiClient] Đang POST kết quả: " + machineName + " - " + score + " - " + className);

            // 1. Tạo JSON body
            String jsonBody = gson.toJson(Map.of(
                "quizId", quizId,
                "studentMachineName", machineName,
                "score", score,
                "className", className
            ));

            // 2. Tạo POST request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/results"))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonBody))
                    .build();

            // 3. Gửi
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // 4. Kiểm tra (201 = Created)
            return response.statusCode() == 201;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy danh sách tất cả bộ đề (chỉ cần ID và Title)
     */
    public List<Quiz> getAllQuizzes() {
        try {
            System.out.println("[ApiClient] Đang lấy danh sách bộ đề...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/quizzes"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonBody = response.body();
                // Dùng TypeToken để dịch List<Quiz>
                java.lang.reflect.Type listType = new TypeToken<List<Quiz>>(){}.getType();
                List<Quiz> quizzes = gson.fromJson(jsonBody, listType);
                return quizzes;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(); // Trả về rỗng nếu lỗi
    }
    
    
    /**
     * HÀM MAIN ĐỂ TEST (Chạy thử file này)
     * Đảm bảo Project 1 (ServerDBNodeJs) đang chạy!
     */
    public static void main(String[] args) {
        ApiClient client = ApiClient.getInstance();
        
        Quiz quiz = client.getQuizData(3);
        
        if (quiz != null) {
            System.out.println("TEST THÀNH CÔNG!");
            System.out.println("Tiêu đề: " + quiz.getTitle());
            if (quiz.getQuestions() != null) {
                System.out.println("Số câu hỏi: " + quiz.getQuestions().size());
                if (quiz.getQuestions().size() > 0) {
                    System.out.println("Câu 1: " + quiz.getQuestions().get(0).getQuestionText());
                }
            } else {
                System.out.println("Bộ đề này không có câu hỏi nào.");
            }
        } else {
            System.out.println("TEST THẤT BẠI!");
        }
    }
}