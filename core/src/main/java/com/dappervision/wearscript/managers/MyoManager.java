package com.dappervision.wearscript.managers;

import android.util.Log;

import com.dappervision.wearscript.BackgroundService;
import com.dappervision.wearscript.Utils;
import com.dappervision.wearscript.events.CallbackRegistration;
import com.dappervision.wearscript.events.MyoAccelerometerDataEvent;
import com.dappervision.wearscript.events.MyoGyroDataEvent;
import com.dappervision.wearscript.events.MyoOrientationDataEvent;
import com.dappervision.wearscript.events.MyoPairEvent;
import com.dappervision.wearscript.events.SendEvent;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;

public class MyoManager extends Manager {
    private static final String TAG = "MyoManager";
    public static final String ONMYO = "onMyo";
    public static final String PAIR = "PAIR";
    private DeviceListener mListener;
    private Myo myo;

    public MyoManager(BackgroundService bs) {
        super(bs);
        reset();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            Hub.getInstance().shutdown();
        }catch(NullPointerException e){
            //it was already shutdown
        }
    }

    public void pair() {
        Log.d(TAG, "Called pair");
        // First, we initialize the Hub singleton.
        Hub hub = Hub.getInstance();
        if (!hub.init(this.service)) {
            Log.e(TAG, "Could not init Myo hub");
            return;
        }
        if (mListener == null) {
            setup();
        }
        // Next, register for DeviceListener callbacks.
        // Finally, scan for Myo devices and connect to the first one found.
        hub.addListener(mListener);

        if (hub.getConnectedDevices().isEmpty()) {
            hub.pairWithAnyMyo();
            Log.d(TAG, "Pairing with a Myo");
        }
    }

    public void onEventMainThread(MyoPairEvent e) {
        pair();
    }

    public void setupCallback(CallbackRegistration r) {
        super.setupCallback(r);
        if (r.getEvent().equals(PAIR)) {
            Utils.eventBusPost(new MyoPairEvent());
        }
    }

    public void unpair() {
        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);
        // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
        Hub.getInstance().shutdown();
    }

    public void setup() {
        mListener = new AbstractDeviceListener() {

            @Override
            public void onConnect(Myo myo, long timestamp) {
                MyoManager.this.myo = myo;
                Log.d(TAG, "Myo connected");
            }

            @Override
            public void onPair(Myo myo, long timestamp) {
                super.onPair(myo, timestamp);
                Log.d(TAG, "Myo paired");
                makeCall(PAIR, "");
            }

            @Override
            public void onArmLost(Myo myo, long timestamp) {
                makeCall(ONMYO, "'ARM_LOST'");
            }

            @Override
            public void onArmRecognized(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
                makeCall(ONMYO, "'ARM_RECOGNIZED'");
            }

            @Override
            public void onDisconnect(Myo myo, long timestamp) {
                Log.d(TAG, "Myo disconnected");
            }

            // onOrientationData() is called whenever the Myo device provides its current orientation,
            // which is represented as a quaternion
            @Override
            public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
                Utils.eventBusPost(new MyoOrientationDataEvent(timestamp, rotation));
            }

            @Override
            public void onGyroscopeData(Myo myo, long timestamp, Vector3 rotationRate) {
                Utils.eventBusPost(new MyoGyroDataEvent(timestamp, rotationRate));
            }

            @Override
            public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
                Utils.eventBusPost(new MyoAccelerometerDataEvent(timestamp, accel));
            }

            @Override
            public void onPose(Myo myo, long timestamp, Pose pose) {
                Log.d(TAG, String.format("Pose: %d %s", timestamp, pose.toString()));
                makeCall(ONMYO + pose.toString(), "");
                makeCall(ONMYO, "'" + pose.toString() + "'");
                // TODO(brandyn): Send timestamp
                Utils.eventBusPost(new SendEvent(String.format("gesture:myo:%s:%s", pose.toString(), myo.getMacAddress().replaceAll(":", "")), pose.toString()));
            }
        };
    }
}
