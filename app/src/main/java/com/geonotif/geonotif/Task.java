package com.geonotif.geonotif;

import androidx.annotation.NonNull;

public class Task {

    private String taskName;
    private String description;
    private LocationItem location;
    private boolean isComplete;
    private String uuid;

    private String taskType;

    private String taskTypeString;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Task() {
        //default constructor
    }

    public Task(String taskName, String description, LocationItem location) {
        this.taskName = taskName;
        this.description = description;
        this.location = location;
    }

    public Task(String taskName, String description, LocationItem location, String uuid, boolean isComplete) {
        this.taskName = taskName;
        this.description = description;
        this.location = location;
        this.uuid = uuid;
        this.isComplete = isComplete;
    }

    public Task(String taskName, String description, LocationItem location, String uuid, boolean isComplete, String taskType, String taskTypeString) {
        this.taskName = taskName;
        this.description = description;
        this.location = location;
        this.uuid = uuid;
        this.isComplete = isComplete;
        this.taskType = taskType;
        this.taskTypeString = taskTypeString;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocationItem getLocation() {
        return location;
    }

    public void setLocation(LocationItem location) {
        this.location = location;
    }

    @NonNull
    @Override
    public String toString() {
        if (this.getLocation() != null)
            return "Task [taskName=" + getTaskName() + ", description=" + getDescription()
                    + ", location()=" + getLocation().toString()
                    + ", uuid=" + getUuid() + ", isComplete=" + getIsComplete() + "]";
        else
            return "Task [taskName=" + getTaskName() + ", description=" + getDescription()
                    + "]";
    }

    public boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(boolean complete) {
        isComplete = complete;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskTypeString() {
        return taskTypeString;
    }

    public void setTaskTypeString(String taskTypeString) {
        this.taskTypeString = taskTypeString;
    }
}
