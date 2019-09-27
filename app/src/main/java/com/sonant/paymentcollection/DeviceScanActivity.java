package com.sonant.paymentcollection;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import com.sonant.paymentcollection.services.BluetoothLeService;
import com.sonant.paymentcollection.services.BluetoothLeService2;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    private static final String TAG = DeviceScanActivity.class.getSimpleName();
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    ProgressBar progressBar;
    ListView deviceListView;
    BleArrayAdapter arrayAdapter;
    ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private ScanCallback mScanCallback;
    private BluetoothLeScanner bluetoothLeScanner;
    Handler mHandler;
    Boolean mScanning = false;
    BluetoothLeService bluetoothLeService;
    BluetoothLeService2 bluetoothLeService2;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor1;
    int first_time_count = 0; // for click count to select device
    String previousDeviceSelected = "";
    int dStatus;
    BluetoothDevice device;


    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor1 = sharedPreferences.edit();

        // check bluetooth manager in device
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Toast.makeText(this, "Bluetooth Manger is not supported",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();

        // check ble support or not
        if (!checkBluetoothAdapter(bluetoothAdapter)) {
            Toast.makeText(this, "Can not proceed, Internal ble problem",
                    Toast.LENGTH_SHORT).show();
            return;
        }

//        progressBar = findViewById(R.id.progress_bar);
        progressBar = findViewById(R.id.spin_kit);
        deviceListView = findViewById(R.id.device_list);
        arrayAdapter = new BleArrayAdapter(this, deviceList);
        deviceListView.setAdapter(arrayAdapter);

        // select device to connect by clicking on it
        deviceListView.setOnItemClickListener((adapterView, view, position, l)
                -> onClickToConnect(position));


        // bind services
        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        this.bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        Intent bleServiceIntent2 = new Intent(this, BluetoothLeService2.class);
        this.bindService(bleServiceIntent2, mServiceConnection2, BIND_AUTO_CREATE);

        // check that already devices are added or not

        if (sharedPreferences.contains("changeGlove")) {
            sharedPreferences.edit().clear().apply();
        }

        if (sharedPreferences.contains("Dev_1") & sharedPreferences.contains("Dev_2")) {
            startActivity(new Intent(DeviceScanActivity.this, ConnectingActivity.class));
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // start scan if all setup is done
        new Handler().postDelayed(() -> startScan(), 1000);
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();

            if (!bluetoothLeService.initBluetooth()) {
                Log.w(TAG, "onServiceConnected: initBluetoothIssue");
            }


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private final ServiceConnection mServiceConnection2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService2 = ((BluetoothLeService2.LocalBinder) service).getService();
            if (!bluetoothLeService2.initBluetooth()) {
                Log.e(TAG, "Failure to start bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("back2", "Failure to start bluetooth");
        }
    };

    private void startScan() {
        if (!hasPermissions()) return;

        deviceList.clear();

        mScanCallback = new BleScanCallback();

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        List<ScanFilter> filters = new ArrayList<>();

        /* Only scan for BLE devices */
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        bluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mHandler = new Handler();

        // scan for 5 seconds
        mHandler.postDelayed(this::stopScan, 5000);
        progressBar.setVisibility(View.VISIBLE);
        mScanning = true;
        Log.d(TAG, "Started scanning.");

    }

    private void stopScan() {
        if (mScanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }
        mScanCallback = null;
        mScanning = false;
        mHandler = null;
    }

    private void scanComplete() {
        if (deviceList.isEmpty()) {
            return;
        }
        for (BluetoothDevice device : deviceList) {
            Log.d(TAG, "Found device: " + device.getName() + " " + device.getAddress());
        }
        progressBar.setVisibility(View.INVISIBLE);
    }

    private class BleScanCallback extends ScanCallback {

        BleScanCallback() {
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (!deviceList.contains(result.getDevice())) {
                if (result.getDevice().getName() != null && result.getDevice().getName().contains("VOIS")) {
                    Log.i(TAG, "onScanResult: name " + result.getDevice().getName()
                            + " address " + result.getDevice().getAddress());
                    deviceList.add(result.getDevice());
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    }

    private boolean hasPermissions() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            return false;

        }
        return true;
    }

    private void requestBluetoothEnable() {
        // enable bluetooth if it is off/disabled
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }

    }

    private void onClickToConnect(int position) {
        if (deviceList.isEmpty()) return;

        if (position >= deviceList.size()) {
            Log.w(TAG, "Illegal position.");
            return;
        }

        if (mScanning) {
            mScanning = false;

            bluetoothLeScanner.flushPendingScanResults(mScanCallback);
            bluetoothLeScanner.stopScan(mScanCallback);
        }

        BluetoothDevice selectedDevice = deviceList.get(position);

        String mDeviceAddress = selectedDevice.getAddress();

        device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);

        Toast.makeText(getApplicationContext(), "Selected: "
                + selectedDevice.getName(), Toast.LENGTH_SHORT).show();


        if (first_time_count == 0) {

            String glove_name = selectedDevice.getName();
            String glove_address = selectedDevice.getAddress();
            BluetoothDevice first_device = device;

            if (glove_name.contains("RH")) {

                boolean res = bluetoothLeService.connectToDevice(glove_address);
                Log.i("tag", "first time right hand device " + res+" service "+bluetoothLeService);
                editor1.putString("Dev_1", glove_address);
                editor1.commit();
            } else {
                boolean res = bluetoothLeService2.connectToDevice(glove_address);
                editor1.putString("Dev_2", glove_address);
                editor1.commit();
                Log.i("tag", "first time left hand device " + res+" service "+bluetoothLeService2);
            }
            dStatus = bluetoothManager.getConnectionState(first_device, BluetoothProfile.GATT);
            Log.i("tag", "first time " + dStatus);
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                dStatus = bluetoothManager.getConnectionState(first_device, BluetoothProfile.GATT);
                if (dStatus == 0) {
                    if (glove_name.contains("RH")) {
                        bluetoothLeService.connectToDevice(glove_address);
                        dStatus = bluetoothManager.getConnectionState(first_device, BluetoothProfile.GATT);
                        Log.i("tag", "first time right hand device handler " + dStatus);
                    } else {
                        bluetoothLeService2.connectToDevice(glove_address);
                        dStatus = bluetoothManager.getConnectionState(first_device, BluetoothProfile.GATT);
                        Log.i("tag", "first time left hand device handler " + dStatus);
                    }
                }
            }, 350);
        }


        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!(previousDeviceSelected.equals(selectedDevice.getAddress())))
            if (first_time_count == 1) {
                first_time_count = 0;
                if (selectedDevice.getName().contains("RH")) {
                    bluetoothLeService.connectToDevice(mDeviceAddress);
                    editor1.putString("Dev_1", selectedDevice.getAddress());
                    editor1.commit();
                    Log.i("tag", "second time right hand device service "+bluetoothLeService);
                } else {
                    bluetoothLeService2.connectToDevice(mDeviceAddress);
                    editor1.putString("Dev_2", selectedDevice.getAddress());
                    editor1.commit();
                    Log.i("tag", "second time left hand device service "+bluetoothLeService2);
                }
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    startActivity(new Intent(DeviceScanActivity.this, ConnectingActivity.class));
                    finish();
                    Log.i("tag", "TheNewConnection1 ");
                }, 500);

            }
        first_time_count = 1;
        previousDeviceSelected = selectedDevice.getAddress();


    }

    private boolean checkBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "checkBluetoothAdapter: adapter is null");
            return false;
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "checkBluetoothAdapter: ble not supported");
            return false;
        }
        return true;
    }


    private class BleArrayAdapter extends ArrayAdapter<BluetoothDevice> {
        private Context mContext;
        private List<BluetoothDevice> devices;

        private BleArrayAdapter(@NonNull Context context, ArrayList<BluetoothDevice> list) {
            super(context, 0, list);
            mContext = context;
            devices = list;
        }

        @SuppressLint("SetTextI18n")
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.device_list, parent,
                        false);
            }

            BluetoothDevice currentDevice = devices.get(position);

            TextView deviceAddress = convertView.findViewById(R.id.device_address);
            deviceAddress.setText(currentDevice.getAddress());
            TextView deviceName = convertView.findViewById(R.id.device_name);

            if (currentDevice.getName() != null) {
                deviceName.setText(currentDevice.getName());
            } else {
                deviceName.setText("No name");
            }

            return convertView;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
        deviceList.clear();
        deviceListView.setAdapter(arrayAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        unbindService(mServiceConnection2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_scan_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.scan) {
            startScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
