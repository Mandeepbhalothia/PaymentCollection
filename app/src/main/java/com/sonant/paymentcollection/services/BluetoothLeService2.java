package com.sonant.paymentcollection.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;

import static com.sonant.paymentcollection.services.GattAttributes.BATTERY_LEVEL_UUID;
import static com.sonant.paymentcollection.services.GattAttributes.BATTERY_SERVICE_UUID;
import static com.sonant.paymentcollection.services.GattAttributes.CLIENT_CHARACTERISTIC_CONFIG_UUID;
import static com.sonant.paymentcollection.services.GattAttributes.HEART_RATE_MEASUREMENT_CHAR_UUID;
import static com.sonant.paymentcollection.services.GattAttributes.HEART_RATE_SERVICE_UUID;


public class BluetoothLeService2 extends Service {

    public final static String ACTION_CONNECTED =
            "ACTION_GATT_CONNECTED2";
    public final static String ACTION_DISCONNECTED =
            "ACTION_GATT_DISCONNECTED2";
    public final static String ACTION_DATA_AVAILABLE =
            "ACTION_DATA_AVAILABLE2";
    public final static String EXTRA_DATA_HEART_RATE =
            "EXTRA_DATA_HEART_RATE2";
    public static final String BATTERY_DATA_AVAILABLE_2 =
            "BATTERY_DATA_AVAILABLE_2";
    private final IBinder mBinder = new LocalBinder();
    private final static String TAG = BluetoothLeService2.class.getSimpleName();
    BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private boolean setBattery;


    public BluetoothLeService2() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService2 getService() {
            return BluetoothLeService2.this;
        }
    }

    public boolean initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
            return true;
        } else {
            Log.e(TAG, "Bluetooth adapter init didn't work");
            return false;
        }
    }

    public void disconnectGattServer() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    public boolean connectToDevice(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "No Bluetooth adapter or no address");
            return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        if (bluetoothManager!=null) {
            if (bluetoothManager.getConnectedDevices(BluetoothGatt.GATT).contains(device)) {
                Log.d(TAG, "connectToDevice: 2 already connected");
                return true;
            }
        }

        mGatt = device.connectGatt(this, true, gattClientCallback);
        Log.d(TAG, "connectToDevice: 2 Connecting to selected device");

        return true;
    }

    private BluetoothGattCallback gattClientCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            String intentAction = "";

            if (status == BluetoothGatt.GATT_FAILURE){
                Log.w(TAG, "onConnectionStateChange: failed" );
                return;
            }

            if (status != BluetoothGatt.GATT_SUCCESS){
                Log.w(TAG, "onConnectionStateChange: not equal to success" );
                return;
            }

            if (newState == BluetoothGatt.STATE_CONNECTED){

                // we can send broadcastUpdate to activity of connection connected
                intentAction =ACTION_CONNECTED;


                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED){
                intentAction = ACTION_DISCONNECTED;

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext());

                String device1 = sharedPreferences.getString("Dev_2",null);


                //disconnect from server

                disconnectGattServer();

                connectToDevice(device1);

            }

            final Intent intent = new Intent(intentAction);

            sendBroadcast(intent);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Device service discovery unsuccessful, status " + status);
                return;
            }
            gatt.getServices();

            setNotification(gatt, true);
//            setBatteryNotification(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i(TAG, "Received  2 onCharacteristicRead: ");

            broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                          int status) {

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG, "Received onDescriptorWrite: ");
            if(CLIENT_CHARACTERISTIC_CONFIG_UUID.equals(descriptor.getUuid())){
                if (status==BluetoothGatt.GATT_SUCCESS){
                    if (setBattery) {
                        setBatteryNotification(gatt);
                    }
                }
            }
        }
    };

    private void setNotification(BluetoothGatt gatt, boolean enabled) {


        BluetoothGattCharacteristic characteristic =gatt.getService(HEART_RATE_SERVICE_UUID)
                .getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID);




        /* Enable notification  on the heart rate measurement characteristic */
        gatt.setCharacteristicNotification(characteristic, enabled);



        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);

        descriptor.setValue(
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        setBattery = true;

        gatt.writeDescriptor(descriptor);


    }

    private void setBatteryNotification(BluetoothGatt gatt){
        // add battery service
        BluetoothGattService batteryService = mGatt.getService(BATTERY_SERVICE_UUID);

        BluetoothGattCharacteristic batteryLevel =
                batteryService.getCharacteristic(BATTERY_LEVEL_UUID);

        gatt.setCharacteristicNotification(batteryLevel, true);

        BluetoothGattDescriptor batteryDescriptor =
                batteryLevel.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);

        batteryDescriptor.setValue(
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        setBattery = false;

        gatt.writeDescriptor(batteryDescriptor);

    }


    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        Intent intent = null;

        /* Following heart rate profile specification */
        if (HEART_RATE_MEASUREMENT_CHAR_UUID.equals(characteristic.getUuid())) {

            intent = new Intent(action);

            int flag = characteristic.getProperties();
            Log.d("Coloe1", String.valueOf((characteristic.getProperties())));
            Log.d("Coloe2", Arrays.toString(characteristic.getValue()));
            Log.d("Coloe3", String.valueOf((HexUtil.formatHexString(characteristic.getValue()))));

            final String heartRate = HexUtil.formatHexString(characteristic.getValue());

            Log.d(TAG, ("Received heart rate:" + heartRate));
            intent.putExtra(EXTRA_DATA_HEART_RATE, (heartRate));
        } else if (BATTERY_LEVEL_UUID.equals(characteristic.getUuid())){
            intent = new Intent(BATTERY_DATA_AVAILABLE_2);
            final int batteryLevel = characteristic.getIntValue
                    (BluetoothGattCharacteristic.FORMAT_UINT8, 0);

            if (batteryLevel != 0) {
                Log.d(TAG, String.format("Received battery level left : %d", batteryLevel));
            }
            intent.putExtra(BATTERY_DATA_AVAILABLE_2,batteryLevel);
        }
        if (intent!=null) {
            sendBroadcast(intent);
        }
    }
}
