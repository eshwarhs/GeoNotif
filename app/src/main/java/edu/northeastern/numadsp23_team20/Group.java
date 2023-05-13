package edu.northeastern.numadsp23_team20;

import java.util.ArrayList;
import java.util.UUID;

public class Group {

    private String uuid;
    private String groupName;
    private ArrayList<String> groupParticipants;
    private Integer groupParticipantsNo;

    public Group() {
    }

    public Group(String groupName, ArrayList<String> groupParticipants) {
        this.groupName = groupName;
        this.groupParticipantsNo = groupParticipants.size();
        this.groupParticipants = groupParticipants;
        this.uuid = String.valueOf(UUID.randomUUID());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public ArrayList<String> getGroupParticipants() {
        return groupParticipants;
    }

    public void setGroupParticipants(ArrayList<String> groupParticipants) {
        this.groupParticipants = groupParticipants;
    }

    public Integer getGroupParticipantsNo() {
        if (groupParticipants != null) {
            return groupParticipants.size();
        }
        return 1;
    }

    @Override
    public String toString() {
        return "Group [groupName =" + getGroupName() + ", groupParticipantsNo=" + getGroupParticipantsNo()
                + ", groupParticipants()=" + getGroupParticipants()
                + ", groupId=" + getUuid() + "]";
    }
}
