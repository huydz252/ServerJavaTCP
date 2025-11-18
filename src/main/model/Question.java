package main.model;

import java.util.List;

public class Question {
    int id;
    String questionText;
    List<String> options; 
    int correctAnswerIndex;
    int quizId;
    
    public int getId() {
        return id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public int getQuizId() {
        return quizId;
    }
}
