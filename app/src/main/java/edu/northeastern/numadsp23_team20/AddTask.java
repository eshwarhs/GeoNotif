package edu.northeastern.numadsp23_team20;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AddTask extends AppCompatActivity {

    private MapView map;
    private IMapController mapController;
    private FusedLocationProviderClient fusedLocationClient;
    private GeoPoint currentUserLocation;
    private String taskLocationName;
    private double taskLatitude;
    private double taskLongitude;
    private Marker mapMarker;
    private ActivityResultLauncher<Intent> addressSearchActivity;
    private TextView addTaskLocationValue;
    //private TaskTypeListAdapter taskTypeListAdapter;
    private TaskTypeListAdapter groupListAdapter;
    private TaskTypeListAdapter friendListAdapter;
    // private OnTaskTypeAssigneeItemClickListener onTaskTypeAssigneeItemClickListener;
    private OnTaskTypeGroupItemClickListener onTaskTypeGroupItemClickListener;
    private OnTaskTypeFriendItemClickListener onTaskTypeFriendItemClickListener;
    private TaskType taskType;
    private Group nonPersonalTaskTypeAssignee;
    private User friendTaskTypeAssignee;
    private GroupService.GroupServiceListener groupServiceListener;
    private List<Group> groupsList;
    private RecyclerView addTaskTypeRecyclerViewContainer;
    private List<User> friendsList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        this.addTaskTypeRecyclerViewContainer = findViewById(R.id.GroupParticipantsRecyclerViewContainer);
//        onTaskTypeAssigneeItemClickListener = assignee -> {
//            taskTypeListAdapter.notifyDataSetChanged();
//            nonPersonalTaskTypeAssignee = assignee;
//        };
        groupsList = new ArrayList<>();
        onTaskTypeGroupItemClickListener = assignee -> {
            groupListAdapter.notifyDataSetChanged();
            nonPersonalTaskTypeAssignee = assignee;
        };
        groupListAdapter = new TaskTypeListAdapter(
                TaskType.GROUP,
                this.groupsList, onTaskTypeGroupItemClickListener,
                null, null);
        friendsList = new ArrayList<>();
        onTaskTypeFriendItemClickListener = assignee -> {
            System.out.println(assignee);
            friendListAdapter.notifyDataSetChanged();
            friendTaskTypeAssignee = assignee;
        };
        friendListAdapter = new TaskTypeListAdapter(
                TaskType.FRIEND,
                null, null,
                this.friendsList, onTaskTypeFriendItemClickListener);
        this.addTaskTypeRecyclerViewContainer.setLayoutManager(new LinearLayoutManager(this));
        GroupService groupService = new GroupService();
        groupService.setGroupServiceListener(new GroupService.GroupServiceListener() {
            @Override
            public void onUserGroupLoaded(Group group) {
                groupsList.add(group);
                groupListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onGroupCreated(Group group) {

            }
        });
        groupService.readGroupsForUser();
        FriendService friendService = new FriendService();
        friendService.setFriendServiceReadListener(new FriendService.FriendServiceReadListener() {
            @Override
            public void onFriendLoad(User friend) {
                friendsList.add(friend);
                friendListAdapter.notifyDataSetChanged();
            }
        });
        friendService.readUserFriends();
        this.taskType = TaskType.PERSONAL;
        this.nonPersonalTaskTypeAssignee = null;
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        this.map = findViewById(R.id.AddTaskMapView);
        this.mapMarker = new Marker(this.map);
        this.customizeMapMarker();
        this.mapController = this.map.getController();
        this.configureMap();
        this.map.setExpectedCenter(new GeoPoint(42.3398, -71.0892));
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        this.getCurrentUserLocation();
        this.addTaskLocationValue = findViewById(R.id.AddTaskLocationValue);
        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        this.addressSearchActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        setMapMarker(place.getLatLng().latitude, place.getLatLng().longitude);
                        this.addTaskLocationValue.setVisibility(View.VISIBLE);
                        this.addTaskLocationValue.setText("\uD83D\uDCCD " + place.getName());
                        this.taskLocationName = place.getName();
                        this.taskLatitude = place.getLatLng().latitude;
                        this.taskLongitude = place.getLatLng().longitude;
                    }
                }
        );
        initialItemData(savedInstanceState);
    }

    public void onAddTaskFindLocationButtonClick(View view) {
        List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.ADDRESS_COMPONENTS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                fields).build(this);
        this.addressSearchActivity.launch(intent);
    }

    public void onAddTaskTypePersonalRadioButtonClick(View view) {
        addTaskTypeRecyclerViewContainer.setVisibility(View.GONE);
        this.taskType = TaskType.PERSONAL;
        this.nonPersonalTaskTypeAssignee = null;
        this.friendTaskTypeAssignee = null;
    }

    public void onAddTaskTypeGroupRadioButtonClick(View view) {
        addTaskTypeRecyclerViewContainer.setVisibility(View.VISIBLE);
        this.taskType = TaskType.GROUP;
        this.nonPersonalTaskTypeAssignee = null;
        this.friendTaskTypeAssignee = null;
        addTaskTypeRecyclerViewContainer.setAdapter(groupListAdapter);
    }

    public void onAddTaskTypeFriendRadioButtonClick(View view) {
        addTaskTypeRecyclerViewContainer.setVisibility(View.VISIBLE);
        this.taskType = TaskType.FRIEND;
        this.friendTaskTypeAssignee = null;
        this.nonPersonalTaskTypeAssignee = null;
        addTaskTypeRecyclerViewContainer.setAdapter(friendListAdapter);
    }

    public void onAddTaskCancelButtonClick(View view) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("NewTask", false);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    public void onAddTaskSubmitButtonClick(View view) {
        String taskTitle = ((TextView) findViewById(R.id.AddTaskTitleValue)).getText().toString();
        String taskDescription = ((TextView) findViewById(R.id.AddTaskDescriptionValue)).getText().toString();

        if (!validateTaskTitle(taskTitle)) {
            return;
        } else if (!validateTaskDescription(taskDescription)) {
            return;
        } else if (!validateTaskType()) {
            if (taskType == TaskType.GROUP) {
                Toast.makeText(this, "Group not selected!", Toast.LENGTH_SHORT).show();
            } else if (taskType == TaskType.FRIEND) {
                Toast.makeText(this, "Friend not selected!", Toast.LENGTH_SHORT).show();
            }
            return;
        } else if (!validateLocation()) {
            Toast.makeText(this, "Please choose a location!", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationItem location = new LocationItem(this.taskLocationName, this.taskLatitude, this.taskLongitude);
        Task task = new Task(taskTitle, taskDescription, location);
        UUID uuid = UUID.randomUUID();
        task.setUuid(uuid.toString());
        TaskService.TaskServiceCreateListener taskServiceCreateListener = new TaskService.TaskServiceCreateListener() {
            @Override
            public void onTaskCreated(String taskUUID) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("NewTask", true);
                returnIntent.putExtra("TaskUUID", taskUUID);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        };
        GroupService.GroupServiceTaskCreateListener groupServiceTaskCreateListener = new GroupService.GroupServiceTaskCreateListener() {
            @Override
            public void onTaskCreated(String taskUUID) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("NewTask", true);
                returnIntent.putExtra("TaskUUID", taskUUID);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        };
        FriendService.FriendServiceCreateListener friendServiceCreateListener = new FriendService.FriendServiceCreateListener() {
            @Override
            public void onTaskCreated(String friendFullname) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("NewFriendTask", true);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        };
        TaskService taskService = new TaskService();
        GroupService groupService = new GroupService();
        FriendService friendService = new FriendService();
        taskService.setTaskServiceCreateListener(taskServiceCreateListener);
        groupService.setGroupServiceTaskCreateListener(groupServiceTaskCreateListener);
        friendService.setFriendServiceCreateListener(friendServiceCreateListener);

        if (taskType == TaskType.PERSONAL) {
            task.setTaskType(TaskType.PERSONAL.toString());
            task.setTaskTypeString("Personal task");
            taskService.createTask(task);
        } else if (taskType == TaskType.GROUP) {
            task.setTaskType(TaskType.GROUP.toString());
            task.setTaskTypeString("Group task: " + nonPersonalTaskTypeAssignee.getGroupName());
            groupService.addTaskToGroup(nonPersonalTaskTypeAssignee.getUuid(), task);
        } else if (taskType == TaskType.FRIEND) {
            task.setTaskType(TaskType.FRIEND.toString());
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser firebaseUser = mAuth.getCurrentUser();;
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(
                    "GeoNotif/Users/" + firebaseUser.getUid());
            mDatabase.get().addOnSuccessListener(dataSnapshot -> {
                User user = dataSnapshot.getValue(User.class);
                // check user points and only then allow to add
                if (user.getAssignableTasks() > 0) {
                    task.setTaskTypeString("Friend task: " + user.getFullname());
                    friendService.createFriendTask(task, friendTaskTypeAssignee.getUid());
                    // decrement AssignableTasks
                    DatabaseReference userAssignableTasksRef = FirebaseDatabase.getInstance().getReference(
                            "GeoNotif/Users/" + user.getUid() + "/assignableTasks");
                    userAssignableTasksRef.setValue(user.getAssignableTasks() - 1);
                }
                 else {
                     Toast.makeText(getApplicationContext(),
                             "You don't have any assignable tasks left!", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private boolean validateTaskType() {
        if (this.taskType == TaskType.GROUP && this.nonPersonalTaskTypeAssignee == null) {
            ((TextView) findViewById(R.id.AddTaskTypeLabel)).setError("Group not selected!");
            return false;
        } else if (this.taskType == TaskType.FRIEND && this.friendTaskTypeAssignee == null) {
            ((TextView) findViewById(R.id.AddTaskTypeLabel)).setError("Friend not selected!");
            return false;
        }
        return true;
    }

    private boolean validateLocation() {
        if (this.taskLocationName == null || this.taskLocationName.isEmpty()) {
            ((TextView) findViewById(R.id.AddTaskLocationLabel)).setError("Please choose a location!");
            return false;
        }
        return true;
    }

    private boolean validateTaskTitle(String taskTitle) {
        if (TextUtils.isEmpty(taskTitle)) {
            ((TextView) findViewById(R.id.AddTaskTitleValue)).requestFocus();
            ((TextView) findViewById(R.id.AddTaskTitleValue)).setError("Task Name is required");
            return false;
        }
        return true;
    }

    private boolean validateTaskDescription(String taskDescription) {
        if (TextUtils.isEmpty(taskDescription)) {
            ((TextView) findViewById(R.id.AddTaskDescriptionValue)).requestFocus();
            ((TextView) findViewById(R.id.AddTaskDescriptionValue)).setError("Task Description is required");
            return false;
        }
        return true;
    }

    private void getCurrentUserLocation() {
        boolean noFineLocationAccess = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean noCoarseLocationAccess = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean noBackgroundLocationAccess = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if (noFineLocationAccess || noCoarseLocationAccess || noBackgroundLocationAccess) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    101);
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        this.currentUserLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        this.taskLatitude = location.getLatitude();
                        this.taskLongitude = location.getLongitude();
                        this.configureMap();
                    }
                });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureMap() {
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.mapController.setZoom(16);
        this.map.setMultiTouchControls(true);
        this.map.setClickable(true);
        this.mapController.setCenter(this.currentUserLocation);
    }

    private void customizeMapMarker() {
        Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.pin, null);
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        Drawable dr = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
                (int) (18.0f * getResources().getDisplayMetrics().density),
                (int) (18.0f * getResources().getDisplayMetrics().density),
                true));
        this.mapMarker.setIcon(dr);
    }

    private void setMapMarker(double latitude, double longitude) {
        this.map.invalidate();
        this.mapMarker.remove(this.map);
        GeoPoint markerPoint = new GeoPoint(latitude, longitude);
        this.mapController.setCenter(markerPoint);
        this.mapMarker.setPosition(markerPoint);
        this.mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        this.map.getOverlays().add(this.mapMarker);
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (((TextView) findViewById(R.id.AddTaskTitleValue)).getText() != null)
            outState.putString("taskTitle", ((TextView) findViewById(R.id.AddTaskTitleValue)).getText().toString());
        else
            outState.putString("taskTitle", "");

        if (((TextView) findViewById(R.id.AddTaskDescriptionValue)).getText() != null)
            outState.putString("taskDesc", ((TextView) findViewById(R.id.AddTaskDescriptionValue)).getText().toString());
        else
            outState.putString("taskDesc", "");

        outState.putString("taskLocationName", this.taskLocationName);
        outState.putDouble("taskLatitude", this.taskLatitude);
        outState.putDouble("taskLongitude", this.taskLongitude);
        outState.putInt("addTaskLocationValueVisibility", this.addTaskLocationValue.getVisibility());
        outState.putString("addTaskLocationValue", this.addTaskLocationValue.getText().toString());
        outState.putInt("addTaskTypeRecyclerViewContainerVisibility", addTaskTypeRecyclerViewContainer.getVisibility());
        outState.putSerializable("taskType", this.taskType);
    }

    private void initialItemData(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.taskLocationName = savedInstanceState.getString("taskLocationName");
            this.taskLatitude = savedInstanceState.getDouble("taskLatitude");
            this.taskLongitude = savedInstanceState.getDouble("taskLongitude");
            this.addTaskLocationValue.setVisibility(savedInstanceState.getInt("addTaskLocationValueVisibility"));
            this.addTaskLocationValue.setText(savedInstanceState.getString("addTaskLocationValue"));
            this.addTaskTypeRecyclerViewContainer.setVisibility(savedInstanceState.getInt("addTaskTypeRecyclerViewContainerVisibility"));
            this.taskType = (TaskType) savedInstanceState.getSerializable("taskType");
            if (this.taskLocationName != null) {
                setMapMarker(this.taskLatitude, this.taskLongitude);
            }
        }
    }
}