package com.geonotif.geonotif;

import androidx.annotation.NonNull;

public class LocationItem {

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    private String key;
    private double lat;
    private double lon;

    LocationItem(String key, double lat, double lon) {
        this.key = key;
        this.lat = lat;
        this.lon = lon;
    }

    LocationItem() {
    }

    @NonNull
    @Override
    public String toString() {
        return "LocationItem [key=" + getKey() + ", lat=" + getLat()
                + ", long()=" + getLon() + "]";
    }
}
