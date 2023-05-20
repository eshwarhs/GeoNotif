package com.geonotif.geonotif;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class FriendsFragment extends Fragment {


    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private RecyclerView mRecyclerView;
    private static FriendsRecyclerView.OnButtonClickListener mListener;

    static FirebaseStorage storage;
    static StorageReference storageRef;

    private static ArrayList<FriendsData> all_users;
    private static ArrayList<FriendsData> friends;
    private static FriendsRecyclerView adapter_friends;

    private static FriendsRecyclerView adapter_all_users;

    private static FriendsData dataStore;

    static FirebaseUser firebaseUser;
    FirebaseAuth mAuth;

    private static ArrayList<String> userIds;

    private static ArrayList<String> friendsIds;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        all_users = new ArrayList<>();
        friends = new ArrayList<>();

        userIds = new ArrayList<>();
        friendsIds = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
    }


    /*
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current tab position
        outState.putInt("tab_position", mTabLayout.getSelectedTabPosition());

        // Save the list of friends
        outState.putParcelableArrayList("fall_users", all_users);

        outState.putParcelableArrayList("friends", friends);
    }
     */


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        mTabLayout = view.findViewById(R.id.tab_layout);
        mViewPager = view.findViewById(R.id.view_pager);

        /*
        // Check if the saved instance state is not null
        if (savedInstanceState != null) {
            // Restore the tab position
            int tabPosition = savedInstanceState.getInt("tab_position");
            mTabLayout.getTabAt(tabPosition).select();
            all_users = savedInstanceState.getParcelableArrayList("all_users");
            friends = savedInstanceState.getParcelableArrayList("friends");
            return view;
        }
         */

        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(pagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        return view;
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        private final String[] tabTitles = {"All Users", "Friends"};

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return tabTitles.length;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new Tab1Fragment();
                case 1:
                    return new Tab2Fragment();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }

    }

    public static class Tab1Fragment extends Fragment {

        private RecyclerView recyclerView;
        //private  FriendsRecyclerView adapter_all_users;

        FriendsRecyclerView.OnButtonClickListener listener = new FriendsRecyclerView.OnButtonClickListener() {
            @Override
            public void onButtonClickChange(int position) {
                //friends.set(position, "New Data"); // Update the data source

                FriendsData datapoint = all_users.get(position);

                if (datapoint.getButtonDetails().equalsIgnoreCase("Unfriend")) {
                    datapoint.setButtonDetails("Add Friend");
                    friends.remove(datapoint);
                    friendsIds.remove(datapoint.getUserID());

                    //remove from database (user's friends) too.
                    String userId = firebaseUser.getUid();
                    DatabaseReference userFriendsRef = FirebaseDatabase.getInstance().getReference(
                            "GeoNotif/Users/" + userId + "/Friends");
                    userFriendsRef.get().addOnCompleteListener(userFriends -> {
                        if (!userFriends.isSuccessful()) {
                            Log.e("firebase", "Error getting data", userFriends.getException());
                        } else {
                            for (DataSnapshot childSnapshot : userFriends.getResult().getChildren()) {
                                //get the user IFD
                                String userID = childSnapshot.getValue(String.class);
                                //Log.d("UserID", userID);
                                if (userID.equals(datapoint.getUserID())) {
                                    //Log.d("I came here", userID);
                                    childSnapshot.getRef().removeValue(); // delete the child node
                                    break;
                                }
                            }

                        }
                    });

                    userFriendsRef = FirebaseDatabase.getInstance().getReference(
                            "GeoNotif/Users/" + datapoint.getUserID() + "/Friends");
                    userFriendsRef.get().addOnCompleteListener(userFriends -> {
                        if (!userFriends.isSuccessful()) {
                            Log.e("firebase", "Error getting data", userFriends.getException());
                        } else {
                            for (DataSnapshot childSnapshot : userFriends.getResult().getChildren()) {
                                //get the user IFD
                                String userID = childSnapshot.getValue(String.class);
                                //Log.d("UserID", userID);
                                if (userID.equals(firebaseUser.getUid())) {
                                    //Log.d("I came here", userID);
                                    childSnapshot.getRef().removeValue(); // delete the child node
                                    break;
                                }
                            }

                        }
                    });
                } else {
                    datapoint.setButtonDetails("Unfriend");
                    if (!friendsIds.contains(datapoint.getUserID())) {
                        friends.add(datapoint);
                        friendsIds.add(datapoint.getUserID());
                        //add to database (user's friends)  too.
                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + firebaseUser.getUid() + "/" + "Friends");
                        String newFriendKey = usersRef.push().getKey();
                        //String newFriendKey = "uid";
                        usersRef.child(newFriendKey).setValue(datapoint.getUserID());

                        usersRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + datapoint.getUserID() + "/" + "Friends");
                        newFriendKey = usersRef.push().getKey();
                        usersRef.child(newFriendKey).setValue(firebaseUser.getUid());
                    }


                }
                adapter_friends.notifyDataSetChanged(); // Notify the adapter that the data has changed
                adapter_all_users.notifyDataSetChanged(); // Notify the adapter that the data has changed

            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_fragment_tab1, container, false);

            /*
            // Check if the saved instance state is not null
            if (savedInstanceState != null) {
                all_users = savedInstanceState.getParcelableArrayList("all_users");
                adapter_all_users.notifyDataSetChanged();
                return view;
            }
             */

            recyclerView = view.findViewById(R.id.recycler_view);
            adapter_all_users = new FriendsRecyclerView(all_users, listener);
            recyclerView.setAdapter(adapter_all_users);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            storage = FirebaseStorage.getInstance();
            storageRef = storage.getReference();

            //get values from database
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/");
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Loop through all child nodes of "users"
                    Log.d("Me:", firebaseUser.getUid());

                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String uid = childSnapshot.child("uid").getValue(String.class);
                        Log.d("uid:", uid);
                        if (!uid.equals(firebaseUser.getUid())) {
                            String emailID = childSnapshot.child("emailId").getValue(String.class);
                            String fullname = childSnapshot.child("fullname").getValue(String.class);
                            String username = childSnapshot.child("username").getValue(String.class);
                            StorageReference pathReference = storageRef.child("profileImages/" + uid + "/profile.jpg");
                            dataStore = new FriendsData(emailID, fullname, uid, username, pathReference);
                            if (!userIds.contains(dataStore.getUserID())) {
                                all_users.add(dataStore);
                                userIds.add(dataStore.getUserID());
                                adapter_all_users.notifyDataSetChanged();
                            }

                        }
                    }


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle errors her
                    Log.d("FirebaseError", databaseError.getMessage());

                }
            });


            return view;
        }
    }

    public static class Tab2Fragment extends Fragment {

        private RecyclerView recyclerView;
        //private  FriendsRecyclerView adapter_friends;

        FriendsRecyclerView.OnButtonClickListener listener = new FriendsRecyclerView.OnButtonClickListener() {
            @Override
            public void onButtonClickChange(int position) {
                //friends.set(position, "New Data"); // Update the data source
                FriendsData data = friends.get(position);

                for (int i = 0; i < all_users.size(); i++) {
                    if (data.getUserName().equals(all_users.get(i).getUserName())) {
                        all_users.get(i).setButtonDetails("Add Friend");
                        adapter_all_users.notifyDataSetChanged(); // Notify the adapter that the data has changed
                    }
                }
                friends.remove(data);
                friendsIds.remove(data.getUserID());
                adapter_friends.notifyDataSetChanged(); // Notify the adapter that the data has changed

                String userId = firebaseUser.getUid();
                DatabaseReference userFriendsRef = FirebaseDatabase.getInstance().getReference(
                        "GeoNotif/Users/" + userId + "/Friends");
                userFriendsRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> userFriends) {
                        if (!userFriends.isSuccessful()) {
                            Log.e("firebase", "Error getting data", userFriends.getException());
                        } else {
                            for (DataSnapshot childSnapshot : userFriends.getResult().getChildren()) {
                                //get the user IFD
                                String userID = childSnapshot.getValue(String.class);
                                //Log.d("UserID", userID);
                                if (userID.equals(data.getUserID())) {
                                    //Log.d("I came here", userID);
                                    childSnapshot.getRef().removeValue(); // delete the child node
                                    break;
                                }
                            }

                        }
                    }
                });

                userFriendsRef = FirebaseDatabase.getInstance().getReference(
                        "GeoNotif/Users/" + data.getUserID() + "/Friends");
                userFriendsRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DataSnapshot> userFriends) {
                        if (!userFriends.isSuccessful()) {
                            Log.e("firebase", "Error getting data", userFriends.getException());
                        } else {
                            for (DataSnapshot childSnapshot : userFriends.getResult().getChildren()) {
                                //get the user IFD
                                String userID = childSnapshot.getValue(String.class);
                                //Log.d("UserID", userID);
                                if (userID.equals(firebaseUser.getUid())) {
                                    //Log.d("I came here", userID);
                                    childSnapshot.getRef().removeValue(); // delete the child node
                                    break;
                                }
                            }

                        }
                    }
                });

            }
        };


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.activity_fragment_tab2, container, false);
            setRetainInstance(true);

            /*
            // Check if the saved instance state is not null
            if (savedInstanceState != null) {
                friends = savedInstanceState.getParcelableArrayList("friends");
                adapter_friends.notifyDataSetChanged();
                return view;
            }
             */

            recyclerView = view.findViewById(R.id.recycler_view);
            adapter_friends = new FriendsRecyclerView(friends, listener);
            recyclerView.setAdapter(adapter_friends);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

            String userId = firebaseUser.getUid();
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

                            //add that person to friends
                            for (int i = 0; i < all_users.size(); i++) {
                                if (userID.equals(all_users.get(i).getUserID())) {
                                    all_users.get(i).setButtonDetails("Unfriend");
                                    if (!friendsIds.contains(all_users.get(i).getUserID())) {
                                        friends.add(all_users.get(i));
                                        friendsIds.add(all_users.get(i).getUserID());
                                        adapter_all_users.notifyDataSetChanged(); // Notify the adapter that the data has changed
                                        adapter_friends.notifyDataSetChanged(); // Notify the adapter that the data has changed
                                        break;
                                    }
                                }
                            }
                        }

                    }
                }
            });


            return view;
        }
    }


}
