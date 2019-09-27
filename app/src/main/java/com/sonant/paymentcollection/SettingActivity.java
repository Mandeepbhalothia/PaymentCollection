package com.sonant.paymentcollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import com.sonant.paymentcollection.database.DatabaseHelper;
import com.sonant.paymentcollection.services.BluetoothLeService;
import com.sonant.paymentcollection.services.BluetoothLeService2;

import static android.content.ContentValues.TAG;

public class SettingActivity extends AppCompatActivity {

    DatabaseHelper db;
    StringBuilder sb, sb1;
    Vibrator v;
    String dataCheck = "", dataCheck2 = "";
    int firstTime = 0, firstTime2 = 0;
    long previousMsgTime = 0, currentMsgTime, timeDiff;
    long previousMsgTime2 = 0, currentMsgTime2, timeDiff2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        TextView edit_word = (TextView)findViewById(R.id.edit_word);
        TextView edit_sent = (TextView)findViewById(R.id.editsent);

        sb = new StringBuilder();
        sb1 = new StringBuilder();
        db = new DatabaseHelper(this);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        this.registerReceiver(mGattUpdateReceiver, makeUpdateIntentFilter());
        this.registerReceiver(mGattUpdateReceiver2, makeUpdateIntentFilter2());

        edit_word.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingActivity.this,EditWordActivity.class));
                finish();
            }
        });
        edit_sent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingActivity.this,EditSentActivity.class));
                finish();
            }
        });
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


                setting(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_HEART_RATE));
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
                setting2(intent.getStringExtra(BluetoothLeService2.EXTRA_DATA_HEART_RATE));
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

    private void setting(String data) {
        String output;
        boolean delaySatisfied = false;
        if (data != null) {

            int pin = hex2decimal(data);
            pin = pin + 32;
            Log.d(TAG, "data available1: " + pin);

            if (firstTime == 0) {
                previousMsgTime = System.currentTimeMillis();
                if (firstTime == 0) {
                    delaySatisfied = true;
                }
                firstTime++;
            }
            currentMsgTime = System.currentTimeMillis();
            timeDiff = previousMsgTime - currentMsgTime;

            if(dataCheck.equals(data)){


                if (timeDiff > 300) {
                    delaySatisfied = true;
                } else
                    delaySatisfied = false;
                Log.i(TAG,"timeDiffer "+System.currentTimeMillis());
            }
            else{
                delaySatisfied = true;
            }
            currentMsgTime = previousMsgTime;

            if (delaySatisfied) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100, 5));
                } else {
                    v.vibrate(70);
                }
                delaySatisfied = false;
                dataCheck = data;
                if (pin == 57)
                {
                    startActivity(new Intent(SettingActivity.this,EditSentActivity.class));
                    finish();
                }
            }

        }
    }
    private void setting2(String data2) {
        String output;
        boolean t = false;
        if (data2 != null) {
            int pin = hex2decimal(data2);
            pin = pin + 1;
            Log.d(TAG, "data available1: " + pin);
            if (firstTime2 == 0) {
                previousMsgTime2 = System.currentTimeMillis();
                if (firstTime2 == 0) {
                    t = true;
                }
                firstTime2++;
            }
            currentMsgTime2 = System.currentTimeMillis();
            timeDiff2 = previousMsgTime2 - currentMsgTime2;
            if (dataCheck2.equals(data2)) {
                if (timeDiff2 > 300) {
                    t = true;
                } else
                    t = false;
                Log.i(TAG, "timeDiffer " + System.currentTimeMillis());
            } else {
                t = true;
            }
            currentMsgTime2 = previousMsgTime2;

            if (t) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100, 5));
                } else {
                    v.vibrate(70);
                }
                t = false;
                dataCheck2 = data2;
                if(pin==15)
                {
                    startActivity(new Intent(SettingActivity.this,EditWordActivity.class));
                    finish();
                }
                else if (pin==27)   {
                    startActivity(new Intent(SettingActivity.this,MainActivity.class));
                    finish();
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
            val = 16*val + d;
        }
        return val;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(mGattUpdateReceiver2);
    }
}
