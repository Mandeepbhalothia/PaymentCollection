package com.sonant.paymentcollection;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.audiofx.Visualizer;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.sonant.paymentcollection.Payment.DeviceList;
import com.sonant.paymentcollection.Payment.PaymentActivity;
import com.sonant.paymentcollection.adapter.DisplayAdapter;
import com.sonant.paymentcollection.database.DatabaseHelper;
import com.sonant.paymentcollection.pojo.VoiceData;
import com.sonant.paymentcollection.services.BluetoothLeService;
import com.sonant.paymentcollection.services.BluetoothLeService2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import eo.view.batterymeter.BatteryMeterView;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener
        , RecognitionListener {


    TextView suggestionTv;
    ImageView micOff;
    Vibrator vibrator;
    AudioManager audioManager;
    private Timer mTimer = null;
    DatabaseHelper databaseHelper;
    private TextToSpeech tts;
    StringBuilder speakListSB, sb3;
    SharedPreferences sharedPreferences;
    BluetoothLeService mBluetoothLeService;
    BluetoothLeService2 mBluetoothLeService2;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice device, device2;
    private RecyclerView mMessageRecycler;
    private DisplayAdapter displayAdapter;
    private List<VoiceData> messageList = new ArrayList<>();
    long notify_interval = 3000L;
    private Handler mHandler = new Handler();
    int deviceStatus1, deviceStatus2;
    TextView toastTextView;
    Toast customToast;
    AlertDialog.Builder builder;
    AlertDialog alertDialog;
    boolean alertDialogOnScreen = false;
    private SpeechRecognizer speech = null;
    boolean showMsg = true;
    String right = "right";
    String left = "left";
    boolean word = false, sent = false, num = false, update = false;
    boolean letter = true, speakOn = false;
    long previousMsgTime = 0, currentMsgTime, timeDiff;
    long previousMsgTime2 = 0, currentMsgTime2, timeDiff2;
    String dataCheck = "", dataCheck2 = "", toSpeak = "";
    int firstTime = 0, firstTime2 = 0;
    boolean addInOldMsg = true, afterSpeaking = false;
    private List<String> speakList = new ArrayList<>();
    VoiceData voiceData = new VoiceData();
    String str, text;
    String[] splitArr;
    private int disableBtnClickedCounter = 0;
    boolean deviceDisabled = false;
    public float intensity = 0;
    TextView inActiveMsg;
    TextClock digitalClock, warnTextClock;
    boolean voiceResult = false;
    TextView dayTv, warnDayTv, batteryTv;
    Button unlockBtn;
    Group groupDefault, warnGroup;
    ForegroundColorSpan fcs;
    BatteryMeterView batteryMeterView;
    private int batteryColor = Color.TRANSPARENT;
    Intent intent = null;
    private boolean deleteAll;
    String cursor = " |";
    SpannableString cursorString = new SpannableString(cursor);
    StringBuilder messageSB;
    ConstraintLayout mainLayout;
    int minPerRight = 0, minPerLeft = 0;
    ImageView leftGlove, rightGlove;
    TextView leftGloveTextView, rightGloveTextView;
    private BluetoothSocket btsocket;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        micOff = findViewById(R.id.imageview);
        suggestionTv = findViewById(R.id.suggestionTV);
        inActiveMsg = findViewById(R.id.inActiveMsg);
        digitalClock = findViewById(R.id.clock);
        warnTextClock = findViewById(R.id.clock2);
        dayTv = findViewById(R.id.day);
        warnDayTv = findViewById(R.id.day2);
        unlockBtn = findViewById(R.id.logo);
        groupDefault = findViewById(R.id.group);
        warnGroup = findViewById(R.id.groupWarn);
        batteryTv = findViewById(R.id.batteryText);
        batteryMeterView = findViewById(R.id.batteryMeter);
        leftGlove = findViewById(R.id.leftGlove);
        rightGlove = findViewById(R.id.rightGlove);
        leftGloveTextView = findViewById(R.id.leftGloveText);
        rightGloveTextView = findViewById(R.id.rightGloveText);


        unlockBtn.setOnTouchListener(onTouchListener);


        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);// for vibration in device

        String day = new SimpleDateFormat("E", Locale.ENGLISH).format(new Date());
        day = day.toUpperCase();
        String date = new SimpleDateFormat("dd", Locale.ENGLISH).format(new Date());
        SpannableStringBuilder string = new SpannableStringBuilder(day + " " + date);
        fcs = new ForegroundColorSpan(Color.RED);
        string.setSpan(fcs, 0, day.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        dayTv.setText(string);
        warnDayTv.setText(string);


        digitalClock.addTextChangedListener(textWatcher);
        warnTextClock.addTextChangedListener(textWatcher);


        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) return;
        bluetoothAdapter = bluetoothManager.getAdapter();


        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

        // get audio manager
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        mTimer = new Timer();

        // full screen display
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // initialise database helper class
        databaseHelper = new DatabaseHelper(this);
        tts = new TextToSpeech(this, this);


        // initialise string builders
        messageSB = new StringBuilder();
        speakListSB = new StringBuilder();
        sb3 = new StringBuilder();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        String mDeviceAddress = sharedPreferences.getString("Dev_1", null);
        String mDeviceAddress2 = sharedPreferences.getString("Dev_2", null);

        // get bluetooth devices
        device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);
        device2 = bluetoothAdapter.getRemoteDevice(mDeviceAddress2);


        // insert data in device
        insertToSQLite();

        // setup recyclerView

        mMessageRecycler = findViewById(R.id.reyclerview_message_list);
        displayAdapter = new DisplayAdapter(this, messageList);
