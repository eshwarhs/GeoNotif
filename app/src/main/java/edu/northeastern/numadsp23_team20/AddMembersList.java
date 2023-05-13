package edu.northeastern.numadsp23_team20;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AddMembersList extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private AddMemberAdapter memberAdapter;
    private static ArrayList<User> memberList;
    private String groupID;
    private String user_id;
    private String groupName;
    private Integer groupParticipantsNo;
    private ArrayList<String> groupParticipants;
    private String currentuser;
    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    private DatabaseReference ref;
    private final String TAG = "AddMembersList";
    private static AddMemberAdapter.OnButtonClickListener listener;
    private Button cancelBttn;
    private Button doneBttn;
    static User dataStore;
    static FirebaseUser firebaseUser;
    private static List<String> friendsUI;

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_members_list);
        //getting current user
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
        String userId = firebaseUser.getUid();

        Intent intent = getIntent();
        groupID = intent.getStringExtra("groupUUID");
        String groupName = intent.getStringExtra("groupName");
        groupParticipants = intent.getStringArrayListExtra("groupParticipants");
        groupParticipantsNo = intent.getIntExtra("groupParticipantsNo", 1);
        cancelBttn = findViewById(R.id.cancelbttn);
        if (groupParticipantsNo < 2) {
            cancelBttn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            cancelBttn.setOnClickListener(v -> {
                finish();
            });
        }

        doneBttn = findViewById(R.id.donebttn);
        if (groupParticipantsNo < 2 || groupParticipantsNo == 2) {
            doneBttn.setOnClickListener(v -> {
                finish();
            });
        } else {
            doneBttn.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("groupUUID", groupID);
                bundle.putString("groupName", groupName);
                bundle.putStringArrayList("groupParticipants", groupParticipants);
                bundle.putInt("groupParticipantsNo", groupParticipantsNo);
                GroupTasksFragment grouptasksFragment = new GroupTasksFragment();
                grouptasksFragment.setArguments(bundle);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.FrameLayout3, grouptasksFragment);
                transaction.addToBackStack(null);
                if (!getSupportFragmentManager().isStateSaved()) { // Add check for isStateSaved()
                    transaction.commit();
                }
            });
        }
        searchView = findViewById(R.id.searchView);
        searchView.clearFocus();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });
        recyclerView = findViewById(R.id.members_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        memberList = new ArrayList<>();

        listener = position -> {
            User datapoint = memberList.get(position);
            if (datapoint.getButtonDetails().equals("Add")) {
                datapoint.setButtonDetails("Added");
                //get the uid of the user
                user_id = datapoint.getUid();
                //add the user uid to the groupParticipants of group
                DatabaseReference groupParticipantsRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" +
                        groupID + "/groupParticipants");
                DatabaseReference groupParticipantsNoRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Groups/" +
                        groupID).getRef().child("groupParticipantsNo");
                // add the user UID to the group participants list
                // set the value of the next available integer key to the new participant ID
                // add the new participant ID to the groupParticipants list
                groupParticipantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean userExists = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (Objects.equals(snapshot.getValue(), user_id)) {
                                userExists = true;
                                break;
                            }
                        }
                        if (!userExists) {
                            // get the current number of participants in the group
                            int numParticipants = (int) dataSnapshot.getChildrenCount();
                            groupParticipants.add(user_id);

                            // set the value of the next available integer key to the new participant ID
                            groupParticipantsRef.child(String.valueOf(numParticipants)).setValue(user_id)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // update the UI and notify the adapter that the data has changed
                                            memberList.get(position).setButtonDetails("Added");
                                            groupParticipantsNoRef.setValue(numParticipants + 1);
                                            memberList.remove(position);
                                            memberAdapter.notifyDataSetChanged();
                                            Toast.makeText(getApplicationContext(), "User Added", Toast.LENGTH_SHORT).show();

                                            // filter out the added users from the memberList and set the adapter
                                            ArrayList<User> filteredList = new ArrayList<>();
                                            for (User user : memberList) {
                                                if (!groupParticipants.contains(user.getUid())) {
                                                    filteredList.add(user);
                                                }
                                            }
                                            memberAdapter.setList(filteredList);
                                            memberAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // handle the failure to add the user to the group participants list
                                            Toast.makeText(getApplicationContext(), "Failed to add user", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                            for (String participantUUID : groupParticipants) {
                                if (!userId.equals(participantUUID)) {
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
                                            if (!groupUUIDs.contains(groupID)) { // check if group ID already exists in the list
                                                groupUUIDs.add(groupID);
                                                userGroupsRef.setValue(groupUUIDs);
                                            }
                                        }
                                    });
                                }
                            }
                        } else {
                            // handle the case where the user already exists in the group
                            Toast.makeText(getApplicationContext(), "User already exists in the group", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // handle the error
                        Toast.makeText(getApplicationContext(), "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            memberAdapter.notifyDataSetChanged(); // Notify the adapter that the data has changed
        };
        memberAdapter = new AddMemberAdapter(memberList, listener);
        recyclerView.setAdapter(memberAdapter);

        //fetch friends user IDs.
        friendsUI = new ArrayList<String>();

        DatabaseReference userFriendsRef = FirebaseDatabase.getInstance().getReference(
                "GeoNotif/Users/" + userId + "/Friends");
        userFriendsRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> userFriends) {
                if (!userFriends.isSuccessful()) {
                    Log.d("firebase", "Error getting data", userFriends.getException());
                } else {
                    for (DataSnapshot childSnapshot : userFriends.getResult().getChildren()) {
                        //get the user IFD
                        String userID = childSnapshot.getValue(String.class);
                        friendsUI.add(userID);
                        Log.d("friends", String.valueOf(friendsUI.size()));
                    }

                }
            }
        });
        //fetch details of the userIDs in friends array
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/");
        usersRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> userFriends) {
                if (!userFriends.isSuccessful()) {
                    Log.d("firebase", "Error getting data", userFriends.getException());
                } else {
                    for (DataSnapshot childSnapshot : userFriends.getResult().getChildren()) {
                        String uid = childSnapshot.child("uid").getValue(String.class);

                        for (int i = 0; i < friendsUI.size(); i++) {
                            if (uid != null && uid.equals(friendsUI.get(i))) {
                                String emailID = childSnapshot.child("emailId").getValue(String.class);
                                String fullname = childSnapshot.child("fullname").getValue(String.class);
                                String username = childSnapshot.child("username").getValue(String.class);
                                dataStore = new User(fullname, username, emailID, uid);
                                memberList.add(dataStore);
                                Log.d("memberList", String.valueOf(memberList.size()));
                                break;
                            }
                        }
                    }
                    memberAdapter.notifyDataSetChanged(); // Notify the adapter that the data has changed

                }
            }

        });
    }

    private void filterList(String newText) {
        ArrayList<User> filteredList = new ArrayList<>();
        for (User user : memberList) {
            if (user.getUsername().toLowerCase().contains(newText.toLowerCase())
                    && !groupParticipants.contains(user.getUid())) {
                filteredList.add(user);
            }
        }
        memberList.clear();
        memberList.addAll(filteredList);
        memberAdapter.notifyDataSetChanged();
    }

//    public void filterList(String text) {
//        ArrayList<User> filteredList = new ArrayList<>();
//        for (User user : memberList) {
//            if (user.getUsername().toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))) {
//                filteredList.add(user);
//            }
//        }
//
//        if (filteredList.isEmpty()) {
////            Toast.makeText(this, "No data found", Toast.LENGTH_SHORT).show();
//        } else {
//            memberAdapter.setFilteredList(filteredList);
//        }
//    }

}