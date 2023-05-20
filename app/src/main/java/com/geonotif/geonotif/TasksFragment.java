package com.geonotif.geonotif;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TasksFragment extends Fragment implements OnTaskItemClickListener {

    Context ctx;
    private MapView map;
    private IMapController mapController;
    private ActivityResultLauncher<Intent> addTaskActivityLaunch;
    private ActivityResultLauncher<Intent> viewTaskActivityLaunch;
    private TaskService taskService;
    private List<Task> taskList;
    private boolean loadingTasks;
    private ProgressBar tasksLoadingSpinner;
    private TextView noTasksTextView;
    private ScrollView tasksScrollView;
    private TaskListAdapter taskListAdapter;
    private HashMap<Task, Marker> mapMarkerCollection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.loadingTasks = true;
        View inflatedView = inflater.inflate(R.layout.fragment_tasks, container, false);
        this.ctx = getContext();
        Configuration.getInstance().load(this.ctx, PreferenceManager.getDefaultSharedPreferences(this.ctx));
        this.map = inflatedView.findViewById(R.id.TasksMapView);
        this.mapController = this.map.getController();
        this.mapMarkerCollection = new HashMap<>();
        this.configureMap();
        RecyclerView tasksRecyclerView = inflatedView.findViewById(R.id.TasksRecyclerView);
        this.tasksLoadingSpinner = inflatedView.findViewById(R.id.TasksLoadingSpinner);
        this.noTasksTextView = inflatedView.findViewById(R.id.NoTasksTextView);
        this.tasksScrollView = inflatedView.findViewById(R.id.TasksScrollView);
        this.taskList = new ArrayList<>();
        this.taskService = new TaskService();
        this.taskListAdapter = new TaskListAdapter(this.taskList, this);
        tasksRecyclerView.setAdapter(taskListAdapter);
        tasksRecyclerView.setHasFixedSize(true);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this.ctx));
        this.taskService.setTaskServiceListener(new TaskService.TaskServiceListener() {
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
                setMapMarker(task);
                taskList.add(task);
                taskListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onTaskLoaded(com.google.android.gms.tasks.Task task) {

            }
        });
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
                        } else if (intentExtras.getBoolean("NewFriendTask")) {
                            String message = "Added task to friend!";
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
        this.viewTaskActivityLaunch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Bundle intentExtras = data.getExtras();
                        if (intentExtras.getBoolean("DeletedTask")) {
                            int deletedTaskPosition = intentExtras.getInt("DeletedTaskPosition");
                            Task delTask = taskList.get(deletedTaskPosition);
                            taskList.remove(deletedTaskPosition);
                            map.getOverlays().remove(mapMarkerCollection.get(delTask));
                            mapMarkerCollection.remove(delTask);
                            taskListAdapter.notifyDataSetChanged();
                            loadingTasks = false;
                            tasksLoadingSpinner.setVisibility(View.INVISIBLE);
                            if (taskList.isEmpty()) {
                                noTasksTextView.setVisibility(View.VISIBLE);
                                tasksScrollView.setVisibility(View.INVISIBLE);
                            } else {
                                noTasksTextView.setVisibility(View.INVISIBLE);
                                tasksScrollView.setVisibility(View.VISIBLE);
                            }
                        } else if (intentExtras.getBoolean("MarkCompleteTask")) {
                            taskList.get(intentExtras.getInt("MarkCompleteTask")).setIsComplete(true);
                            taskListAdapter.notifyItemChanged(intentExtras.getInt("EditedTaskPosition"));
                            loadingTasks = false;
                            tasksLoadingSpinner.setVisibility(View.INVISIBLE);
                            noTasksTextView.setVisibility(View.INVISIBLE);
                            tasksScrollView.setVisibility(View.VISIBLE);
                        }
                    }
                });
        inflatedView.findViewById(R.id.AddTaskButton).setOnClickListener(this::onAddTaskButtonClick);
        return inflatedView;
    }

    public void onAddTaskButtonClick(View view) {
        Intent intent = new Intent(getContext(), AddTask.class);
        this.addTaskActivityLaunch.launch(intent);
    }

    @SuppressLint({"ClickableViewAccessibility", "MissingPermission"})
    private void configureMap() {
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.mapController.setZoom(15);
        this.map.setMultiTouchControls(true);
        this.map.setClickable(true);
        this.map.setExpectedCenter(new GeoPoint(42.3398, -71.0892));

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.getActivity());

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this.getActivity(), location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        GeoPoint markerPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        this.mapController.setCenter(markerPoint);
                        Marker mapMarker = this.getCustomizedMapMarker();
                        mapMarker.setPosition(markerPoint);
                    }
                });
    }

    private Marker getCustomizedMapMarker() {
        Marker mapMarker = new Marker(this.map);
        Drawable pin_drawable = ResourcesCompat.getDrawable(this.ctx.getResources(), R.drawable.pin, null);
        // assert pin_drawable != null;
        Bitmap bitmap = ((BitmapDrawable) pin_drawable).getBitmap();
        Drawable dr = new BitmapDrawable(this.ctx.getResources(), Bitmap.createScaledBitmap(bitmap,
                (int) (18.0f * this.ctx.getResources().getDisplayMetrics().density),
                (int) (18.0f * this.ctx.getResources().getDisplayMetrics().density),
                true));
        mapMarker.setIcon(dr);
        return mapMarker;
    }

    private void setMapMarker(Task task) {
        GeoPoint markerPoint = new GeoPoint(task.getLocation().getLat(), task.getLocation().getLon());
        this.mapController.setCenter(markerPoint);
        Marker mapMarker = this.getCustomizedMapMarker();
        this.mapMarkerCollection.put(task, mapMarker);
        System.out.println(this.mapMarkerCollection);
        mapMarker.setPosition(markerPoint);
        mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapMarker.setOnMarkerClickListener((marker, mapView) -> {
            Intent intent = new Intent(getContext(), TaskView.class);
            intent.putExtra("taskTitle", task.getTaskName());
            intent.putExtra("taskDescription", task.getDescription());
            intent.putExtra("taskLocation", task.getLocation().getKey());
            intent.putExtra("taskLatitude", task.getLocation().getLat());
            intent.putExtra("taskLongitude", task.getLocation().getLon());
            intent.putExtra("taskComplete", task.getIsComplete());
            intent.putExtra("taskUUID", task.getUuid());
            intent.putExtra("taskType", task.getTaskType());
            intent.putExtra("taskTypeString", task.getTaskTypeString());
            startActivity(intent);
            return true;
        });
        this.map.getOverlays().add(mapMarker);
    }

    @Override
    public void onTaskItemClick(int position) {
        Task task = this.taskList.get(position);
        Intent intent = new Intent(getContext(), TaskView.class);
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