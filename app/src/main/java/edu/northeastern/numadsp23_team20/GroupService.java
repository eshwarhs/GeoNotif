package edu.northeastern.numadsp23_team20;

import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupService {
    private FirebaseUser firebaseUser;
    private FirebaseAuth mAuth;
    private DatabaseReference ref;
    private GroupServiceListener groupServiceListener;
    private ValueEventListener valueEventListener;
    private GroupServiceTaskCreateListener groupServiceTaskCreateListener;
    private GroupServiceReadParticipantsListener groupServiceReadParticipantsListener;
    private GroupServiceDeleteListener groupServiceDeleteListener;

    public GroupService() {
        this.mAuth = FirebaseAuth.getInstance();
        this.firebaseUser = mAuth.getCurrentUser();
    }

    public void setGroupServiceListener(GroupServiceListener groupServiceListener) {
        this.groupServiceListener = groupServiceListener;
    }

    public void setGroupServiceTaskCreateListener(GroupServiceTaskCreateListener groupServiceTaskCreateListener) {
        this.groupServiceTaskCreateListener = groupServiceTaskCreateListener;
    }

    public void setGroupServiceReadParticipantsListener(GroupServiceReadParticipantsListener groupServiceReadParticipantsListener) {
        this.groupServiceReadParticipantsListener = groupServiceReadParticipantsListener;
    }

    public void setGroupServiceDeleteListener(GroupServiceDeleteListener groupServiceDeleteListener) {
        this.groupServiceDeleteListener = groupServiceDeleteListener;
    }

    public String getFirebaseUserUID() {
        return firebaseUser.getUid();
    }

    public void createGroup(Group group) {
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/"
                + group.getUuid());
        this.ref.setValue(group);
        String currentUserUUID = firebaseUser.getUid();
        group.getGroupParticipants().add(currentUserUUID);
        for (String participantUUID : group.getGroupParticipants()) {
            DatabaseReference userGroupsRef = FirebaseDatabase.getInstance().getReference(
                    "GeoNotif/Users/" + participantUUID + "/Groups");
            userGroupsRef.get().addOnCompleteListener(userGroups -> {
                if (!userGroups.isSuccessful()) {
                    Log.e("firebase", "Error getting data", userGroups.getException());
                } else {
                    List<String> groupUUIDs = (List<String>) userGroups.getResult().getValue();
                    if (groupUUIDs == null || groupUUIDs.isEmpty()) {
                        groupUUIDs = new ArrayList<>();
                    }
                    groupUUIDs.add(group.getUuid());
                    userGroupsRef.setValue(groupUUIDs);
                }
            });
        }
    }

    public void editGroupName(String groupUUID, String updatedGroupName) {
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" + groupUUID);
        this.ref.child("groupName").setValue(updatedGroupName);
    }

    private void removeUserFromGroup(String groupId, List<String> groupParticipants) {
        groupParticipants.remove(firebaseUser.getUid());
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" + groupId + "/groupParticipants");
        this.ref.setValue(groupParticipants);
        this.ref.removeEventListener(this.valueEventListener);
    }

    private void removeGroupFromUser(String groupId, List<String> groups) {
        groups.remove(groupId);
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + this.firebaseUser.getUid() + "/Groups");
        this.ref.setValue(groups);
        this.ref.removeEventListener(this.valueEventListener);
    }

    private void updateGroupParticipantsNo(String groupId) {
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" + groupId + "/groupParticipantsNo");
        myRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer value = mutableData.getValue(Integer.class);
                if (value == null || value == 1) {
                    deleteGroup(groupId);
                    return Transaction.success(mutableData);
                } else {
                    mutableData.setValue(value - 1);
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.d("Firebase", "Transaction failed: " + databaseError.getMessage());
                } else {
                }
            }
        });
    }

    private void removeTasks(Map<String, String> tasks) {
        if (tasks.size() > 0) {
            for (String taskUUID : tasks.values()) {
                this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Tasks/" + taskUUID);
                this.ref.removeValue();
            }
        }
    }

    private void deleteGroup(String groupId) {
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" + groupId + "/Tasks");

        this.valueEventListener = this.ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> tasks = new HashMap<>();
                tasks = (Map<String, String>) snapshot.getValue();
                if (tasks == null || tasks.isEmpty()) {
                    tasks = new HashMap<>();
                }
                removeTasks(tasks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" + groupId);
        this.ref.removeValue();
    }

    private void removeTaskFromUser(Map<String, String> tasks) {
        if (tasks.size() > 0) {
            String userId = this.firebaseUser.getUid();
            for (String taskUUID : tasks.values()) {
                this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Locations/"
                        + taskUUID);
                this.ref.removeValue();
                this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Tasks");
                this.valueEventListener = this.ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> taskUUIDs = new ArrayList<>();
                        taskUUIDs = (List<String>) snapshot.getValue();
                        if (taskUUIDs == null || taskUUIDs.isEmpty()) {
                            taskUUIDs = new ArrayList<>();
                        }
                        removeUserTaskList(taskUUIDs, taskUUID, userId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }

    public void leaveGroup(String groupID) {
        String userId = this.firebaseUser.getUid();
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" + groupID + "/groupParticipants");

        this.valueEventListener = this.ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> groupParticipants = new ArrayList<>();
                groupParticipants = (List<String>) snapshot.getValue();
                if (groupParticipants == null || groupParticipants.isEmpty()) {
                    groupParticipants = new ArrayList<>();
                }
                removeUserFromGroup(groupID, groupParticipants);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Groups");
        this.valueEventListener = this.ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> userGroups = new ArrayList<>();
                userGroups = (List<String>) snapshot.getValue();
                if (userGroups == null || userGroups.isEmpty()) {
                    userGroups = new ArrayList<>();
                }
                removeGroupFromUser(groupID, userGroups);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" + groupID + "/Tasks");

        this.valueEventListener = this.ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> tasks = new HashMap<>();
                tasks = (Map<String, String>) snapshot.getValue();
                if (tasks == null || tasks.isEmpty()) {
                    tasks = new HashMap<>();
                }
                removeTaskFromUser(tasks);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        updateGroupParticipantsNo(groupID);
    }


    public void addTaskToGroup(String groupUuid, Task task) {
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/"
                + groupUuid + "/Tasks");
        String newTaskKey = this.ref.push().getKey();
        this.ref.child(newTaskKey).setValue(task.getUuid());

        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/"
                + groupUuid);
        this.ref.get().addOnCompleteListener(group -> {
            if (!group.isSuccessful()) {
                Log.e("firebase", "Error getting data", group.getException());
            } else {
                DatabaseReference taskRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Tasks/" + task.getUuid());
                taskRef.setValue(task);
                for (DataSnapshot groupChildren : group.getResult().getChildren()) {
                    List<String> groupParticipants = new ArrayList<>();
                    if (groupChildren.getKey().equals("groupParticipants")) {
                        groupParticipants = (ArrayList<String>) groupChildren.getValue();
                    }
                    for (String userId : groupParticipants) {
                        DatabaseReference locationRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Locations");
                        GeoFire geoFire = new GeoFire(locationRef);
                        geoFire.setLocation(task.getUuid(), new GeoLocation(task.getLocation().getLat(), task.getLocation().getLon()));
                        DatabaseReference userTasksRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Tasks");
                        userTasksRef.get().addOnCompleteListener(tasks -> {
                            if (!tasks.isSuccessful()) {
                                Log.e("firebase", "Error getting data", tasks.getException());
                            } else {
                                List<String> taskUUIDs = (List<String>) tasks.getResult().getValue();
                                if (taskUUIDs == null || taskUUIDs.isEmpty()) {
                                    taskUUIDs = new ArrayList<>();
                                }
                                taskUUIDs.add(task.getUuid());
                                userTasksRef.setValue(taskUUIDs);
                            }
                        });
                    }
                }
                groupServiceTaskCreateListener.onTaskCreated(task.getUuid());
            }
        });
    }

    public void readGroupsForUser() {
        String userUUID = this.firebaseUser.getUid();
        DatabaseReference userGroupsRef = FirebaseDatabase.getInstance().getReference(
                "GeoNotif/Users/" + userUUID + "/Groups");
        userGroupsRef.get().addOnCompleteListener(userGroups -> {
            if (!userGroups.isSuccessful()) {
                Log.e("firebase", "Error getting data", userGroups.getException());
            } else {
                if (userGroups.getResult().getValue() != null) {
                    List<String> groupUUIDs = (List<String>) userGroups.getResult().getValue();
                    for (String groupUUID : groupUUIDs) {
                        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference(
                                "GeoNotif/Groups/" + groupUUID);
                        groupRef.get().addOnCompleteListener(group -> {
                            if (!group.isSuccessful()) {
                                Log.e("firebase", "Error getting data", group.getException());
                            } else {
                                Group g = group.getResult().getValue(Group.class);
                                groupServiceListener.onUserGroupLoaded(g);
//                            for (DataSnapshot groupDetails : group.getResult().getChildren()) {
//                                if (groupDetails.getKey().equals("groupName")) {
//                                    Group g = group.getResult().getValue(Group.class);
//                                    groupServiceListener.onUserGroupLoaded(g);
//                                }
//                            }
                            }
                        });
                    }
                }
            }
        });
    }

    public void readParticipantsForGroup(String groupUUID) {
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference(
                "GeoNotif/Groups/" + groupUUID + "/groupParticipants");
        groupRef.get().addOnCompleteListener(group -> {
            if (!group.isSuccessful()) {
                Log.e("firebase", "Error getting data", group.getException());
            } else {
                List<String> participantUUIDs = (List<String>) group.getResult().getValue();
                for (String participantUUID : participantUUIDs) {
                    DatabaseReference participantRef = FirebaseDatabase.getInstance().getReference(
                            "GeoNotif/Users/" + participantUUID);
                    participantRef.get().addOnCompleteListener(participant -> {
                        if (!participant.isSuccessful()) {
                            Log.e("firebase", "Error getting data", participant.getException());
                        } else {
                            for (DataSnapshot userDetails : participant.getResult().getChildren()) {
                                if (userDetails.getKey().equals("fullname")) {
                                    groupServiceReadParticipantsListener.onParticipantRead(userDetails.getValue().toString());
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    public void deleteGroupTask(String uuid, String groupId) {
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Tasks/" + uuid);
        this.ref.removeValue();


        this.ref = FirebaseDatabase.getInstance().getReference(
                "GeoNotif/Groups/" + groupId + "/Tasks");
        Query query = this.ref.orderByValue().equalTo(uuid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String keyToRemove = snapshot.getKey();
                    snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(
                                "GeoNotif/Groups/" + groupId + "/Tasks/" + keyToRemove);
                        ref.removeValue();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
            }
        });

        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/"
                + groupId);
        this.ref.get().addOnCompleteListener(group -> {
            if (!group.isSuccessful()) {
                Log.e("firebase", "Error getting data", group.getException());
            } else {
                for (DataSnapshot groupChildren : group.getResult().getChildren()) {
                    List<String> groupParticipants = new ArrayList<>();
                    if (groupChildren.getKey().equals("groupParticipants")) {
                        groupParticipants = (ArrayList<String>) groupChildren.getValue();
                    }
                    for (String userId : groupParticipants) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Locations/"
                                + uuid);
                        userRef.removeValue();
                        DatabaseReference userTaskref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Tasks");
                        this.valueEventListener = userTaskref.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                List<String> taskUUIDs = new ArrayList<>();
                                taskUUIDs = (List<String>) snapshot.getValue();
                                if (taskUUIDs == null || taskUUIDs.isEmpty()) {
                                    taskUUIDs = new ArrayList<>();
                                }
                                removeUserTaskList(taskUUIDs, uuid, userId);
                                groupServiceDeleteListener.onGroupTaskDeleted();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }
            }
        });
    }

    public void removeUserTaskList(List<String> tasks, String uuid, String userId) {
        tasks.remove(uuid);
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Tasks");
        this.ref.setValue(tasks);
        this.ref.removeEventListener(this.valueEventListener);
    }

    public interface GroupServiceListener {
        void onUserGroupLoaded(Group group);

        void onGroupCreated(Group group);
    }

    public interface GroupServiceTaskCreateListener {
        void onTaskCreated(String taskUUID);
    }

    public interface GroupServiceReadParticipantsListener {
        void onParticipantRead(String participant);
    }

    public interface GroupServiceDeleteListener {
        void onGroupTaskDeleted();
    }
}