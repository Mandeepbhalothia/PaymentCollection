package com.sonant.paymentcollection.Payment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.Group;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sonant.paymentcollection.ConnectingActivity;
import com.sonant.paymentcollection.MainActivity;
import com.sonant.paymentcollection.R;
import com.sonant.paymentcollection.database.DatabaseHelper;
import com.sonant.paymentcollection.services.BluetoothLeService;
import com.sonant.paymentcollection.services.BluetoothLeService2;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class PaymentActivity extends AppCompatActivity {

    DatabaseReference databaseReference;
    ProgressDialog progressDialog;
    ProgressBar progressBar;

    String serialNo = "";
    TextView nametv, mobileNoTv, serialNoTv, totalTv;
    Button collectBtn;
    LinearLayout linearLayout;
    static String[] monthArray = {"january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"};
    static ArrayList<String> monthList = new ArrayList<>(Arrays.asList(monthArray));

    int checkBoxId = 1000;
    int totalAmount = 0;
    String monthlyCharge = "";
    ArrayList<String> checkedMonthList = new ArrayList<>();
    DatabaseReference setCollectedAmountRef;
    private static BluetoothSocket btsocket;
    private static OutputStream outputStream;
    TextInputEditText inputEditText;
    Button nextButton;
    CardView cardView;
    Group group;


    // ble stuff
    Handler mHandler = new Handler();
    int deviceStatus1, deviceStatus2;
    BluetoothDevice device, device2;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    SharedPreferences sharedPreferences;
    private Timer mTimer = null;
    Vibrator v;
    DatabaseHelper db;
    int p_id;
    int check = 0;
    int check2 = 0;
    long diff2, a1 = 0, b2;
    long diff, a = 0, b;
    String dataCheck = "", dataCheck2 = "";
    StringBuilder sb = new StringBuilder();
    private ArrayList<CheckBox> checkBoxList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        nametv = findViewById(R.id.nameTv);
        mobileNoTv = findViewById(R.id.mobileNoTv);
        serialNoTv = findViewById(R.id.serialNoTv);
        totalTv = findViewById(R.id.totalTv);
        collectBtn = findViewById(R.id.collectBtn);
        linearLayout = findViewById(R.id.linearLayout);
        inputEditText = findViewById(R.id.cardNoET);
        nextButton = findViewById(R.id.nextButton);
        cardView = findViewById(R.id.cardView);
        group = findViewById(R.id.group2);
        progressBar = findViewById(R.id.spin_kit);

//        if (getIntent()!=null) {
//            serialNo = getIntent().getStringExtra("serialNo");
//        }

        progressDialog = new ProgressDialog(PaymentActivity.this);
        progressDialog.setTitle("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.create();


        bleStuff();


        databaseReference = FirebaseDatabase.getInstance("https://dtdnavigatortesting.firebaseio.com/").getReference();

//        inputEditText.setText("0062277799");
        nextButton.setOnClickListener(view -> checkCard());


//        getHouseDetails(serialNo);

        collectBtn.setOnClickListener(v -> {


            setDataToDatabase();


            AlertDialog.Builder builder = new AlertDialog.Builder(PaymentActivity.this);
            builder.setMessage("क्या आपने " + totalAmount + " रुपए ले लिए ?");
            builder.setCancelable(true);

            builder.setPositiveButton(
                    "हाँ ",
                    (dialog, id) -> {

                        setDataToDatabase();

                        dialog.dismiss();
                    });
            builder.setNegativeButton("नहीं  ", (dialog, which) -> {
                Toast.makeText(PaymentActivity.this, "Collect Money", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
//            AlertDialog alert = builder.create();
//            alert.show();
        });

    }

    // set data of paid month in database and print bill

    private void setDataToDatabase(){
        SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
        String date = dateFormat1.format(new Date());
        if (setCollectedAmountRef != null) {
            if (checkedMonthList.size() > 0) {
                int counter = 1;
                for (String monthKey : checkedMonthList) {
                    monthKey = monthKey.replaceAll("[^A-Za-z]+", "");
                    setCollectedAmountRef.child(monthKey).child("amount-paid").setValue(monthlyCharge);
                    setCollectedAmountRef.child(monthKey).child("payment-date").setValue(date);
                    if (counter >= checkedMonthList.size()) {
                        printHousebill();

                        Toast.makeText(PaymentActivity.this, "Done", Toast.LENGTH_SHORT).show();
//                                        startActivity(new Intent(PaymentActivity.this, CardScanActivity.class));
//                                        finish();
                    }
                    counter++;
                }
            }
        } else {
            Toast.makeText(PaymentActivity.this, "Data not send", Toast.LENGTH_SHORT).show();
        }
    }

    private void bleStuff() {
        db = new DatabaseHelper(this);
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

                outputRight(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_HEART_RATE));
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
                outputLeft(intent.getStringExtra(BluetoothLeService2.EXTRA_DATA_HEART_RATE));
            }
        }
    };

    private void outputRight(String data) {

        String output;
        boolean t;
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
                        inputEditText.setText(sb.toString());
                        inputEditText.setSelection(sb.length());
                    }
                } else if (pin == 61) {  // 61 is for next
                    if (cardView.getVisibility() == View.VISIBLE) {
                        checkCard();
                    } else {
                        Log.i(TAG, "outputRight: 61 pressed");
                        setDataToDatabase(); // set data of paid month and print bill
                    }
                } else if (pin == 46) {  // 46 is for back
                    startActivity(new Intent(PaymentActivity.this, MainActivity.class));
                    finish();
                } else {
                    Cursor cursor = db.getDigit(pin);
                    if (cursor.moveToFirst()) {
                        output = cursor.getString(0);
                        sb.append(output);
                        inputEditText.setText(sb.toString());
                        inputEditText.setSelection(sb.length());
                    }
                }
            }
        }

    }

    private void outputLeft(String data2) {

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
                    inputEditText.setText(sb);
                    inputEditText.setSelection(sb.length());

                } else {
                    Cursor cursor = db.getDigit(pin);
                    if (cursor.moveToFirst()) {
                        output = cursor.getString(0);
                        if (cardView.getVisibility() == View.VISIBLE) {
                            sb.append(output);
                            inputEditText.setText(sb.toString());
                            inputEditText.setSelection(sb.length());
                        } else {
                            // Now user is typing to select month from list
                            switch (output) {
                                case "0":
                                    if (checkBoxList.size() > 0) {
                                        CheckBox checkBox = checkBoxList.get(0);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    }
                                    break;
                                case "1":
                                    if (checkBoxList.size() > 0) {
                                        CheckBox checkBox = checkBoxList.get(0);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "2":
                                    if (checkBoxList.size() > 1) {
                                        CheckBox checkBox = checkBoxList.get(1);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "3":
                                    if (checkBoxList.size() > 2) {
                                        CheckBox checkBox = checkBoxList.get(2);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "4":
                                    if (checkBoxList.size() > 3) {
                                        CheckBox checkBox = checkBoxList.get(3);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "5":
                                    if (checkBoxList.size() > 4) {
                                        CheckBox checkBox = checkBoxList.get(4);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "6":
                                    if (checkBoxList.size() > 5) {
                                        CheckBox checkBox = checkBoxList.get(5);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "7":
                                    if (checkBoxList.size() > 6) {
                                        CheckBox checkBox = checkBoxList.get(6);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "8":
                                    if (checkBoxList.size() > 7) {
                                        CheckBox checkBox = checkBoxList.get(7);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                                case "9":
                                    if (checkBoxList.size() > 8) {
                                        CheckBox checkBox = checkBoxList.get(8);
                                        checkBox.setChecked(!checkBox.isChecked());
                                    } else {
                                        Toast.makeText(this, "Wrong Pressed", Toast.LENGTH_SHORT).show();
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        }


    }

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
                    startActivity(new Intent(PaymentActivity.this, ConnectingActivity.class));
                    finish();
                }
            });
        }

    }

    private void checkCard() {
        String cardUID = inputEditText.getText().toString().trim();
        if (cardUID.length() != 5) {
            inputEditText.setError("Enter 5 digits");
            inputEditText.requestFocus();
            return;
        }
        showProgressDialog();
        cardView.setVisibility(View.GONE);
        group.setVisibility(View.VISIBLE);
        cardUID = "SIKA"+cardUID;
        getHouseDetails(cardUID);
//        databaseReference.child("CardScanData").child(cardUID).child("SerialNo")
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        if (dataSnapshot.getValue() == null) {
//                            dismissProgressDialog();
//                            inputEditText.setError("Wrong Card Number");
//                            inputEditText.requestFocus();
//                            return;
//                        }
//                        cardView.setVisibility(View.GONE);
//                        group.setVisibility(View.VISIBLE);
//
//                        getHouseDetails(dataSnapshot.getValue().toString());
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                    }
//                });
    }


    private void getHouseDetails(String serialNo) {
        databaseReference.child("CardWardMapping/" + serialNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {
                    Toast.makeText(PaymentActivity.this, "Data not found for this serial no: "
                            + serialNo, Toast.LENGTH_SHORT).show();
                    dismissProgressDialog();
                    return;
                }

                String lineNo = "";
                String ward = "";

                if (dataSnapshot.hasChild("line")) {
                    if (dataSnapshot.child("line").getValue() != null) {
                        lineNo = dataSnapshot.child("line").getValue().toString();
                    }
                }
                if (dataSnapshot.hasChild("ward")) {
                    if (dataSnapshot.child("ward").getValue() != null) {
                        ward = dataSnapshot.child("ward").getValue().toString();
                    }
                }
                if (lineNo.length() > 0 && ward.length() > 0) {
                    databaseReference.child("Houses/" + ward + "/" + lineNo).orderByChild("card-no").equalTo(serialNo)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() == null) {
                                        Toast.makeText(PaymentActivity.this, "Data not found "
                                                + serialNo, Toast.LENGTH_SHORT).show();
                                        dismissProgressDialog();
                                        return;
                                    }
                                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                                        String mobileNo = dataSnapshot1.getKey();
                                        mobileNoTv.setText(mobileNo);
                                        serialNoTv.setText(serialNo);
                                        if (dataSnapshot1.hasChild("name")) {
                                            if (dataSnapshot1.child("name").getValue() != null) {
                                                String name = dataSnapshot1.child("name").getValue().toString();
                                                nametv.setText(name);
                                            }
                                        }
                                        if (dataSnapshot1.hasChild("house-type")) {
                                            if (dataSnapshot1.getValue() != null) {
                                                String houseType = dataSnapshot1.child("house-type").getValue().toString();
                                                databaseReference.child("Defaults/HouseTypes/" + houseType)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                if (dataSnapshot.getValue() == null) {
                                                                    Toast.makeText(PaymentActivity.this, "House type Not Found : "
                                                                            + houseType, Toast.LENGTH_SHORT).show();
                                                                    dismissProgressDialog();
                                                                    return;
                                                                }
                                                                if (dataSnapshot.hasChild("amount")) {
                                                                    if (dataSnapshot.child("amount").getValue() != null) {
                                                                        String amount = dataSnapshot.child("amount").getValue().toString();
                                                                        int year = Calendar.getInstance().get(Calendar.YEAR);
                                                                        DatabaseReference reference = dataSnapshot1.child("payments/" + year).getRef();
                                                                        if (dataSnapshot1.hasChild("payments")) {
                                                                            if (dataSnapshot1.child("payments").getValue() == null) { // not needed if condition
                                                                                addMonths(amount, null, reference, true);
                                                                            } else {
                                                                                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                                                        ArrayList<String> paidMonthList = new ArrayList<>();
                                                                                        long counter = 1;
                                                                                        for (DataSnapshot dataSnapshot2 : dataSnapshot.getChildren()) {
                                                                                            String month = dataSnapshot2.getKey();
                                                                                            paidMonthList.add(month);

                                                                                            if (counter >= dataSnapshot.getChildrenCount()) {
                                                                                                addMonths(amount, paidMonthList, reference, false);
                                                                                            }
                                                                                            counter++;
                                                                                        }

                                                                                    }

                                                                                    @Override
                                                                                    public void onCancelled(DatabaseError databaseError) {

                                                                                    }
                                                                                });
                                                                            }
                                                                        } else {
                                                                            addMonths(amount, null, reference, true);
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {

                                                            }
                                                        });
                                            }
                                        }

                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                } else {
                    Toast.makeText(PaymentActivity.this, "Data not found "
                            + serialNo, Toast.LENGTH_SHORT).show();
                    dismissProgressDialog();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addMonths(String amount, ArrayList<String> paidMonthList, DatabaseReference reference, boolean b) {
        //TODO complete this method. This will show months with checkbox.
        /**
         * @params amount depends on house type
         * boolean show that all month should be added or not.
         * Starting month will be april and ending will be current.
         * monthList contains name of months in which collection has been done
         */
        monthlyCharge = amount;
        setCollectedAmountRef = reference;
        if (linearLayout == null) {
            Toast.makeText(this, "Some UI rendering problem", Toast.LENGTH_SHORT).show();
            return;
        }
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        if (b && currentMonth < monthList.size()) {
            for (int i = 3; i <= currentMonth; i++) {
                CheckBox checkBox = new CheckBox(getApplicationContext());
                checkBox.setText(monthList.get(i) + "\t \t \t \t " + amount);
                checkBox.setTextSize(18f);
                checkBox.setTextColor(Color.BLACK);
                checkBox.setId(checkBoxId);
                checkBox.setOnCheckedChangeListener(checkedChangeListener(checkBox, amount));
                checkBoxId++;
                linearLayout.addView(checkBox);
                checkBoxList.add(checkBox);
            }
        } else {
            if (paidMonthList.size() > 0) {
                int counter = 1;
                for (String month : paidMonthList) {
                    if (monthList.contains(month)) {
                        CheckBox checkBox = new CheckBox(getApplicationContext());
                        checkBox.setText(month + "\t \t \t \t" + amount);
                        checkBox.setTextSize(18f);
                        checkBox.setId(checkBoxId + monthList.indexOf(month));
                        checkBox.setChecked(true);
                        checkBox.setEnabled(false);
                        checkBox.setOnCheckedChangeListener(checkedChangeListener(checkBox, amount));
                        checkBoxId++;
                        linearLayout.addView(checkBox);
                        checkBoxList.add(checkBox);

                        if (counter >= paidMonthList.size()) { // for non paid month till current month
                            for (int i = 3; i <= currentMonth; i++) {
                                if (!(paidMonthList.contains(monthList.get(i)))) {
                                    checkBox = new CheckBox(getApplicationContext());
                                    checkBox.setText(monthList.get(i) + "\t \t \t \t " + amount);
                                    checkBox.setTextSize(18f);
                                    checkBox.setTextColor(Color.BLACK);
                                    checkBox.setId(checkBoxId);
                                    checkBox.setOnCheckedChangeListener(checkedChangeListener(checkBox, amount));
                                    checkBoxId++;
                                    linearLayout.addView(checkBox);
                                    checkBoxList.add(checkBox);
                                }
                            }
                        }
                        counter++;
                    }
                }
            } else {
                Toast.makeText(this, "Error in data fetching", Toast.LENGTH_SHORT).show();
            }
        }

        dismissProgressDialog();
    }

    private CompoundButton.OnCheckedChangeListener checkedChangeListener(CheckBox checkBox, String amount) {
        return (CompoundButton buttonView, boolean isChecked) -> {
            String text = checkBox.getText().toString();
            String[] arr = text.split(" ");
            if (isChecked) {
                if (!checkedMonthList.contains(arr[0])) {
                    checkedMonthList.add(arr[0]);
                }
                totalAmount += Integer.parseInt(amount);
                totalTv.setText("Total : " + totalAmount);
            }
            if (!isChecked) {
                if (checkedMonthList.contains(arr[0])) {
                    checkedMonthList.remove(arr[0]);
                }
                totalAmount -= Integer.parseInt(amount);
                totalTv.setText("Total : " + totalAmount);
            }
        };
    }


    private void printHousebill() {
        btsocket = DeviceList.getSocket();
        if (btsocket == null) {
            Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
            this.startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
        } else {
            Log.i("tag", "printHousebill: " + btsocket);
            OutputStream opstream = null;
            try {
                opstream = btsocket.getOutputStream();
            } catch (IOException e) {
                Log.i("tag", "printHousebill: 1 " + e);
                e.printStackTrace();
            }
            outputStream = opstream;

            //print command
            try {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                outputStream = btsocket.getOutputStream();
                byte[] printformat = new byte[]{0x1B, 0x21, 0x03};
                outputStream.write(printformat);

                if (checkedMonthList.size() > 0) {

                    printCustom("Nagar Parishad, Sikar", 2, 1);
                    printCustom("D2D Waste Collection", 0, 1);

                    Log.i("tag", "printHousebill: 13");

                    String[] dateTime = getDateTime();
                    printText(leftRightAlign(dateTime[0], dateTime[1]));
                    printNewLine();
                    printText(leftRightAlign("Citizen Name:", nametv.getText().toString()));
                    printNewLine();
                    printText(leftRightAlign("House Type:", "General"));
                    printNewLine();
                    printCustom(new String(new char[32]).replace("\0", "-"), 0, 1);
                    printText(leftRightAlign2("Month:", "Charge"));
                    printNewLine();
                    printCustom(new String(new char[32]).replace("\0", "-"), 0, 1);
                    int counter = 1;
                    for (String month : checkedMonthList) {
                        printText(leftRightAlign2(month + ":", monthlyCharge));
                        printNewLine();
                        if (counter >= checkedMonthList.size()) {
                            printCustom(new String(new char[32]).replace("\0", "-"), 0, 1);
                            printText(leftRightAlign2("Total", totalAmount + "/="));
                            printNewLine();
                            printCustom(new String(new char[32]).replace("\0", "-"), 0, 1);
                            printNewLine();
                            printPhotoCenter(R.drawable.slogo);
                            printNewLine();
                            printCustom("Help Line Number : 1234567899", 0, 1);
//                            printCustom("Look forward to serve you again", 0, 1);
//                            printCustom("WeVOIS Labs Pvt. Ltd.", 0, 1);
                            printNewLine();
                            printNewLine();

                            outputStream.flush();

//                            group.setVisibility(View.GONE);
//                            cardView.setVisibility(View.VISIBLE);
                        }
                        counter++;
                    }

                    // bill printed

                    Objects.requireNonNull(inputEditText.getText()).clear();

                    group.setVisibility(View.GONE);
                    linearLayout.removeAllViews();
                    sb.delete(0, sb.length());
                    checkBoxList.clear();
                    checkedMonthList.clear();

                    nametv.setText("");
                    mobileNoTv.setText("");
                    serialNoTv.setText("");
                    totalTv.setText("");
                    totalAmount = 0;
                    monthlyCharge = "0";
                    checkBoxId = 1000;

                    cardView.setVisibility(View.VISIBLE);

                } else {
                    Toast.makeText(this, "First select month to pay", Toast.LENGTH_SHORT).show();
                    outputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("tag", "printHousebill: 12 " + e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            btsocket = DeviceList.getSocket();
            Log.i("tag", "onActivityResult: " + btsocket);
            if (btsocket != null) {
                printHousebill();
//                printText(messageEt.getText().toString());
            }

        } catch (Exception e) {
            Log.i("tag", "onActivityResult: " + e);
            e.printStackTrace();
        }
    }

    private void printCustom(String msg, int size, int align) {
        //Print config "mode"
        byte[] cc = new byte[]{0x1B, 0x21, 0x03};  // 0- normal size text
        //byte[] cc1 = new byte[]{0x1B,0x21,0x00};  // 0- normal size text
        byte[] bb = new byte[]{0x1B, 0x21, 0x08};  // 1- only bold text
        byte[] bb2 = new byte[]{0x1B, 0x21, 0x20}; // 2- bold with medium text
        byte[] bb3 = new byte[]{0x1B, 0x21, 0x10}; // 3- bold with large text
        try {
            switch (size) {
                case 0:
                    outputStream.write(cc);
                    break;
                case 1:
                    outputStream.write(bb);
                    break;
                case 2:
                    outputStream.write(bb2);
                    break;
                case 3:
                    outputStream.write(bb3);
                    break;
            }

            switch (align) {
                case 0:
                    //left align
                    outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                    break;
                case 1:
                    //center align
                    outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                    break;
                case 2:
                    //right align
                    outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                    break;
            }
            outputStream.write(msg.getBytes());
            outputStream.write(PrinterCommands.LF);
            //outputStream.write(cc);
            //printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //print photo
    public void printPhoto(int img) {
        try {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    img);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    public void printPhotoLeft(int img) {
        try {
            Bitmap bmp1 = BitmapFactory.decodeResource(getResources(),
                    img);
            Bitmap bmp = Bitmap.createScaledBitmap(bmp1, 150, 100, false);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    public void printPhotoCenter(int img) {
        try {
            Bitmap bmp1 = BitmapFactory.decodeResource(getResources(),
                    img);
            Bitmap bmp = Bitmap.createScaledBitmap(bmp1, 400, 150, false);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);
                outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    public void printPhotoRight(int img) {
        try {
            Bitmap bmp1 = BitmapFactory.decodeResource(getResources(),
                    img);
            Bitmap bmp = Bitmap.createScaledBitmap(bmp1, 150, 100, false);
            if (bmp != null) {
                byte[] command = Utils.decodeBitmap(bmp);
                outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT);
                printText(command);
            } else {
                Log.e("Print Photo error", "the file isn't exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PrintTools", "the file isn't exists");
        }
    }

    //print unicode
    public void printUnicode() {
        try {
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER);
            printText(Utils.UNICODE_TEXT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //print new line
    private void printNewLine() {
        try {
            outputStream.write(PrinterCommands.FEED_LINE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void resetPrint() {
        try {
            outputStream.write(PrinterCommands.ESC_FONT_COLOR_DEFAULT);
            outputStream.write(PrinterCommands.FS_FONT_ALIGN);
            outputStream.write(PrinterCommands.ESC_ALIGN_LEFT);
            outputStream.write(PrinterCommands.ESC_CANCEL_BOLD);
            outputStream.write(PrinterCommands.LF);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //print text
    private void printText(String msg) {
        try {
            // Print normal text
            outputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //print byte[]
    private void printText(byte[] msg) {
        try {
            // Print normal text
            outputStream.write(msg);
            printNewLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String leftRightAlign(String str1, String str2) {
        String ans = str1 + str2;
        Log.i("tag", "leftRightAlign: " + ans.length());
        if (ans.length() < 31) {
            int n = (31 - (str1.length() + str2.length()));
            if (n >= 0) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    ans = str1 + String.join("", Collections.nCopies(n, "n")) + str2;
//                } else {
//                    ans = str1 + new String(new char[n]).replace("\0", " ") + str2;
//                }
                ans = str1 + new String(new char[n]).replace("\0", " ") + str2;
            }
        }
        return ans;
    }

    private String leftRightAlign2(String str1, String str2) {
        String ans = str1 + str2;
        Log.i("tag", "leftRightAlign: " + ans.length());
        if (ans.length() < 31) {
            int n = (31 - (str1.length() + str2.length()));
            if (n >= 0) {
                ans = str1 + new String(new char[n]).replace("\0", " ") + str2;
                ans = ans.substring(0, 13) + "|" + ans.substring(15);
            }
        }
        return ans;
    }


    private String[] getDateTime() {
        final Calendar c = Calendar.getInstance();
        String dateTime[] = new String[2];
        dateTime[0] = c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.MONTH) + "/" + c.get(Calendar.YEAR);
        dateTime[1] = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
        return dateTime;
    }


    private void showProgressDialog() {
        if (progressBar != null){
            progressBar.setVisibility(View.VISIBLE);
        }
//        if (progressDialog != null) {
//            if (!progressDialog.isShowing()) {
//                progressDialog.show();
//            }
//        }
    }

    private void dismissProgressDialog() {

        if (progressBar != null){
            progressBar.setVisibility(View.INVISIBLE);
        }

//        if (progressDialog != null && !isFinishing()) {
//            if (progressDialog.isShowing()) {
//                progressDialog.dismiss();
//            }
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
//        try {
//            if (btsocket != null) {
//                outputStream.close();
//                btsocket.close();
//                btsocket = null;
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
