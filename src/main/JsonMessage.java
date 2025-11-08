package main;

import java.util.Map;

public class JsonMessage {
    
    String type;
    Map<String, Object> payload;

    // Constructor 
    public JsonMessage(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
    }
    
}