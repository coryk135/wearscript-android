package com.dappervision.wearscript.managers;

import android.content.pm.PackageManager;

import com.dappervision.wearscript.BackgroundService;
import com.dappervision.wearscript.HardwareDetector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ManagerManager {
    private static ManagerManager singleton;
    Map<String, Manager> managers;

    private ManagerManager() {
        managers = new ConcurrentHashMap<String, Manager>();
    }

    public static ManagerManager get() {
        if (singleton != null) {
            return singleton;
        }
        singleton = new ManagerManager();
        return singleton;
    }

    public static boolean hasManager(Class<? extends Manager> c) {
        return get().get(c) != null;
    }

    public void newManagers(BackgroundService bs) {
        add(new WifiManager(bs));
        add(new SpeechManager(bs));
        add(new MyoManager(bs));
        add(new ConnectionManager(bs));
        add(new PicarusManager(bs));
        add(new OpenCVManager(bs));
        add(new DataManager(bs));
        add(new AudioManager(bs));

        //Really just FEATURE_CAMERA_ANY should work, but someone is a dumb head and broke Android.
        if(bs.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) || bs.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            add(new CameraManager(bs));
            add(new BarcodeManager(bs));
        }

        if(bs.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            add(new BluetoothManager(bs));
        }

        if (bs.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            add(new BluetoothLEManager(bs));
        }
        if (HardwareDetector.hasGDK) {
            add(new WarpManager(bs));
            add(new LiveCardManager(bs));
            add(new CardTreeManager(bs));
            add(new EyeManager(bs));
        }
    }

    public void add(Manager manager) {
        String name = manager.getClass().getName();
        Manager old = managers.remove(name);
        if (old != null)
            old.shutdown();
        managers.put(name, manager);
    }

    public Manager remove(Class<? extends Manager> manager) {
        String name = manager.getName();
        return managers.remove(name);
    }

    public Manager get(Class<? extends Manager> c) {
        return managers.get(c.getName());
    }

    public void resetAll() {
        for (Manager m : managers.values()) {
            m.reset();
        }
    }

    public void shutdownAll() {
        for (String name : managers.keySet()) {
            Manager m = managers.remove(name);
            m.shutdown();
        }
    }
}
