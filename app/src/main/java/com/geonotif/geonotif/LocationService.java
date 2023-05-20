package com.geonotif.geonotif;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class LocationService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private PowerManager.WakeLock wakeLock;

    private LocationManager locationManager;

    DatabaseReference ref;
    GeoFire geoFire;
    GeoQuery geoQuery;
    private FirebaseUser firebaseUser;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.mAuth = FirebaseAuth.getInstance();
        this.firebaseUser = mAuth.getCurrentUser();
        String userId = this.firebaseUser.getUid();
        this.ref = FirebaseDatabase.getInstance().getReference("GeoNotif/Users/" + userId + "/Locations");
        this.geoFire = new GeoFire(this.ref);
        ref.keepSynced(true);
        this.geoQuery = geoFire.queryAtLocation(new GeoLocation(0.0, 0.0), 0.3);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        geoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
            @Override
            public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
                DatabaseReference taskRef = FirebaseDatabase.getInstance().getReference("GeoNotif/Tasks/" + dataSnapshot.getKey());
                taskRef.get().addOnCompleteListener(t -> {
                    if (!t.isSuccessful()) {
                        Log.e("firebase", "Error getting data", t.getException());
                    } else {
                        Task task = new Task();
                        for (DataSnapshot item : t.getResult().getChildren()) {
                            if (item.getKey().equals("taskName"))
                                task.setTaskName(item.getValue().toString());
                            else if (item.getKey().equals("isComplete"))
                                task.setIsComplete((Boolean) item.getValue());
                            else if (item.getKey().equals("description"))
                                task.setDescription(item.getValue().toString());
                            else if (item.getKey().equals("location")) {
                                String key = "";
                                double lat = 0.0;
                                double lon = 0.0;

                                for (DataSnapshot locationDetail : item.getChildren()) {
                                    if (locationDetail.getKey().equals("key")) {
                                        key = locationDetail.getValue().toString();
                                    }
                                    if (locationDetail.getKey().equals("lat")) {
                                        lat = (double) locationDetail.getValue();
                                    }
                                    if (locationDetail.getKey().equals("lon")) {
                                        lon = (double) locationDetail.getValue();
                                    }
                                    LocationItem locationItem = new LocationItem(key, lat, lon);
                                    task.setLocation(locationItem);
                                }
                            } else if (item.getKey().equals("taskType")) {
                                System.out.println(item.getValue());
                                task.setTaskType(item.getValue().toString());
                            } else if (item.getKey().equals("taskTypeString")) {
                                task.setTaskTypeString((item.getValue().toString()));
                            } else if (item.getKey().equals("uuid")) {
                                task.setUuid(item.getValue().toString());
                            }
                        }
                        if (!task.getIsComplete()) {
                            System.out.println("Before Notification - " + task.toString());
                            sendNotification(task);
                        }
                    }
                });

            }

            @Override
            public void onDataExited(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {

            }

            @Override
            public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                System.err.println("There was an error with this query: " + error);
            }
        });

        startLocationUpdates();
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationServiceWakeLock");
        wakeLock.acquire();

        createNotificationChannelForService();
        createNotificationChannelForTasks();

        Notification notification = new NotificationCompat.Builder(this, "LocationServiceChannel")
                .setContentTitle("Location Service")
                .setContentText("Location updates are running...")
                .setSmallIcon(R.drawable.ic_stat_name)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            geoQuery.setCenter(new GeoLocation(location.getLatitude(), location.getLongitude()));
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        locationManager.removeUpdates(locationListener);
        stopForeground(true);
    }

    private void createNotificationChannelForService() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("LocationServiceChannel",
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createNotificationChannelForTasks() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("TaskNotificationChannel",
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(Task task) {
        Intent intent = new Intent(getApplicationContext(), TaskView.class);
        intent.putExtra("taskTitle", task.getTaskName());
        intent.putExtra("taskDescription", task.getDescription());
        intent.putExtra("taskLocation", task.getLocation().getKey());
        intent.putExtra("taskLatitude", task.getLocation().getLat());
        intent.putExtra("taskLongitude", task.getLocation().getLon());
        intent.putExtra("taskComplete", task.getIsComplete());
        intent.putExtra("taskUUID", task.getUuid());
        intent.putExtra("taskType", task.getTaskType());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TaskNotificationChannel")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("You have a task nearby!")
                .setContentText(task.getTaskName())
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Random random = new Random();
        notificationManager.notify(random.nextInt(), builder.build());
    }
}

