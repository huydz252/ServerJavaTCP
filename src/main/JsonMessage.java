package main;

import java.util.Map;

//POJO (Plain Old Java Object)
//Gson sẽ tự động chuyển đổi:
//JSON (String) -> Đối tượng JsonMessage
//Đối tượng JsonMessage -> JSON (String)
public class JsonMessage {
 
 String type;
 Map<String, Object> payload;

 // (Gson cần constructor rỗng, nhưng thường nó tự xử lý)
 // (Chúng ta có thể thêm constructor và getter/setter sau nếu cần)
}
