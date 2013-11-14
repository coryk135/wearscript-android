package com.dappervision.wearscript;

import android.content.Intent;
import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.List;
import java.util.TreeMap;

public class WearScript {
    BackgroundService bs;
    String TAG = "WearScript";
    TreeMap<String, Integer> sensors;
    String sensorsJS;


    WearScript(BackgroundService bs) {
        this.bs = bs;
        this.sensors = new TreeMap<String, Integer>();
        // Sensor Types
        this.sensors.put("pupil", -2);
        this.sensors.put("gps", -1);
        this.sensors.put("accelerometer", 1);
        this.sensors.put("magneticField", 2);
        this.sensors.put("orientation", 3);
        this.sensors.put("gyroscope", 4);
        this.sensors.put("light", 5);
        this.sensors.put("gravity", 9);
        this.sensors.put("linearAcceleration", 10);
        this.sensors.put("rotationVector", 11);
        this.sensorsJS = (new JSONObject(this.sensors)).toJSONString();
    }

    public int sensor(String name) {
        return this.sensors.get(name);
    }

    public void shutdown() {
        bs.shutdown();
    }

    public String sensors() {
        return this.sensorsJS;
    }

    public void say(String text) {
        Log.i(TAG, "say: " + text);
        bs.say(text);
    }

    public void serverTimeline(String ti) {
        Log.i(TAG, "timeline");
        // TODO: Require WS connection
        bs.serverTimeline(ti);
    }

    public void sensorOn(int type, double sampleTime) {
        Log.i(TAG, "sensorOn: " + Integer.toString(type));
        bs.getDataManager().registerProvider(type, Math.round(sampleTime * 1000000000L));
    }

    public void sensorOn(int type, double sampleTime, String callback) {
        Log.i(TAG, "sensorOn: " + Integer.toString(type) + " callback: " + callback);
        sensorOn(type, sampleTime);
        bs.getDataManager().registerCallback(type, callback);
    }

    public void log(String msg) {
        Log.i(TAG, "log: " + msg);
        bs.log(msg);
    }

    public void sensorOff(int type) {
        Log.i(TAG, "sensorOff: " + Integer.toString(type));
        bs.getDataManager().unregister(type);
    }

    public void serverConnect(String server, String callback) {
        Log.i(TAG, "serverConnect: " + server);
        bs.serverConnect(server, callback);
    }

    public void displayWebView() {
        Log.i(TAG, "displayWebView");
        bs.updateActivityView("webview");
    }

    public void data(int type, String name, String values) {
        Log.i(TAG, "data");
        DataPoint dp = new DataPoint(name, type, System.currentTimeMillis() / 1000., System.nanoTime());
        JSONArray valuesArray = (JSONArray) JSONValue.parse(values);
        for (Object j : valuesArray) {
            try {
                dp.addValue((Double) j);
            } catch (ClassCastException e) {
                dp.addValue(((Long) j).doubleValue());
            }
        }
        bs.handleSensor(dp, null);
    }

    public void cameraOff() {
        bs.dataImage = false;
        // NOTE(brandyn): This resets all callbacks, we should determine if that's the behavior we want
        bs.getCameraManager().unregister(true);
    }

    public void cameraPhoto() {
        this.bs.getCameraManager().cameraPhoto();
    }

    public void cameraVideo() {
        this.bs.getCameraManager().cameraVideo();
    }

    public void cameraOn(double imagePeriod) {
        bs.dataImage = true;
        bs.imagePeriod = imagePeriod * 1000000000L;
        bs.getCameraManager().register();
    }

    public void cameraCallback(int type, String callback) {
        bs.getCameraManager().registerCallback(type, callback);
    }

    public void activityCreate() {
        Intent i = new Intent(bs, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        bs.startActivity(i);
    }

    public void activityDestroy() {
        bs.activity.get().finish();
    }

    public void wifiOff() {
        bs.dataWifi = false;
    }

    public void wifiOn() {
        bs.dataWifi = true;
    }

    public void wifiOn(String callback) {
        bs.wifiScanCallback = callback;
        wifiOn();
    }

    public void wifiScan() {
        bs.wifiStartScan();
    }

    public void dataLog(boolean local, boolean server, double sensorDelay) {
        bs.dataRemote = server;
        bs.dataLocal = local;
        bs.sensorDelay = sensorDelay * 1000000000L;
    }

    public boolean scriptVersion(int version) {
        if (version == 0) {
            return false;
        } else {
            bs.say("Script version incompatible with client");
            return true;
        }
    }

    public void wake() {
        Log.i(TAG, "wake");
        bs.wake();
    }
}
