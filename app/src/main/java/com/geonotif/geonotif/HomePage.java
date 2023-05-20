package com.geonotif.geonotif;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class HomePage extends AppCompatActivity {

    private Fragment tasksFragment;
    private Fragment groupsFragment;
    private Fragment friendsFragment;
    private Fragment profileFragment;
    private FragmentManager fragmentManager;

    private static final String TASKS_FRAGMENT_TAG = "TASKS_FRAGMENT_TAG";
    private static final String GROUPS_FRAGMENT_TAG = "GROUPS_FRAGMENT_TAG";
    private static final String FRIENDS_FRAGMENT_TAG = "FRIENDS_FRAGMENT_TAG";
    private static final String PROFILE_FRAGMENT_TAG = "PROFILE_FRAGMENT_TAG";
    private String currentFragmentTag;

    private GeoNotif settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        this.settings = (GeoNotif) getApplication();
        this.checkLocationPermissions();
        this.tasksFragment = new TasksFragment();
        this.groupsFragment = new GroupsFragment();
        this.friendsFragment = new FriendsFragment();
        this.profileFragment = new ProfileFragment();
        BottomNavigationView bottomNavigationMenu = findViewById(R.id.BottomNavigationMenu);
        this.setFragment(this.tasksFragment);
        bottomNavigationMenu.setOnItemSelectedListener(item -> {
            this.currentFragmentTag = item.getTitle().toString();
            this.setFragment(this.getFragment(item.getTitle().toString()));
            return true;
        });

        if (savedInstanceState != null) {
            // Restore the previously selected fragment
            currentFragmentTag = savedInstanceState.getString("FRAGMENT_TAG");
            fragmentManager = getSupportFragmentManager();
            Fragment fragment = getFragment(currentFragmentTag);
            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .replace(R.id.FrameLayout, fragment, currentFragmentTag)
                        .commit();
                // Update the current fragment tag to the restored fragment
                currentFragmentTag = fragment.getTag();
            }
        }
        loadSharedPreferences();
        setSettings();
    }

    private void setSettings() {
        if (this.settings.getNotifSetting().equalsIgnoreCase(GeoNotif.ENABLE_NOTIF_SETTING)) {
            Intent intent = new Intent(this, LocationService.class);
            startService(intent);
        }
    }

    private void loadSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(GeoNotif.PREFERENCES, MODE_PRIVATE);
        String notifSetting = sharedPreferences.getString(GeoNotif.NOTIF_SETTING, GeoNotif.ENABLE_NOTIF_SETTING);
        this.settings.setNotifSetting(notifSetting);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the tag of the current fragment
        outState.putString("FRAGMENT_TAG", currentFragmentTag);
    }


    private void checkLocationPermissions() {
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
    }

    @Override
    protected void onRestart() {
        super.onRestart();


    }

    private Fragment getFragment(String itemTitle) {
        switch (itemTitle) {
            case "Tasks":
            case TASKS_FRAGMENT_TAG:
                this.currentFragmentTag = TASKS_FRAGMENT_TAG;
                return this.tasksFragment;
            case "Groups":
            case GROUPS_FRAGMENT_TAG:
                this.currentFragmentTag = GROUPS_FRAGMENT_TAG;
                return this.groupsFragment;
            case "Friends":
            case FRIENDS_FRAGMENT_TAG:
                this.currentFragmentTag = FRIENDS_FRAGMENT_TAG;
                return this.friendsFragment;
            case "Profile":
            case PROFILE_FRAGMENT_TAG:
                this.currentFragmentTag = PROFILE_FRAGMENT_TAG;
                return this.profileFragment;
        }
        return null;
    }

    private void setFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayout, fragment).commit();
    }
}