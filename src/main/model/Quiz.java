package main.model;

import java.util.List;

public class Quiz {
	
    private int id;
    private String title;
    private String subject;
    private List<Question> questions;
    private int timeLimit;


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
    
    public int getTimeLimit() { return timeLimit; }
    
    @Override
    public String toString() {
        return this.title + " (" + this.subject + ") " +"TG: "+ timeLimit + " phút)"; 
    }
}