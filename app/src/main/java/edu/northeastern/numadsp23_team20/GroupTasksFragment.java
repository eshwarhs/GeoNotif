package edu.northeastern.numadsp23_team20;

import com.google.android.gms.tasks.OnCompleteListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.checkerframework.checker.units.qual.A;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GroupTasksFragment extends Fragment implements OnTaskItemClickListener {

    Context ctx;
    private MapView map;
    private IMapController mapController;
    private ActivityResultLauncher<Intent> addTaskActivityLaunch;
    private ActivityResultLauncher<Intent> viewTaskActivityLaunch;
    private TaskService taskService;
    private List<Task> grouptaskList;
    private boolean loadingTasks;
    private ProgressBar tasksLoadingSpinner;
    private TextView noTasksTextView;
    private ScrollView tasksScrollView;
    private GroupTasksAdapter taskListAdapter;
    private ImageButton settings;
    private String groupName;
    private ArrayList<String> groupParticipants;
    private ArrayList<String> recyclerViewParticipantList;
    private ParticipantListAdapter participantListAdapter;
    private ActivityResultLauncher<Intent> groupSettingsActivityLauncher;

    static FirebaseUser firebaseUser;
    FirebaseAuth mAuth;
    private String groupId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        firebaseUser = mAuth.getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View inflatedView = inflater.inflate(R.layout.fragment_group_tasks_new, container, false);
        assert getArguments() != null;
        this.groupId = getArguments().getString("groupUUID");
        this.groupName = getArguments().getString("groupName");
        this.groupParticipants = getArguments().getStringArrayList("groupParticipants");
        Integer groupParticipantsNo = getArguments().getInt("groupParticipantsNo");
        // Set the group name as the text of the TextView
        TextView groupNameTextView = inflatedView.findViewById(R.id.GroupNameTitle);
        groupNameTextView.setText(groupName);

        this.loadingTasks = true;
        this.ctx = getContext();
        Configuration.getInstance().load(this.ctx, PreferenceManager.getDefaultSharedPreferences(this.ctx));
//        this.map = inflatedView.findViewById(R.id.TasksMapView);
//        this.mapController = this.map.getController();
//        this.configureMap();
        RecyclerView tasksRecyclerView = inflatedView.findViewById(R.id.TasksRecyclerView);
        this.tasksLoadingSpinner = inflatedView.findViewById(R.id.TasksLoadingSpinner);
        this.noTasksTextView = inflatedView.findViewById(R.id.NoTasksGroupFragmentTextView);
        this.tasksScrollView = inflatedView.findViewById(R.id.TasksScrollView);
        this.grouptaskList = new ArrayList<>();
        this.taskService = new TaskService();

        //pass filtered list based on groupID) and pass the filtered list here
        //create another recycler
        this.taskListAdapter = new GroupTasksAdapter(this.grouptaskList, this);
        tasksRecyclerView.setAdapter(taskListAdapter);
        tasksRecyclerView.setHasFixedSize(true);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this.ctx));
        this.taskService.setTaskServiceListener(new TaskService.TaskServiceListener() {

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onTaskLoaded(Task task) {
                if (task == null) {
                    loadingTasks = false;
                    tasksLoadingSpinner.setVisibility(View.INVISIBLE);
                    noTasksTextView.setVisibility(View.VISIBLE);
                    tasksScrollView.setVisibility(View.INVISIBLE);
                    return;
                }
                if (loadingTasks) {
                    loadingTasks = false;
                    tasksLoadingSpinner.setVisibility(View.INVISIBLE);
                    noTasksTextView.setVisibility(View.INVISIBLE);
                    tasksScrollView.setVisibility(View.VISIBLE);
                }
                taskListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onTaskLoaded(com.google.android.gms.tasks.Task task) {

            }

        });

        RecyclerView groupParticipantsRecyclerView = inflatedView.findViewById(R.id.GroupParticipantsRecyclerViewContainer);
        this.recyclerViewParticipantList = new ArrayList<>();
        this.participantListAdapter = new ParticipantListAdapter(this.recyclerViewParticipantList);
        groupParticipantsRecyclerView.setAdapter(participantListAdapter);
        groupParticipantsRecyclerView.setHasFixedSize(true);
        groupParticipantsRecyclerView.setLayoutManager(new LinearLayoutManager(this.ctx));
        GroupService groupService = new GroupService();
        GroupService.GroupServiceReadParticipantsListener groupServiceReadParticipantsListener = new GroupService.GroupServiceReadParticipantsListener() {
            @Override
            public void onParticipantRead(String participant) {
                recyclerViewParticipantList.add(participant);
                participantListAdapter.notifyDataSetChanged();
            }
        };
        groupService.setGroupServiceReadParticipantsListener(groupServiceReadParticipantsListener);
        groupService.readParticipantsForGroup(this.groupId);

        this.taskService.readTasks();
        this.addTaskActivityLaunch = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bundle intentExtras = data.getExtras();
                        if (intentExtras.getBoolean("NewTask")) {
                            taskService.readTask(intentExtras.getString("TaskUUID"));
                            loadingTasks = false;
                            tasksLoadingSpinner.setVisibility(View.INVISIBLE);
                            noTasksTextView.setVisibility(View.INVISIBLE);
                            tasksScrollView.setVisibility(View.VISIBLE);
                            grouptaskList.clear();
                            this.loadGroupTasks();
                        }
                    }
                });
        this.viewTaskActivityLaunch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bundle intentExtras = data.getExtras();
                        if (intentExtras.getBoolean("DeletedGroupTask")) {
                            grouptaskList.remove(intentExtras.getInt("DeletedGroupTaskPosition"));
                            taskListAdapter.notifyDataSetChanged();
                            loadingTasks = false;
                            tasksLoadingSpinner.setVisibility(View.INVISIBLE);
                            if (grouptaskList.isEmpty()) {
                                noTasksTextView.setVisibility(View.VISIBLE);
                                tasksScrollView.setVisibility(View.INVISIBLE);
                            } else {
                                noTasksTextView.setVisibility(View.INVISIBLE);
                                tasksScrollView.setVisibility(View.VISIBLE);
                            }
                        } else if (intentExtras.getBoolean("MarkCompleteGroupTask")) {
                            grouptaskList.get(intentExtras.getInt("MarkCompleteGroupTaskPosition")).setIsComplete(true);
                            taskListAdapter.notifyItemChanged(intentExtras.getInt("MarkCompleteGroupTaskPosition"));
                            loadingTasks = false;
                            tasksLoadingSpinner.setVisibility(View.INVISIBLE);
                            noTasksTextView.setVisibility(View.INVISIBLE);
                            tasksScrollView.setVisibility(View.VISIBLE);
                        }
                    }
                });

        this.groupSettingsActivityLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                Intent data = result.getData();
                                Bundle intentExtras = data.getExtras();
                                if (intentExtras.getBoolean("LeaveGroup")) {
                                    getFragmentManager().popBackStack();
                                } else {
                                    groupNameTextView.setText(intentExtras.getString("GroupName"));
                                    recyclerViewParticipantList.clear();
                                    groupService.readParticipantsForGroup(groupId);
                                }
                            }
                        });
        settings = inflatedView.findViewById(R.id.GroupSettingsActionButton);
        settings.setOnClickListener(v -> {
            // create a new intent to open the new activity
            Intent intent = new Intent(getContext(), GroupSettingsView.class);
            intent.putExtra("groupUUID", groupId);
            intent.putExtra("groupName", groupName);
            intent.putExtra("groupParticipantsNo", groupParticipantsNo);
            intent.putExtra("groupParticipants", groupParticipants);
            this.groupSettingsActivityLauncher.launch(intent);
        });
        //query all the tasks
        //fetch the tasks where the group name is the current group name
        String newGroupName1 = "Group task: " + "";
        String newGroupName = "Group task: " + groupName;
        this.loadGroupTasks();
        inflatedView.findViewById(R.id.GroupTasksAddActionButton).setOnClickListener(this::onAddTaskButtonClick);
        return inflatedView;
    }

    private void loadGroupTasks() {
        String newGroupName1 = "Group task: " + "";
        String newGroupName = "Group task: " + groupName;
        DatabaseReference userFriendsRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Tasks");
        userFriendsRef.get().addOnCompleteListener(userFriends -> {
            if (!userFriends.isSuccessful()) {
                Log.e("firebase", "Error getting data", userFriends.getException());
            } else {
                for (DataSnapshot childSnapshot : userFriends.getResult().getChildren()) {
                    String groupName = childSnapshot.child("taskTypeString").getValue(String.class);
                    if (groupName != null && (groupName.equals(newGroupName) || groupName.equals(newGroupName1))) {
                        String description = childSnapshot.child("description").getValue(String.class);
                        Boolean isComplete = childSnapshot.child("isComplete").getValue(Boolean.class);
                        String taskName = childSnapshot.child("taskName").getValue(String.class);
                        String uuid = childSnapshot.child("uuid").getValue(String.class);

                        String locationKey = childSnapshot.child("location").child("key").getValue(String.class);
                        double locationLat = childSnapshot.child("location").child("lat").getValue(Double.class);
                        double locationLon = childSnapshot.child("location").child("lon").getValue(Double.class);

                        LocationItem location = new LocationItem(locationKey, locationLat, locationLon);

                        String taskType = childSnapshot.child("taskType").getValue(String.class);
                        String taskTypeString = childSnapshot.child("taskTypeString").getValue(String.class);

                        Task addTask = new Task(taskName, description, location, uuid, isComplete);
                        addTask.setTaskType(taskType);
                        addTask.setTaskTypeString(taskTypeString);
                        grouptaskList.add(addTask);
                        taskListAdapter.notifyDataSetChanged(); // Notify the adapter that the data has changed

                    }
                }

            }
        });
    }

    public void onAddTaskButtonClick(View view) {
        Intent intent = new Intent(getContext(), AddGroupTask.class);
        intent.putExtra("groupUUID", this.groupId);
        intent.putExtra("groupName", this.groupName);
        intent.putExtra("groupParticipants", this.groupParticipants);
        this.addTaskActivityLaunch.launch(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureMap() {
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.mapController.setZoom(15);
        this.map.setMultiTouchControls(true);
        this.map.setClickable(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!groupParticipants.contains(firebaseUser)) {
            FragmentManager manager = getChildFragmentManager();
            manager.popBackStackImmediate();
        }
    }

    @Override
    public void onTaskItemClick(int position) {
        Task task = this.grouptaskList.get(position);
        Intent intent = new Intent(getContext(), GroupTaskView.class);
        intent.putExtra("groupId", this.groupId);
        intent.putExtra("groupName", this.groupName);
        intent.putExtra("groupParticipants", this.groupParticipants);
        intent.putExtra("position", position);
        intent.putExtra("taskTitle", task.getTaskName());
        intent.putExtra("taskDescription", task.getDescription());
        intent.putExtra("taskLocation", task.getLocation().getKey());
        intent.putExtra("taskLatitude", task.getLocation().getLat());
        intent.putExtra("taskLongitude", task.getLocation().getLon());
        intent.putExtra("taskComplete", task.getIsComplete());
        intent.putExtra("taskUUID", task.getUuid());
        intent.putExtra("taskType", task.getTaskType());
        intent.putExtra("taskTypeString", task.getTaskTypeString());
        this.viewTaskActivityLaunch.launch(intent);
    }
}

