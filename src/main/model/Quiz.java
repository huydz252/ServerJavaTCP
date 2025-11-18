package main.model;

import java.util.List;

public class Quiz {
	
    private int id;
    private String title;
    private String subject;
    private List<Question> questions;



    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public List<Question> getQuestions() {
        return questions;
    }
}