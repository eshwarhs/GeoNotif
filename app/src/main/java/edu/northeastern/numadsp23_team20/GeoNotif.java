package edu.northeastern.numadsp23_team20;

import android.app.Application;
import android.os.Bundle;

import com.google.firebase.database.FirebaseDatabase;

public class GeoNotif extends Application {

    public static final String PREFERENCES = "preferences";
    public static final String NOTIF_SETTING = "enable";
    public static final String ENABLE_NOTIF_SETTING = "enable";
    public static final String DISABLE_NOTIF_SETTING = "disable ";

    private String notifSetting;

    public String getNotifSetting() {
        return notifSetting;
    }

    public void setNotifSetting(String notifSetting) {
        this.notifSetting = notifSetting;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
