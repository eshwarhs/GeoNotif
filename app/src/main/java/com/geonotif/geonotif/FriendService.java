package com.geonotif.geonotif;

import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class FriendService {
    private FirebaseUser firebaseUser;
    private FirebaseAuth mAuth;
    private DatabaseReference ref;
    private GeoFire geoFire;
    private ValueEventListener valueEventListener;
    private FriendServiceReadListener friendServiceReadListener;
    private FriendServiceCreateListener friendServiceCreateListener;

    public FriendService() {
        this.mAuth = FirebaseAuth.getInstance();
        this.firebaseUser = mAuth.getCurrentUser();
    }

    public void setFriendServiceReadListener(FriendServiceReadListener friendServiceReadListener) {
        this.friendServiceReadListener = friendServiceReadListener;
    }

    public void setFriendServiceCreateListener(FriendServiceCreateListener friendServiceCreateListener) {
        this.friendServiceCreateListener = friendServiceCreateListener;
    }

    public void readUserFriends() {
        String userId = this.firebaseUser.getUid();
        DatabaseReference userFriendsRef = FirebaseDatabase.getInstance().getReference(
                "GeoNotif/Users/" + userId + "/Friends");
        userFriendsRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> userFriends) {
                if (!userFriends.isSuccessful()) {
                    Log.e("firebase", "Error getting data", userFriends.getException());
                } else {
                    if (userFriends.getResult().getValue() != null) {
                        HashMap<String, String> friendUUIDs = (HashMap<String, String>) userFriends.getResult().getValue();
                        for (String friendUUID : friendUUIDs.values()) {
                            DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference(
                                    "GeoNotif/Users/" + friendUUID);
                            friendRef.get().addOnCompleteListener(friend -> {
                                if (!friend.isSuccessful()) {
                                    Log.e("firebase", "Error getting data", friend.getException());
                                } else {
                                    User f = friend.getResult().getValue(User.class);
                                    friendServiceReadListener.onFriendLoad(f);
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    public void addUserTaskList(List<String> tasks, String taskUUID, String friendUUID) {
        tasks.add(taskUUID);
        List<String> newTasks = new ArrayList<>(new HashSet<>(tasks));
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + friendUUID + "/Tasks");
        this.ref.setValue(newTasks);
        this.ref.removeEventListener(this.valueEventListener);
    }

    public void createFriendTask(com.geonotif.geonotif.Task task, String friendUUID) {
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + friendUUID + "/Locations");
        this.geoFire = new GeoFire(this.ref);
        this.geoFire.setLocation(task.getUuid(), new GeoLocation(
                task.getLocation().getLat(), task.getLocation().getLon()));
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Tasks/" + task.getUuid());
        this.ref.setValue(task);
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + friendUUID + "/Tasks");
        this.valueEventListener = this.ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> taskUUIDs = new ArrayList<>();
                taskUUIDs = (List<String>) snapshot.getValue();
                if (taskUUIDs == null || taskUUIDs.isEmpty()) {
                    taskUUIDs = new ArrayList<>();
                }
                addUserTaskList(taskUUIDs, task.getUuid(), friendUUID);
                friendServiceCreateListener.onTaskCreated(task.getUuid());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public interface FriendServiceReadListener {
        void onFriendLoad(User friend);
    }

    public interface FriendServiceCreateListener {
        void onTaskCreated(String taskUUID);
    }
}
