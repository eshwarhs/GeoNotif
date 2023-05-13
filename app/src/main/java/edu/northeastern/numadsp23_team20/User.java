package edu.northeastern.numadsp23_team20;

import java.util.ArrayList;
import java.util.List;

public class User {
    public String fullname, username, emailId, uid;
    private String buttonDetails = "Add";

    public int getAssignableTasks() {
        return assignableTasks;
    }

    public void setAssignableTasks(int assignableTasks) {
        this.assignableTasks = assignableTasks;
    }

    private int assignableTasks;

    public String getButtonDetails() {
        return buttonDetails;
    }

    public void setButtonDetails(String buttonDetails) {
        this.buttonDetails = buttonDetails;
    }

    public List<String> getTasks() {
        return tasks;
    }

    public void setTasks(List<String> tasks) {
        this.tasks = tasks;
    }

    public List<String> tasks;

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }



    public User() {
    }

    public User(String username) {
        this.username = username;
    }

    public User(String fullname, String username, String emailId, String uid) {
        this.fullname = fullname;
        this.username = username;
        this.emailId = emailId;
        this.uid = uid;
        this.assignableTasks = 1;
    }

    @Override
    public String toString() {
        return "User [Fullname=" + getFullname() + ", username=" + getUsername()
                + ", email=" + getEmailId() + ", uid=" + getUid() + "]";
    }
}
