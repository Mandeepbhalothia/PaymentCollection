package com.sonant.paymentcollection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import com.sonant.paymentcollection.database.DatabaseHelper;
import com.sonant.paymentcollection.services.BluetoothLeService;
import com.sonant.paymentcollection.services.BluetoothLeService2;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class EditWordActivity extends AppCompatActivity implements View.OnClickListener {

    Button sameWordBtn, updateBtn;
    ScrollView newWordSV, oldWordSV;
    public TextView oldWord, newWord, newWordTv;
    boolean sameWordBtnClick = true;
    DatabaseHelper db;
    StringBuilder sb, sb1;
    Vibrator v;
    int p_id;
    int check = 0;
    int check2 = 0;
    long diff2, a1 = 0, b2;
    long diff, a = 0, b;
    String dataCheck = "", dataCheck2 = "";
    boolean letr, sent, num = false;
    boolean word = true;
    Handler mHandler = new Handler();
    int deviceStatus1, deviceStatus2;
    BluetoothDevice device, device2;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    SharedPreferences sharedPreferences;
    private Timer mTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_word);

        sb = new StringBuilder();
        sb1 = new StringBuilder();
        db = new DatabaseHelper(this);
        oldWord = findViewById(R.id.oldWord);
        newWord = findViewById(R.id.newWord);
        newWordTv = findViewById(R.id.newWordTV);
        sameWordBtn = findViewById(R.id.sameWord);
        newWordSV = findViewById(R.id.newWordSV);
        oldWordSV = findViewById(R.id.oldWordSV);
        updateBtn = findViewById(R.id.updateBtn);
        sameWordBtn.setOnClickListener(this);
        updateBtn.setOnClickListener(this);
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        String mDeviceAddress = sharedPreferences.getString("Dev_1", null);
        String mDeviceAddress2 = sharedPreferences.getString("Dev_2", null);

        mTimer = new Timer();

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) return;
        bluetoothAdapter = bluetoothManager.getAdapter();

        device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);
        device2 = bluetoothAdapter.getRemoteDevice(mDeviceAddress2);

    }


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_CONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_CONNECTED");

            } else if (BluetoothLeService.ACTION_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_DISCONNECTED");

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "ACTION_DATA_AVAILABLE");

                updateWord(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_HEART_RATE));
            }
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService2.ACTION_CONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_CONNECTED");

            } else if (BluetoothLeService2.ACTION_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_DISCONNECTED");

            } else if (BluetoothLeService2.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "ACTION_DATA_AVAILABLE");
                updateWord2(intent.getStringExtra(BluetoothLeService2.EXTRA_DATA_HEART_RATE));
            }
        }
    };

    private IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private static IntentFilter makeUpdateIntentFilter2() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService2.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothLeService2.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService2.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateWord(String data) {
        String output;
        boolean t = false;
        if (data != null) {
            int pin = hex2decimal(data);
            pin = pin + 32;
            Log.d(TAG, "data available1: " + pin);
            if (check == 0) {
                a = System.currentTimeMillis();
                if (check == 0) {
                    t = true;
                }
                check++;
            }
            b = System.currentTimeMillis();
            diff = b - a;
            if (dataCheck.equals(data)) {
                Log.d(TAG, "timeSystem" + diff);
                if (diff > 300) {
                    t = true;
                } else
                    t = false;
                Log.i(TAG, "timeDiffer " + System.currentTimeMillis());
            } else {
                t = true;
            }
            a = b;
            Log.i(TAG, "difference1 " + diff);

            if (t) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100, 5));
                } else {
                    v.vibrate(70);
                }
                t = false;
                dataCheck = data;

                if (pin == 58) {
                    //backspace
                    int n = sb.length();
                    if (sb.length() > 0) {
                        if (n > 0)
                            sb.deleteCharAt(n - 1);
                        newWord.setText(sb);
                    }
                } else {
                    if (word) {
                        Cursor cursor = db.getWord(pin);
                        p_id = pin;
                        if (cursor.moveToFirst()) {
                            output = cursor.getString(0);

                            sameWordBtn.setVisibility(View.VISIBLE);
                            if (sameWordBtnClick)
                                oldWord.setText(output);
                        }
                    } else if (letr) {
                        Cursor cursor = db.getAlpha(pin);

                        if (cursor.moveToFirst()) {
                            output = cursor.getString(0);
                            updateBtn.setVisibility(View.VISIBLE);

                            //ed2.setBackgroundColor(Color.parseColor("#1EE4D7"));
                            sb.append(output);
                            newWord.setText(sb);
                        }
                    }
                }
            }
        }
    }

    private void updateWord2(String data2) {
        String output;
        boolean t = false;
        if (data2 != null) {
            int pin = hex2decimal(data2);
            pin = pin + 1;
            Log.d(TAG, "data available1: " + pin);
            if (check2 == 0) {
                a1 = System.currentTimeMillis();
                if (check2 == 0) {
                    t = true;
                }
                check2++;
            }
            b2 = System.currentTimeMillis();
            diff2 = b2 - a1;
            if (dataCheck2.equals(data2)) {
                Log.d(TAG, "timeSystem" + diff2);
                if (diff2 > 300) {
                    t = true;
                } else
                    t = false;
                Log.i(TAG, "timeDiffer " + System.currentTimeMillis());
            } else {
                t = true;
            }
            a1 = b2;
            Log.i(TAG, "difference1 " + diff2);

            if (t) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100, 5));
                } else {
                    v.vibrate(70);
                }
                t = false;
                dataCheck2 = data2;

                if (pin == 26) {
                    sb.delete(0, sb.length());
                    newWord.setText(sb);

                } else if (letr) {
                    if (pin == 15) {
                        letr = false;
                        word = false;
                        startActivity(new Intent(EditWordActivity.this, SettingActivity.class));
                        finish();
                    } else {
                        Cursor cursor = db.getAlpha(pin);

                        if (cursor.moveToFirst()) {
                            output = cursor.getString(0);

                            //ed2.setBackgroundColor(Color.parseColor("#1EE4D7"));
                            updateBtn.setVisibility(View.VISIBLE);
                            sb.append(output);
                            newWord.setText(sb);
                        }
                    }
                } else if (word) {
                    Cursor cursor = db.getWord(pin);
                    p_id = pin;
                    if (cursor.moveToFirst()) {
                        output = cursor.getString(0);
                        sameWordBtn.setVisibility(View.VISIBLE);
                        oldWord.setText(output);
                    }
                }


            }


        }

    }


    public static int hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        return val;
    }

    @Override
    public void onClick(View v) {
        if (v == sameWordBtn) {
            sameWordBtnClick = false;
            sameWordBtn.setVisibility(View.GONE);
            newWord.setVisibility(View.VISIBLE);
            newWordSV.setVisibility(View.VISIBLE);
            newWordTv.setVisibility(View.VISIBLE);
            word = false;
            letr = true;
        } else if (v == updateBtn) {
            String new_word = newWord.getText().toString();
            db.updateWord(p_id, new_word);
            startActivity(new Intent(EditWordActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {

        this.registerReceiver(mGattUpdateReceiver, makeUpdateIntentFilter());
        this.registerReceiver(mGattUpdateReceiver2, makeUpdateIntentFilter2());

        mTimer.cancel();
        mTimer.purge();
        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetVoice(), 0L, 3000);

        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(mGattUpdateReceiver2);

        mTimer.cancel();
        mTimer.purge();
    }
    private class TimerTaskToGetVoice extends TimerTask {

        @Override
        public void run() {

            mHandler.post(() -> {
                deviceStatus1 = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
                deviceStatus2 = bluetoothManager.getConnectionState(device2, BluetoothProfile.GATT);

                if (deviceStatus1 == 0 || deviceStatus2 == 0) {
                    startActivity(new Intent(EditWordActivity.this, ConnectingActivity.class));
                    finish();
                }
            });
        }

    }
}
