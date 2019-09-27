package com.sonant.paymentcollection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.sonant.paymentcollection.services.BluetoothLeService;
import com.sonant.paymentcollection.services.BluetoothLeService2;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectingActivity extends AppCompatActivity {


    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeService2 mBluetoothLeService2;
    private String mDeviceAddress;
    private String mDeviceAddress2;
    BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice device,device2;
    Intent bleServiceIntent;
    int dStatus,dStatus2;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    Handler handler;
    private Timer mTimer = null;
    long notify_interval = 2500L;
    private static final String TAG = "ConnectingActivity";
    TextView alertTextView;
    private boolean newActivity = true;
    int timeCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connecting);

        handler = new Handler();
        mTimer = new Timer();

        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager==null)return;
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        editor = sharedPreferences.edit();
        if (sharedPreferences.contains("Dev_1")) {
            mDeviceAddress = sharedPreferences.getString("Dev_1", null);
            mDeviceAddress2 = sharedPreferences.getString("Dev_2", null);

            if (mDeviceAddress==null && mDeviceAddress2 == null) {
                Toast.makeText(this, "Bluetooth devices are not saved", Toast.LENGTH_SHORT).show();
                return;
            }
            device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
            device2 = mBluetoothAdapter.getRemoteDevice(mDeviceAddress2);
        }

        bleServiceIntent = new Intent(this, BluetoothLeService.class);
        this.bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        Intent bleServiceIntent2 = new Intent(this, BluetoothLeService2.class);
        this.bindService(bleServiceIntent2, mServiceConnection2, BIND_AUTO_CREATE);


        alertTextView  = findViewById(R.id.textView2);
        mTimer.schedule(new TimerTaskToGetVoice(), 0L, notify_interval);


        Button changeGlove = findViewById(R.id.changeGlove);
        changeGlove.setOnClickListener(v -> {
            mTimer.cancel();
            mTimer.purge();

            mBluetoothLeService.disconnectGattServer();
            mBluetoothLeService2.disconnectGattServer();

            mBluetoothLeService = null;
            mBluetoothLeService2 = null;

            sharedPreferences.edit().clear().apply();
            editor.putString("changeGlove","Connecting");
            editor.apply();
//            mBluetoothAdapter.disable();
            startActivity(new Intent(ConnectingActivity.this,DeviceScanActivity.class));
            finish();
        });

    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            if (!mBluetoothLeService.initBluetooth()) {
                Log.e(TAG, "Failure to start bluetooth");
                //  finish();
            }

            dStatus = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
            Log.i("tag","TheConnectionStatus "+dStatus);
            if (dStatus==0) {
                mBluetoothLeService.connectToDevice(mDeviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("back", "Failure to start bluetooth");

        }
    };


    private final ServiceConnection mServiceConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService2 = ((BluetoothLeService2.LocalBinder) service).getService();
            if (!mBluetoothLeService2.initBluetooth()) {
                Log.e("tag", "Failure to start bluetooth");
                //finish();
            }
            dStatus2 = bluetoothManager.getConnectionState(device2,BluetoothProfile.GATT);
            if (dStatus2==0){
                mBluetoothLeService2.connectToDevice(mDeviceAddress2);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("back2", "Failure to start bluetooth");
        }
    };


    private class TimerTaskToGetVoice extends TimerTask {

        @Override
        public void run() {

            handler.post(() -> {
                dStatus = bluetoothManager.getConnectionState(device,BluetoothProfile.GATT);
                if(dStatus==0){  // status of right glove (if 0 that means not connected)
//                    Log.i("tag","TheConnection "+mDeviceAddress);
//                    Log.i("tag","TheConnection "+mBluetoothLeService);
                    if (mBluetoothLeService != null){
                        mBluetoothLeService.connectToDevice(mDeviceAddress);
                    }
//                        dStatus = bluetoothManager.getConnectionState(device,BluetoothProfile.GATT);
//                    Log.i("tag","TheConnection "+dStatus);
                }
                if (dStatus ==2) { // after connected right glove connect  left glove
                    dStatus2 = bluetoothManager.getConnectionState(device2, BluetoothProfile.GATT);
                    if (dStatus2 == 0) {
//                        Log.i("tag", "TheConnection2 " + mDeviceAddress2);
//                        Log.i("tag", "TheConnection2 " + mBluetoothLeService2);
                        if (mBluetoothLeService2 != null)
                            mBluetoothLeService2.connectToDevice(mDeviceAddress2);
//                            dStatus2 = bluetoothManager.getConnectionState(device2, BluetoothProfile.GATT);
//                        Log.i("tag", "TheConnection2 " + dStatus2);
                    }
                }

                if (dStatus==2&&dStatus2==2) {
                    if (newActivity) {
                        newActivity = false;
                        mTimer.cancel();
                        mTimer.purge();
                        Log.i("tag", "TheConnection in new ");
                        startActivity(new Intent(ConnectingActivity.this, MainActivity.class));
                        finish();
                    }
                }
                if (timeCount==40) // after 1 min it will show alert for gloves missing
                {
                    findViewById(R.id.spin_kit).setVisibility(View.GONE);
                    alertTextView.setVisibility(View.VISIBLE);
                    timeCount = 121;
                }
                else{
                    timeCount++;
                }

            });

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        unbindService(mServiceConnection2);
    }
}
