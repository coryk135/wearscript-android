package com.dappervision.wearscript.managers;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.util.Pair;

import com.dappervision.wearscript.BackgroundService;
import com.dappervision.wearscript.Log;
import com.dappervision.wearscript.events.WifiEvent;
import com.dappervision.wearscript.events.WifiScanEvent;
import com.dappervision.wearscript.events.WifiScanResultsEvent;
import com.dappervision.wearscript.events.WifiStrengthEvent;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class WifiManager extends Manager {
    public static final String SCAN_RESULTS_AVAILABLE_ACTION = android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION;
    public static String WIFI = "WIFI";
    public static String WIFI_STRENGTH = "WIFI_STRENGTH";
    android.net.wifi.WifiManager manager;
    private boolean enabled;

    public WifiManager(BackgroundService bs) {
        super(bs);
        reset();
    }

    public String getMacAddress() {
        WifiInfo info = manager.getConnectionInfo();
        return info.getMacAddress();
    }

    public Pair<String,Integer> getWifiStrength() {
        try {
            WifiInfo info = manager.getConnectionInfo();
            int rssi = manager.getConnectionInfo().getRssi();
            int level = android.net.wifi.WifiManager.calculateSignalLevel(rssi, 10);
            int percentage = (int) ((level / 10.0) * 100);
            return new Pair<String, Integer>(info.getSSID(), percentage);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public String getScanResults() {
        Double timestamp = System.currentTimeMillis() / 1000.;
        JSONArray a = new JSONArray();
        for (ScanResult s : manager.getScanResults()) {
            JSONObject r = new JSONObject();
            r.put("timestamp", timestamp);
            r.put("capabilities", new String(s.capabilities));
            r.put("SSID", new String(s.SSID));
            r.put("BSSID", new String(s.BSSID));
            r.put("level", Integer.valueOf(s.level));
            r.put("frequency", Integer.valueOf(s.frequency));
            a.add(r);
        }
        return a.toJSONString();
    }

    public void onEvent(WifiScanEvent e) {
        manager.startScan();
    }

    public void onEvent(WifiStrengthEvent e){
        Pair<String,Integer> result = this.getWifiStrength();

        Log.d(TAG,"here: "+result.first+" "+result.second);
        if (result!=null){
        this.makeCall(WIFI_STRENGTH,
                String.format("%d", result.second));}
        else {
            this.makeCall(WIFI_STRENGTH,
                    String.format("%d", 0));
        }
    }

    public void onEventBackgroundThread(WifiScanResultsEvent e) {
        makeCall(WIFI, getScanResults());
    }

    @Override
    public void reset() {
        super.reset();
        enabled = false;
        manager = (android.net.wifi.WifiManager) service.getSystemService(Context.WIFI_SERVICE);
    }

    public void onEvent(WifiEvent e) {
        enabled = e.getStatus();
    }
}