//        mMessageRecycler.addItemDecoration(new DividerItemDecoration(this));

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mMessageRecycler.setLayoutManager(mLayoutManager);
//        mMessageRecycler.setItemAnimator(new DefaultItemAnimator());
        //set adapter to recyclerView
        mMessageRecycler.setAdapter(displayAdapter);

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout,
                findViewById(R.id.toast_layout_root));

        toastTextView = layout.findViewById(R.id.text);
        toastTextView.setTextSize(100);

        customToast = new Toast(getApplicationContext());
        customToast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        customToast.setDuration(Toast.LENGTH_SHORT);
        customToast.setView(layout);
        builder = new AlertDialog.Builder(this);
        alertDialogOnScreen = true;
        builder.setMessage("Touch Anywhere or Any Point to Enjoy VOIS 1.0");
        alertDialog = builder.create();
        alertDialog.show();
        TextView textView = alertDialog.findViewById(android.R.id.message);
        assert textView != null;
        textView.setTextSize(25);
        textView.setTextColor(Color.parseColor("#FF3B30"));

        createVisualizer();

        mainLayout = findViewById(R.id.constraintLayout);

        btsocket = DeviceList.getSocket();
        if (btsocket == null) {
            Intent BTIntent = new Intent(getApplicationContext(), DeviceList.class);
            this.startActivityForResult(BTIntent, DeviceList.REQUEST_CONNECT_BT);
        }

    }


    //  set system battery info when device is in inactive mode
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            batteryTv.setText(level + "%");
            batteryMeterView.setChargeLevel(level);
            batteryMeterView.setCharging(isCharging);
            if (level > 15 && level <= 25) {
                batteryColor = Color.YELLOW;
            } else if (level > 25 && level <= 50) {
                batteryColor = Color.parseColor("#808000");
            } else if (level > 50 && level <= 75) {
                batteryColor = Color.parseColor("#0277bd");
            } else if (level > 75) {
                batteryColor = Color.parseColor("#4caf50");
            }
            batteryMeterView.setColor(batteryColor);
        }
    };

    View.OnTouchListener onTouchListener = new View.OnTouchListener() {

        float initX;
        float initY, buttonY;
        boolean swipe = true;

        @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initX = event.getX();
                    initY = event.getY();
                    buttonY = unlockBtn.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (swipe) {
                        if (buttonY - unlockBtn.getY() > 50) {
                            Log.d(TAG, "onTouch: swiped");
                            unlockBtn.setText("Swiped");
                            swipe = false;
                            deviceDisabled = !deviceDisabled;
                            disableDevice(deviceDisabled);
                            break;
                        } else {
                            unlockBtn.setY(unlockBtn.getY() + event.getY());
                            unlockBtn.setText("Swiping");
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (buttonY - unlockBtn.getY() < 50) {
                        unlockBtn.setY(buttonY);
                        unlockBtn.setText("Swipe Again");
                        swipe = true;
                    }

                    break;
            }
            return true;
        }
    };

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (editable.length() > 5) {
                Log.d(TAG, "afterTextChanged: " + editable);
                editable.setSpan(new RelativeSizeSpan(0.5f), editable.length() - 2, editable.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                editable.setSpan(fcs, editable.length() - 2, editable.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
    };


    public void insertToSQLite() {
        int rows = databaseHelper.getcount("project_table");

        if (rows < 6) {
            //left
            databaseHelper.insertData("8", "H", " GOOD MORNING ", " How much does this cost");
            databaseHelper.insertData("7", "G", " HELLO ", " Medium ");
            databaseHelper.insertData("6", "F", " THANKYOU ", " I want one chocolate and one Papaya shake");
            databaseHelper.insertData("9", "I", " FINE ", " WHAT WOULD YOU LIKE ");
            databaseHelper.insertData("0", "J", " BYE ", " Okay, I will get it.");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("3", "C", " BYE ", " Okay, I will get it.");
            databaseHelper.insertData("2", "B", " PAPA ", " MY NAME IS JOR SINGH");
            databaseHelper.insertData("1", "A", " MUMMY ", " Hello Abhishek, How are you?"); //12
            databaseHelper.insertData("4", "D", " GOOD NIGHT ", " I AM A SOFTWARE ENGINEER ");
            databaseHelper.insertData("5", "E", " OKAY ", " NICE TO MEET YOU ");
            databaseHelper.insertData("", "", "", "");//word mode 14
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("15", "O", " WELCOME ", " WHERE DO YOU WORK? ");
            databaseHelper.insertData("14", "N", " PLEASE ", " WHERE DO YOU LIVE? ");
            databaseHelper.insertData("11", "K", "NICE", " HELLO EVERYONE");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("12", "L", " ENJOY ", " WHAT IS YOUR PHONE NUMBER? ");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", ""); // 25 DELETE ALL
            databaseHelper.insertData("", "", "", ""); // 26 EDIT MODE
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("13", "M", " FINE ", " WHAT WOULD YOU LIKE "); //29
            databaseHelper.insertData("", "", "", "");

//Right
            databaseHelper.insertData("50", "R", "LOVE", "    मैं   आपका मंथली  यूजर चार्ज लेने आया हूँ "); //(32)
            databaseHelper.insertData("25", "Y", " BAD ", " ये लीजिये");
            databaseHelper.insertData("29", "X", "FINE", " एक मिनट वेट कीजिये मैं आपको  नगर परिषद्  की तरफ से रसीद प्रिंट करके देता हूँ ");
            databaseHelper.insertData("36", "U", " EXCUSE ME ", " आपको उनसठ रूपये देने हैं  ");
            databaseHelper.insertData("26", "V", " HEY ", " ओके  ");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("38", "W", "VOIS", " VOIS IS VERY GOOD ");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("29", "Z", "FINE", " WHAT WOULD YOU LIKE "); //13
            databaseHelper.insertData("", "", "", ""); //MIC ON/OFF 14
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("55", "Q", "NICE", " मैं वीवॉयस लैब्स से आया हूँ ");
            databaseHelper.insertData("23", "P", " BEAUTIFUL ", " नमस्ते मेरा नाम मक़सूद है  ");
            databaseHelper.insertData("34", "S", " OK FINE ", " I AM DEAF ");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("35", "T", " GREAT ", " मैं देखकर बताता हूँ  "); //23
            databaseHelper.insertData("", "", "", "");
            databaseHelper.insertData("", "", "", ""); //SENTENCE MODE
            databaseHelper.insertData("", "", "", ""); //BACK SPACE
            databaseHelper.insertData("", "", "", ""); // SPEAK
            databaseHelper.insertData(" ", " ", " ", " "); // SPACE  28(29+31 = 60)
            databaseHelper.insertData("", "", "", ""); // SPACE  28(29+31 = 60)

////60 deleteAll,61 setting,32 speak,62 space

            Log.d(TAG, "Data inserted");

        }
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();


            if (!mBluetoothLeService.initBluetooth()) {
                Log.e(TAG, "Failure to start bluetooth");
            }


//            mBluetoothLeService.connectToDevice(mDeviceAddress);


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
                Log.e(TAG, "Failure to start bluetooth");
            }
//            mBluetoothLeService2.connectToDevice(mDeviceAddress2);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("back22", "Failure to start bluetooth");
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_CONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_CONNECTED");

            } else if (BluetoothLeService.ACTION_DISCONNECTED.equals(action)) {
                Log.d(TAG, "ACTION_DISCONNECTED");


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "ACTION_DATA_AVAILABLE_RIGHT");
                displayHeartRateData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_HEART_RATE));
            } else if (BluetoothLeService.BATTERY_DATA_AVAILABLE.equals(action)){
                int batteryLevel = intent.getIntExtra(BluetoothLeService.BATTERY_DATA_AVAILABLE,15);
                Log.d(TAG, " Received right battery onReceive: "+batteryLevel);

                int batteryPercent;

                if (minPerRight!=0) {
                    if (batteryLevel > minPerRight) {
                        batteryPercent = minPerRight;
                        minPerRight = batteryLevel;
                    } else {
                        batteryPercent = batteryLevel;
                        minPerRight = batteryLevel;
                    }
                } else {
                    batteryPercent = batteryLevel;
                    minPerRight = batteryLevel;
                }




                gloveBatteryLevel("right",batteryPercent);
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
                Log.d(TAG, "ACTION_DATA_AVAILABLE_LEFT");
                displayHeartRateData2(intent.getStringExtra(BluetoothLeService2.EXTRA_DATA_HEART_RATE));
            } else if (BluetoothLeService2.BATTERY_DATA_AVAILABLE_2.equals(action)){
                int batteryLevel = intent.getIntExtra(BluetoothLeService2.BATTERY_DATA_AVAILABLE_2,15);
                Log.d(TAG, " Received left battery onReceive: "+batteryLevel);

                int batteryPercent;

                if (minPerLeft!=0) {
                    if (batteryLevel > minPerLeft) {
                        batteryPercent = minPerLeft;
                        minPerLeft = batteryLevel;
                    } else {
                        batteryPercent = batteryLevel;
                        minPerLeft = batteryLevel;
                    }
                } else {
                    batteryPercent = batteryLevel;
                    minPerLeft = batteryLevel;
                }

                gloveBatteryLevel("left",batteryPercent);
            }
        }
    };

    private void gloveBatteryLevel(String glove, int batteryPercent) {
        String percent = ""+batteryPercent+"%";

        if (glove.contains("right")){
            rightGloveTextView.setText(percent);
            int background = R.drawable.right100;
            if (batteryPercent<=15){
                background = R.drawable.right15;
            } else if (batteryPercent<=25){
                background = R.drawable.right25;
            } else if (batteryPercent<=50){
                background = R.drawable.right50;
            } else if (batteryPercent<=75){
                background = R.drawable.right75;
            }
            rightGlove.setBackgroundResource(background);
        } else {
            leftGloveTextView.setText(percent);
            int background = R.drawable.left100;
            if (batteryPercent<=15){
                background = R.drawable.left15;
            } else if (batteryPercent<=25){
                background = R.drawable.left25;
            } else if (batteryPercent<=50){
                background = R.drawable.left50;
            } else if (batteryPercent<=75){
                background = R.drawable.left75;
            }
            leftGlove.setBackgroundResource(background);
        }
    }

    @SuppressLint("SetTextI18n")
    private void displayHeartRateData(String data) {

        // right hand data will receive here

        String output;
        boolean delaySatisfied; // check for multiple input
        if (data != null) {

            int pin = hex2decimal(data);
            pin = pin + 32;
            Log.d(TAG, "data available1: " + pin);


            if (firstTime == 0) {
                previousMsgTime = System.currentTimeMillis();
                firstTime++;
            }
            currentMsgTime = System.currentTimeMillis();
            timeDiff = currentMsgTime - previousMsgTime;

            if (dataCheck.equals(data)) {  // check previous msg equal to current msg
                delaySatisfied = timeDiff > 300;
            } else {
                delaySatisfied = true;
            }
            previousMsgTime = currentMsgTime;

            if (delaySatisfied) { // if delay of 300ms is satisfied then show output

                dataCheck = data;

                if (pin == 62) {
                    startVibration(120);
                    // we will disable inputs if this point is three time pressed
                    disableBtnClickedCounter++;
                    if (disableBtnClickedCounter >= 3) {
                        deviceDisabled = !deviceDisabled;
                        disableDevice(deviceDisabled);
                    }
                } else {
                    disableBtnClickedCounter = 0;
                }

                if (!deviceDisabled) {
                    startVibration(70);


                    if (alertDialogOnScreen) { // dismiss alert dialog on input if it is displayed
                        alertDialogOnScreen = false;
                        dismissDialog();
                    }


                    if (pin == 46) {
                        //for listening on/off
                        speakOn = !speakOn;
                        Log.i(TAG, "data av" + speakOn);
                        if (speakOn) {
                            micOff.setVisibility(View.VISIBLE);
                            stopListen();
                        } else {
                            micOff.setVisibility(View.INVISIBLE);
                            listen();
                        }
                    } else if (pin == 57) {

                        sent = !(sent);
                        word = false;
                        update = false;
                        num = false;
                        if (sent) {  // sentence mode enable
                            showDialog("Sentence Mode Enabled");
                            mainLayout.setBackgroundResource(R.drawable.sent_back);
//                        swi.setBackgroundColor(Color.parseColor("#00e676"));
                            letter = false;
                        } else {  // letter mode enable
                            letter = true;
                            mainLayout.setBackgroundColor(Color.BLACK);
                            showDialog("Default Mode Enabled");
//                        swi.setBackgroundColor(Color.parseColor("#ff7043"));
                        }
                    } else if (pin == 58) {
                        //backspace
                        int n = messageSB.length();
                        if (messageSB.length() > 0) {
                            if (n > cursorString.length()) {
                                messageSB.delete(n - cursorString.length() - 1, n - cursorString.length());
                            }
                            VoiceData d = new VoiceData(messageSB.toString(), right);
                            if (messageList.size() > 0 && messageList.size() > voiceData.getSize()) {
                                messageList.remove(voiceData.getSize());
                            }
                            voiceData.addsize(messageList.size());
                            messageList.add(d);
                            notifyAdapter();
                            if (sb3.length() > 0)
                                sb3.deleteCharAt(sb3.length() - 1);
                        } else {
                            // get previous message, this will work as undo
                            if (voiceData.getSize() >= 0) {
                                if (messageList.size() > voiceData.getSize()) {
                                    VoiceData previousData = messageList.get(voiceData.getSize());
                                    if (previousData.getType().contains("right")) {
                                        if (previousData.getMSG().length() > 0) {
                                            showDialog("Last message copied");
                                            new Handler().postDelayed(this::dismissDialog, 500);
                                            voiceData.addsize(messageList.size());
                                            messageList.add(previousData);
                                            messageSB.append(previousData.getMSG());
                                            addInOldMsg = true;
                                            notifyAdapter();
                                        }
                                    }

                                }
                            }
                        }

                    } else if (pin == 59) {  // 32 TO 60
                        //speak
                        if (messageSB.length() > 0) {
                            stopListen();
                            addInOldMsg = false;
                            afterSpeaking = true;
                            showMsg = false;
                            // to insert data in local
//                            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat =
//                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                            String millisInString = dateFormat.format(new Date());
//                            databaseHelper.insertData22(messageSB.toString() + "  " + millisInString);
                            suggestionTv.setText("");
                            suggestionTv.setVisibility(View.GONE);
                            int set_volume = 100;
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, set_volume, 0);

                            toSpeak = messageSB.toString();
                            int msgLength = messageSB.length();
                            if (msgLength >= 1) {
                                messageSB.delete(0, messageSB.length());
                                speakList.add(toSpeak);
                                if (!checkHeadsetConnected()) {
                                    Toast.makeText(this, "Speaker Disconnected",
                                            Toast.LENGTH_SHORT).show();
                                    bluetoothAdapter.disable();
                                }
                                speakOut();
                            }
                        }
                    } else if (pin==61){
                        startActivity(new Intent(MainActivity.this, PaymentActivity.class));
                        finish();
                    }else {
                        if (word) {
                            Cursor cursor = databaseHelper.getWord(pin);

                            if (cursor.moveToFirst()) {
                                output = cursor.getString(0);
                                int len = messageSB.length();
                                if (len > cursorString.length())
                                    messageSB.delete(len - cursorString.length(), len);

                                messageSB.append(output).append(cursorString);
                                Log.d(TAG, "word space " + output);
                                VoiceData d = new VoiceData(messageSB.toString(), right);
                                if (addInOldMsg && messageList.size() > voiceData.getSize()) {
                                    if (len > 1 || deleteAll) {
                                        messageList.remove(voiceData.getSize());
                                        if (deleteAll) {
                                            deleteAll = false;
                                        }
                                    }
                                }
                                voiceData.addsize(messageList.size());
                                messageList.add(d);
                                if (afterSpeaking) {
                                    afterSpeaking = false;
                                    addInOldMsg = true;
                                }
                                notifyAdapter();
                            }
                        } else if (sent) {
                            Cursor cursor = databaseHelper.getSentence(pin);

                            if (cursor.moveToFirst()) {
                                output = cursor.getString(0);
                                int len = messageSB.length();
                                if (len > 0 || deleteAll) {
                                    if (len >= cursorString.length())
                                        messageSB.delete(len - cursorString.length(), len);

                                    messageSB.append(output).append(cursorString);
                                    VoiceData d = new VoiceData(messageSB.toString(), right);
                                    if (messageList.size() > voiceData.getSize()) {
                                        messageList.remove(voiceData.getSize());
                                    }
                                    if (deleteAll) {
                                        deleteAll = false;
                                    }
                                    voiceData.addsize(messageList.size());
                                    messageList.add(d);
                                    notifyAdapter();
                                } else {
                                    messageSB.append(output);

                                    // to insert data in local for our history
//                                    @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat =
//                                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                                    String millisInString = dateFormat.format(new Date());
//                                    if (messageSB.length() > 2)
//                                        databaseHelper.insertData22(messageSB.toString() + "  " + millisInString);

//
                                    speakListSB.append(output);
                                    speakList.add(speakListSB.toString());
                                    speakListSB.delete(0, speakListSB.length());
                                    str = messageSB.toString();
                                    splitArr = str.split("\\s+");
                                    Log.d(TAG, "length" + messageSB.length());
                                    Log.d(TAG, "split" + splitArr.length);
                                    VoiceData d = new VoiceData(messageSB.toString(), right);
                                    voiceData.addsize(messageList.size());
                                    messageList.add(d);
                                    notifyAdapter();
                                    text = messageSB.toString();
                                    messageSB.delete(0, messageSB.length());
                                    int set_volume = 100;
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, set_volume, 0);
                                    stopListen();
                                    showMsg = false;
                                    if (!checkHeadsetConnected()) {
                                        Toast.makeText(this, "Speaker Disconnected",
                                                Toast.LENGTH_SHORT).show();
                                        bluetoothAdapter.disable();
                                    }
                                    if (speakList.size() == 1) {
                                        speakOut();
                                    }


                                }
                            }
                        } else if (letter) {
                            Cursor cursor = databaseHelper.getAlpha(pin);

                            if (cursor.moveToFirst()) {
                                output = cursor.getString(0);
                                toastTextView.setText("" + output);
                                customToast.show();
                                Handler handler = new Handler();
                                handler.postDelayed(() -> customToast.cancel(), 200);

                                int msgLength = messageSB.length();

                                if (msgLength > 0)
                                    messageSB.delete(msgLength - cursorString.length(), msgLength);
                                messageSB.append(output).append(cursorString);
                                VoiceData d = new VoiceData(messageSB.toString(), right);
                                if (addInOldMsg && messageList.size() > voiceData.getSize()) {
                                    // check because in fresh condition if 1st msg by speaker that time
                                    // all upper condition will be satisfied
                                    if (msgLength > 0 || deleteAll) {
                                        messageList.remove(voiceData.getSize());
                                        if (deleteAll) {
                                            deleteAll = false;
                                        }
                                    }
                                }
                                voiceData.addsize(messageList.size());
                                messageList.add(d);

                                if (afterSpeaking) {
                                    afterSpeaking = false;
                                    addInOldMsg = true;
                                }
                                notifyAdapter();

                                suggestionTv.setText("");
                                sb3.append(output);
                            }

                        } else if (num) {
                            Cursor cursor = databaseHelper.getDigit(pin);

                            if (cursor.moveToFirst()) {
                                output = cursor.getString(0);
                                Log.d(TAG, "displayHeartRateData: " + output);
                            }

                        }

                    }

                }
            }
        }
    }

    private void startVibration(long millis) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, 5));
        } else {
            vibrator.vibrate(millis);
        }
    }

    @SuppressLint("SetTextI18n")
    private void disableDevice(boolean disable) {
        disableBtnClickedCounter = 0;
        warnGroup.setVisibility(View.GONE);
        if (disable) {
            mainLayout.setBackgroundColor(Color.BLACK);
            mMessageRecycler.setVisibility(View.GONE);
            groupDefault.setVisibility(View.VISIBLE);
        } else {
            if (letter) {
                mainLayout.setBackgroundColor(Color.BLACK);
            } else if (word) {
                mainLayout.setBackgroundResource(R.drawable.word_back);
            } else if (sent) {
                mainLayout.setBackgroundResource(R.drawable.sent_back);
            }
            groupDefault.setVisibility(View.GONE);
            unlockBtn.setText("Swipe up to unlock");
            unlockBtn.setY(151);// set back to its original pos
            mMessageRecycler.setVisibility(View.VISIBLE);
        }

    }

    private void createVisualizer() {
        int rate = Visualizer.getMaxCaptureRate();
        Visualizer audioOutput = new Visualizer(0); // get output audio stream
        audioOutput.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                intensity = ((float) waveform[0] + 128f) / 256;

                if (intensity > 0.1 && checkHeadsetConnected()) {
                    vibrator.vibrate(50);
                } else if (intensity <= 0.1) {
                    vibrator.cancel();
                }
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {

            }
        }, rate, true, false); // waveform not freq data
        Log.d("rate", String.valueOf(Visualizer.getMaxCaptureRate()));
        audioOutput.setEnabled(true);
    }

    @SuppressLint("SetTextI18n")
    private void displayHeartRateData2(String data) {

        // left hand data will receive here
        String output;
        boolean delaySatisfied;
        if (data != null && !deviceDisabled) {
            if (firstTime2 == 0) {

                previousMsgTime2 = TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis());
                firstTime2++;
            }
            currentMsgTime2 = TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis());
            timeDiff2 = currentMsgTime2 - previousMsgTime2;

            int pin = hex2decimal(data);
            pin = pin + 1;
            Log.d(TAG, "data available2: " + pin);

            if (dataCheck2.equals(data)) {

                delaySatisfied = timeDiff2 > 300;
            } else
                delaySatisfied = true;

            previousMsgTime2 = currentMsgTime2;
            if (delaySatisfied) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, 5));
                } else {
                    vibrator.vibrate(75);
                }
                dataCheck2 = data;

                if (alertDialogOnScreen) {
                    alertDialogOnScreen = false;
                    dismissDialog();
                }


                if (pin == 27) { // Open Setting activity
                    word = false;
                    sent = false;
                    letter = false;
                    num = false;
                    startActivity(new Intent(MainActivity.this, SettingActivity.class));
                    finish();
                }


                if (pin == 15) {
                    word = !(word);
                    sent = false;
                    num = false;
                    update = false;
                    if (word) { // enable word mode
                        mainLayout.setBackgroundResource(R.drawable.word_back);
//                        swi.setBackgroundColor(Color.parseColor("#c5cae9"));
                        showDialog("Word Mode Enabled");
                        letter = false;
                    } else { // enable letter mode
                        mainLayout.setBackgroundColor(Color.BLACK);
                        letter = true;
                        showDialog("Default Mode Enabled");
//                        swi.setBackgroundColor(Color.parseColor("#ff7043"));
                    }
                } else if (pin == 26) { // delete all on message text view
                    messageSB.delete(0, messageSB.length());
                    sb3.delete(0, sb3.length());
                    suggestionTv.setText("");
                    suggestionTv.setVisibility(View.GONE);
                    VoiceData d = new VoiceData(messageSB.toString(), right);
                    if (messageList.size() > 0 && messageList.size() > voiceData.getSize()) {
                        messageList.remove(voiceData.getSize());
                    }
                    addInOldMsg = true;
                    deleteAll = true; // to append new message in this
                    voiceData.addsize(messageList.size());
                    messageList.add(d);
                    notifyAdapter();

                } else {
                    if (word) {
                        Cursor cursor = databaseHelper.getWord(pin);

                        if (cursor.moveToFirst()) {
                            output = cursor.getString(0);
                            int len = messageSB.length();
                            if (len > cursorString.length()) {
                                messageSB.delete(len - cursorString.length(), len);
                            }
                            messageSB.append(output).append(cursorString);
                            VoiceData d = new VoiceData(messageSB.toString(), right);
                            if (addInOldMsg && messageList.size() > voiceData.getSize()) {
                                if (len > 0 || deleteAll) {
                                    if (deleteAll) {
                                        deleteAll = false;
                                    }
                                    messageList.remove(voiceData.getSize());
                                }
                            }
                            voiceData.addsize(messageList.size());
                            messageList.add(d);
                            if (afterSpeaking) {
                                afterSpeaking = false;
                                addInOldMsg = true;
                            }
                            notifyAdapter();
                        }
                    } else if (sent) {
                        Cursor cursor = databaseHelper.getSentence(pin);

                        if (cursor.moveToFirst()) {
                            output = cursor.getString(0);
                            int len = messageSB.length(); // this is for checking that message may be append 
                            // in old message as in default mode or in word message
                            if (len > 0 || deleteAll) { // if satisfied that means msg will append

                                if (len >= cursorString.length())
                                    messageSB.delete(len - cursorString.length(), len);

                                messageSB.append(output).append(cursorString);

                                VoiceData d = new VoiceData(messageSB.toString(), right);
                                if (messageList.size() > voiceData.getSize()) {
                                    messageList.remove(voiceData.getSize());
                                }

                                if (deleteAll) {
                                    deleteAll = false;
                                }

                                voiceData.addsize(messageList.size());
                                messageList.add(d);
                                notifyAdapter();
                            } else {
                                messageSB.append(output);

                                speakListSB.append(output);
                                speakList.add(speakListSB.toString());
                                speakListSB.delete(0, speakListSB.length());

                                str = messageSB.toString();
                                splitArr = str.split("\\s+");
                                Log.d(TAG, "length" + messageSB.length());
                                Log.d(TAG, "split" + splitArr.length);
                                VoiceData d = new VoiceData(messageSB.toString(), right);
                                voiceData.addsize(messageList.size());
                                messageList.add(d);
                                notifyAdapter();
                                text = messageSB.toString();
                                messageSB.delete(0, messageSB.length());
                                int set_volume = 100;
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, set_volume, 0);
                                stopListen();
                                showMsg = false;
                                if (!checkHeadsetConnected()) {
                                    Toast.makeText(this, "Speaker Disconnected",
                                            Toast.LENGTH_SHORT).show();
                                    bluetoothAdapter.disable();
                                }
                                if (speakList.size() == 1) {
                                    speakOut();
                                }
                            }
                        }
                    } else if (letter) {
                        Cursor cursor = databaseHelper.getAlpha(pin);

                        if (cursor.moveToFirst()) {
                            output = cursor.getString(0);

                            int msgLength = messageSB.length();
                            if (msgLength > 0)
                                messageSB.delete(msgLength - cursorString.length(), msgLength);

                            messageSB.append(output).append(cursorString);
                            VoiceData d = new VoiceData(messageSB.toString(), right);
                            if (addInOldMsg && messageList.size() > voiceData.getSize()) {
                                if (msgLength > 0 || deleteAll) {
                                    messageList.remove(voiceData.getSize());
                                    if (deleteAll) {
                                        deleteAll = false;
                                    }
                                }
                            }
                            voiceData.addsize(messageList.size());
                            messageList.add(d);
                            if (afterSpeaking) {
                                afterSpeaking = false;
                                addInOldMsg = true;
                            }
                            notifyAdapter();

                            toastTextView.setText("" + output);
                            customToast.show();
                            Handler handler = new Handler();
                            handler.postDelayed(() -> customToast.cancel(), 200);

                            suggestionTv.setText("");
                            sb3.append(output);
                        }

                    } else if (num) {

                        Cursor cursor = databaseHelper.getDigit(pin);

                        if (cursor.moveToFirst()) {
                            output = cursor.getString(0);
                            Log.i(TAG, "displayHeartRateData2: number mode "+output);
                        }

                    }
                }

            }
        }
    }

    private void initialiseSpeak() {
        speakList.add("Hii ");

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        speakOutDummy();
    }

    private void notifyAdapter() {
        if (displayAdapter != null && messageList.size() > 0) {
            displayAdapter.notifyDataSetChanged();
            mMessageRecycler.smoothScrollToPosition(messageList.size() - 1);
        }
    }

    private void speakOutDummy() {

        Bundle params = new Bundle();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

            }

            @Override
            public void onDone(String s) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                Log.d("checking", "data in speakOutDummy");
                if (speakList.size() > 0) {
                    speakList.remove(0);
                }
            }

            @Override
            public void onError(String s) {
            }
        });


        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");

        if (speakList.size() > 0)
            tts.speak(speakList.get(0), TextToSpeech.QUEUE_ADD, params, "Dummy String");

    }

    private void speakOut() {

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0);

        Bundle params = new Bundle();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0);
            }

            @Override
            public void onDone(String s) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                showMsg = true;
                Log.d("checking", "data in speakOut");
                if (speakList.size() > 0) {
                    speakList.remove(0);

                    runOnUiThread(() -> listen());
                    if (speakList.size() > 0) {
                        runOnUiThread(() -> {
                            Log.d("arrayList", "data in arrayList" + speakList.toString() + "size "
                                    + speakList.get(0));
                            stopListen();

                            new Handler().postDelayed(() -> speakOut(), 150);

                        });
                        // speakOut();
                    }
                }
            }

            @Override
            public void onError(String s) {
            }
        });


        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");

        if (speakList.size() > 0)
            tts.speak(speakList.get(0), TextToSpeech.QUEUE_ADD, params, "Dummy String");

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

    private void listen() {
        // start listening
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"en", "hi"});
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra("android.speech.extra.PARTIAL_RESULTS", true);
        intent.putExtra("android.speech.extra.DICTATION_MODE", true);
        speech.startListening(intent);
    }

    private void stopListen() {
        if (speech != null) {
            speech.stopListening();
            speech.cancel();
            speech.destroy();
        }
    }

    @Override
    public void onResume() {
        Log.i("abc", "onResume  ");
        // start speech to text
        listen();
        // bind services and register receivers

        Intent bleServiceIntent = new Intent(this, BluetoothLeService.class);
        this.bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        Intent bleServiceIntent2 = new Intent(this, BluetoothLeService2.class);
        this.bindService(bleServiceIntent2, mServiceConnection2, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver2, makeUpdateIntentFilter2());
        registerReceiver(mGattUpdateReceiver, makeUpdateIntentFilter());
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        mTimer.cancel();
        mTimer.purge();
        mTimer = new Timer();
        mTimer.schedule(new TimerTaskToGetVoice(), 0L, notify_interval);

        new Handler().postDelayed(this::initialiseSpeak, 300);

        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i("abc", "onPause  ");
        super.onPause();

        stopListen();

        // cancel timer of continue checking device is connected or not
        mTimer.cancel();
        mTimer.purge();

        // unregister receivers
        unregisterReceiver(mBatInfoReceiver);
        unregisterReceiver(mGattUpdateReceiver);
        unregisterReceiver(mGattUpdateReceiver2);

        // unbind service
        unbindService(mServiceConnection);
        unbindService(mServiceConnection2);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // dismiss alert dialog
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }

        // unbind textToSpeech service or listener
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }

        // not calling stopListen() for testing otherwise we can replace
        // stop listen service

        if (speech != null) {
            speech.destroy();
        }
    }

    private void dismissDialog() {
        if (alertDialog != null) {
            if (!isFinishing() && alertDialog.isShowing())
                alertDialog.dismiss();
        }
    }

    private void showDialog(String message) {
        if (builder != null) {
            alertDialogOnScreen = true;
            builder.setMessage(message);
            alertDialog = builder.create();
            alertDialog.show();
            TextView textView = alertDialog.findViewById(android.R.id.message);
            assert textView != null;
            textView.setTextSize(25);
            textView.setTextColor(Color.parseColor("#FF3B30"));
        }
    }


    public void onBeginningOfSpeech() {
        Log.i("abc", "onBeginningOfSpeech");

    }


    public void onRmsChanged(float rmsDB) {

    }


    public void onBufferReceived(byte[] buffer) {
    }


    public void onEndOfSpeech() {
    }


    public void onError(int errorCode) {
        // keep going
        stopListen();
        listen();
    }

    public void onEvent(int i, Bundle bundle) {

    }


    public void onReadyForSpeech(Bundle arg0) {
    }

    public void onResults(Bundle results) {

        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        voiceResult = true; // means full result of voice to text is on screen

        stopListen();

        if (matches != null && matches.size() > 0) {
            String msg = matches.get(0);
            if (msg.length() > 1) {
                if (messageList.size() > 0) {
                    VoiceData voiceData = messageList.get(messageList.size() - 1);
                    if (voiceData.getType().contains("left")) {
                        messageList.remove(messageList.size() - 1);
                    }
                }
                VoiceData d = new VoiceData(msg, left);
                messageList.add(d);
                notifyAdapter();
                Log.i(TAG, "onPartialResults onResults: " + msg);

                if (deviceDisabled) {
                    if (msg.contains("mandeep") || msg.contains("abhinav")) {
                        startVibration(1000);
                        groupDefault.setVisibility(View.INVISIBLE);
                        warnGroup.setVisibility(View.VISIBLE);
                    }
                }
            }
        }


        listen();

      /*  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String millisInString = dateFormat.format(new Date());
        if (speechToTextSB.length() > 1)
            databaseHelper.insertData2(speechToTextSB.toString() + "  " + millisInString);
        speechToTextSB.delete(0, speechToTextSB.length());*/


    }

    public void onPartialResults(Bundle bundle) {

        String onPart = Objects.requireNonNull(bundle.getStringArrayList("results_recognition")).
                get(0);

        Log.i("abc", "onPartialResults " + showMsg + " " + onPart);

        if (showMsg) {
            if (!voiceResult && messageList.size() > 0) {
                VoiceData voiceData = messageList.get(messageList.size() - 1);
                if (voiceData.getType().contains("left")) {
                    messageList.remove(messageList.size() - 1);
                }
            }
            if (onPart.length() > 1) {
                VoiceData d = new VoiceData(onPart, left);
                messageList.add(d);
            }
            notifyAdapter();
            voiceResult = false;
        }
    }


    private static IntentFilter makeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.BATTERY_DATA_AVAILABLE);


        return intentFilter;
    }

    private static IntentFilter makeUpdateIntentFilter2() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService2.ACTION_CONNECTED);
        intentFilter.addAction(BluetoothLeService2.ACTION_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService2.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService2.BATTERY_DATA_AVAILABLE_2);
        return intentFilter;
    }

    private class TimerTaskToGetVoice extends TimerTask {

        @Override
        public void run() {

            mHandler.post(() -> {
                deviceStatus1 = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
                deviceStatus2 = bluetoothManager.getConnectionState(device2, BluetoothProfile.GATT);

                if (deviceStatus1 == 0 || deviceStatus2 == 0) {
                    startActivity(new Intent(MainActivity.this, ConnectingActivity.class));
                    finish();
                }
            });
        }

    }

    @Override
    public void onInit(int status) {
        // TextToSpeech.onInitListener() init method
        if (status == TextToSpeech.SUCCESS) {
            Log.i(TAG, "onInit");
            // In real scenario speakOut() should be used here
//            speakOut();
        } else {
            Log.i(TAG, "onInit Initialization Failed!");
        }
    }

    private boolean checkHeadsetConnected() {
        boolean isConnected = false;
        if (bluetoothAdapter != null) {
            isConnected = bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET)
                    == BluetoothHeadset.STATE_CONNECTED;
        }
        return isConnected;
    }
}
