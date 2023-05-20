package com.geonotif.geonotif;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
import org.osmdroid.views.overlay.Polygon;

import java.io.Serializable;

public class TaskView extends AppCompatActivity implements Serializable {

    MapView map;
    String taskName;
    Button markComplete;
    String uuid;
    Intent thisIntent;

    double taskLatitude;
    double taskLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_view);
        this.thisIntent = getIntent();
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        this.map = findViewById(R.id.MapView);

        this.markComplete = findViewById(R.id.TaskDetailsCompleteButton);

        Intent intent = getIntent();
        this.taskName = intent.getExtras().getString("taskTitle");
        this.uuid = intent.getExtras().getString("taskUUID");
        this.taskLatitude = intent.getExtras().getDouble("taskLatitude");
        this.taskLongitude = intent.getExtras().getDouble("taskLongitude");
        ((TextView) findViewById(R.id.TaskTitleTextView)).setText(intent.getExtras().getString("taskTitle"));
        ((TextView) findViewById(R.id.TaskDetailsDescription)).setText("Description: " + intent.getExtras().getString("taskDescription"));
        ((TextView) findViewById(R.id.TaskDetailsLocation)).setText("\uD83D\uDCCD " + intent.getExtras().getString("taskLocation"));
        this.customizeMap();
        if (intent.getExtras().getBoolean("taskComplete")) {
            ViewGroup layout = (ViewGroup) this.markComplete.getParent();
            layout.removeView(this.markComplete);
        }
        if (intent.getExtras().getString("taskType").equalsIgnoreCase("group")) {
            ViewGroup layout = (ViewGroup) findViewById(R.id.TaskDeleteFloatingButton).getParent();
            layout.removeView(findViewById(R.id.TaskDeleteFloatingButton));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void customizeMap() {
        // Set map tiles
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        IMapController mapController = this.map.getController();
        mapController.setZoom(18.8);
        // Set center
        GeoPoint centerPoint = new GeoPoint(this.taskLatitude, this.taskLongitude);
        mapController.setCenter(centerPoint);
        // Set marker
        Marker marker = new Marker(this.map);
        marker.setPosition(centerPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        Drawable d = ResourcesCompat.getDrawable(getResources(), R.drawable.pin, null);
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        Drawable dr = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap,
                (int) (18.0f * getResources().getDisplayMetrics().density),
                (int) (18.0f * getResources().getDisplayMetrics().density),
                true));
        marker.setIcon(dr);
        // Disable scroll and zoom
        this.map.setOnTouchListener((v, event) -> true);
        // Define radius circle
        double radiusInMeters = 100;
        GeoPoint circleCenter = marker.getPosition();
        Polygon circlePolygon = new Polygon();
        circlePolygon.setPoints(Polygon.pointsAsCircle(circleCenter, radiusInMeters));
        circlePolygon.getFillPaint().setColor(ContextCompat.getColor(
                this, R.color.map_radius_fill));
        circlePolygon.getOutlinePaint().setColor(ContextCompat.getColor(
                this, R.color.map_radius_outline));
        circlePolygon.getOutlinePaint().setStrokeWidth(0f);
        // Add layers to map
        this.map.getOverlays().add(circlePolygon);
        this.map.getOverlays().add(marker);
        // Refresh the map view to update the overlays
        this.map.invalidate();
    }

    public void onTaskEditFloatingButtonClick(View view) {
        Intent intent = new Intent(this, EditTask.class);
        intent.putExtra("taskTitle", thisIntent.getExtras().getString("taskTitle"));
        intent.putExtra("taskDescription", thisIntent.getExtras().getString("taskDescription"));
        intent.putExtra("taskLocation", thisIntent.getExtras().getString("taskLocation"));
        intent.putExtra("taskLatitude", thisIntent.getExtras().getDouble("taskLatitude"));
        intent.putExtra("taskLongitude", thisIntent.getExtras().getDouble("taskLongitude"));
        intent.putExtra("taskComplete", thisIntent.getExtras().getBoolean("taskComplete"));
        intent.putExtra("taskUUID", thisIntent.getExtras().getString("taskUUID"));
        intent.putExtra("taskType", thisIntent.getExtras().getString("taskType"));
        intent.putExtra("taskTypeString", thisIntent.getExtras().getString("taskTypeString"));
        startActivityForResult(intent, 1423);
    }

    public void onTaskMarkCompleteButtonClick(View view) {
        Task task = new Task(thisIntent.getExtras().getString("taskTitle"),
                thisIntent.getExtras().getString("taskDescription"),
                new LocationItem(thisIntent.getExtras().getString("taskLocation"),
                        thisIntent.getExtras().getDouble("taskLatitude"),
                        thisIntent.getExtras().getDouble("taskLongitude")),
                thisIntent.getExtras().getString("taskUUID"), true);
        task.setTaskType(thisIntent.getExtras().getString("taskType"));
        task.setTaskTypeString(thisIntent.getExtras().getString("taskTypeString"));

        TaskService.TaskServiceCreateListener taskServiceCreateListener = new TaskService.TaskServiceCreateListener() {
            @Override
            public void onTaskCreated(String taskUUID) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("MarkCompleteTask", true);
                returnIntent.putExtra("MarkCompleteTaskPosition", thisIntent.getExtras().getInt("position"));
                if (task.getTaskType().equals(TaskType.FRIEND.toString())) {
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference(
                            "GeoNotif/Users/" + firebaseUser.getUid());
                    mDatabase.get().addOnSuccessListener(dataSnapshot -> {
                        User user = dataSnapshot.getValue(User.class);
                        DatabaseReference userAssignableTasksRef = FirebaseDatabase.getInstance().getReference(
                                "GeoNotif/Users/" + user.getUid() + "/assignableTasks");
                        userAssignableTasksRef.setValue(user.getAssignableTasks() + 1);
                    });

                }
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        };
        TaskService taskService = new TaskService();
        taskService.setTaskServiceCreateListener(taskServiceCreateListener);
        taskService.createTask(task);
    }

    public void onTaskDeleteFloatingButtonClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Are you sure you want to delete this task?")
                .setIcon(R.drawable.warning)
                .setPositiveButton("CONFIRM", (dialogInterface, whichButton) -> {
                    TaskService.TaskServiceDeleteListener taskServiceDeleteListener = () -> {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("DeletedTask", true);
                        returnIntent.putExtra("DeletedTaskPosition", thisIntent.getExtras().getInt("position"));
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    };
                    TaskService taskService = new TaskService();
                    taskService.setTaskServiceDeleteListener(taskServiceDeleteListener);
                    taskService.deleteTask(this.uuid);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1423) {
            this.thisIntent = data;

            ((TextView) findViewById(R.id.TaskTitleTextView)).setText(data.getExtras().getString("taskTitle"));
            ((TextView) findViewById(R.id.TaskDetailsDescription)).setText("Description: " + data.getExtras().getString("taskDescription"));
            ((TextView) findViewById(R.id.TaskDetailsLocation)).setText("\uD83D\uDCCD " + data.getExtras().getString("taskLocation"));

            this.taskLongitude = data.getExtras().getDouble("taskLongitude");
            this.taskLatitude = data.getExtras().getDouble("taskLatitude");
            customizeMap();
        }
    }
}