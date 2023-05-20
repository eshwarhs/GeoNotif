package com.geonotif.geonotif;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.Arrays;
import java.util.List;

public class EditTask extends AppCompatActivity {

    private ActivityResultLauncher<Intent> addressSearchActivity;
    private TextView editTaskLocationValue;
    private MapView map;
    private IMapController mapController;
    private Marker mapMarker;
    private Task task;

    private TextView editTaskTitleValue;
    private TextView editTaskDescriptionValue;
    private String taskLocation;
    private double taskLatitude;
    private double taskLongitude;
    private boolean isComplete;
    private String uuid;
    private String taskType;
    private String taskTypeString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        this.editTaskLocationValue = findViewById(R.id.EditTaskLocationValue);
        Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        this.addressSearchActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        this.taskLocation = place.getName();
                        this.taskLatitude = place.getLatLng().latitude;
                        this.taskLongitude = place.getLatLng().longitude;
                        setMapMarker(this.taskLatitude, this.taskLongitude);
                        editTaskLocationValue.setText("\uD83D\uDCCD " + place.getName());

                    }
                }
        );

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        this.map = findViewById(R.id.EditTaskMapView);
        this.mapController = this.map.getController();
        this.mapMarker = new Marker(this.map);
        Intent intent = this.getIntent();
        String taskTitle = intent.getExtras().getString("taskTitle");
        String taskDescription = intent.getExtras().getString("taskDescription");
        this.taskLocation = intent.getExtras().getString("taskLocation");
        this.taskLatitude = intent.getExtras().getDouble("taskLatitude");
        this.taskLongitude = intent.getExtras().getDouble("taskLongitude");
        this.isComplete = intent.getExtras().getBoolean("taskComplete");
        this.uuid = intent.getExtras().getString("taskUUID");
        this.taskType = intent.getExtras().getString("taskType");
        this.taskTypeString = intent.getExtras().getString("taskTypeString");
        LocationItem locationItem = new LocationItem(this.taskLocation, this.taskLatitude, this.taskLongitude);
        this.task = new Task(taskTitle, taskDescription, locationItem);
        this.configureMap();
        this.customizeMapMarker();
        this.setMapMarker(this.taskLatitude, this.taskLongitude);
        this.editTaskTitleValue = findViewById(R.id.EditTaskTitleValue);
        this.editTaskTitleValue.setText(taskTitle);
        this.editTaskDescriptionValue = findViewById(R.id.EditTaskDescriptionValue);
        this.editTaskDescriptionValue.setText(taskDescription);
        ((TextView) findViewById(R.id.EditTaskLocationValue)).setText("\uD83D\uDCCD " + taskLocation);
        if (this.taskType.equalsIgnoreCase("group")) {
            ViewGroup layout = (ViewGroup) findViewById(R.id.EditTaskUpdateButton).getParent();
            layout.removeView(findViewById(R.id.EditTaskUpdateButton));
        }
        initialItemData(savedInstanceState);
    }

    public void onEditTaskUpdateButtonClick(View view) {
        List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.ADDRESS_COMPONENTS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                fields).build(this);
        this.addressSearchActivity.launch(intent);
    }

    public void onEditTaskCancelButtonClick(View view) {
        this.finish();
    }

    private boolean validateLocation() {
        if (this.taskLocation == null || this.taskLocation.isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean validateTaskTitle() {
        if (TextUtils.isEmpty(this.editTaskTitleValue.getText().toString())) {
            ((TextView) findViewById(R.id.EditTaskTitleValue)).requestFocus();
            ((TextView) findViewById(R.id.EditTaskTitleValue)).setError("Task Name is required");
            return false;
        }
        return true;
    }

    private boolean validateTaskDescription() {
        if (TextUtils.isEmpty(this.editTaskDescriptionValue.getText().toString())) {
            ((TextView) findViewById(R.id.EditTaskDescriptionValue)).requestFocus();
            ((TextView) findViewById(R.id.EditTaskDescriptionValue)).setError("Task Description is required");
            return false;
        }
        return true;
    }

    public void onEditTaskSubmitButtonClick(View view) {

        if (!validateTaskTitle()) {
            return;
        } else if (!validateTaskDescription()) {
            return;
        } else if (!validateLocation()) {
            Toast.makeText(this, "Please choose a location", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to edit this task?")
                .setPositiveButton("CONFIRM", (dialogInterface, whichButton) -> {
                    LocationItem locationItem = new LocationItem(
                            this.taskLocation, this.taskLatitude, this.taskLongitude
                    );
                    Task updatedTask = new Task(this.editTaskTitleValue.getText().toString(),
                            this.editTaskDescriptionValue.getText().toString(), locationItem);
                    updatedTask.setUuid(this.uuid);
                    updatedTask.setIsComplete(this.isComplete);
                    updatedTask.setTaskType(this.taskType);
                    updatedTask.setTaskTypeString(this.taskTypeString);
                    TaskService taskService = new TaskService();
                    taskService.editTask(this.task, updatedTask);
                    Intent data = new Intent();
                    data.putExtra("taskTitle", this.editTaskTitleValue.getText().toString());
                    data.putExtra("taskDescription", this.editTaskDescriptionValue.getText().toString());
                    data.putExtra("taskLocation", this.taskLocation);
                    data.putExtra("taskLatitude", this.taskLatitude);
                    data.putExtra("taskLongitude", this.taskLongitude);
                    data.putExtra("taskComplete", this.isComplete);
                    data.putExtra("taskUUID", this.uuid);
                    data.putExtra("taskType", this.taskType);
                    data.putExtra("taskTypeString", this.taskTypeString);
                    setResult(RESULT_OK, data);
                    this.finish();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void configureMap() {
        this.map.setTileSource(TileSourceFactory.MAPNIK);
        this.mapController.setZoom(18.8);
        this.map.setOnTouchListener((v, event) -> true);
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
        if (this.editTaskTitleValue.getText() != null)
            outState.putString("taskTitle", this.editTaskTitleValue.getText().toString());
        else
            outState.putString("taskTitle", "");

        if (this.editTaskTitleValue.getText() != null)
            outState.putString("taskDesc", this.editTaskTitleValue.getText().toString());
        else
            outState.putString("taskDesc", "");

        outState.putString("taskLocation", this.taskLocation);
        outState.putDouble("taskLatitude", this.taskLatitude);
        outState.putDouble("taskLongitude", this.taskLongitude);
        outState.putString("editTaskLocationValue", this.editTaskLocationValue.getText().toString());
    }

    private void initialItemData(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            this.taskLocation = savedInstanceState.getString("taskLocation");
            this.taskLatitude = savedInstanceState.getDouble("taskLatitude");
            this.taskLongitude = savedInstanceState.getDouble("taskLongitude");
            this.editTaskLocationValue.setText(savedInstanceState.getString("editTaskLocationValue"));
            if (this.editTaskLocationValue != null) {
                setMapMarker(this.taskLatitude, this.taskLongitude);
            }
        }
    }
}